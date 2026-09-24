package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import `in`.ankitsaroj.diable.service.DiableAccessibilityService
import `in`.ankitsaroj.diable.service.DiableNotificationListener
import `in`.ankitsaroj.diable.ui.screens.settings.LeisureAppsSheet
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.navigation.Routes
import `in`.ankitsaroj.diable.ui.components.DiableSettingsRow
import `in`.ankitsaroj.diable.ui.components.DiableToggle
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import kotlinx.coroutines.launch

@Composable
fun ProductivityScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = `in`.ankitsaroj.diable.data.DiablePrefs())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showLeisure by remember { mutableStateOf(false) }
    var showSearchSettings by remember { mutableStateOf(false) }
    // A toggle waiting on special access: which access, and what to do once granted.
    var accessRequest by remember { mutableStateOf<AccessRequest?>(null) }
    var pending by remember { mutableStateOf<PendingEnable?>(null) }

    // The user grants access in system Settings, so finish the toggle when we come back.
    LifecycleResumeEffect(pending) {
        pending?.let { p ->
            if (p.access.isGranted(context)) {
                p.enable()
                pending = null
            }
        }
        onPauseOrDispose { }
    }

    /** Runs [enable] now if [access] is already granted, otherwise explains and asks. */
    fun withAccess(access: AccessRequest, enable: () -> Unit) {
        if (access.isGranted(context)) enable() else {
            pending = PendingEnable(access, enable)
            accessRequest = access
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
    ) {
        PillHeader("Productivity")
        ProductivitySection("Instant Access") {
            ToggleRow(
                title = "Quick Replies",
                subtitle = "Reply right from your home screen",
                checked = prefs.quickReplies,
            ) { on ->
                val apply = { repos.prefs.launchUpdate { p -> p.copy(quickReplies = on) } }
                if (on) withAccess(AccessRequest.Notifications) { apply() } else apply()
            }
            ChevronRow(
                title = "Calendar Agenda",
                subtitle = "Open your calendar agenda by tapping the date",
            ) { onNavigate(Routes.CalendarAgenda) }
            ToggleRow(
                title = "Calendar Preview",
                subtitle = "See upcoming events on your home screen",
                checked = prefs.calendarPreview,
            ) {
                repos.prefs.launchUpdate { p -> p.copy(calendarPreview = it, showUpcomingEvents = it) }
            }
            ToggleRow(
                title = "Weather Forecast",
                subtitle = "See the full forecast by tapping the current weather",
                checked = prefs.weatherForecast,
            ) {
                // Forecast only adds tap-for-forecast; whether weather shows at all is the
                // widget sheet's Weather switch.
                repos.prefs.launchUpdate { p -> p.copy(weatherForecast = it) }
            }
            ToggleRow(
                title = "Media Player",
                subtitle = "Control music and videos from your home screen",
                checked = prefs.mediaPlayer,
            ) { on ->
                val apply = {
                    repos.prefs.launchUpdate { p -> p.copy(mediaPlayer = on, showMediaWidget = on) }
                }
                if (on) withAccess(AccessRequest.Notifications) { apply() } else apply()
            }
            ChevronRow(
                title = "Music Apps",
                subtitle = "Show music apps when connecting headphones or speakers",
            ) { onNavigate(Routes.MusicApps) }
        }
        ProductivitySection("Digital Wellbeing") {
            DiableSettingsRow(
                title = "Usage Breaker",
                summary = if (prefs.usageBreaker) {
                    "${prefs.leisurePackages.size} leisure ${if (prefs.leisurePackages.size == 1) "app" else "apps"} · reminder after ${prefs.usageBreakerMinutes} min"
                } else {
                    "Get reminders to take breaks"
                },
                trailing = {
                    DiableToggle(checked = prefs.usageBreaker, onCheckedChange = { on ->
                        if (on) {
                            showLeisure = true
                        } else {
                            repos.prefs.launchUpdate { p -> p.copy(usageBreaker = false) }
                        }
                    })
                },
                // Tapping the row edits the leisure apps; the switch turns it on and off.
                onClick = { showLeisure = true },
            )
            ToggleRow(
                title = "Notification Summary",
                subtitle = "Bundle notifications to reduce distractions",
                checked = prefs.notificationSummary,
            ) { on ->
                val apply = { repos.prefs.launchUpdate { p -> p.copy(notificationSummary = on) } }
                if (on) withAccess(AccessRequest.Notifications) { apply() } else apply()
            }
        }
        ProductivitySection("App Organization") {
            ChevronRow(
                title = "Hide Apps",
                subtitle = "Hide apps from your app list",
            ) { onNavigate(Routes.HideApps) }
            ChevronRow(
                title = "Folders & Pop-Ups",
                subtitle = "Group apps to keep your app list tidy",
            ) { onNavigate(Routes.Folders) }
            ToggleRow(
                title = "Suggest Often-Used Apps",
                subtitle = "Directly access often-used apps from your favorites",
                checked = prefs.suggestOftenUsed,
            ) {
                repos.prefs.launchUpdate { p -> p.copy(suggestOftenUsed = it) }
            }
            ToggleRow(
                title = "Sort All Apps List by Usage",
                subtitle = "Prioritize most-used apps for each letter",
                checked = prefs.sortByUsage,
            ) {
                repos.prefs.launchUpdate { p -> p.copy(sortByUsage = it) }
            }
        }
        ProductivitySection("Smart Search") {
            ChevronRow(
                title = "Smart Search",
                subtitle = "Apps, contacts, and web in one place",
            ) { showSearchSettings = true }
        }
        ProductivitySection("Shortcuts & Gestures") {
            ToggleRow(
                title = "Diable Button",
                subtitle = "Run quick actions from your home screen",
                checked = prefs.diableButton,
            ) {
                repos.prefs.launchUpdate { p -> p.copy(diableButton = it) }
            }
            ToggleRow(
                title = "Quick Lock",
                subtitle = "Double-tap the alphabet to lock your phone",
                checked = prefs.quickLock,
            ) { on ->
                val apply = { repos.prefs.launchUpdate { p -> p.copy(quickLock = on) } }
                if (on) withAccess(AccessRequest.Accessibility) { apply() } else apply()
            }
        }
        ProductivitySection("Advanced") {
            ChevronRow(title = "Add Custom Widget") { onNavigate(Routes.SelectWidget) }
            ChevronRow(title = "Move Widget") {
                // Move mode lives on the home screen itself.
                HomeCommands.moveWidget.value = true
                onBack()
                onBack()
            }
            ToggleRow(
                title = "Show Battery Percentage",
                checked = prefs.showBatteryPercentage,
            ) {
                repos.prefs.launchUpdate { p -> p.copy(showBatteryPercentage = it) }
            }
            ToggleRow(
                title = "Allow Home Screen Rotation",
                subtitle = "When device is rotated",
                checked = prefs.allowRotation,
            ) {
                repos.prefs.launchUpdate { p -> p.copy(allowRotation = it) }
            }
            ToggleRow(
                title = "Allow Haptic Feedback",
                subtitle = "Follow system",
                checked = prefs.allowHaptic,
            ) {
                repos.prefs.launchUpdate { p -> p.copy(allowHaptic = it) }
            }
        }
    }

    if (showSearchSettings) {
        SearchSettingsSheet(onDismiss = { showSearchSettings = false })
    }
    if (showLeisure) {
        LeisureAppsSheet(
            onDismiss = { showLeisure = false },
            onDone = { apps, minutes ->
                showLeisure = false
                repos.prefs.launchUpdate { p ->
                        p.copy(usageBreaker = true, leisurePackages = apps, usageBreakerMinutes = minutes)
                    }
                // The reminder is a notification, which Android 13+ must be allowed to post.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }
    accessRequest?.let { request ->
        AlertDialog(
            onDismissRequest = {
                accessRequest = null
                pending = null
            },
            containerColor = DiableCard,
            title = { Text(request.title, color = DiableText) },
            text = { Text(request.body, color = DiableTextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    accessRequest = null
                    request.open(context)
                }) { Text(request.action, color = DiableAccentText) }
            },
            dismissButton = {
                TextButton(onClick = {
                    accessRequest = null
                    pending = null
                }) { Text("Keep disabled", color = DiableAccentText) }
            },
        )
    }
}

/** Special access some toggles need; granted in system Settings, not by a runtime prompt. */
private enum class AccessRequest(val title: String, val body: String, val action: String) {
    Notifications(
        "Allow notification access",
        "This feature reads your notifications and media sessions to show them on your " +
            "home screen. Nothing leaves your device.",
        "Allow notification access",
    ),
    Accessibility(
        "Allow accessibility access",
        "Quick Lock uses an accessibility service only to lock your screen when you " +
            "double-tap the alphabet. It reads no screen content.",
        "Open settings",
    ),
    ;

    fun isGranted(context: Context): Boolean = when (this) {
        Notifications -> DiableNotificationListener.isEnabled(context)
        Accessibility -> DiableAccessibilityService.isEnabled(context)
    }

    fun open(context: Context) = when (this) {
        Notifications -> DiableNotificationListener.openAccessSettings(context)
        Accessibility -> DiableAccessibilityService.openSettings(context)
    }
}

private class PendingEnable(val access: AccessRequest, val enable: () -> Unit)


@Composable
private fun ProductivitySection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 80.dp, top = 24.dp, bottom = 4.dp),
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = DiableText,
    )
    content()
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    onChange: (Boolean) -> Unit,
) {
    DiableSettingsRow(
        title = title,
        summary = subtitle,
        trailing = { DiableToggle(checked = checked, onCheckedChange = onChange) },
        onClick = { onChange(!checked) },
    )
}

@Composable
private fun ChevronRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    DiableSettingsRow(title = title, summary = subtitle, onClick = onClick)
}
