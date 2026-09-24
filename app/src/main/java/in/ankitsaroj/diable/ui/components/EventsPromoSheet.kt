package `in`.ankitsaroj.diable.ui.components

import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.ui.theme.ClockCardEnd
import `in`.ankitsaroj.diable.ui.theme.ClockCardStart
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsPromoSheet(onDismissRequest: () -> Unit) {
    val repos = LocalDiableRepos.current
    val scope = rememberCoroutineScope()
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val now = System.currentTimeMillis()

    DiableBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LavenderIconCircle(Icons.Default.AutoAwesome, size = 48.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Show upcoming events",
                style = MaterialTheme.typography.titleLarge,
                color = DiableText,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(ClockCardStart, ClockCardEnd)),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(16.dp),
            ) {
                Text(timeFmt.format(Date(now)), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text(dateFmt.format(Date(now)), color = Color.White.copy(alpha = 0.85f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("72°F · Sunny", color = Color.White.copy(alpha = 0.85f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Team standup · 10:00 AM",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DiableCard.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    color = DiableText,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "See your next calendar events on the home screen alongside the clock and weather.",
                color = DiableTextMuted,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = {
                    repos.prefs.launchUpdate { p ->
                            p.copy(showUpcomingEvents = true, calendarPreview = true)
                        }
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.linearGradient(listOf(DiableAccent, DiableAccent)),
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DiableText),
            ) {
                Text("Enable upcoming events")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Keep disabled",
                color = DiableTextMuted,
                modifier = Modifier.clickable { onDismissRequest() },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
