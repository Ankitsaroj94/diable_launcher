package `in`.ankitsaroj.diable.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * One wallpaper from Openverse. [creator] and [licenseName] are not decoration — the
 * results are Creative Commons licensed and most of them require attribution, so the
 * gallery surfaces them.
 */
/** The result of applying a wallpaper: a marker id plus how bright its top edge is. */
data class AppliedWallpaper(
    val marker: String,
    /** Mean luminance (0..1) of the strip beneath the status bar. */
    val topLuminance: Float,
)

data class WebWallpaper(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val fullUrl: String,
    val creator: String,
    val licenseName: String,
    val licenseUrl: String,
    val sourcePage: String,
)

/**
 * Fetches wallpapers by category from Openverse, which is keyless and only indexes
 * openly-licensed media. Results are cached per category for the process lifetime so
 * re-opening a category doesn't re-hit the network.
 */
class WallpaperRepository(private val context: Context) {

    suspend fun search(category: String, limit: Int = MAX_ANONYMOUS_PAGE_SIZE): List<WebWallpaper> {
        searchCache[category]?.let { return it }
        val results = withContext(Dispatchers.IO) {
            runCatching { fetch(WallpaperCatalog.queryFor(category), limit) }
                .onFailure { android.util.Log.w("WallpaperRepo", "search($category) failed", it) }
                .getOrDefault(emptyList())
        }
        if (results.isNotEmpty()) searchCache[category] = results
        return results
    }

    private fun fetch(query: String, limit: Int): List<WebWallpaper> {
        val url = buildString {
            append("https://api.openverse.org/v1/images/?q=")
            append(URLEncoder.encode(query, "UTF-8"))
            append("&page_size=${limit.coerceAtMost(MAX_ANONYMOUS_PAGE_SIZE)}")
            // Commercial-use licences only, and large enough to fill a phone screen.
            // No aspect filter: it cuts results ~12x and phones crop anyway.
            append("&license_type=commercial&size=medium")
        }
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", "Diable/1.0 (launcher wallpaper picker)")
        }
        return conn.use { c ->
            if (c.responseCode !in 200..299) {
                android.util.Log.w("WallpaperRepo", "HTTP ${c.responseCode} for $url")
                return emptyList()
            }
            val body = c.inputStream.bufferedReader().readText()
            val arr = JSONObject(body).optJSONArray("results") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val full = o.optString("url").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                WebWallpaper(
                    id = o.optString("id"),
                    title = o.optString("title").ifBlank { "Untitled" },
                    thumbnailUrl = scaledUrl(full, THUMB_WIDTH)
                        ?: o.optString("thumbnail").ifBlank { full },
                    fullUrl = scaledUrl(full, FULL_WIDTH) ?: full,
                    creator = o.optString("creator").ifBlank { "Unknown" },
                    licenseName = o.optString("license").uppercase(),
                    licenseUrl = o.optString("license_url"),
                    sourcePage = o.optString("foreign_landing_url"),
                )
            }
        }
    }

    /**
     * Downloads [wallpaper] and sets it as the system wallpaper.
     *
     * Nothing is written to app storage: the launcher window is transparent with
     * `windowShowWallpaper`, so the system wallpaper is already what shows through. The
     * returned marker just records which image is active.
     */
    suspend fun applyWallpaper(wallpaper: WebWallpaper): AppliedWallpaper? =
        withContext(Dispatchers.IO) {
            runCatching {
                val downloaded = downloadBitmap(wallpaper.fullUrl) ?: return@runCatching null
                // setBitmap fits rather than fills, which letterboxes the wallpaper with
                // black bars. Crop to the screen's aspect ratio first.
                val bitmap = downloaded.cropToScreenAspect(context)
                WallpaperManager.getInstance(context).setBitmap(bitmap)
                // Measured here because reading the wallpaper back later needs storage
                // permission, and the whole-image average says nothing about the strip
                // the status bar actually sits on.
                val topLuminance = bitmap.topStripLuminance()
                if (bitmap !== downloaded) downloaded.recycle()
                bitmap.recycle()
                AppliedWallpaper("openverse:${wallpaper.id}", topLuminance)
            }.onFailure {
                android.util.Log.w("WallpaperRepo", "applyWallpaper failed", it)
            }.getOrNull()
        }

    private fun downloadBitmap(url: String): Bitmap? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Diable/1.0")
        }
        return conn.use { c ->
            if (c.responseCode !in 200..299) return null
            // Downsample to roughly screen size; full-res Flickr originals are huge.
            val bytes = c.inputStream.readBytes()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val metrics = context.resources.displayMetrics
            var sample = 1
            while (bounds.outHeight / sample > metrics.heightPixels * 2 ||
                bounds.outWidth / sample > metrics.widthPixels * 2
            ) {
                sample *= 2
            }
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample },
            )
        }
    }
}

