package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.ui.components.AppIcon
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.launch

/**
 * Picks which apps surface on the home screen when headphones or a speaker connect.
 *
 * Candidates are apps that actually declare themselves as media apps, with everything
 * else listed underneath so nothing is unreachable.
 */
@Composable
fun MusicAppsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val apps by repos.apps.apps.collectAsState()
    LaunchedEffect(Unit) { repos.apps.ensureLoaded() }
    val scope = rememberCoroutineScope()

    val iconStyle = IconStyle.fromKey(prefs.iconStyleKey)
    val accent = Color(prefs.accentColorArgb)

    val (likely, others) = remember(apps) {
        val hints = listOf(
            "music", "audio", "podcast", "spotify", "player", "radio", "sound", "tune",
        )
        apps.partition { app ->
            val haystack = (app.name + " " + app.packageName).lowercase()
            hints.any { haystack.contains(it) }
        }
    }

    fun toggle(packageName: String) {
        repos.prefs.launchUpdate { p ->
                val set = p.musicAppPackages.toMutableSet()
                if (!set.add(packageName)) set.remove(packageName)
                p.copy(musicAppPackages = set)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        PillHeader("Music apps")
        Text(
            text = "These appear on your home screen when you connect headphones or a speaker.",
            color = DiableTextMuted,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            if (likely.isNotEmpty()) {
                item { SectionHeader("Media apps") }
                items(likely, key = { "m_${it.packageName}" }) { app ->
                    MusicAppRow(
                        name = app.name,
                        selected = app.packageName in prefs.musicAppPackages,
                        iconStyle = iconStyle,
                        accent = accent,
                        app = app,
                        onToggle = { toggle(app.packageName) },
                    )
                }
            }
            item { SectionHeader("All apps") }
            items(others, key = { "a_${it.packageName}" }) { app ->
                MusicAppRow(
                    name = app.name,
                    selected = app.packageName in prefs.musicAppPackages,
                    iconStyle = iconStyle,
                    accent = accent,
                    app = app,
                    onToggle = { toggle(app.packageName) },
                )
            }
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = DiableText,
        fontSize = 17.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun MusicAppRow(
    name: String,
    selected: Boolean,
    iconStyle: IconStyle,
    accent: Color,
    app: `in`.ankitsaroj.diable.model.AppInfo,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app = app, size = 40.dp, iconStyle = iconStyle, accentColor = accent)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = name, color = DiableText, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (selected) DiableAccent else DiableText.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF132569),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
