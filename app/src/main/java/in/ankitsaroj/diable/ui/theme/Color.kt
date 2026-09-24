package `in`.ankitsaroj.diable.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Sampled from the real Diable launcher. It is a navy/periwinkle system, not the
// purple/lavender one the original spec described.
val DiableNavy = Color(0xFF0A122A)
val PitchBlack = Color(0xFF000000)

/**
 * Every colour that differs between Diable's dark and light settings surfaces. The home
 * screen sits on the wallpaper and doesn't use these; settings, sheets and dialogs do.
 */
data class DiablePalette(
    val background: Color,
    val card: Color,
    val text: Color,
    val textMuted: Color,
    val iconCircle: Color,
    val iconGlyph: Color,
    val toggleOn: Color,
    val toggleOff: Color,
    val toggleKnobOn: Color,
    val headerPill: Color,
    val searchPill: Color,
    val editorChrome: Color,
    /** Links, selected labels and text buttons: the pastel accent washes out on light. */
    val accentText: Color,
    val isLight: Boolean,
)

val DarkDiablePalette = DiablePalette(
    background = DiableNavy,
    card = Color(0xFF222D47),
    text = Color.White,
    textMuted = Color(0xFF909DBB),
    iconCircle = Color(0xFF9BB6F8),
    iconGlyph = Color(0xFF132569),
    toggleOn = Color(0xFFA1BCFF),
    toggleOff = Color(0xFFA2AEC8),
    toggleKnobOn = Color(0xFF222D47),
    headerPill = Color(0xFF222D47),
    searchPill = Color(0xFF0A122A),
    editorChrome = Color(0xFF343E65),
    accentText = Color(0xFFB4C5FF),
    isLight = false,
)

val LightDiablePalette = DiablePalette(
    background = Color(0xFFF4F6FC),
    card = Color(0xFFE3E8F5),
    text = Color(0xFF111522),
    textMuted = Color(0xFF5A6480),
    iconCircle = Color(0xFF2F4A9E),
    iconGlyph = Color(0xFFE8EDFF),
    toggleOn = Color(0xFF2F4A9E),
    toggleOff = Color(0xFF8C95AD),
    toggleKnobOn = Color(0xFFFFFFFF),
    headerPill = Color(0xFFE3E8F5),
    searchPill = Color(0xFFE3E8F5),
    editorChrome = Color(0xFFD5DCEE),
    accentText = Color(0xFF2F4A9E),
    isLight = true,
)

val LocalDiablePalette = staticCompositionLocalOf { DarkDiablePalette }

/** Screen/sheet background; Pitch-Black swaps it for true black in dark mode. */
val LocalDiableBg = staticCompositionLocalOf { DiableNavy }

val DiableBg: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiableBg.current

val DiableSheet: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiableBg.current

val DiableCard: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.card

/** Primary text on settings surfaces (white in dark mode, near-black in light). */
val DiableText: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.text

val DiableTextMuted: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.textMuted

val DiableAccent = Color(0xFFB4C5FF)

/** Text and glyphs drawn ON the pastel accent (buttons, selected chips): always dark. */
val DiableOnAccent = Color(0xFF132569)

/** Accent for text and icons on settings surfaces; readable in light mode too. */
val DiableAccentText: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.accentText

/** Settings-row icons: a solid disc with a contrasting glyph. */
val DiableIconCircle: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.iconCircle

val DiableIconGlyph: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.iconGlyph

val DiableToggleOn: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.toggleOn

val DiableToggleOff: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.toggleOff

val DiableToggleKnobOn: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.toggleKnobOn

val DiableHeaderPill: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.headerPill

val DiableSearchPill: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.searchPill

val ClockCardStart = Color(0xFF5C6BC0)
val ClockCardEnd = Color(0xFF3949AB)

/** Clock-style picker tiles: a blue-violet diagonal wash. */
val ClockTileStart = Color(0xFF6889ED)
val ClockTileEnd = Color(0xFF3A5BB6)

/** Theme-editor chrome: back/apply buttons and the toolbar pill. */
val EditorChrome: Color
    @Composable @ReadOnlyComposable
    get() = LocalDiablePalette.current.editorChrome
