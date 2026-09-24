package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import `in`.ankitsaroj.diable.data.CalendarRepository
import `in`.ankitsaroj.diable.data.AppActions
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.MediaRepository
import `in`.ankitsaroj.diable.data.PopupFolder
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.data.WeatherRepository
import `in`.ankitsaroj.diable.data.wellbeing.UsageBreaker
import `in`.ankitsaroj.diable.model.AppInfo
import `in`.ankitsaroj.diable.navigation.Routes
import `in`.ankitsaroj.diable.service.DiableAccessibilityService
import `in`.ankitsaroj.diable.service.DiableNotificationListener
import `in`.ankitsaroj.diable.ui.components.AlphabetScrubber
import `in`.ankitsaroj.diable.ui.components.EventsPromoSheet
import `in`.ankitsaroj.diable.ui.components.FavoriteEntry
import `in`.ankitsaroj.diable.ui.components.HomeListActions
import `in`.ankitsaroj.diable.ui.components.DiableBottomSheet
import `in`.ankitsaroj.diable.ui.components.DiableWidgetSheet
import `in`.ankitsaroj.diable.ui.components.WidgetContextSheet
import `in`.ankitsaroj.diable.ui.home.AppMenuSheet
import `in`.ankitsaroj.diable.ui.home.AppPopupOverlay
import `in`.ankitsaroj.diable.ui.home.ButtonAction
import `in`.ankitsaroj.diable.ui.home.ClockWidget
import `in`.ankitsaroj.diable.ui.home.FolderEditSheet
import `in`.ankitsaroj.diable.ui.home.FolderMenuSheet
import `in`.ankitsaroj.diable.ui.home.HomePromo
import `in`.ankitsaroj.diable.ui.home.HomePromoRow
import `in`.ankitsaroj.diable.ui.home.MediaBar
import `in`.ankitsaroj.diable.ui.home.DiableButton
import `in`.ankitsaroj.diable.ui.home.DiableButtonSheet
import `in`.ankitsaroj.diable.ui.home.PopupRequest
import `in`.ankitsaroj.diable.ui.home.WidgetStack
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import `in`.ankitsaroj.diable.ui.theme.wallpaperBackground
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/** Requests other screens make of the home screen. */
object HomeCommands {
    /** "Move Widget": show the drag handle under the clock/widget block. */
    val moveWidget = kotlinx.coroutines.flow.MutableStateFlow(false)

