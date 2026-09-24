package `in`.ankitsaroj.diable.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import `in`.ankitsaroj.diable.data.WallpaperCatalog
import androidx.compose.ui.graphics.toArgb

/**
 * Gradient fallbacks keyed as "gradient:<Category>". A web wallpaper is applied as the
 * real system wallpaper, which already shows through the launcher's transparent window,
 * so only gradients are painted here.
 */
object WallpaperCategories {

    fun brushForKey(key: String): Brush? {
        if (!key.startsWith("gradient:")) return null
        val colors = WallpaperCatalog.colorsFor(key.removePrefix("gradient:")) ?: return null
        return Brush.verticalGradient(colors)
    }

    fun colorsForKey(key: String): List<Color>? =
        WallpaperCatalog.colorsFor(key.removePrefix("gradient:"))
}

fun applyWallpaperFromPrefs(context: Context, wallpaperUri: String?) {
    if (wallpaperUri.isNullOrBlank()) return
    val wm = WallpaperManager.getInstance(context)
    try {
        when {
            wallpaperUri.startsWith("gradient:") -> {
                val colors = WallpaperCategories.colorsForKey(wallpaperUri) ?: return
                val bitmap = gradientBitmap(colors.map { it.toArgb() }, 1080, 1920)
                wm.setBitmap(bitmap)
            }
            else -> {
                context.contentResolver.openInputStream(Uri.parse(wallpaperUri))?.use { stream ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream) ?: return
                    wm.setBitmap(bitmap)
                }
            }
        }
    } catch (_: Exception) {
    }
}

private fun gradientBitmap(colors: List<Int>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val drawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        colors.toIntArray(),
    )
    drawable.setBounds(0, 0, width, height)
    drawable.draw(Canvas(bitmap))
    return bitmap
}

@Composable
fun Modifier.wallpaperBackground(wallpaperUri: String?): Modifier {
    val brush = wallpaperUri?.let { WallpaperCategories.brushForKey(it) }
    return if (brush != null) then(Modifier.background(brush)) else this
}
