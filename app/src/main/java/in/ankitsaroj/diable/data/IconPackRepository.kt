package `in`.ankitsaroj.diable.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.Xml
import androidx.core.graphics.drawable.toBitmap
import org.xmlpull.v1.XmlPullParser

/** An installed third-party icon pack. */
data class InstalledIconPack(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

/**
 * Third-party icon packs in the ADW / Nova / Apex format that almost every pack on the
 * Play Store ships: an `appfilter.xml` mapping `ComponentInfo{pkg/activity}` to a drawable
 * name, plus optional `iconback` / `iconmask` / `iconupon` / `scale` rules used to theme
 * apps the pack doesn't cover.
 */
object IconPackRepository {

    private const val TAG = "IconPacks"

    /** Every launcher's "I am an icon pack" action; packs declare one or more of these. */
    private val PACK_ACTIONS = listOf(
        "org.adw.launcher.THEMES",
        "org.adw.launcher.icons.ACTION_PICK_ICON",
        "com.novalauncher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.gau.go.launcherex.theme",
        "com.anddoes.launcher.THEME",
        "com.fede.launcher.THEME_ICONPACK",
        "com.dlto.atom.launcher.THEME",
        "ch.deletescape.lawnchair.ICONPACK",
    )

    fun installed(context: Context): List<InstalledIconPack> {
        val pm = context.packageManager
        val seen = LinkedHashMap<String, InstalledIconPack>()
        for (action in PACK_ACTIONS) {
            val matches = try {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA)
            } catch (_: Exception) {
                emptyList()
            }
            for (info in matches) {
                val pkg = info.activityInfo?.packageName ?: continue
                if (pkg in seen || pkg == context.packageName) continue
                seen[pkg] = InstalledIconPack(
                    packageName = pkg,
                    label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg),
                    icon = runCatching { info.loadIcon(pm) }.getOrNull(),
                )
            }
        }
        return seen.values.sortedBy { it.label.lowercase() }
    }

    /** Parsed packs, so switching back and forth doesn't re-read the XML. */
    private val loaded = HashMap<String, IconPack?>()

    @Synchronized
    fun load(context: Context, packageName: String): IconPack? =
        loaded.getOrPut(packageName) {
            try {
                IconPack.parse(context, packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Could not load icon pack $packageName", e)
                null
            }
        }
}

class IconPack private constructor(
    val packageName: String,
    private val res: Resources,
    private val components: Map<String, String>,
    private val byPackage: Map<String, String>,
    private val backs: List<String>,
    private val mask: String?,
    private val upon: String?,
    private val scale: Float,
) {
    /**
     * The pack's icon for an activity, or — when the pack doesn't cover it but defines a
     * backdrop — the original icon composed onto that backdrop the way the pack intends.
     */
    fun iconFor(packageName: String, className: String, original: Drawable?, sizePx: Int): Drawable? {
        val name = components["$packageName/$className"] ?: byPackage[packageName]
        if (name != null) drawable(name)?.let { return it }
        if (backs.isEmpty() || original == null) return null
        return compose(packageName, original, sizePx)
    }

    val coversUnmappedApps: Boolean get() = backs.isNotEmpty()

    @SuppressLint("DiscouragedApi")
    private fun drawable(name: String): Drawable? {
        val id = res.getIdentifier(name, "drawable", packageName).takeIf { it != 0 }
            ?: res.getIdentifier(name, "mipmap", packageName).takeIf { it != 0 }
            ?: return null
        return runCatching { res.getDrawable(id, null) }.getOrNull()
    }

    private fun compose(appPackage: String, original: Drawable, size: Int): Drawable? {
        // Packs list several backdrops; pick one stably per app so it never flickers.
        val back = drawable(backs[Math.floorMod(appPackage.hashCode(), backs.size)])
            ?: return null
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        back.setBounds(0, 0, size, size)
        back.draw(canvas)

        val inner = (size * scale).toInt().coerceAtLeast(1)
        val app = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val appCanvas = Canvas(app)
        val offset = (size - inner) / 2
        appCanvas.drawBitmap(original.toBitmap(inner, inner), offset.toFloat(), offset.toFloat(), null)
        mask?.let(::drawable)?.let { m ->
            // The mask's opaque pixels punch holes in the app artwork.
            val maskBitmap = m.toBitmap(size, size)
            appCanvas.drawBitmap(
                maskBitmap,
                0f,
                0f,
                Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) },
            )
        }
        canvas.drawBitmap(app, 0f, 0f, null)
        upon?.let(::drawable)?.let { u ->
            u.setBounds(0, 0, size, size)
            u.draw(canvas)
        }
        return BitmapDrawable(res, out)
    }

    companion object {
        @SuppressLint("DiscouragedApi")
        fun parse(context: Context, packageName: String): IconPack? {
            val packContext = context.createPackageContext(packageName, 0)
            val res = packContext.resources
            val parser: XmlPullParser = res.getIdentifier("appfilter", "xml", packageName)
                .takeIf { it != 0 }
                ?.let { res.getXml(it) }
                ?: runCatching {
                    Xml.newPullParser().apply {
                        setInput(packContext.assets.open("appfilter.xml"), "UTF-8")
                    }
                }.getOrNull()
                ?: return null

            val components = HashMap<String, String>()
            val byPackage = HashMap<String, String>()
            val backs = mutableListOf<String>()
            var mask: String? = null
            var upon: String? = null
            var scale = 1f

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item" -> {
                            val component = parser.getAttributeValue(null, "component")
                            val drawable = parser.getAttributeValue(null, "drawable")
                            val key = component?.let(::componentKey)
                            if (key != null && !drawable.isNullOrBlank()) {
                                components.putIfAbsent(key, drawable)
                                byPackage.putIfAbsent(key.substringBefore('/'), drawable)
                            }
                        }
                        "iconback" -> for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeName(i).startsWith("img")) {
                                backs += parser.getAttributeValue(i)
                            }
                        }
                        "iconmask" -> mask = parser.getAttributeValue(null, "img1")
                        "iconupon" -> upon = parser.getAttributeValue(null, "img1")
                        "scale" -> scale = parser.getAttributeValue(null, "factor")
                            ?.toFloatOrNull()?.coerceIn(0.1f, 1f) ?: 1f
                    }
                }
                event = parser.next()
            }
            if (components.isEmpty() && backs.isEmpty()) return null
            return IconPack(packageName, res, components, byPackage, backs, mask, upon, scale)
        }

        /** `ComponentInfo{com.app/com.app.Main}` → `com.app/com.app.Main`, resolving `.Main`. */
        private fun componentKey(raw: String): String? {
            val inner = raw.substringAfter('{', "").substringBefore('}', "")
            val pkg = inner.substringBefore('/', "").trim()
            var cls = inner.substringAfter('/', "").trim()
            if (pkg.isEmpty() || cls.isEmpty()) return null
            if (cls.startsWith('.')) cls = pkg + cls
            return "$pkg/$cls"
        }
    }
}