/** Openverse rejects anything larger than this without an API key (HTTP 401). */
private const val MAX_ANONYMOUS_PAGE_SIZE = 20

/**
 * Search results live for the whole process, not per repository instance — navigating
 * back into a category should not re-hit the network.
 */
private val searchCache = java.util.concurrent.ConcurrentHashMap<String, List<WebWallpaper>>()

private const val THUMB_WIDTH = 640
private const val FULL_WIDTH = 1440

private val WIKIMEDIA = Regex("""^(https://upload\.wikimedia\.org/wikipedia/[^/]+)/([0-9a-f])/([0-9a-f]{2})/(.+)$""")
private val FLICKR = Regex("""^(https://live\.staticflickr\.com/.+)_[a-z]\.(jpg|png)$""")

/**
 * Rewrites a source URL to a size-capped variant.
 *
 * Openverse's own /thumb/ proxy answers 424 "Thumbnail unavailable from provider" for a
 * lot of entries, and the originals behind them run 4-14MB — which is why the picker both
 * showed blanks and crawled. The two providers that dominate results expose predictable
 * resize URLs, so use those and fall back only when the shape is unfamiliar.
 */
private fun scaledUrl(sourceUrl: String, width: Int): String? {
    WIKIMEDIA.find(sourceUrl)?.let { m ->
        val (base, d1, d2, name) = m.destructured
        // Sub-paths aren't plain file names and don't follow the /thumb/ scheme.
        if (name.contains('/')) return null
        // Wikimedia only serves a fixed ladder of widths; anything else is a 400.
        val allowed = if (width <= 500) 500 else 1280
        return "$base/thumb/$d1/$d2/$name/${allowed}px-$name"
    }
    FLICKR.find(sourceUrl)?.let { m ->
        // Flickr suffixes: _z=640, _c=800, _b=1024.
        val suffix = when {
            width <= 640 -> "z"
            width <= 800 -> "c"
            else -> "b"
        }
        return "${m.groupValues[1]}_$suffix.${m.groupValues[2]}"
    }
    return null
}

/**
 * Centre-crops to the screen's aspect ratio so the wallpaper fills rather than letterboxes.
 */
private fun Bitmap.cropToScreenAspect(context: Context): Bitmap {
    val metrics = context.resources.displayMetrics
    val target = metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat()
    val current = width.toFloat() / height.toFloat()
    if (kotlin.math.abs(current - target) < 0.01f) return this

    return if (current > target) {
        // Too wide: trim the sides.
        val newWidth = (height * target).toInt().coerceAtLeast(1)
        Bitmap.createBitmap(this, ((width - newWidth) / 2).coerceAtLeast(0), 0, newWidth, height)
    } else {
        // Too tall: trim top and bottom, biased slightly upward to favour skylines.
        val newHeight = (width / target).toInt().coerceAtLeast(1)
        val top = ((height - newHeight) / 3).coerceAtLeast(0)
        Bitmap.createBitmap(this, 0, top, width, newHeight)
    }
}

/**
 * Mean luminance of the top 6% of the image — roughly what sits behind the status bar
 * once the wallpaper is scaled to fill the screen.
 */
private fun Bitmap.topStripLuminance(): Float {
    val stripHeight = (height * 0.06f).toInt().coerceAtLeast(1)
    var total = 0.0
    var samples = 0
    val stepX = (width / 32).coerceAtLeast(1)
    val stepY = (stripHeight / 8).coerceAtLeast(1)
    var y = 0
    while (y < stripHeight) {
        var x = 0
        while (x < width) {
            val pixel = getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            total += (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            samples++
            x += stepX
        }
        y += stepY
    }
    return if (samples == 0) 0f else (total / samples).toFloat()
}

private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
    try {
        block(this)
    } finally {
        disconnect()
    }
