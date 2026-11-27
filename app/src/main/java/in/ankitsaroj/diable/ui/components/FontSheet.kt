package `in`.ankitsaroj.diable.ui.components

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import `in`.ankitsaroj.diable.data.AvailableFont
import `in`.ankitsaroj.diable.data.CustomFonts
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.openPlayStoreSearch
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableOnAccent
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import `in`.ankitsaroj.diable.ui.theme.ThemeFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Typeface picker. Each row previews itself in its own face so the choice is visible
 * before committing, and applying it restyles the whole launcher.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val scope = rememberCoroutineScope()
    var savedFonts by remember { mutableStateOf<List<AvailableFont>>(emptyList()) }
    var storeFonts by remember { mutableStateOf<List<AvailableFont>>(emptyList()) }
    LifecycleResumeEffect(Unit) {
        scope.launch {
            val (saved, store) = withContext(Dispatchers.IO) {
                CustomFonts.saved(context) to CustomFonts.inInstalledApps(context)
            }
            savedFonts = saved
            storeFonts = store
        }
        onPauseOrDispose { }
    }
    val fontFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = withContext(Dispatchers.IO) { CustomFonts.import(context, uri) }
            if (imported?.path == null) {
                Toast.makeText(context, "Choose a .ttf or .otf font file", Toast.LENGTH_SHORT).show()
            } else {
                savedFonts = withContext(Dispatchers.IO) { CustomFonts.saved(context) }
                repos.prefs.launchUpdate {
                    it.copy(
                        fontKey = CustomFonts.KEY,
                        customFontPath = imported.path,
                        customFontLabel = imported.label,
                    )
                }
            }
        }
    }
    fun useSaved(font: AvailableFont) {
        val path = font.path ?: return
        repos.prefs.launchUpdate {
            it.copy(fontKey = CustomFonts.KEY, customFontPath = path, customFontLabel = font.label)
        }
    }
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

            SheetSectionTitle("Your fonts", "Files you add, and fonts inside apps installed from the Play Store")
            if (savedFonts.isEmpty() && storeFonts.isEmpty()) {
                Text(
                    text = "No extra fonts yet.",
                    color = DiableTextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            savedFonts.forEach { font ->
                val selected = prefs.fontKey == CustomFonts.KEY && prefs.customFontPath == font.path
                FontChoiceRow(
                    label = font.label,
                    family = CustomFonts.family(context, CustomFonts.KEY, font.path),
                    selected = selected,
                    onClick = { useSaved(font) },
                )
            }
            storeFonts.forEach { font ->
                FontChoiceRow(
                    label = font.label,
                    family = ThemeFont.Rounded.family,
                    selected = false,
                    onClick = {
                        scope.launch {
                            val copied = withContext(Dispatchers.IO) { CustomFonts.copyFromApp(context, font) }
                            if (copied?.path == null) {
                                Toast.makeText(context, "Could not read that font", Toast.LENGTH_SHORT).show()
                            } else {
                                savedFonts = withContext(Dispatchers.IO) { CustomFonts.saved(context) }
                                useSaved(copied)
                            }
                        }
                    },
                )
            }
            Text(
                text = "Add a font file",
                color = DiableAccentText,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        fontFile.launch(
                            arrayOf(
                                "font/ttf",
                                "font/otf",
                                "application/x-font-ttf",
                                "application/x-font-otf",
                                "application/octet-stream",
                            ),
                        )
                    }
                    .padding(vertical = 12.dp),
            )
            Text(
                text = "Get fonts on the Play Store",
                color = DiableAccentText,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openPlayStoreSearch(context, "fonts") }
                    .padding(vertical = 12.dp),
            )

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
private fun FontChoiceRow(
    label: String,
    family: androidx.compose.ui.text.font.FontFamily,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(DiableCard, RoundedCornerShape(16.dp))
            .then(
                if (selected) Modifier.border(2.dp, DiableText, RoundedCornerShape(16.dp)) else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = "9:41", color = DiableText, fontSize = 28.sp, fontFamily = family)
            Text(
                text = label,
                color = if (selected) DiableText else DiableTextMuted,
                fontSize = 14.sp,
                fontFamily = family,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SheetSectionTitle(text: String, subtitle: String? = null) {
    Text(
        text = text,
        color = DiableText,
        fontSize = 17.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = if (subtitle == null) 8.dp else 2.dp),
    )
    if (subtitle != null) {
        Text(
            text = subtitle,
            color = DiableTextMuted,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
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
