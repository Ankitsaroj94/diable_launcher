package `in`.ankitsaroj.diable.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import `in`.ankitsaroj.diable.data.IconShape
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.IconTreatment
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders a launcher icon in a built-in icon style: cut to the style's shape and
 * recoloured by its treatment, using the theme accent where the treatment asks for it.
 *
 * Everything happens once per (icon, style, accent) into a bitmap, so the list only ever
 * draws finished images — no per-frame colour filters or clipping.
 */
internal object IconRenderer {

    private const val SIZE_PX = 144

    /**
     * One background thread: drawing mutates a Drawable's bounds, and the same app's
     * drawable is often rendered in several styles at once (the icon-style previews).
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dispatcher = kotlinx.coroutines.Dispatchers.Default.limitedParallelism(1)

    // ~80KB per entry; 64 entries is ~5MB at most.
    private val cache = object : LruCache<String, Bitmap>(64) {
        override fun sizeOf(key: String, value: Bitmap) = 1
    }

    /** The cached render, if any, without doing work — safe on the main thread. */
    fun peek(
        packageName: String,
        drawable: Drawable,
        style: IconStyle,
        accentArgb: Int,
    ): Bitmap? = cache.get(key(packageName, drawable, style, accentArgb))

    private fun key(packageName: String, drawable: Drawable, style: IconStyle, accentArgb: Int): String {
        val accentKey = if (style.followsAccent) accentArgb else 0
        return "$packageName#${style.key}#$accentKey#${System.identityHashCode(drawable)}"
    }

    fun get(
        packageName: String,
        drawable: Drawable,
        style: IconStyle,
        accentArgb: Int,
        fromIconPack: Boolean,
    ): Bitmap? {
        // The drawable's identity changes whenever apps reload (an update, a new icon
        // pack), which is exactly when a cached render goes stale.
        val key = key(packageName, drawable, style, accentArgb)
        cache.get(key)?.let { return it }
        val rendered = runCatching { render(drawable, style, accentArgb, fromIconPack) }.getOrNull()
        if (rendered != null) cache.put(key, rendered)
        return rendered
    }

    private fun render(drawable: Drawable, style: IconStyle, accent: Int, fromIconPack: Boolean): Bitmap {
        val size = SIZE_PX
        val adaptive = drawable as? AdaptiveIconDrawable

        // Icon-pack artwork already has the designer's shape; only recolour it.
        if (fromIconPack || (style.shape == IconShape.System && style.treatment == IconTreatment.Original)) {
            val art = drawable.toBitmap(size, size, Bitmap.Config.ARGB_8888)
            return if (style.treatment == IconTreatment.Original) art else recolorWhole(art, style.treatment, accent)
        }

        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val shape = shapePath(style.shape, size.toFloat())

        when (style.treatment) {
            IconTreatment.Original, IconTreatment.Grayscale, IconTreatment.Duotone -> {
                val art = fullBleed(drawable, size)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                paint.colorFilter = when (style.treatment) {
                    IconTreatment.Grayscale -> ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                    IconTreatment.Duotone -> luminanceRamp(shade(accent, 0.28f), tint(accent, 0.55f))
                    else -> null
                }
                drawClipped(canvas, shape, art, paint)
            }

            IconTreatment.AccentGlyph -> {
                val glyph = glyph(drawable, size)
                if (glyph != null) {
                    // A true silhouette: flat accent, no backdrop, like Diable's themed icons.
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        colorFilter = PorterDuffColorFilter(accent, PorterDuff.Mode.SRC_IN)
                    }
                    canvas.drawBitmap(glyph, 0f, 0f, paint)
                } else {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        colorFilter = luminanceRamp(shade(accent, 0.35f), accent)
                    }
                    drawClipped(canvas, shape, fullBleed(drawable, size), paint)
                }
            }

