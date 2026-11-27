package `in`.ankitsaroj.diable.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb

/**
 * Wallpapers drawn in the app. They need no network, so a theme can always be saved
 * even when a photo host refuses the download. The pictures are original.
 */
object PaintedWallpapers {

    fun render(context: Context, category: String): Bitmap {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels.coerceIn(720, 1440)
        val height = metrics.heightPixels.coerceIn(1280, 2560)
        val colors = WallpaperCatalog.colorsFor(category)
            ?: listOf(androidx.compose.ui.graphics.Color(0xFF1A1A2E), androidx.compose.ui.graphics.Color(0xFF0F3460))
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val top = colors.first().toArgb()
        val bottom = colors.last().toArgb()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
        val seed = category.hashCode()
        repeat(4) { i ->
            val cx = width * (0.2f + ((seed ushr (i * 3)) and 7) / 10f)
            val cy = height * (0.15f + i * 0.22f)
            val radius = width * (0.35f + (i * 0.08f))
            val orb = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(top or 0x55FFFFFF.toInt(), 0x00000000),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawCircle(cx, cy, radius, orb)
        }
        return bitmap
    }
}
