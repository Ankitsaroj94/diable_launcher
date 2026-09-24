package `in`.ankitsaroj.diable.ui.screens.settings

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sets a photo the user picked from their gallery as the system wallpaper. Returns the
 * brightness of its top strip (for status-bar icon colour), or null on failure.
 */
suspend fun applyGalleryWallpaper(context: Context, uri: Uri): Float? = withContext(Dispatchers.IO) {
    runCatching {
        val metrics = context.resources.displayMetrics
        val bitmap = decodeSampled(context, uri, metrics.widthPixels, metrics.heightPixels)
            ?: return@runCatching null
        val cropped = bitmap.cropTo(metrics.widthPixels.toFloat() / metrics.heightPixels)
        WallpaperManager.getInstance(context).setBitmap(cropped)
        val luminance = cropped.topLuminance()
        if (cropped !== bitmap) cropped.recycle()
        bitmap.recycle()
        luminance
    }.onFailure { Log.w("GalleryWallpaper", "Could not apply $uri", it) }.getOrNull()
}

/** Decodes at no more than twice the screen size, so a 50MP photo can't exhaust memory. */
private fun decodeSampled(context: Context, uri: Uri, width: Int, height: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= width && bounds.outHeight / (sample * 2) >= height) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

private fun Bitmap.cropTo(target: Float): Bitmap {
    val current = width.toFloat() / height
    if (kotlin.math.abs(current - target) < 0.01f) return this
    return if (current > target) {
        val w = (height * target).toInt().coerceAtLeast(1)
        Bitmap.createBitmap(this, ((width - w) / 2).coerceAtLeast(0), 0, w, height)
    } else {
        val h = (width / target).toInt().coerceAtLeast(1)
        Bitmap.createBitmap(this, 0, ((height - h) / 3).coerceAtLeast(0), width, h)
    }
}

private fun Bitmap.topLuminance(): Float {
    val strip = (height * 0.06f).toInt().coerceAtLeast(1)
    var total = 0.0
    var n = 0
    val stepX = (width / 32).coerceAtLeast(1)
    val stepY = (strip / 8).coerceAtLeast(1)
    for (y in 0 until strip step stepY) {
        for (x in 0 until width step stepX) {
            val p = getPixel(x, y)
            total += (0.299 * ((p shr 16) and 0xFF) + 0.587 * ((p shr 8) and 0xFF) + 0.114 * (p and 0xFF)) / 255.0
            n++
        }
    }
    return if (n == 0) 0f else (total / n).toFloat()
}
