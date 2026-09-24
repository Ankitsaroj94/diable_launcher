package `in`.ankitsaroj.diable.ui.theme

import android.app.Activity
import android.app.WallpaperManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

/**
 * Keeps the status- and navigation-bar icons readable against the wallpaper.
 *
 * The launcher window is transparent, so the system bars sit directly on the wallpaper.
 * With a fixed appearance the clock and icons disappear whenever the wallpaper's top
 * edge happens to match them — so flip them with the wallpaper instead.
 *
 * @param measuredTopLuminance brightness of the wallpaper strip under the status bar,
 *   measured when we applied it, or -1 if this wallpaper did not come from us. This is
 *   preferred over the system's hint because that describes the whole image — a picture
 *   can be bright overall yet dark exactly where the bar sits.
 */
@Composable
fun SystemBarAppearance(measuredTopLuminance: Float = -1f) {
    val context = LocalContext.current
    val view = LocalView.current
    var wallpaperLuminance by remember { mutableStateOf(0.0) }

    DisposableEffect(context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            return@DisposableEffect onDispose { }
        }
        val manager = WallpaperManager.getInstance(context)

        fun refresh() {
            wallpaperLuminance = manager.wallpaperLuminance()
        }

        refresh()
        val listener = WallpaperManager.OnColorsChangedListener { _, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) refresh()
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { manager.removeOnColorsChangedListener(listener) }
    }

    // Measured against the wallpaper as the system renders it behind the bars. The
    // launcher's Dim Wallpaper scrim deliberately does NOT factor in here: it is drawn
    // inside the window and measurement showed it does not darken the bar region.
    val effective = if (measuredTopLuminance >= 0f) {
        measuredTopLuminance.toDouble()
    } else {
        wallpaperLuminance
    }
    val useDarkIcons = effective > LIGHT_THRESHOLD

    LaunchedEffect(useDarkIcons, view) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = useDarkIcons
            isAppearanceLightNavigationBars = useDarkIcons
        }
    }
}

private const val LIGHT_THRESHOLD = 0.5

/**
 * Approximate brightness of the wallpaper behind the system bars, 0 (black) to 1 (white).
 *
 * Android 12+ analyses the whole image and tells us directly whether dark text works; on
 * older releases fall back to the luminance of the reported primary colour.
 */
private fun WallpaperManager.wallpaperLuminance(): Double = try {
    val colors = getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
    when {
        colors == null -> 0.0
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (colors.colorHints and android.app.WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0 ->
            // The system says dark text is legible; treat it as clearly light.
            1.0
        else -> ColorUtils.calculateLuminance(colors.primaryColor.toArgb())
    }
} catch (_: Exception) {
    0.0
}
