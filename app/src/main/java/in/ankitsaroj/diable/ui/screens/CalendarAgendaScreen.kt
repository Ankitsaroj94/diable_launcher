package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.content.Intent
import android.provider.CalendarContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.CalendarEvent
import `in`.ankitsaroj.diable.data.CalendarRepository
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The agenda reached by tapping the date on the home screen: upcoming events for the next
 * month, grouped by day, each opening in the system calendar.
 */
@Composable
fun CalendarAgendaScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val repo = remember(context) { CalendarRepository(context) }
    val scope = rememberCoroutineScope()

    val prefs by LocalDiableRepos.current.prefs.prefsFlow
        .collectAsState(initial = `in`.ankitsaroj.diable.data.DiablePrefs())
    val hidden = prefs.hiddenCalendarIds
    var granted by remember { mutableStateOf(repo.hasPermission()) }
    var events by remember { mutableStateOf<List<CalendarEvent>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        if (result) scope.launch { events = repo.getUpcomingEvents(hidden = hidden) }
    }

    LaunchedEffect(granted, hidden) {
        events = if (granted) repo.getUpcomingEvents(hidden = hidden) else emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        PillHeader("Calendar agenda")

        val list = events
        when {
            !granted -> AgendaMessage(
                text = "Allow calendar access to see your upcoming events here.",
                actionLabel = "Allow access",
                onAction = {
                    permissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                },
            )

            list == null -> AgendaMessage(text = "Loading…")

            list.isEmpty() -> AgendaMessage(
                text = "Nothing scheduled in the next 30 days.",
            )

            else -> {
                val dayFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
                val grouped = remember(list) {
                    list.groupBy { dayFormat.format(Date(it.beginMillis)) }
                }
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    grouped.forEach { (day, dayEvents) ->
                        item(key = "hdr_$day") {
                            Text(
                                text = day,
                                color = DiableText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
                            )
                        }
                        dayEvents.forEach { event ->
                            item(key = "ev_${event.id}_${event.beginMillis}") {
                                AgendaRow(
                                    event = event,
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW).setData(
                                                    android.content.ContentUris.withAppendedId(
                                                        CalendarContract.Events.CONTENT_URI,
                                                        event.id,
                                                    ),
                                                ),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(48.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AgendaRow(event: CalendarEvent, onClick: () -> Unit) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(DiableCard, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (event.calendarColor != 0) {
                        Color(event.calendarColor)
                    } else {
                        DiableAccent
                    },
                ),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, color = DiableText, fontSize = 16.sp)
            val when_ = if (event.allDay) {
                "All day"
            } else {
                "${timeFormat.format(Date(event.beginMillis))} – " +
                    timeFormat.format(Date(event.endMillis))
            }
            Text(
                text = listOfNotNull(when_, event.location).joinToString(" · "),
                color = DiableTextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun AgendaMessage(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        Text(
            text = text,
            color = DiableTextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = actionLabel,
                color = DiableAccentText,
                fontSize = 16.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}
