package `in`.ankitsaroj.diable.ui.theme

import android.app.Activity
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import `in`.ankitsaroj.diable.data.DarkThemeMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs

/**
 * Colour for text drawn straight onto the wallpaper (clock, favorites, alphabet). The
 * theme editor's "Text color → Dark" flips it for light wallpapers.
 */
val LocalHomeTextColor = staticCompositionLocalOf { Color.White }

/** Near-black used for dark home text; pure black reads harsh against photos. */
val HomeTextDark = Color(0xFF111522)

private fun diableColorScheme(accent: Color, background: Color, palette: DiablePalette) =
    if (palette.isLight) {
        // Light surfaces need a deeper accent to keep contrast; the pastel one washes out.
        lightColorScheme(
            primary = palette.iconCircle,
            onPrimary = palette.iconGlyph,
            secondary = palette.textMuted,
            onSecondary = background,
            tertiary = ClockCardStart,
            background = background,
            onBackground = palette.text,
            surface = background,
            onSurface = palette.text,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.textMuted,
            outline = palette.toggleOff,
        )
    } else {
        darkColorScheme(
            primary = accent,
            onPrimary = background,
            secondary = palette.textMuted,
            onSecondary = background,
            tertiary = ClockCardStart,
            background = background,
            onBackground = palette.text,
            surface = background,
            onSurface = palette.text,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.textMuted,
            outline = palette.toggleOff,
        )
    }

/**
 * App theme. Font size, text colour and theme colour come from the theme editor's prefs;
 * callers may pass them explicitly, otherwise they are read from the shared repository.
 */
@Composable
fun DiableTheme(
    font: ThemeFont = ThemeFont.Rounded,
    fontScale: Float? = null,
    darkText: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val prefs by LocalDiableRepos.current.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val scale = (fontScale ?: prefs.fontScale).coerceIn(0.8f, 1.4f)
    val darkHomeText = darkText ?: prefs.darkText
    val accent = Color(prefs.accentColorArgb)

    val typography = remember(font) { typographyFor(font.family) }
    // Global settings → Dark Theme. Both "device" modes follow the system night mode;
    // Pitch-Black only means something while dark.
    val systemDark = isSystemInDarkTheme()
    val dark = when (prefs.darkThemeMode) {
        DarkThemeMode.AlwaysEnabled -> true
        DarkThemeMode.AlwaysDisabled -> false
        DarkThemeMode.UseDeviceTheme, DarkThemeMode.DeviceThemeAndWallpaper -> systemDark
    }
    val palette = if (dark) DarkDiablePalette else LightDiablePalette
    val background = when {
        !dark -> palette.background
        prefs.pitchBlackTheme -> PitchBlack
        else -> DiableNavy
    }
    val colorScheme = remember(accent, background, palette) { diableColorScheme(accent, background, palette) }

    // Global settings → Hide Status Bar. Swiping from the top edge still reveals it.
    val view = LocalView.current
    LaunchedEffect(view, prefs.hideStatusBar) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (prefs.hideStatusBar) {
                hide(WindowInsetsCompat.Type.statusBars())
            } else {
                show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }
    val density = LocalDensity.current
    // Scaling the density's fontScale resizes every sp in the app at once, including
    // hardcoded fontSize values, which a Typography swap alone would miss.
    val scaledDensity = remember(density, scale) {
        Density(density = density.density, fontScale = density.fontScale * scale)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
    ) {
        // Nothing wraps the app in a Surface, so bare Text() would fall back to Roboto.
        // Seed LocalTextStyle so the rounded face applies everywhere by default.
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = font.family),
            LocalDensity provides scaledDensity,
            LocalHomeTextColor provides if (darkHomeText) HomeTextDark else Color.White,
            LocalDiableBg provides background,
            LocalDiablePalette provides palette,
            content = content,
        )
    }
}
