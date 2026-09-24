package `in`.ankitsaroj.diable.data

import androidx.compose.ui.graphics.Color

/**
 * The wallpaper categories offered in Themes, and the search each one runs against
 * Openverse. The gradient is what the category tile paints before any image loads.
 */
object WallpaperCatalog {

    data class Category(
        val name: String,
        /**
         * Search terms are deliberately biased toward calm, low-detail images — a busy
         * photo makes the home screen's text unreadable no matter how much it is dimmed.
         */
        val query: String,
        val colors: List<Color>,
    )

    val categories = listOf(
        Category("Minimal", "minimal simple background negative space", listOf(Color(0xFF434343), Color(0xFF1A1A1A))),
        Category("Gradient", "smooth gradient soft colour blur", listOf(Color(0xFFFA709A), Color(0xFFFEE140))),
        Category("Abstract", "abstract minimal shapes soft", listOf(Color(0xFF667EEA), Color(0xFF764BA2))),
        Category("Mountains", "mountain mist minimal landscape fog", listOf(Color(0xFF5C258D), Color(0xFF4389A2))),
        Category("Ocean", "calm sea horizon minimal water", listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))),
        Category("Sky", "clear sky clouds soft pastel", listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))),
        Category("Forest", "misty forest fog minimal trees", listOf(Color(0xFF134E5E), Color(0xFF71B280))),
        Category("Desert", "desert dunes minimal sand", listOf(Color(0xFFD4A574), Color(0xFF8B6F47))),
        Category("Space", "nebula stars deep space dark", listOf(Color(0xFF0B486B), Color(0xFF14213D))),
        Category("Snow", "snow minimal white landscape", listOf(Color(0xFFE8EDF2), Color(0xFFB8C6D9))),
        Category("Texture", "paper concrete texture plain surface", listOf(Color(0xFFBDC3C7), Color(0xFF2C3E50))),
        Category("Architecture", "minimal architecture facade lines", listOf(Color(0xFF3E5151), Color(0xFFDECBA4))),
    )

    fun queryFor(name: String): String =
        categories.firstOrNull { it.name == name }?.query ?: name

    fun colorsFor(name: String): List<Color>? =
        categories.firstOrNull { it.name == name }?.colors
}
