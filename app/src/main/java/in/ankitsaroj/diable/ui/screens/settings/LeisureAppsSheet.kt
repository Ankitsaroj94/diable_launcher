package `in`.ankitsaroj.diable.ui.screens.settings

import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.ui.components.AppIcon
import `in`.ankitsaroj.diable.ui.components.DiableBottomSheet
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableOnAccent
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted

private val Intervals = listOf(5, 10, 15, 30)

/**
 * Usage Breaker setup: which apps count as leisure, and how long before a reminder.
 * [onDone] receives the chosen apps and minutes; dismissing without Done keeps nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeisureAppsSheet(
    onDismiss: () -> Unit,
    onDone: (Set<String>, Int) -> Unit,
) {
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val apps by repos.apps.apps.collectAsState()
    LaunchedEffect(Unit) { repos.apps.ensureLoaded() }

    var selected by remember { mutableStateOf<Set<String>?>(null) }
    var minutes by remember { mutableIntStateOf(0) }
    // Seed from prefs once they arrive, not from the empty initial value.
    LaunchedEffect(prefs) {
        if (selected == null) {
            selected = prefs.leisurePackages
            minutes = prefs.usageBreakerMinutes
        }
    }
    val chosen = selected.orEmpty()
    val iconStyle = IconStyle.fromKey(prefs.iconStyleKey)
    val accent = Color(prefs.accentColorArgb)

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                text = "Usage Breaker",
                modifier = Modifier.padding(start = 8.dp, top = 16.dp),
                fontSize = 23.sp,
                color = DiableText,
            )
            Text(
                text = "Pick your leisure apps. After the time below you'll get a reminder to take a break.",
                color = DiableTextMuted,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 12.dp),
            )
            Row(
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Intervals.forEach { m ->
                    val on = m == minutes
                    Text(
                        text = "$m min",
                        color = if (on) DiableOnAccent else DiableText,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (on) DiableAccent else DiableCard)
                            .clickable { minutes = m }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    val on = app.packageName in chosen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (on) chosen - app.packageName else chosen + app.packageName
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app = app, size = 36.dp, iconStyle = iconStyle, accentColor = accent)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(app.name, color = DiableText, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (on) DiableAccent else DiableText.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (on) {
                                Icon(Icons.Default.Check, null, tint = DiableOnAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Text(
                text = if (chosen.isEmpty()) "Done · no apps" else "Done · ${chosen.size} selected",
                color = DiableOnAccent,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DiableAccent)
                    .clickable { onDone(chosen, minutes) }
                    .padding(vertical = 14.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
