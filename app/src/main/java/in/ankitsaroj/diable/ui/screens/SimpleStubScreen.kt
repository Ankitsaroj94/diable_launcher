package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.ui.components.LavenderIconCircle
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.screens.settings.LinkListScreen
import `in`.ankitsaroj.diable.ui.screens.settings.LinkRow
import `in`.ankitsaroj.diable.ui.screens.settings.HowToScreen
import `in`.ankitsaroj.diable.ui.screens.settings.openStore
import `in`.ankitsaroj.diable.ui.screens.settings.sendFeedback
import `in`.ankitsaroj.diable.ui.screens.settings.shareText
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted

@Composable
fun SimpleStubScreen(
    title: String,
    onBack: () -> Unit,
    body: String = "Coming soon.",
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .verticalScroll(rememberScrollState()),
    ) {
        PillHeader(title)
        Text(
            text = body,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = DiableTextMuted,
        )
    }
}

/** Pro is free here: the same "Discover features" list, every entry marked included. */
@Composable
fun DiableProScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val features = listOf(
        Triple(Icons.Default.Palette, "All themes", "Every ready-made and community theme"),
        Triple(
            Icons.Default.Tune,
            "Full theme customization",
            "Icons and icon packs, fonts, font size, theme colors and clock styles",
        ),
        Triple(
            Icons.Default.CalendarMonth,
            "Calendar agenda & weather forecast",
            "Tap the date or the weather on your home screen",
        ),
        Triple(
            Icons.Default.Widgets,
            "Pop-ups & widget stacks",
            "Group apps, shortcuts and widgets behind a single swipe",
        ),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
    ) {
        PillHeader("Diable Pro")
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth()
                .background(DiableCard, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LavenderIconCircle(Icons.Default.WorkspacePremium, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Pro is unlocked", color = DiableText, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Every Pro feature is included for free.",
                    color = DiableTextMuted,
                    fontSize = 14.sp,
                )
            }
        }
        Text(
            text = "Discover features",
            color = DiableText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp),
        )
        features.forEach { (icon, title, subtitle) ->
            FeatureRow(icon = icon, title = title, subtitle = subtitle)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LavenderIconCircle(icon, size = 36.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DiableText, fontSize = 17.sp)
            Text(subtitle, color = DiableTextMuted, fontSize = 14.sp, lineHeight = 19.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Default.Check, contentDescription = "Included", tint = DiableAccentText)
    }
}

/** Help lives in the app: gesture tips, and feedback sent through the share sheet. */
@Composable
fun HelpFeedbackScreen(onBack: () -> Unit) {
    var showHowTo by remember { mutableStateOf(false) }
    if (showHowTo) {
        HowToScreen(onBack = { showHowTo = false })
        return
    }
    LinkListScreen(
        title = "Help & Feedback",
        onBack = onBack,
        rows = listOf(
            LinkRow("How-To's", Icons.AutoMirrored.Filled.MenuBook, "Gestures and tips for your launcher") {
                showHowTo = true
            },
            LinkRow(
                "Send feedback",
                Icons.Default.Email,
                "Report a bug or suggest a feature, with your device info attached",
            ) { sendFeedback(it) },
        ),
    )
}

@Composable
fun CommunityScreen(onBack: () -> Unit) {
    var showCredits by remember { mutableStateOf(false) }
    LinkListScreen(
        title = "Community",
        onBack = onBack,
        rows = listOf(
            LinkRow("Invite a friend", Icons.Default.PersonAdd, "Share the launcher with a friend") {
                shareText(
                    it,
                    "I've been using this minimal launcher and love it: " +
                        "https://play.google.com/store/apps/details?id=${it.packageName}",
                )
            },
            LinkRow("Rate us", Icons.Default.Star) { openStore(it, it.packageName) },
            LinkRow("Credits", Icons.Default.Groups) { showCredits = true },
        ),
    )
    if (showCredits) {
        AlertDialog(
            onDismissRequest = { showCredits = false },
            containerColor = DiableCard,
            title = { Text("Credits", color = DiableText) },
            text = {
                Text(
                    "Diable Launcher by Ankit Saroj.\n\n" +
                        "Wallpapers from Openverse, used under their Creative Commons licenses. " +
                        "Fonts: Quicksand, Nunito, Space Grotesk, Playfair Display and Outfit " +
                        "(SIL Open Font License).",
                    color = DiableTextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = { showCredits = false }) { Text("Okay", color = DiableAccentText) }
            },
        )
    }
}

private data class ChangelogEntry(val version: String, val date: String, val changes: List<String>)

private val Changelog = listOf(
    ChangelogEntry(
        "1.4", "Sep 2026",
        listOf(
            "Icon packs: 11 built-in styles that follow your theme color, plus support for installed ADW/Nova icon packs",
            "Per-app renames and icons from the long-press menu",
            "Theme color picker with wallpaper and Material You colors",
            "Font size and dark text options in the theme editor",
        ),
    ),
    ChangelogEntry(
        "1.3", "Sep 2026",
        listOf(
            "Media player controls now drive the playing app, with album art",
            "Notification access powers notification dots and app pop-ups",
            "Quick Lock: double-tap the alphabet to lock your phone",
            "Allow Home Screen Rotation now works",
        ),
    ),
    ChangelogEntry(
        "1.2", "Sep 2026",
        listOf(
            "Usage Breaker reminds you to take a break from leisure apps",
            "Help & Feedback, Community and privacy settings",
            "Backup & Restore saves to a file you choose",
        ),
    ),
    ChangelogEntry(
        "1.1", "Sep 2026",
        listOf(
            "Wallpapers from Openverse with creator credits",
            "Ready-made themes applied in one tap",
            "Much faster home screen: apps load once, icons decode once",
        ),
    ),
    ChangelogEntry(
        "1.0", "Aug 2026",
        listOf("First release: favorites, alphabet scrubber, search, clock styles and widgets"),
    ),
)

@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
    ) {
        PillHeader("Changelog")
        Changelog.forEach { entry ->
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .background(DiableCard, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Version ${entry.version}",
                        color = DiableText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(entry.date, color = DiableTextMuted, fontSize = 13.sp)
                }
                entry.changes.forEach { change ->
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Text("•", color = DiableAccentText, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(change, color = DiableTextMuted, fontSize = 14.sp, lineHeight = 19.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun FoldersStubScreen(onBack: () -> Unit) {
    SimpleStubScreen(title = "Folders", onBack = onBack)
}

@Composable
fun RouteStubScreen(name: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg),
    ) {
        PillHeader(name)
        Text(
            text = name,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = DiableText,
        )
    }
}
