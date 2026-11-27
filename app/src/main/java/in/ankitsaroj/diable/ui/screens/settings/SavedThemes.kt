package `in`.ankitsaroj.diable.ui.screens.settings

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.content.edit
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.wallpaper.DiableLiveWallpaper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * The look-and-feel slice of [DiablePrefs]: what a theme is. Used by the theme editor's
 * discard-on-back, by My Themes, and by theme sharing/import.
 *
 * The system wallpaper itself can't be read back on modern Android, so only locally
 * painted `gradient:` wallpapers travel with a theme; photo wallpapers stay as they are.
 */
data class ThemeSnapshot(
    val fontKey: String,
    val fontScale: Float,
    val darkText: Boolean,
    val clockStyleId: Int,
    val accentColorArgb: Int,
    val iconStyleKey: String,
    val iconPackPackage: String?,
    val hideAlphabet: Boolean,
    val hideFavoriteNames: Boolean,
    val hideStatusBar: Boolean,
    val dimWallpaper: Boolean,
    val pitchBlackTheme: Boolean,
    val gradientWallpaper: String?,
    /** JPEG saved with the theme, so applying it restores the picture. */
    val wallpaperFile: String? = null,
    val wallpaperCredit: String? = null,
    /** One of the built-in live wallpapers, or null. */
    val liveStyle: String? = null,
) {
    fun applyTo(prefs: DiablePrefs): DiablePrefs = prefs.copy(
        fontKey = fontKey,
        fontScale = fontScale,
        darkText = darkText,
        clockStyleId = clockStyleId,
        accentColorArgb = accentColorArgb,
        iconStyleKey = iconStyleKey,
        iconPackPackage = iconPackPackage,
        hideAlphabet = hideAlphabet,
        hideFavoriteNames = hideFavoriteNames,
        hideStatusBar = hideStatusBar,
        dimWallpaper = dimWallpaper,
        pitchBlackTheme = pitchBlackTheme,
    ).let { p ->
        when {
            liveStyle != null -> p.copy(
                wallpaperUri = "live:$liveStyle",
                wallpaperCredit = "Live wallpaper",
                wallpaperTopLuminance = -1f,
            )
            wallpaperFile != null -> p.copy(
                wallpaperUri = "file:$wallpaperFile",
                wallpaperCredit = wallpaperCredit,
                wallpaperTopLuminance = -1f,
            )
            gradientWallpaper != null -> p.copy(
                wallpaperUri = gradientWallpaper,
                wallpaperCredit = null,
                wallpaperTopLuminance = -1f,
            )
            else -> p
        }
    }

    /** Puts the saved picture or live wallpaper on the system home screen. */
    fun applyWallpaper(context: Context) {
        runCatching {
            when {
                liveStyle != null -> DiableLiveWallpaper.request(context, liveStyle)
                wallpaperFile != null && File(wallpaperFile).isFile -> {
                    val bitmap = BitmapFactory.decodeFile(wallpaperFile) ?: return
                    WallpaperManager.getInstance(context).setBitmap(bitmap)
                    bitmap.recycle()
                }
                gradientWallpaper != null ->
                    `in`.ankitsaroj.diable.ui.theme.applyWallpaperFromPrefs(context, gradientWallpaper)
            }
        }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("fontKey", fontKey)
        .put("fontScale", fontScale.toDouble())
        .put("darkText", darkText)
        .put("clockStyleId", clockStyleId)
        .put("accentColorArgb", accentColorArgb)
        .put("iconStyleKey", iconStyleKey)
        .put("iconPackPackage", iconPackPackage ?: JSONObject.NULL)
        .put("hideAlphabet", hideAlphabet)
        .put("hideFavoriteNames", hideFavoriteNames)
        .put("hideStatusBar", hideStatusBar)
        .put("dimWallpaper", dimWallpaper)
        .put("pitchBlackTheme", pitchBlackTheme)
        .put("gradientWallpaper", gradientWallpaper ?: JSONObject.NULL)
        .put("wallpaperFile", wallpaperFile ?: JSONObject.NULL)
        .put("wallpaperCredit", wallpaperCredit ?: JSONObject.NULL)
        .put("liveStyle", liveStyle ?: JSONObject.NULL)

    companion object {
        fun of(prefs: DiablePrefs) = ThemeSnapshot(
            fontKey = prefs.fontKey,
            fontScale = prefs.fontScale,
            darkText = prefs.darkText,
            clockStyleId = prefs.clockStyleId,
            accentColorArgb = prefs.accentColorArgb,
            iconStyleKey = prefs.iconStyleKey,
            iconPackPackage = prefs.iconPackPackage,
            hideAlphabet = prefs.hideAlphabet,
            hideFavoriteNames = prefs.hideFavoriteNames,
            hideStatusBar = prefs.hideStatusBar,
            dimWallpaper = prefs.dimWallpaper,
            pitchBlackTheme = prefs.pitchBlackTheme,
            gradientWallpaper = prefs.wallpaperUri?.takeIf { it.startsWith("gradient:") },
            wallpaperFile = prefs.wallpaperUri?.removePrefix("file:")?.takeIf { prefs.wallpaperUri.startsWith("file:") },
            wallpaperCredit = prefs.wallpaperCredit,
            liveStyle = prefs.wallpaperUri?.removePrefix("live:")?.takeIf { prefs.wallpaperUri.startsWith("live:") },
        )

        /** Parses theme JSON, using [base] for anything missing. Throws on non-JSON. */
        fun fromJson(o: JSONObject, base: DiablePrefs = DiablePrefs()): ThemeSnapshot {
            val d = of(base)
            fun optStringOrNull(key: String) =
                o.optString(key).takeIf { o.has(key) && !o.isNull(key) && it.isNotBlank() }
            return ThemeSnapshot(
                fontKey = o.optString("fontKey", d.fontKey),
                fontScale = o.optDouble("fontScale", d.fontScale.toDouble()).toFloat().coerceIn(0.8f, 1.4f),
                darkText = o.optBoolean("darkText", d.darkText),
                clockStyleId = o.optInt("clockStyleId", d.clockStyleId),
                accentColorArgb = o.optInt("accentColorArgb", d.accentColorArgb),
                iconStyleKey = o.optString("iconStyleKey", d.iconStyleKey),
                iconPackPackage = optStringOrNull("iconPackPackage"),
                hideAlphabet = o.optBoolean("hideAlphabet", d.hideAlphabet),
                hideFavoriteNames = o.optBoolean("hideFavoriteNames", d.hideFavoriteNames),
                hideStatusBar = o.optBoolean("hideStatusBar", d.hideStatusBar),
                dimWallpaper = o.optBoolean("dimWallpaper", d.dimWallpaper),
                pitchBlackTheme = o.optBoolean("pitchBlackTheme", d.pitchBlackTheme),
                gradientWallpaper = optStringOrNull("gradientWallpaper"),
                wallpaperFile = optStringOrNull("wallpaperFile"),
                wallpaperCredit = optStringOrNull("wallpaperCredit"),
                liveStyle = optStringOrNull("liveStyle"),
            )
        }
    }
}

/** A theme saved in My Themes. */
data class SavedTheme(
    val id: String,
    val name: String,
    val createdAt: Long,
    val snapshot: ThemeSnapshot,
)

/**
 * My Themes storage: a single JSON array in SharedPreferences. It is written only when
 * the user saves or deletes, and nothing observes it as a stream.
 */
object SavedThemes {

    private const val FILE = "saved_themes"
    private const val KEY = "themes"

    /** Share/import envelope, so pasting random JSON is rejected with a clear message. */
    private const val FORMAT = "open-diable-theme"

    fun load(context: Context): List<SavedTheme> = runCatching {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        val a = JSONArray(raw)
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            SavedTheme(
                id = o.getString("id"),
                name = o.optString("name", "Theme"),
                createdAt = o.optLong("createdAt"),
                snapshot = ThemeSnapshot.fromJson(o.getJSONObject("theme")),
            )
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, name: String, snapshot: ThemeSnapshot): List<SavedTheme> {
        val theme = SavedTheme(
            id = System.currentTimeMillis().toString(36),
            name = name.trim().ifBlank { "My theme" },
            createdAt = System.currentTimeMillis(),
            snapshot = snapshot,
        )
        return (load(context) + theme).also { write(context, it) }
    }

    fun delete(context: Context, id: String): List<SavedTheme> =
        load(context).filterNot { it.id == id }.also { write(context, it) }

    private fun write(context: Context, themes: List<SavedTheme>) {
        val a = JSONArray()
        themes.forEach { t ->
            a.put(
                JSONObject()
                    .put("id", t.id)
                    .put("name", t.name)
                    .put("createdAt", t.createdAt)
                    .put("theme", t.snapshot.toJson()),
            )
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putString(KEY, a.toString()) }
    }

    fun shareJson(name: String, snapshot: ThemeSnapshot): String = JSONObject()
        .put("format", FORMAT)
        .put("name", name)
        .put("theme", snapshot.toJson())
        .toString(2)

    /** Returns (name, snapshot) or throws [IllegalArgumentException] with a user-facing message. */
    fun parseShared(text: String, base: DiablePrefs): Pair<String, ThemeSnapshot> {
        val o = runCatching { JSONObject(text.trim()) }.getOrNull()
            ?: throw IllegalArgumentException("The clipboard doesn't contain a theme")
        require(o.optString("format") == FORMAT && o.has("theme")) { "That isn't a launcher theme" }
        return o.optString("name", "Imported theme") to ThemeSnapshot.fromJson(o.getJSONObject("theme"), base)
    }
}
