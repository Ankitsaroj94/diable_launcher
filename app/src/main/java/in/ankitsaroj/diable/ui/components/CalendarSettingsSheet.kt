package `in`.ankitsaroj.diable.ui.components

import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.launch

/**
 * The Calendar sub-sheet from the Diable widget sheet. "Show date" carries no icon and
 * sits flush at the sheet margin; the other two rows use the normal card layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsSheet(onDismiss: () -> Unit) {
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    var showFilter by remember { mutableStateOf(false) }
    if (showFilter) {
        CalendarFilterSheet(onDismiss = { showFilter = false })
        return
    }

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
            Text(
                text = "Calendar",
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                fontSize = 23.sp,
                color = DiableText,
            )
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
                    Text("Show date", color = DiableText, fontSize = 17.sp)
                    Text(
                        text = "Tapping on the date shows your calendar agenda",
                        color = DiableTextMuted,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                DiableToggle(
                    checked = prefs.showDate,
                    onCheckedChange = {
                        repos.prefs.launchUpdate { p -> p.copy(showDate = it) }
                    },
                )
            }
            DiableSettingCard(
                icon = Icons.Default.Event,
                title = "Show upcoming events",
                checked = prefs.showUpcomingEvents,
                onCheckedChange = {
                    repos.prefs.launchUpdate { p ->
                            p.copy(showUpcomingEvents = it, calendarPreview = it)
                        }
                },
            )
            DiableSettingCard(
                icon = Icons.Default.FilterList,
                title = "Filter events",
                subtitle = if (prefs.hiddenCalendarIds.isEmpty()) "All calendars" else "${prefs.hiddenCalendarIds.size} hidden",
                onClick = { showFilter = true },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Filter events: choose which calendars feed the home preview and the agenda. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarFilterSheet(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val repo = remember { `in`.ankitsaroj.diable.data.CalendarRepository(context) }
    val calendars by androidx.compose.runtime.produceState<List<`in`.ankitsaroj.diable.data.CalendarSource>?>(null) {
        value = repo.listCalendars()
    }
    androidx.activity.compose.BackHandler(onBack = onDismiss)

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
            Text(
                text = "Filter events",
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                fontSize = 23.sp,
                color = DiableText,
            )
            val list = calendars
            when {
                !repo.hasPermission() -> Text(
                    "Allow calendar access to choose calendars.",
                    color = DiableTextMuted,
                    fontSize = 14.sp,
                )
                list == null -> Text("Loading…", color = DiableTextMuted, fontSize = 14.sp)
                list.isEmpty() -> Text("No calendars on this device.", color = DiableTextMuted, fontSize = 14.sp)
                else -> list.forEach { cal ->
                    val shown = cal.id !in prefs.hiddenCalendarIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .heightIn(min = 56.dp)
                            .background(DiableCard, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(androidx.compose.ui.graphics.Color(cal.color), androidx.compose.foundation.shape.CircleShape),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cal.name, color = DiableText, fontSize = 16.sp)
                            if (cal.account.isNotBlank() && cal.account != cal.name) {
                                Text(cal.account, color = DiableTextMuted, fontSize = 13.sp)
                            }
                        }
                        DiableToggle(
                            checked = shown,
                            onCheckedChange = { on ->
                                repos.prefs.launchUpdate { p ->
                                    p.copy(
                                        hiddenCalendarIds = if (on) p.hiddenCalendarIds - cal.id
                                        else p.hiddenCalendarIds + cal.id,
                                    )
                                }
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
