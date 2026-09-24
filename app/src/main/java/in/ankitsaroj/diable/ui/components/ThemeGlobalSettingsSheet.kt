package `in`.ankitsaroj.diable.ui.components

import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.DarkThemeMode
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.launch

/**
 * "Global settings" from the theme editor's toolbar — settings that apply across every
 * theme rather than the current one. Five rows; Dark Theme opens a radio sub-sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeGlobalSettingsSheet(onDismiss: () -> Unit) {
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val scope = rememberCoroutineScope()
    var showDarkTheme by remember { mutableStateOf(false) }

    if (showDarkTheme) {
        DarkThemeSheet(onDismiss = { showDarkTheme = false })
        return
    }

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
            Text(
                text = "Global settings",
                modifier = Modifier.padding(top = 16.dp),
                fontSize = 23.sp,
                color = DiableText,
            )
            Text(
                text = "These settings apply to all your themes, not just the current one.",
                color = DiableTextMuted,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            DiableSettingCard(
                icon = Icons.Default.DarkMode,
                title = "Dark Theme",
                subtitle = prefs.darkThemeMode.label,
                onClick = { showDarkTheme = true },
            )
            DiableSettingCard(
                icon = Icons.Default.VisibilityOff,
                title = "Hide Status Bar",
                checked = prefs.hideStatusBar,
                onCheckedChange = {
                    repos.prefs.launchUpdate { p -> p.copy(hideStatusBar = it) }
                },
            )
            DiableSettingCard(
                icon = Icons.Default.VisibilityOff,
                title = "Hide Alphabet",
                subtitle = "Still visible in your all apps list",
                checked = prefs.hideAlphabet,
                onCheckedChange = {
                    repos.prefs.launchUpdate { p -> p.copy(hideAlphabet = it) }
                },
            )
            DiableSettingCard(
                icon = Icons.Default.VisibilityOff,
                title = "Hide Names of Favorites",
                subtitle = "Show icons without app labels",
                checked = prefs.hideFavoriteNames,
                onCheckedChange = {
                    repos.prefs.launchUpdate { p -> p.copy(hideFavoriteNames = it) }
                },
            )
            DiableSettingCard(
                icon = Icons.Default.Image,
                title = "Dim Wallpaper",
                subtitle = "Makes text easier to read in your all apps list",
                checked = prefs.dimWallpaper,
                onCheckedChange = {
                    repos.prefs.launchUpdate { p -> p.copy(dimWallpaper = it) }
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** The Dark Theme radio list. Rows carry no icon — labels sit flush at the sheet margin. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkThemeSheet(onDismiss: () -> Unit) {
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val scope = rememberCoroutineScope()

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
            Text(
                text = "Dark Theme",
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                fontSize = 23.sp,
                color = DiableText,
            )
            DarkThemeMode.entries.forEach { mode ->
                RadioRow(
                    label = mode.label,
                    selected = prefs.darkThemeMode == mode,
                    onClick = {
                        repos.prefs.launchUpdate { p -> p.copy(darkThemeMode = mode) }
                    },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .heightIn(min = 64.dp)
                    .background(DiableCard, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pitch-Black Theme", color = DiableText, fontSize = 17.sp)
                    Text(
                        text = "Requires active dark theme",
                        color = DiableTextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                DiableToggle(
                    checked = prefs.pitchBlackTheme,
                    onCheckedChange = {
                        repos.prefs.launchUpdate { p -> p.copy(pitchBlackTheme = it) }
                    },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .heightIn(min = 64.dp)
            .background(DiableCard, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = DiableText, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) DiableAccent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF132569)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(DiableText.copy(alpha = 0.18f)),
                )
            }
        }
    }
}
