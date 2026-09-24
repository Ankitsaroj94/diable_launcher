package `in`.ankitsaroj.diable.ui.screens.settings

import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.ui.components.DiableBottomSheet
import `in`.ankitsaroj.diable.ui.theme.DiableOnAccent
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One swatch in the theme-colour picker. */
private data class ThemeColorOption(val label: String, val argb: Int)

/** Diable's fixed palette, lightened to read as accents on the navy UI. */
private val FixedColors = listOf(
    ThemeColorOption("Gray", 0xFFC4C7CF.toInt()),
    ThemeColorOption("Red", 0xFFFFB3AE.toInt()),
    ThemeColorOption("Orange", 0xFFFFB86C.toInt()),
    ThemeColorOption("Yellow", 0xFFF3D46B.toInt()),
    ThemeColorOption("Green", 0xFFA8D5BA.toInt()),
    ThemeColorOption("Turquoise", 0xFF7FDCD6.toInt()),
    ThemeColorOption("Blue", 0xFFB4C5FF.toInt()),
    ThemeColorOption("Indigo", 0xFFBDB6FF.toInt()),
    ThemeColorOption("Purple", 0xFFE0B6FF.toInt()),
)

/**
 * Theme colour: Auto (from the wallpaper), the wallpaper's own colours, Material You on
 * Android 12+, then Diable's fixed palette. Everything resolves to one accent ARGB.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeColorSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val scope = rememberCoroutineScope()

    var wallpaperColors by remember { mutableStateOf<List<Int>>(emptyList()) }
    LaunchedEffect(Unit) {
        wallpaperColors = withContext(Dispatchers.IO) { wallpaperAccents(context) }
    }
    val dynamic = remember { materialYouAccents(context) }

    val groups = buildList {
        wallpaperColors.firstOrNull()?.let { add("Auto" to listOf(ThemeColorOption("Auto", it))) }
        if (wallpaperColors.isNotEmpty()) {
            add(
                "Wallpaper" to wallpaperColors.mapIndexed { i, c ->
                    ThemeColorOption("Wallpaper ${i + 1}", c)
                },
            )
        }
        if (dynamic.isNotEmpty()) {
            add(
                "Material You" to dynamic.mapIndexed { i, c ->
                    ThemeColorOption("Material You ${i + 1}", c)
                },
            )
        }
        add("Colors" to FixedColors)
    }

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Theme color",
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                fontSize = 23.sp,
                color = DiableText,
            )
            groups.forEach { (title, options) ->
                Text(
                    text = title,
                    color = DiableTextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    options.forEach { option ->
                        Swatch(
                            option = option,
                            selected = option.argb == prefs.accentColorArgb,
                            onClick = {
                                repos.prefs.launchUpdate { it.copy(accentColorArgb = option.argb) }
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Swatch(option: ThemeColorOption, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(option.argb))
                .then(if (selected) Modifier.border(2.dp, DiableText, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = DiableOnAccent)
            }
        }
        Text(
            text = option.label,
            color = if (selected) DiableText else DiableTextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * The wallpaper's primary/secondary/tertiary colours, lifted to accent lightness so they
 * stay legible as text and toggles on the dark UI.
 */
private fun wallpaperAccents(context: Context): List<Int> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return emptyList()
    val colors = runCatching {
        WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
    }.getOrNull() ?: return emptyList()
    return listOfNotNull(colors.primaryColor, colors.secondaryColor, colors.tertiaryColor)
        .map { toAccent(it.toArgb()) }
        .distinct()
}

/** Android 12+ dynamic accents (system_accent1_200 / system_accent2_200). */
private fun materialYouAccents(context: Context): List<Int> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
    return listOf(android.R.color.system_accent1_200, android.R.color.system_accent2_200)
        .mapNotNull { runCatching { context.getColor(it) }.getOrNull() }
        .distinct()
}

private fun toAccent(argb: Int): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[2] = hsl[2].coerceIn(0.72f, 0.82f)
    hsl[1] = hsl[1].coerceAtMost(0.85f)
    return ColorUtils.HSLToColor(hsl)
}
