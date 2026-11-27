package `in`.ankitsaroj.diable.data

import android.graphics.Bitmap
import `in`.ankitsaroj.diable.ui.screens.settings.ThemeSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Saves a ready-made theme, including a wallpaper file, so several looks can live in
 * My Themes and be applied later. A photo is used when Openverse answers; otherwise
 * the theme still saves with a picture drawn in the app.
 */
object ThemeLibrary {

    suspend fun materialize(context: android.content.Context, repos: DiableRepos, preset: ThemePreset): ThemeSnapshot =
        withContext(Dispatchers.IO) {
            val cover = runCatching { repos.wallpapers.search(preset.wallpaperCategory).firstOrNull() }.getOrNull()
            val photo = cover?.let { runCatching { repos.wallpapers.downloadBitmap(it) }.getOrNull() }
            val bitmap = photo ?: PaintedWallpapers.render(context, preset.wallpaperCategory)
            val dir = File(context.filesDir, "theme_wallpapers").apply { mkdirs() }
            val file = File(dir, "${preset.id}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            if (photo != null && photo !== bitmap) photo.recycle()
            bitmap.recycle()
            ThemeSnapshot(
                fontKey = preset.fontKey,
                fontScale = 1f,
                darkText = false,
                clockStyleId = preset.clockStyleId,
                accentColorArgb = preset.accentArgb,
                iconStyleKey = preset.iconStyle.key,
                iconPackPackage = null,
                hideAlphabet = false,
                hideFavoriteNames = false,
                hideStatusBar = false,
                dimWallpaper = true,
                pitchBlackTheme = false,
                gradientWallpaper = "gradient:${preset.wallpaperCategory}",
                wallpaperFile = file.absolutePath,
                wallpaperCredit = cover?.let { "${it.creator} · ${it.licenseName}" } ?: "Drawn in Diable",
            )
        }
}
