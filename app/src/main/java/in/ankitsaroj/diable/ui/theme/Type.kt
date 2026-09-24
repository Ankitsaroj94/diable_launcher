package `in`.ankitsaroj.diable.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.R

// Each face is a variable font, so every weight is the same file driven through the
// `wght` axis. Below API 26 the settings are ignored and Android synthesizes the weight.
@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private fun familyOf(resId: Int) = FontFamily(
    variable(resId, FontWeight.Light),
    variable(resId, FontWeight.Normal),
    variable(resId, FontWeight.Medium),
    variable(resId, FontWeight.SemiBold),
    variable(resId, FontWeight.Bold),
)

val Quicksand = familyOf(R.font.quicksand)
val Nunito = familyOf(R.font.nunito)
val SpaceGrotesk = familyOf(R.font.space_grotesk)
val PlayfairDisplay = familyOf(R.font.playfair_display)
val Outfit = familyOf(R.font.outfit)

/**
 * The fonts a theme can select. Stored by [key], so renaming one won't orphan prefs.
 * [featured] faces show in the sheet up front; the rest sit behind "See all". The system
 * families are always on the device, so nothing here is ever downloaded.
 */
enum class ThemeFont(
    val key: String,
    val label: String,
    val family: FontFamily,
    val featured: Boolean = true,
) {
    Rounded("quicksand", "Quicksand", Quicksand),
    Soft("nunito", "Nunito", Nunito),
    Technical("space_grotesk", "Space Grotesk", SpaceGrotesk),
    Elegant("playfair", "Playfair Display", PlayfairDisplay),
    Geometric("outfit", "Outfit", Outfit),
    System("system", "System", FontFamily.Default),
    SystemSerif("system_serif", "Serif", FontFamily.Serif, featured = false),
    SystemMono("system_mono", "Monospace", FontFamily.Monospace, featured = false),
    SystemCursive("system_cursive", "Handwriting", FontFamily.Cursive, featured = false),
    SystemSans("system_sans", "Sans Serif", FontFamily.SansSerif, featured = false),
    ;

    companion object {
        fun fromKey(key: String?): ThemeFont =
            entries.firstOrNull { it.key == key } ?: Rounded
    }
}

/** Material typography rebuilt around [family] so a theme swap restyles everything. */
fun typographyFor(family: FontFamily) = Typography(
    displayLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
