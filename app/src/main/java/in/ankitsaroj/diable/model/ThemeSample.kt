package `in`.ankitsaroj.diable.model

data class ThemeSample(
    val id: String,
    val name: String,
    val accentColorArgb: Int,
    val wallpaperKey: String? = null,
)

object SampleThemes {
    val carousel = listOf(
        ThemeSample("lavender", "Lavender Dream", 0xFFC5B4E3.toInt(), "gradient:Abstract"),
        ThemeSample("ocean", "Ocean Breeze", 0xFF4FACFE.toInt(), "gradient:Dreamscapes"),
        ThemeSample("sunset", "Sunset Glow", 0xFFFA709A.toInt(), "gradient:Gradient"),
    )

    val myThemes = listOf(
        ThemeSample("custom1", "My Dark Theme", 0xFF9C88FF.toInt()),
        ThemeSample("custom2", "Minimal", 0xFFB0BEC5.toInt()),
    )

    val readyMade = carousel + listOf(
        ThemeSample("cyber", "Cyber Night", 0xFF00DBDE.toInt(), "gradient:Cyberpunk"),
        ThemeSample("lofi", "Lofi Chill", 0xFF3498DB.toInt(), "gradient:Lofi"),
    )

    val community = listOf(
        ThemeSample("comm1", "Neon Streets", 0xFFFC00FF.toInt(), "gradient:Cyberpunk"),
        ThemeSample("comm2", "Forest Walk", 0xFF38EF7D.toInt(), "gradient:Animals"),
    )
}
