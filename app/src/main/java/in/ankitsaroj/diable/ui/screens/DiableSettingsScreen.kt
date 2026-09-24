package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.navigation.Routes
import `in`.ankitsaroj.diable.ui.components.DiableSettingsRow
import `in`.ankitsaroj.diable.ui.theme.ClockCardEnd
import `in`.ankitsaroj.diable.ui.theme.ClockCardStart
import `in`.ankitsaroj.diable.ui.theme.DiableBg

@Composable
fun DiableSettingsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Diable's expanded header: a 120dp mark, then the screen title, then the list.
        Spacer(modifier = Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(ClockCardStart, ClockCardEnd))),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "D", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Diable settings",
            fontSize = 26.sp,
            color = DiableText,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Column(modifier = Modifier.fillMaxSize()) {
            DiableSettingsRow(
                title = "Productivity",
                icon = Icons.Default.Speed,
                summary = "Stay efficient and organized",
                onClick = { onNavigate(Routes.Productivity) },
            )
            DiableSettingsRow(
                title = "Themes",
                icon = Icons.Default.Palette,
                summary = "Give your home screen your personal touch",
                onClick = { onNavigate(Routes.Themes) },
            )
            DiableSettingsRow(
                title = "Diable Pro",
                icon = Icons.Default.Star,
                onClick = { onNavigate(Routes.DiablePro) },
            )
            DiableSettingsRow(
                title = "Help & Feedback",
                icon = Icons.AutoMirrored.Filled.Help,
                onClick = { onNavigate(Routes.HelpFeedback) },
            )
            DiableSettingsRow(
                title = "Community",
                icon = Icons.Default.Groups,
                onClick = { onNavigate(Routes.Community) },
            )
            DiableSettingsRow(
                title = "Advanced",
                icon = Icons.Default.Settings,
                onClick = { onNavigate(Routes.Advanced) },
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
