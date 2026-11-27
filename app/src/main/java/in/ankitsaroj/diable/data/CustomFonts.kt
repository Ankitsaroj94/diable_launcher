package `in`.ankitsaroj.diable.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import `in`.ankitsaroj.diable.ui.theme.ThemeFont
import java.io.File

/** A .ttf or .otf the user added, or one found inside a font app from the Play Store. */
data class AvailableFont(
    val label: String,
    /** Absolute path after the file has been copied into app storage. Null until imported. */
    val path: String?,
    /** Set when the face still lives in another app's assets. */
    val sourcePackage: String? = null,
    val assetPath: String? = null,
)

/**
 * Fonts beyond the ones bundled with Diable. A Play Store font app can be opened from
 * the font sheet; its .ttf/.otf files are copied here, and a font file the user saved
 * can be picked directly. The copy lives in app storage so the face still works if the
 * source app is removed.
 */
object CustomFonts {
    const val KEY = "custom"

    private val extensions = setOf("ttf", "otf", "ttc")
    private val typefaces = HashMap<String, Typeface>()

    fun family(context: Context, fontKey: String, customPath: String?): FontFamily {
        if (fontKey == KEY && !customPath.isNullOrBlank()) {
            val file = File(customPath)
            if (file.isFile) {
                val face = typefaces.getOrPut(customPath) { Typeface.createFromFile(file) }
                return FontFamily(face)
            }
        }
        return ThemeFont.fromKey(fontKey).family
    }

    fun saved(context: Context): List<AvailableFont> =
        fontDir(context).listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in extensions }
            ?.sortedBy { it.name.lowercase() }
            ?.map { AvailableFont(label = it.nameWithoutExtension.replace('_', ' '), path = it.absolutePath) }
            .orEmpty()

    /**
     * Font files shipped inside installed apps whose name looks like a font pack.
     * Scanning every app's assets is slow, so only those packages are opened.
     */
    fun inInstalledApps(context: Context): List<AvailableFont> {
        val pm = context.packageManager
        val apps = runCatching { pm.getInstalledApplications(0) }.getOrDefault(emptyList())
        val found = mutableListOf<AvailableFont>()
        for (app in apps) {
            if (found.size >= 60) break
            val label = runCatching { pm.getApplicationLabel(app).toString() }.getOrDefault(app.packageName)
            if (!looksLikeFontPack(app.packageName, label)) continue
            val assets = runCatching { context.createPackageContext(app.packageName, 0).assets }.getOrNull()
                ?: continue
            collect(assets, "").take(24).forEach { asset ->
                val name = asset.substringAfterLast('/').substringBeforeLast('.')
                found += AvailableFont(
                    label = "$name · $label",
                    path = null,
                    sourcePackage = app.packageName,
                    assetPath = asset,
                )
            }
        }
        return found
    }

    fun import(context: Context, uri: Uri): AvailableFont? {
        val name = displayName(context, uri) ?: "font.ttf"
        if (name.substringAfterLast('.').lowercase() !in extensions) return null
        val dest = File(fontDir(context), safeFileName(name))
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        } ?: return null
        typefaces.remove(dest.absolutePath)
        return AvailableFont(label = dest.nameWithoutExtension.replace('_', ' '), path = dest.absolutePath)
    }

    fun copyFromApp(context: Context, source: AvailableFont): AvailableFont? {
        val pkg = source.sourcePackage ?: return null
        val asset = source.assetPath ?: return null
        val assets = runCatching { context.createPackageContext(pkg, 0).assets }.getOrNull() ?: return null
        val dest = File(fontDir(context), safeFileName(asset.substringAfterLast('/')))
        runCatching {
            assets.open(asset).use { input -> dest.outputStream().use { input.copyTo(it) } }
        }.getOrElse { return null }
        typefaces.remove(dest.absolutePath)
        return AvailableFont(label = dest.nameWithoutExtension.replace('_', ' '), path = dest.absolutePath)
    }

    private fun fontDir(context: Context): File =
        File(context.filesDir, "fonts").apply { mkdirs() }

    private fun looksLikeFontPack(packageName: String, label: String): Boolean {
        val text = "$packageName $label".lowercase()
        return "font" in text || "typeface" in text
    }

    private fun collect(assets: android.content.res.AssetManager, dir: String, depth: Int = 0): List<String> {
        if (depth > 3) return emptyList()
        val names = runCatching { assets.list(dir) }.getOrNull() ?: return emptyList()
        val out = mutableListOf<String>()
        for (name in names) {
            val path = if (dir.isEmpty()) name else "$dir/$name"
            val ext = name.substringAfterLast('.', "")
            if (ext.lowercase() in extensions) out += path
            else if ('.' !in name) out += collect(assets, path, depth + 1)
        }
        return out
    }

    private fun displayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun safeFileName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "font.ttf" }
}