            IconTreatment.ThemedLight, IconTreatment.ThemedDark -> {
                val light = style.treatment == IconTreatment.ThemedLight
                val backdrop = if (light) tint(accent, 0.35f) else shade(accent, 0.22f)
                val ink = if (light) shade(accent, 0.25f) else tint(accent, 0.15f)
                canvas.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backdrop })

                val glyph = glyph(drawable, size)
                val save = canvas.save()
                canvas.clipPath(shape)
                if (glyph != null) {
                    canvas.drawBitmap(
                        glyph,
                        0f,
                        0f,
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            colorFilter = PorterDuffColorFilter(ink, PorterDuff.Mode.SRC_IN)
                        },
                    )
                } else {
                    // No monochrome layer: re-tone the foreground so its detail survives,
                    // from full ink in the shadows to a softer ink in the highlights.
                    val art = foregroundOnly(drawable, size)
                    val soft = ColorUtils.blendARGB(ink, backdrop, 0.45f)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        colorFilter = if (light) luminanceRamp(ink, soft) else luminanceRamp(soft, ink)
                    }
                    canvas.drawBitmap(art, 0f, 0f, paint)
                }
                canvas.restoreToCount(save)
            }
        }
        return out
    }

    // --- Artwork sources -----------------------------------------------------------------

    /**
     * The icon as a full square with no system mask, so the style's own shape decides the
     * outline. Adaptive icons reserve an 18% bleed per edge; legacy icons are inset on a
     * white plate so they don't get their corners cut off.
     */
    private fun fullBleed(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val adaptive = drawable as? AdaptiveIconDrawable
        if (adaptive != null) {
            val bleed = (size * 0.18f).toInt()
            val bounds = Rect(-bleed, -bleed, size + bleed, size + bleed)
            adaptive.background?.apply { setBounds(bounds); draw(canvas) }
            adaptive.foreground?.apply { setBounds(bounds); draw(canvas) }
        } else {
            canvas.drawColor(android.graphics.Color.WHITE)
            val inset = (size * 0.14f).toInt()
            val art = drawable.toBitmap(size - inset * 2, size - inset * 2, Bitmap.Config.ARGB_8888)
            canvas.drawBitmap(art, inset.toFloat(), inset.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        return bitmap
    }

    /** Just the foreground layer (the logo), or the whole icon for legacy drawables. */
    private fun foregroundOnly(drawable: Drawable, size: Int): Bitmap {
        val adaptive = drawable as? AdaptiveIconDrawable
            ?: return drawable.toBitmap(size, size, Bitmap.Config.ARGB_8888)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val bleed = (size * 0.18f).toInt()
        adaptive.foreground?.apply {
            setBounds(-bleed, -bleed, size + bleed, size + bleed)
            draw(Canvas(bitmap))
        }
        return bitmap
    }

    /** The Android 13+ monochrome layer: a ready-made single-colour glyph. */
    private fun glyph(drawable: Drawable, size: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val mono = (drawable as? AdaptiveIconDrawable)?.monochrome ?: return null
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val bleed = (size * 0.18f).toInt()
        mono.setBounds(-bleed, -bleed, size + bleed, size + bleed)
        mono.draw(Canvas(bitmap))
        return bitmap
    }

    private fun recolorWhole(art: Bitmap, treatment: IconTreatment, accent: Int): Bitmap {
        val filter = when (treatment) {
            IconTreatment.Grayscale -> ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            IconTreatment.ThemedDark, IconTreatment.AccentGlyph -> luminanceRamp(shade(accent, 0.3f), accent)
            else -> luminanceRamp(shade(accent, 0.25f), tint(accent, 0.5f))
        }
        val out = Bitmap.createBitmap(art.width, art.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(art, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = filter })
        return out
    }

    private fun drawClipped(canvas: Canvas, shape: Path, art: Bitmap, paint: Paint) {
        // Mask with the path's alpha rather than clipPath, so the edge is anti-aliased.
        val size = art.width
        val layer = canvas.saveLayer(0f, 0f, size.toFloat(), size.toFloat(), null)
        canvas.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK })
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(art, 0f, 0f, paint)
        paint.xfermode = null
        canvas.restoreToCount(layer)
    }

    // --- Colour helpers ------------------------------------------------------------------

    /** Maps each pixel's luminance onto a straight ramp from [dark] to [light]. */
    private fun luminanceRamp(dark: Int, light: Int): ColorMatrixColorFilter {
        fun ch(c: Int, shift: Int) = ((c shr shift) and 0xFF).toFloat()
        val d = floatArrayOf(ch(dark, 16), ch(dark, 8), ch(dark, 0))
        val l = floatArrayOf(ch(light, 16), ch(light, 8), ch(light, 0))
        val row = { i: Int ->
            val span = (l[i] - d[i]) / 255f
            floatArrayOf(0.299f * span, 0.587f * span, 0.114f * span, 0f, d[i])
        }
        return ColorMatrixColorFilter(
            ColorMatrix(row(0) + row(1) + row(2) + floatArrayOf(0f, 0f, 0f, 1f, 0f)),
        )
    }

    private fun shade(color: Int, amount: Float) =
        ColorUtils.blendARGB(color, android.graphics.Color.BLACK, 1f - amount)

    private fun tint(color: Int, amount: Float) =
        ColorUtils.blendARGB(color, android.graphics.Color.WHITE, amount)

    // --- Shapes --------------------------------------------------------------------------

    fun shapePath(shape: IconShape, size: Float): Path = Path().apply {
        val rect = RectF(0f, 0f, size, size)
        when (shape) {
            IconShape.System, IconShape.Squircle -> squircle(size)
            IconShape.Circle -> addOval(rect, Path.Direction.CW)
            IconShape.RoundedSquare -> addRoundRect(rect, size * 0.16f, size * 0.16f, Path.Direction.CW)
            IconShape.Teardrop -> {
                val r = size / 2f
                val small = size * 0.14f
                // Round everywhere except a tight bottom-right corner.
                addRoundRect(
                    rect,
                    floatArrayOf(r, r, r, r, small, small, r, r),
                    Path.Direction.CW,
                )
            }
            IconShape.Hexagon -> {
                val c = size / 2f
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60.0 * i) - 90.0)
                    val x = c + c * cos(angle).toFloat()
                    val y = c + c * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        }
    }

    /** A superellipse (n≈5), the Pixel/One UI squircle. */
    private fun Path.squircle(size: Float) {
        val c = size / 2f
        val n = 5.0
        val steps = 64
        for (i in 0..steps) {
            val t = 2 * Math.PI * i / steps
            val ct = cos(t)
            val st = sin(t)
            val x = c + c * Math.signum(ct) * Math.pow(Math.abs(ct), 2 / n)
            val y = c + c * Math.signum(st) * Math.pow(Math.abs(st), 2 / n)
            if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
        }
        close()
    }
}