    /** Return the list to favorites at rest (HOME from an app or a launcher screen). */
    val resetList = kotlinx.coroutines.flow.MutableStateFlow(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    /** Bumped when HOME is pressed while the home screen is already showing. */
    homePresses: SharedFlow<Unit>? = null,
) {
    val context = LocalContext.current
    // Share the app-wide repositories rather than constructing a second set per screen.
    val repos = LocalDiableRepos.current
    val prefsRepo = repos.prefs
    val appRepo = repos.apps
    val calendarRepo = remember(context) { CalendarRepository(context) }
    val weatherRepo = remember { WeatherRepository() }
    val mediaRepo = remember(context) { MediaRepository(context) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val prefs by prefsRepo.prefsFlow.collectAsState(initial = DiablePrefs())
    val apps by appRepo.apps.collectAsState()
    LaunchedEffect(Unit) { appRepo.ensureLoaded() }
    // Session access only exists once notification access is granted, so the flow is
    // rebuilt when the listener connects — and only then, never per recomposition.
    val listenerConnected by DiableNotificationListener.isConnected.collectAsState()
    val mediaFlow = remember(mediaRepo, listenerConnected) { mediaRepo.activeMediaFlow() }
    val media by mediaFlow.collectAsState(initial = null)
    val notifications by DiableNotificationListener.notifications.collectAsState()

    var weatherTemp by remember { mutableStateOf<Float?>(null) }
    var calendarEvent by remember {
        mutableStateOf<`in`.ankitsaroj.diable.data.CalendarEventPreview?>(null)
    }

    LaunchedEffect(prefs.weatherEnabled, prefs.weatherCityName, prefs.weatherLat, prefs.weatherLon) {
        weatherTemp = if (prefs.weatherEnabled) weatherRepo.currentTemperatureF(context, prefs) else null
    }
    LaunchedEffect(prefs.showUpcomingEvents, prefs.calendarPreview, prefs.hiddenCalendarIds) {
        if (prefs.showUpcomingEvents && prefs.calendarPreview) {
            calendarEvent = calendarRepo.getNextUpcomingEvent(prefs.hiddenCalendarIds)
        }
    }

    val accent = Color(prefs.accentColorArgb)
    val iconStyle = IconStyle.fromKey(prefs.iconStyleKey)
    val textColor = if (prefs.darkText) Color(0xFF111318) else Color.White

    val visibleApps = remember(apps, prefs.hiddenPackages, prefs.sortByUsage, prefs.usageCounts) {
        val filtered = apps.filter { it.packageName !in prefs.hiddenPackages }
        if (prefs.sortByUsage) {
            // Sorting within each letter: grouping later keeps letters in A–Z order.
            filtered.sortedWith(
                compareBy<AppInfo> { it.name.uppercase().firstOrNull() ?: ' ' }
                    .thenByDescending { prefs.usageCounts[it.packageName] ?: 0 }
                    .thenBy { it.name.uppercase() },
            )
        } else {
            filtered
        }
    }
    val favorites = remember(visibleApps, prefs.favoritePackages, prefs.folders, prefs.suggestOftenUsed, prefs.usageCounts) {
        val byPackage = visibleApps.associateBy { it.packageName }
        val chosen = prefs.favoritePackages.mapNotNull { key ->
            if (key.startsWith(PopupFolder.FOLDER_PREFIX)) {
                prefs.folders.firstOrNull { it.favoriteKey == key }?.let { folder ->
                    FavoriteEntry.Folder(folder, folder.packages.mapNotNull { byPackage[it] })
                }
            } else {
                byPackage[key]?.let { FavoriteEntry.App(it) }
            }
        }
        // "Suggest Often-Used Apps": the most-launched apps that aren't favorites yet.
        val suggestions = if (prefs.suggestOftenUsed) {
            prefs.usageCounts.entries
                .filter { it.value >= 3 && it.key !in prefs.favoritePackages }
                .sortedByDescending { it.value }
                .mapNotNull { byPackage[it.key] }
                .take(2)
                .map { FavoriteEntry.App(it) }
        } else {
            emptyList()
        }
        chosen + suggestions
    }

    var showWidgetSheet by remember { mutableStateOf(false) }
    var showDiableWidgetSheet by remember { mutableStateOf(false) }
    var showEventsPromo by remember { mutableStateOf(false) }
    var showButtonSheet by remember { mutableStateOf(false) }
    var showButtonSetup by remember { mutableStateOf(false) }
    var showAccessPrompt by remember { mutableStateOf<String?>(null) }
    var widgetProviderPackage by remember { mutableStateOf<String?>(null) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var menuFolder by remember { mutableStateOf<PopupFolder?>(null) }
    var editFolder by remember { mutableStateOf<PopupFolder?>(null) }
    var popup by remember { mutableStateOf<PopupRequest?>(null) }
    var originY by remember { mutableFloatStateOf(0f) }

    val listState = rememberLazyListState()
    val moveMode by HomeCommands.moveWidget.collectAsState()
    // Live position while dragging; committed to prefs when the drag ends.
    var dragTopDp by remember { mutableStateOf<Float?>(null) }
    val clockTop = dragTopDp ?: prefs.clockTopDp.toFloat()
    // Widget height: Fill screen > the user's resize > the widget's own minimum size.
    // Left to wrap_content, most RemoteViews stretch and push favorites off screen.
    val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val naturalWidgetDp = remember(prefs.widgetAppWidgetId) {
        runCatching {
            val info = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetInfo(prefs.widgetAppWidgetId)
            (info.minHeight / context.resources.displayMetrics.density).toInt()
        }.getOrDefault(120).coerceIn(80, 240)
    }
    val widgetHeightDp = when {
        prefs.fillScreenWidget -> (screenHeightDp * 0.5f).toInt()
        prefs.widgetHeightDp > 0 -> prefs.widgetHeightDp
        else -> naturalWidgetDp
    }
    val widgetFill = Modifier.height(widgetHeightDp.dp)
    val scrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }

    // Long-pressing the widget itself opens its context menu (move/resize/remove).
    var menuWidgetId by remember { mutableStateOf(-1) }
    val openWidgetMenu = { id: Int ->
        menuWidgetId = id
        widgetProviderPackage = try {
            android.appwidget.AppWidgetManager.getInstance(context)
                .getAppWidgetInfo(id)?.provider?.packageName
        } catch (_: Exception) {
            null
        }
        showWidgetSheet = true
    }
    // The home widget slot: the main widget, then any stacked ones (swiped sideways).
    // Only ids still bound to a provider: restored or migrated settings can carry ids
    // this install never bound, which would render as empty boxes.
    val widgetIds = remember(prefs.widgetAppWidgetId, prefs.widgetStackIds) {
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        (listOf(prefs.widgetAppWidgetId) + prefs.widgetStackIds)
            .filter { it >= 0 && runCatching { manager.getAppWidgetInfo(it) }.getOrNull() != null }
    }

    val onLaunch: (AppInfo) -> Unit = { app ->
        prefsRepo.launchUpdate { p ->
                val count = (p.usageCounts[app.packageName] ?: 0) + 1
                p.copy(usageCounts = p.usageCounts + (app.packageName to count))
            }
        if (AppActions.launch(context, app.packageName)) {
            UsageBreaker.onAppLaunched(context, app.packageName, prefs)
        }
    }

    fun runButtonAction(key: String?) {
        when (val action = ButtonAction.fromKey(key)) {
            ButtonAction.Search -> onNavigate(Routes.Search)
            ButtonAction.Agenda -> onNavigate(Routes.CalendarAgenda)
            ButtonAction.Assistant -> runCatching {
                context.startActivity(Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ButtonAction.Notifications -> expandNotificationShade(context)
            ButtonAction.LockScreen -> DiableAccessibilityService.lockScreen(context)
            null -> if (key == null) {
                showButtonSetup = true
            } else {
                apps.firstOrNull { it.packageName == key.removePrefix("app:") }?.let(onLaunch)
            }
        }
    }

    // HOME while already home: scrolled → back to favorites, else toggle search (Diable).
    // State, not an event: home may not be composed when the request is made.
    val resetRequested by HomeCommands.resetList.collectAsState()
    LaunchedEffect(resetRequested) {
        if (!resetRequested) return@LaunchedEffect
        popup = null
        menuApp = null
        menuFolder = null
        listState.scrollToItem(0)
        HomeCommands.resetList.value = false
    }
    LaunchedEffect(homePresses) {
        homePresses?.collect {
            popup = null
            if (scrolled) listState.animateScrollToItem(0) else onNavigate(Routes.Search)
        }
    }
    // BACK on home must never leave the launcher; it only unwinds the list.
    BackHandler(enabled = true) {
        when {
            moveMode -> {
                dragTopDp = null
                HomeCommands.moveWidget.value = false
            }
            popup != null -> popup = null
            scrolled -> scope.launch { listState.animateScrollToItem(0) }
        }
    }

    // Music Apps: when headphones or a speaker connect, offer the chosen players.
    MusicAppsOnHeadphones(enabled = prefs.musicAppPackages.isNotEmpty()) {
        val musicApps = apps.filter { it.packageName in prefs.musicAppPackages }
        if (musicApps.isNotEmpty()) {
            popup = PopupRequest.AppList("Music apps", musicApps, originY + with(density) { 360.dp.toPx() })
        }
    }

    val actions = remember(onNavigate, prefs.swipeUpToSearch, prefs.quickLock, apps) {
        HomeListActions(
            onLaunch = onLaunch,
            onLongPress = { menuApp = it },
            onSwipeRight = { app, y -> popup = PopupRequest.App(app, y) },
            onFolderOpen = { folder, y ->
                val byPackage = apps.associateBy { it.packageName }
                popup = PopupRequest.Folder(folder, folder.packages.mapNotNull { byPackage[it] }, y)
            },
            onFolderLongPress = { menuFolder = it },
            onRecentlyInstalled = { y ->
                val recent = apps.sortedByDescending { it.firstInstallTime }.take(8)
                popup = PopupRequest.AppList("Recently installed", recent, y)
            },
            onSettings = { onNavigate(Routes.DiableSettings) },
            onSwipeUpAtTop = { if (prefs.swipeUpToSearch) onNavigate(Routes.Search) },
            onSwipeDownAtTop = { expandNotificationShade(context) },
            onStarLongPress = { onNavigate(Routes.EditFavorites) },
            onAlphabetDoubleTap = {
                if (prefs.quickLock) {
                    if (!DiableAccessibilityService.lockScreen(context)) showAccessPrompt = "lock"
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // The window is transparent with windowShowWallpaper, so the live system
            // wallpaper shows through unless the user picked a gradient in Themes.
            .wallpaperBackground(prefs.wallpaperUri)
            .then(
                if (prefs.dimWallpaper) {
                    Modifier.background(Color.Black.copy(alpha = 0.35f))
                } else {
                    Modifier
                },
            )
            // Text lives in the left column, so bias a scrim there. This keeps the clock
            // and labels legible over busy wallpapers without flattening the whole image.
            .background(
                Brush.horizontalGradient(
                    0.0f to Color.Black.copy(alpha = if (prefs.darkText) 0f else 0.45f),
                    0.55f to Color.Black.copy(alpha = if (prefs.darkText) 0f else 0.12f),
                    1.0f to Color.Transparent,
                ),
            )
            .windowInsetsPadding(
                if (prefs.hideStatusBar) WindowInsets.navigationBars else WindowInsets.systemBars,
            )
            .onGloballyPositioned { originY = it.positionInRoot().y },
    ) {
        // Every home label sits directly on the wallpaper, so give them all a soft
        // shadow. Dimming alone cannot win against a high-contrast photo.
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(
                shadow = if (prefs.darkText) {
                    null
                } else {
                    Shadow(color = Color.Black.copy(alpha = 0.75f), offset = Offset(0f, 1.5f), blurRadius = 6f)
                },
            ),
        ) {
            AlphabetScrubber(
                favorites = favorites,
                apps = visibleApps,
                listState = listState,
                actions = actions,
                notifications = notifications,
                accentColor = accent,
                textColor = textColor,
                hideAlphabet = prefs.hideAlphabet,
                hideFavoriteNames = prefs.hideFavoriteNames,
                iconStyle = iconStyle,
                allowHaptic = prefs.allowHaptic,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Diable drops the clock ~125dp below the status bar; Move Widget
                        // lets the user shift it.
                        .padding(top = clockTop.dp, bottom = 16.dp),
                ) {
                    if (prefs.replaceClockWithWidget && widgetIds.isNotEmpty()) {
                        WidgetStack(
                            ids = widgetIds,
                            accent = accent,
                            onLongPress = openWidgetMenu,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .then(widgetFill),
                        )
                    } else {
                        ClockWidget(
                            clockStyleId = prefs.clockStyleId,
                            showBattery = prefs.showBatteryPercentage,
                            showWeather = prefs.weatherEnabled,
                            showEvent = prefs.showUpcomingEvents && prefs.calendarPreview,
                            weatherTempF = weatherTemp,
                            event = calendarEvent,
                            accentColor = textColor,
                            showDate = prefs.showDate,
                            weatherCelsius = prefs.weatherCelsius,
                            onTimeClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            },
                            onDateClick = { onNavigate(Routes.CalendarAgenda) },
                            // Productivity → Weather Forecast gates the tap-for-forecast.
                            onWeatherClick = if (prefs.weatherForecast) {
                                { openWeatherForecast(context, prefs.weatherCityName) }
                            } else {
                                null
                            },
                            onLongClick = { showDiableWidgetSheet = true },
                        )
                        if (widgetIds.isNotEmpty()) {
                            WidgetStack(
                                ids = widgetIds,
                                accent = accent,
                                onLongPress = openWidgetMenu,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .then(widgetFill),
                            )
                        }
                    }
                    if (moveMode) {
                        MoveWidgetHandle(
                            onDrag = { deltaDp ->
                                dragTopDp = ((dragTopDp ?: prefs.clockTopDp.toFloat()) + deltaDp).coerceIn(0f, 420f)
                            },
                            onDragEnd = {
                                val committed = dragTopDp?.toInt()
                                if (committed != null) prefsRepo.launchUpdate { it.copy(clockTopDp = committed) }
                            },
                            onReset = {
                                dragTopDp = null
                                prefsRepo.launchUpdate {
                                    it.copy(clockTopDp = DiablePrefs().clockTopDp, widgetHeightDp = 0)
                                }
                            },
                            onResize = if (prefs.widgetAppWidgetId >= 0) {
                                { stepDp ->
                                    val next = (widgetHeightDp + stepDp).coerceIn(64, (screenHeightDp * 0.7f).toInt())
                                    prefsRepo.launchUpdate { it.copy(widgetHeightDp = next, fillScreenWidget = false) }
                                }
                            } else {
                                null
                            },
                            onDone = {
                                dragTopDp = null
                                HomeCommands.moveWidget.value = false
                            },
                        )
                    }
                    if (prefs.showMediaWidget && media != null) {
                        MediaBar(
                            media = media!!,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    // First-run rows between the date and the favorites; each disappears once used.
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HomePromo.entries
                            .filter { it.name !in prefs.dismissedPromos }
                            .forEach { promo ->
                                HomePromoRow(
                                    promo = promo,
                                    onAction = {
                                        prefsRepo.launchUpdate { p ->
                                                p.copy(dismissedPromos = p.dismissedPromos + promo.name)
                                            }
                                        when (promo) {
                                            HomePromo.Accessibility -> DiableAccessibilityService.openSettings(context)
                                            HomePromo.Pro -> onNavigate(Routes.DiablePro)
                                            HomePromo.Customize -> onNavigate(Routes.Themes)
                                            HomePromo.Productivity -> onNavigate(Routes.Productivity)
                                        }
                                    },
                                )
                            }
                    }
                }
            }
        }

        // The Diable button; once the list is scrolled it becomes "Search apps".
        if (prefs.diableButton) {
            val tapIcon = when {
                scrolled -> Icons.Default.Search
                prefs.buttonTapAction == null -> Icons.Default.Add
                else -> ButtonAction.fromKey(prefs.buttonTapAction)?.icon ?: Icons.Default.Apps
            }
            DiableButton(
                accentColor = accent,
                icon = tapIcon,
                onTap = { if (scrolled) onNavigate(Routes.Search) else runButtonAction(prefs.buttonTapAction) },
                onSwipeUp = { prefs.buttonSwipeAction?.let { runButtonAction(it) } },
                onLongPress = { showButtonSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 28.dp, bottom = 28.dp),
            )
        }

        popup?.let { request ->
            AppPopupOverlay(
                request = request,
                allApps = apps,
                notifications = notifications,
                iconStyle = iconStyle,
                accent = accent,
                originY = originY,
                onLaunch = onLaunch,
                onEditFolder = { editFolder = it },
                onDismiss = { popup = null },
            )
        }
    }

    menuApp?.let { app ->
        AppMenuSheet(
            app = app,
            iconStyle = iconStyle,
            accent = accent,
            onNavigate = onNavigate,
            onDismiss = { menuApp = null },
        )
    }
    menuFolder?.let { folder ->
        FolderMenuSheet(
            folder = folder,
            iconStyle = iconStyle,
            accent = accent,
            onNavigate = onNavigate,
            onDismiss = { menuFolder = null },
        )
    }
    editFolder?.let { folder ->
        FolderEditSheet(folder = folder, iconStyle = iconStyle, accent = accent, onDismiss = { editFolder = null })
    }
    if (showButtonSheet) {
        DiableButtonSheet(onNavigate = onNavigate, onDismiss = { showButtonSheet = false })
    }
    if (showButtonSetup) {
        AlertDialog(
            onDismissRequest = { showButtonSetup = false },
            containerColor = DiableCard,
            title = { Text("Set up the Diable Button", color = DiableText) },
            text = {
                Text(
                    "Long-press the button to choose what a tap and a swipe up do.",
                    color = DiableTextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showButtonSetup = false
                    showButtonSheet = true
                }) { Text("Set up", color = DiableAccentText) }
            },
            dismissButton = {
                TextButton(onClick = { showButtonSetup = false }) { Text("Okay", color = DiableTextMuted) }
            },
        )
    }
    if (showAccessPrompt == "lock") {
        AlertDialog(
            onDismissRequest = { showAccessPrompt = null },
            containerColor = DiableCard,
            title = { Text("Allow Quick Lock", color = DiableText) },
            text = {
                Text(
                    "To lock your screen, turn on this launcher under Accessibility → Installed apps.",
                    color = DiableTextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAccessPrompt = null
                    DiableAccessibilityService.openSettings(context)
                }) { Text("Open settings", color = DiableAccentText) }
            },
            dismissButton = {
                TextButton(onClick = { showAccessPrompt = null }) { Text("Cancel", color = DiableTextMuted) }
            },
        )
    }
    if (showWidgetSheet) {
        DiableBottomSheet(onDismissRequest = { showWidgetSheet = false }) {
            WidgetContextSheet(
                onNavigate = onNavigate,
                onDismiss = { showWidgetSheet = false },
                widgetProviderPackage = widgetProviderPackage,
                widgetId = menuWidgetId,
            )
        }
    }
    if (showDiableWidgetSheet) {
        DiableWidgetSheet(
            onDismissRequest = { showDiableWidgetSheet = false },
            onNavigate = onNavigate,
            onShowEventsPromo = { showEventsPromo = true },
        )
    }
    if (showEventsPromo) {
        EventsPromoSheet(onDismissRequest = { showEventsPromo = false })
    }
}

/** Pulls down the notification shade, like a swipe down on Diable's home. */
@android.annotation.SuppressLint("WrongConstant")
private fun expandNotificationShade(context: Context) {
    runCatching {
        val service = context.getSystemService("statusbar") ?: return
        Class.forName("android.app.StatusBarManager")
            .getMethod("expandNotificationsPanel")
            .invoke(service)
    }
}

private fun openWeatherForecast(context: Context, city: String?) {
    // Name the chosen city; with device location the search engine localises by itself.
    val query = android.net.Uri.encode(if (city != null) "weather $city" else "weather")
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=$query"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/**
 * Calls [onConnected] when wired or Bluetooth audio output appears. The device callback
 * also fires once for already-connected devices at registration, which is skipped.
 */
@Composable
private fun MusicAppsOnHeadphones(enabled: Boolean, onConnected: () -> Unit) {
    val context = LocalContext.current
    val latest by androidx.compose.runtime.rememberUpdatedState(onConnected)
    DisposableEffect(enabled) {
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return@DisposableEffect onDispose { }
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val headphoneTypes = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
        var initial = true
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                if (initial) {
                    initial = false
                    return
                }
                if (addedDevices.any { it.isSink && it.type in headphoneTypes }) latest()
            }
        }
        audio.registerAudioDeviceCallback(callback, android.os.Handler(android.os.Looper.getMainLooper()))
        onDispose { audio.unregisterAudioDeviceCallback(callback) }
    }
}

/** Diable's move bar: drag to shift the clock/widget block, ↺ to reset, ✓ to finish. */
@Composable
private fun MoveWidgetHandle(
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    /** Present when there's a widget to resize; called with a height step in dp. */
    onResize: ((Int) -> Unit)? = null,
) {
    val density = LocalDensity.current
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .padding(top = 12.dp, end = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DiableCard.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.IconButton(onClick = onReset) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Default.Refresh,
                contentDescription = "Reset position",
                tint = DiableText,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = onDragEnd,
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            onDrag(with(density) { amount.toDp().value })
                        },
                    )
                }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Default.UnfoldMore,
                contentDescription = "Drag to move",
                tint = DiableAccentText,
            )
        }
        if (onResize != null) {
            androidx.compose.material3.IconButton(onClick = { onResize(-24) }) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Default.Remove,
                    contentDescription = "Smaller widget",
                    tint = DiableText,
                )
            }
            androidx.compose.material3.IconButton(onClick = { onResize(24) }) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Default.Add,
                    contentDescription = "Bigger widget",
                    tint = DiableText,
                )
            }
        }
        androidx.compose.material3.IconButton(onClick = onDone) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Default.Check,
                contentDescription = "Done",
                tint = DiableText,
            )
        }
    }
}
