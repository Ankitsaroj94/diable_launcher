package `in`.ankitsaroj.diable.data

import androidx.compose.ui.graphics.Color

/** The silhouette an icon is cut to. */
enum class IconShape {
    /** Whatever the system mask draws — the icon exactly as the app ships it. */
    System,
    Squircle,
    Circle,
    RoundedSquare,
    Teardrop,
    Hexagon,
}

/** How an icon's colours respond to the theme accent. */
enum class IconTreatment {
    /** The app's own colours. */
    Original,
    /** Accent-coloured glyph with no backdrop. */
    AccentGlyph,
    /** Light accent backdrop, dark glyph — Android's themed icons. */
    ThemedLight,
    /** Dark accent backdrop, accent glyph. */
    ThemedDark,
    /** Desaturated app colours. */
    Grayscale,
    /** The app's artwork re-toned between a dark and a light shade of the accent. */
    Duotone,
}

/**
 * A built-in icon pack: a shape plus a colour treatment. Treatments other than
 * [IconTreatment.Original] recolour with the theme accent, so switching themes restyles
 * every icon on the home screen.
 */
enum class IconStyle(
    val key: String,
    val label: String,
    val shape: IconShape,
    val treatment: IconTreatment,
) {
    Original("original", "System", IconShape.System, IconTreatment.Original),
    Rounded("rounded", "Squircle", IconShape.Squircle, IconTreatment.Original),
    Circle("circle", "Circle", IconShape.Circle, IconTreatment.Original),
    Square("square", "Square", IconShape.RoundedSquare, IconTreatment.Original),
    Teardrop("teardrop", "Teardrop", IconShape.Teardrop, IconTreatment.Original),
    Hexagon("hexagon", "Hexagon", IconShape.Hexagon, IconTreatment.Original),
    Monochrome("monochrome", "Glyph", IconShape.Circle, IconTreatment.AccentGlyph),
    Themed("themed", "Themed", IconShape.Circle, IconTreatment.ThemedLight),
    ThemedDark("themed_dark", "Midnight", IconShape.Squircle, IconTreatment.ThemedDark),
    Duotone("duotone", "Duotone", IconShape.Squircle, IconTreatment.Duotone),
    Grayscale("grayscale", "Mono", IconShape.Circle, IconTreatment.Grayscale),
    ;

    /** True when the accent colour changes how icons look. */
    val followsAccent: Boolean
        get() = treatment != IconTreatment.Original && treatment != IconTreatment.Grayscale

    companion object {
        fun fromKey(key: String?): IconStyle =
            entries.firstOrNull { it.key == key } ?: Rounded
    }
}

/**
 * A ready-made look: wallpaper category, typeface, clock face, accent colour and icon
 * treatment applied together. This is what "Ready-Made Themes" applies in one tap.
 */
data class ThemePreset(
    val id: String,
    val name: String,
    val description: String,
    /** Category in [WallpaperCatalog] the wallpaper is pulled from. */
    val wallpaperCategory: String,
    val fontKey: String,
    val clockStyleId: Int,
    val accentArgb: Int,
    val iconStyle: IconStyle,
    /** Fallback gradient shown until the wallpaper downloads. */
    val previewColors: List<Color>,
)

object ThemePresets {

    val all = listOf(
        ThemePreset(
            id = "midnight",
            name = "Midnight",
            description = "Deep space, thin type, quiet clock",
            wallpaperCategory = "Space",
            fontKey = "outfit",
            clockStyleId = 23,
            accentArgb = 0xFF9BB6F8.toInt(),
            iconStyle = IconStyle.ThemedDark,
            previewColors = listOf(Color(0xFF0B486B), Color(0xFF14213D)),
        ),
        ThemePreset(
            id = "summit",
            name = "Summit",
            description = "Mountain light with tracked capitals",
            wallpaperCategory = "Mountains",
            fontKey = "space_grotesk",
            clockStyleId = 16,
            accentArgb = 0xFFB8D8E8.toInt(),
            iconStyle = IconStyle.Hexagon,
            previewColors = listOf(Color(0xFF5C258D), Color(0xFF4389A2)),
        ),
        ThemePreset(
            id = "atelier",
            name = "Atelier",
            description = "Editorial serif over soft botanicals",
            wallpaperCategory = "Flowers",
            fontKey = "playfair",
            clockStyleId = 19,
            accentArgb = 0xFFF2C6C2.toInt(),
            iconStyle = IconStyle.Teardrop,
            previewColors = listOf(Color(0xFFFF6B6B), Color(0xFFFFB88C)),
        ),
        ThemePreset(
            id = "neon",
            name = "Neon",
            description = "City nights, monospace, stacked digits",
            wallpaperCategory = "Cyberpunk",
            fontKey = "space_grotesk",
            clockStyleId = 26,
            accentArgb = 0xFF00DBDE.toInt(),
            iconStyle = IconStyle.Monochrome,
            previewColors = listOf(Color(0xFFFC00FF), Color(0xFF00DBDE)),
        ),
        ThemePreset(
            id = "grove",
            name = "Grove",
            description = "Forest greens with a rounded face",
            wallpaperCategory = "Forest",
            fontKey = "nunito",
            clockStyleId = 15,
            accentArgb = 0xFFA8D5BA.toInt(),
            iconStyle = IconStyle.Themed,
            previewColors = listOf(Color(0xFF134E5E), Color(0xFF71B280)),
        ),
        ThemePreset(
            id = "tide",
            name = "Tide",
            description = "Ocean blues, light and airy",
            wallpaperCategory = "Ocean",
            fontKey = "quicksand",
            clockStyleId = 3,
            accentArgb = 0xFF6DD5ED.toInt(),
            iconStyle = IconStyle.Circle,
            previewColors = listOf(Color(0xFF2193B0), Color(0xFF6DD5ED)),
        ),
        ThemePreset(
            id = "paper",
            name = "Paper",
            description = "Minimal ground, monospace clock",
            wallpaperCategory = "Minimal",
            fontKey = "space_grotesk",
            clockStyleId = 17,
            accentArgb = 0xFFD8D8D8.toInt(),
            iconStyle = IconStyle.Grayscale,
            previewColors = listOf(Color(0xFF434343), Color(0xFF1A1A1A)),
        ),
        ThemePreset(
            id = "bloom",
            name = "Bloom",
            description = "Abstract colour with an exponent clock",
            wallpaperCategory = "Abstract",
            fontKey = "outfit",
            clockStyleId = 25,
            accentArgb = 0xFFC9A7EB.toInt(),
            iconStyle = IconStyle.Duotone,
            previewColors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
        ),
    )

    fun byId(id: String?): ThemePreset? = all.firstOrNull { it.id == id }
}
