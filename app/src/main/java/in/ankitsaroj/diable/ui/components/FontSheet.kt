package `in`.ankitsaroj.diable.ui.components

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableOnAccent
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import `in`.ankitsaroj.diable.ui.theme.ThemeFont
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Typeface picker. Each row previews itself in its own face so the choice is visible
 * before committing, and applying it restyles the whole launcher.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSheet(onDismiss: () -> Unit) {
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val scope = rememberCoroutineScope()
    val current = ThemeFont.fromKey(prefs.fontKey)
    // The current face stays visible even when it lives behind "See all".
    var showAll by remember { mutableStateOf(!current.featured) }
    val fonts = if (showAll) ThemeFont.entries else ThemeFont.entries.filter { it.featured }

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Font",
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                fontSize = 23.sp,
                color = DiableText,
            )
            fonts.forEach { font ->
                val selected = font == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(DiableCard, RoundedCornerShape(16.dp))
                        .then(
                            if (selected) {
                                Modifier.border(2.dp, DiableText, RoundedCornerShape(16.dp))
                            } else {
                                Modifier
                            },
                        )
                        .clickable {
                            repos.prefs.launchUpdate { it.copy(fontKey = font.key) }
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Preview in the face itself — the point of the choice.
                        Text(
                            text = "9:41",
                            color = DiableText,
                            fontSize = 28.sp,
                            fontFamily = font.family,
                        )
                        Text(
                            text = font.label,
                            color = if (selected) DiableText else DiableTextMuted,
                            fontSize = 14.sp,
                            fontFamily = font.family,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
            if (!showAll) {
                Text(
                    text = "See all",
                    color = DiableAccentText,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAll = true }
                        .padding(vertical = 14.dp),
                )
            }

            SheetSectionTitle("Font size")
            ChipRow(
                options = FontScales.map { "${(it * 100).roundToInt()}%" },
                selectedIndex = FontScales.indexOfFirst { abs(it - prefs.fontScale) < 0.01f },
                onSelect = { i ->
                    repos.prefs.launchUpdate { it.copy(fontScale = FontScales[i]) }
                },
            )

            SheetSectionTitle("Text color")
            ChipRow(
                options = listOf("Light", "Dark"),
                selectedIndex = if (prefs.darkText) 1 else 0,
                onSelect = { i ->
                    repos.prefs.launchUpdate { it.copy(darkText = i == 1) }
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Diable's font-size steps. */
private val FontScales = listOf(0.8f, 0.9f, 1f, 1.1f, 1.2f, 1.4f)

@Composable
private fun SheetSectionTitle(text: String) {
    Text(
        text = text,
        color = DiableText,
        fontSize = 17.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

/** Single-choice pill chips, wrapping onto a second line when needed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Text(
                text = label,
                color = if (selected) DiableOnAccent else DiableText,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) DiableAccent else DiableCard)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}
