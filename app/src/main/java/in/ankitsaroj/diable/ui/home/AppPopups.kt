@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package `in`.ankitsaroj.diable.ui.home

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import `in`.ankitsaroj.diable.data.AppActions
import `in`.ankitsaroj.diable.data.AppShortcut
import `in`.ankitsaroj.diable.data.IconPackRepository
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.InstalledIconPack
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.PopupFolder
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.model.AppInfo
import `in`.ankitsaroj.diable.navigation.Routes
import `in`.ankitsaroj.diable.service.AppNotification
import `in`.ankitsaroj.diable.service.DiableNotificationListener
import `in`.ankitsaroj.diable.ui.components.AppIcon
import `in`.ankitsaroj.diable.ui.components.DiableBottomSheet
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableSheet
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// ---------------------------------------------------------------------------------------
// Pop-up card (swipe right on an app, tap a folder, "Recently installed")
// ---------------------------------------------------------------------------------------

/** What the floating pop-up card is showing, and the row it hangs under (root px). */
sealed interface PopupRequest {
    val anchorY: Float

    data class App(val app: AppInfo, override val anchorY: Float) : PopupRequest
    data class Folder(val folder: PopupFolder, val apps: List<AppInfo>, override val anchorY: Float) : PopupRequest
    /** A titled list of apps: "Recently installed", or music apps when headphones connect. */
    data class AppList(val title: String, val apps: List<AppInfo>, override val anchorY: Float) : PopupRequest
}

/**
 * Diable's pop-up: a dark card under the row with a "↗ App" header, the app's waiting
 * notifications, its launcher shortcuts and any apps the user pinned to it. Tapping
 * outside closes it.
 */
@Composable
fun AppPopupOverlay(
    request: PopupRequest,
    allApps: List<AppInfo>,
    notifications: Map<String, List<AppNotification>>,
    iconStyle: IconStyle,
    accent: Color,
    originY: Float,
    onLaunch: (AppInfo) -> Unit,
    onEditFolder: (PopupFolder) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val density = LocalDensity.current
    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    var cardHeight by remember { mutableStateOf(0) }

    val shortcuts by produceState<List<AppShortcut>>(emptyList(), request) {
        value = if (request is PopupRequest.App) {
            withContext(Dispatchers.IO) { AppActions.shortcuts(context, request.app.packageName) }
        } else {
            emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        // Hang under the row, but flip above it when it would run off the bottom.
        val below = request.anchorY - originY
        val y = if (below + cardHeight > screenHeight - originY - 48f) {
            (below - cardHeight - with(density) { 64.dp.toPx() }).coerceAtLeast(0f)
        } else {
            below
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .offset { IntOffset(0, y.toInt()) }
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .onSizeChanged { cardHeight = it.height }
                .clip(RoundedCornerShape(20.dp))
                .background(DiableSheet)
                // Swallow taps so they don't fall through to the dismiss scrim.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            when (request) {
                is PopupRequest.App -> {
                    PopupHeader(request.app.name) {
                        onDismiss()
                        onLaunch(request.app)
                    }
                    // Diable Pro's pop-up widget: the app's own widget, right in the card.
                    prefs.popupWidgets[request.app.packageName]?.let { widgetId ->
                        val heightDp = remember(widgetId) {
                            runCatching {
                                val info = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId)
                                (info.minHeight / context.resources.displayMetrics.density).toInt()
                            }.getOrDefault(140).coerceIn(80, 220)
                        }
                        WidgetHostSlot(
                            appWidgetId = widgetId,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .height(heightDp.dp),
                        )
                    }
                    val waiting = notifications[request.app.packageName].orEmpty()
                    waiting.forEach { n ->
                        NotificationRow(n, quickReplies = prefs.quickReplies, onOpen = {
                            onDismiss()
                            runCatching { n.contentIntent?.send() }
                                .onFailure { onLaunch(request.app) }
                            if (n.clearable) DiableNotificationListener.dismiss(n.key)
                        })
                    }
                    if (waiting.any { it.clearable }) {
                        PopupTextAction("Clear all") {
                            DiableNotificationListener.dismissAll(request.app.packageName)
                        }
                    }
                    shortcuts.forEach { sc ->
                        PopupRow(
                            label = sc.label,
                            leading = {
                                if (sc.icon != null) {
                                    Image(
                                        painter = rememberDrawablePainter(sc.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                    )
                                } else {
                                    PopupGlyph(Icons.AutoMirrored.Filled.OpenInNew, accent)
                                }
                            },
                        ) {
                            onDismiss()
                            AppActions.launchShortcut(context, sc)
                        }
                    }
                    prefs.popupApps[request.app.packageName].orEmpty()
                        .mapNotNull { pkg -> allApps.firstOrNull { it.packageName == pkg } }
                        .forEach { extra ->
                            PopupRow(extra.name, leading = {
                                AppIcon(app = extra, size = 36.dp, iconStyle = iconStyle, accentColor = accent)
                            }) {
                                onDismiss()
                                onLaunch(extra)
                            }
                        }
                    if (waiting.isEmpty() && shortcuts.isEmpty() &&
                        prefs.popupApps[request.app.packageName].isNullOrEmpty()
                    ) {
                        Text(
                            text = "No shortcuts or notifications",
                            color = DiableTextMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }

                is PopupRequest.Folder -> {
                    PopupHeader(request.folder.name, arrow = false) {}
                    request.apps.forEach { app ->
                        PopupRow(app.name, leading = {
                            AppIcon(app = app, size = 36.dp, iconStyle = iconStyle, accentColor = accent)
                        }) {
                            onDismiss()
                            onLaunch(app)
                        }
                    }
                    PopupTextAction("Edit pop-up") {
                        onDismiss()
                        onEditFolder(request.folder)
                    }
                }

                is PopupRequest.AppList -> {
                    PopupHeader(request.title, arrow = false) {}
                    request.apps.forEach { app ->
                        PopupRow(app.name, leading = {
                            AppIcon(app = app, size = 36.dp, iconStyle = iconStyle, accentColor = accent)
                        }) {
                            onDismiss()
                            onLaunch(app)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupHeader(title: String, arrow: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (arrow) {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = DiableText,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = title, color = DiableText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PopupRow(label: String, leading: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, color = DiableText, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun NotificationRow(n: AppNotification, quickReplies: Boolean, onOpen: () -> Unit) {
    Column {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(DiableCard),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Notifications, null, tint = DiableAccentText, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (n.title.isNotBlank()) {
                Text(n.title, color = DiableText, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (n.text.isNotBlank()) {
                Text(n.text, color = DiableTextMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (n.clearable) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = DiableTextMuted,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { DiableNotificationListener.dismiss(n.key) }
                    .padding(6.dp),
            )
        }
    }
    // Quick Replies: answer right from the home screen through the app's reply action.
    if (quickReplies && n.replyAction != null) QuickReplyField(n)
    }
}

@Composable
private fun QuickReplyField(n: AppNotification) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    if (sent) {
        Text(
            text = "Reply sent",
            color = DiableTextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 72.dp, bottom = 8.dp),
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 72.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Reply", color = DiableTextMuted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (text.isNotBlank() && n.reply(context, text.trim())) sent = true
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DiableText,
                unfocusedTextColor = DiableText,
                focusedBorderColor = DiableAccentText,
                cursorColor = DiableAccentText,
            ),
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = if (text.isBlank()) DiableTextMuted else DiableAccentText,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(enabled = text.isNotBlank()) {
                    if (n.reply(context, text.trim())) sent = true
                }
                .padding(8.dp),
        )
    }
}

@Composable
private fun PopupTextAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = DiableAccentText,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun PopupGlyph(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(DiableCard),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
    }
}

// ---------------------------------------------------------------------------------------
// App long-press menu
// ---------------------------------------------------------------------------------------

/**
 * Diable's app menu: header (tap icon = change icon, tap name = rename), Favorite /
 * Edit favorites, App info, Screen time, Add to category, Hide, Uninstall (or the
 * system-app note), an Advanced section, and Diable settings.
 */
@Composable
fun AppMenuSheet(
    app: AppInfo,
    iconStyle: IconStyle,
    accent: Color,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val scope = rememberCoroutineScope()
    val pkg = app.packageName
    val isFavorite = pkg in prefs.favoritePackages
    val uninstallable = remember(pkg) { AppActions.isUninstallable(context, pkg) }
    var advanced by remember { mutableStateOf(false) }
    var sub by remember { mutableStateOf<String?>(null) }

    fun hide() {
        repos.prefs.launchUpdate { it.copy(hiddenPackages = it.hiddenPackages + pkg, favoritePackages = it.favoritePackages - pkg) }
        Toast.makeText(context, "${app.name} hidden", Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.clip(CircleShape).clickable { sub = "icon" }) {
                    AppIcon(app = app, size = 36.dp, iconStyle = iconStyle, accentColor = accent)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = app.name,
                    color = DiableText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { sub = "rename" },
                )
            }
            if (isFavorite) {
                MenuRow(Icons.Default.StarBorder, "Edit favorites") {
                    onDismiss()
                    onNavigate(Routes.EditFavorites)
                }
            } else {
                MenuRow(Icons.Default.Star, "Favorite") {
                    repos.prefs.launchUpdate { it.copy(favoritePackages = it.favoritePackages + pkg) }
                    onDismiss()
                }
            }
            MenuRow(Icons.Default.Info, "App info") {
                AppActions.openAppInfo(context, pkg)
                onDismiss()
            }
            MenuRow(Icons.Default.HourglassEmpty, "Screen time") {
                openScreenTime(context, pkg)
                onDismiss()
            }
            MenuRow(Icons.Default.Category, "Add to category") { sub = "category" }
            if (!uninstallable) {
                MenuRow(Icons.Default.VisibilityOff, "Hide from app list") { hide() }
                Text(
                    text = "This is a system app and can't be uninstalled.",
                    color = DiableTextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 72.dp, end = 24.dp, top = 6.dp, bottom = 6.dp),
                )
            } else {
                MenuRow(Icons.Default.Delete, "Uninstall") {
                    AppActions.uninstall(context, pkg)
                    onDismiss()
                }
            }
            MenuRow(if (advanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Advanced") {
                advanced = !advanced
            }
            if (advanced) {
                MenuRow(Icons.Default.Apps, "Edit ${app.name} pop-up", indent = true) { sub = "popup" }
                MenuRow(Icons.Default.Edit, "Edit icon/name", indent = true) { sub = "rename" }
                MenuRow(Icons.Default.Brush, "Change icon", indent = true) { sub = "icon" }
                if (uninstallable) {
                    MenuRow(Icons.Default.VisibilityOff, "Hide from app list", indent = true) { hide() }
                }
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DiableAccent.copy(alpha = 0.5f)),
            )
            MenuRow(Icons.Default.Settings, "Diable settings") {
                onDismiss()
                onNavigate(Routes.DiableSettings)
            }
        }
    }

    when (sub) {
        "rename" -> RenameDialog(
            current = app.name,
            onDismiss = { sub = null },
            onRename = { name ->
                repos.prefs.launchUpdate {
                        val labels = if (name.isBlank()) it.appLabels - pkg else it.appLabels + (pkg to name.trim())
                        it.copy(appLabels = labels)
                    }
                sub = null
                onDismiss()
            },
            onReset = if (pkg in prefs.appLabels) {
                {
                    repos.prefs.launchUpdate { it.copy(appLabels = it.appLabels - pkg) }
                    sub = null
                    onDismiss()
                }
            } else {
                null
            },
        )
        "icon" -> ChangeIconSheet(app = app, accent = accent, iconStyle = iconStyle, onDismiss = { sub = null })
        "category" -> CategorySheet(app = app, onDismiss = { sub = null; onDismiss() }, onNavigate = onNavigate)
        "popup" -> PopupAppsSheet(
            app = app,
            iconStyle = iconStyle,
            accent = accent,
            onDismiss = { sub = null },
            onAddWidget = {
                sub = null
                onDismiss()
                `in`.ankitsaroj.diable.ui.screens.WidgetPick.next =
                    `in`.ankitsaroj.diable.ui.screens.WidgetTarget.Popup(app.packageName)
                onNavigate(Routes.SelectWidget)
            },
        )
    }
}

private fun openScreenTime(context: android.content.Context, pkg: String) {
    // Digital Wellbeing's per-app page; fall back to the app's info page elsewhere.
    val intent = Intent("android.settings.action.APP_USAGE_SETTINGS")
        .putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { AppActions.openAppInfo(context, pkg) }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, indent: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = if (indent) 44.dp else 24.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = DiableText, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Text(text = label, color = DiableText, fontSize = 16.sp)
    }
}

@Composable
fun RenameDialog(
    current: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onReset: (() -> Unit)? = null,
    title: String = "Rename",
) {
    // Cursor at the end, like Diable's rename field.
    var text by remember {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(current, androidx.compose.ui.text.TextRange(current.length)))
    }
    val focus = remember { FocusRequester() }
    // The dialog's window attaches a frame later; focusing earlier is silently dropped.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        runCatching { focus.requestFocus() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DiableCard,
        title = { Text(title, color = DiableText) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onRename(text.text) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DiableText,
                    unfocusedTextColor = DiableText,
                    focusedBorderColor = DiableAccentText,
                    cursorColor = DiableAccentText,
                ),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onRename(text.text) }) { Text("Okay", color = DiableAccentText) } },
        dismissButton = {
            Row {
                if (onReset != null) {
                    TextButton(onClick = onReset) { Text("Reset", color = DiableTextMuted) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", color = DiableTextMuted) }
            }
        },
    )
}

/** Per-app icon: the default, or this app's icon from any installed icon pack. */
@Composable
private fun ChangeIconSheet(app: AppInfo, accent: Color, iconStyle: IconStyle, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val scope = rememberCoroutineScope()
    val packs by produceState<List<Pair<InstalledIconPack, android.graphics.drawable.Drawable?>>?>(null) {
        value = withContext(Dispatchers.IO) {
            IconPackRepository.installed(context).map { pack ->
                pack to IconPackRepository.load(context, pack.packageName)
                    ?.iconFor(app.packageName, app.className, null, 192)
            }
        }
    }
    val current = prefs.appIconPacks[app.packageName]

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Change icon", color = DiableText, fontSize = 20.sp, modifier = Modifier.padding(bottom = 12.dp))
            IconChoiceRow(
                label = "Default",
                selected = current == null,
                leading = { AppIcon(app = app, size = 40.dp, iconStyle = iconStyle, accentColor = accent) },
            ) {
                repos.prefs.launchUpdate { it.copy(appIconPacks = it.appIconPacks - app.packageName) }
                onDismiss()
            }
            packs?.forEach { (pack, icon) ->
                IconChoiceRow(
                    label = pack.label + if (icon == null) " (no icon for this app)" else "",
                    selected = current == pack.packageName,
                    enabled = icon != null,
                    leading = {
                        val shown = icon ?: pack.icon
                        if (shown != null) {
                            Image(rememberDrawablePainter(shown), null, modifier = Modifier.size(40.dp))
                        }
                    },
                ) {
                    repos.prefs.launchUpdate { it.copy(appIconPacks = it.appIconPacks + (app.packageName to pack.packageName)) }
                    onDismiss()
                }
            }
            IconChoiceRow(
                label = "More icon packs",
                selected = false,
                leading = { PopupGlyph(Icons.Default.Add, accent) },
            ) {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=icon%20pack&c=apps"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IconChoiceRow(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) DiableAccent.copy(alpha = 0.18f) else DiableCard)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = if (enabled) DiableText else DiableTextMuted, fontSize = 16.sp)
    }
}

/** "Add to category": Music Apps, Hidden apps, or a new pop-up folder. */
@Composable
private fun CategorySheet(app: AppInfo, onDismiss: () -> Unit, onNavigate: (String) -> Unit) {
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val scope = rememberCoroutineScope()
    val pkg = app.packageName
    val isMusic = pkg in prefs.musicAppPackages
    val context = LocalContext.current
    var naming by remember { mutableStateOf(false) }

    if (naming) {
        RenameDialog(
            current = "Folder",
            title = "New pop-up folder",
            onDismiss = { naming = false },
            onRename = { name ->
                val folder = PopupFolder(
                    id = UUID.randomUUID().toString().take(8),
                    name = name.ifBlank { "Folder" },
                    packages = listOf(pkg),
                )
                repos.prefs.launchUpdate {
                    it.copy(
                        folders = it.folders + folder,
                        favoritePackages = LinkedHashSet(it.favoritePackages + folder.favoriteKey),
                    )
                }
                Toast.makeText(
                    context,
                    "\"${folder.name}\" added to favorites. Long-press it to add apps.",
                    Toast.LENGTH_LONG,
                ).show()
                naming = false
                onDismiss()
            },
        )
        return
    }

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                "Add to category",
                color = DiableText,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            MenuRow(Icons.Default.MusicNote, if (isMusic) "Remove from Music Apps" else "Music Apps") {
                repos.prefs.launchUpdate {
                        it.copy(musicAppPackages = if (isMusic) it.musicAppPackages - pkg else it.musicAppPackages + pkg)
                    }
                onDismiss()
            }
            MenuRow(Icons.Default.VisibilityOff, "Hidden apps") {
                repos.prefs.launchUpdate { it.copy(hiddenPackages = it.hiddenPackages + pkg, favoritePackages = it.favoritePackages - pkg) }
                onDismiss()
            }
            MenuRow(Icons.Default.Add, "Create pop-up folder") { naming = true }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Picks extra apps that appear in an app's swipe-right pop-up. */
@Composable
private fun PopupAppsSheet(
    app: AppInfo,
    iconStyle: IconStyle,
    accent: Color,
    onDismiss: () -> Unit,
    onAddWidget: () -> Unit,
) {
    val context = LocalContext.current
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val apps by repos.apps.apps.collectAsState()
    val scope = rememberCoroutineScope()
    val selected = prefs.popupApps[app.packageName].orEmpty()

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Edit ${app.name} pop-up", color = DiableText, fontSize = 20.sp)
            Text(
                "Apps added here appear when you swipe right on ${app.name}.",
                color = DiableTextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            // Widgets: only this app's own widgets can live in its pop-up.
            val hasWidgets = remember(app.packageName) {
                runCatching {
                    android.appwidget.AppWidgetManager.getInstance(context).installedProviders
                        .any { it.provider.packageName == app.packageName }
                }.getOrDefault(false)
            }
            val currentWidget = prefs.popupWidgets[app.packageName]
            if (hasWidgets) {
                MenuRow(Icons.Default.Add, if (currentWidget == null) "Add widget" else "Change widget", onClick = onAddWidget)
            }
            if (currentWidget != null) {
                MenuRow(Icons.Default.DeleteOutline, "Remove widget") {
                    (context.applicationContext as `in`.ankitsaroj.diable.DiableApplication).appWidgetHost
                        .deleteAppWidgetId(currentWidget)
                    repos.prefs.launchUpdate { it.copy(popupWidgets = it.popupWidgets - app.packageName) }
                }
            }
            AppChecklist(
                apps = apps.filter { it.packageName != app.packageName },
                selected = selected.toSet(),
                iconStyle = iconStyle,
                accent = accent,
            ) { pkg ->
                repos.prefs.launchUpdate { p ->
                        val list = p.popupApps[app.packageName].orEmpty()
                        val next = if (pkg in list) list - pkg else list + pkg
                        p.copy(popupApps = p.popupApps + (app.packageName to next))
                    }
            }
        }
    }
}

@Composable
private fun AppChecklist(
    apps: List<AppInfo>,
    selected: Set<String>,
    iconStyle: IconStyle,
    accent: Color,
    onToggle: (String) -> Unit,
) {
    Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
        // Selected first, the way Diable's editors list them.
        apps.sortedBy { it.packageName !in selected }.forEach { a ->
            val on = a.packageName in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(a.packageName) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app = a, size = 36.dp, iconStyle = iconStyle, accentColor = accent)
                Spacer(modifier = Modifier.width(16.dp))
                Text(a.name, color = DiableText, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (on) DiableAccent else DiableText.copy(alpha = 0.12f)),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Pop-up folders
// ---------------------------------------------------------------------------------------

/** Long-press on a folder: edit, rename, show in list, edit favorites, remove. */
@Composable
fun FolderMenuSheet(
    folder: PopupFolder,
    iconStyle: IconStyle,
    accent: Color,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val repos = LocalDiableRepos.current
    val scope = rememberCoroutineScope()
    var sub by remember { mutableStateOf<String?>(null) }

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                folder.name,
                color = DiableText,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            MenuRow(Icons.Default.Apps, "Edit pop-up") { sub = "edit" }
            MenuRow(Icons.Default.StarBorder, "Edit favorites") {
                onDismiss()
                onNavigate(Routes.EditFavorites)
            }
            MenuRow(Icons.Default.Edit, "Edit icon/name") { sub = "rename" }
            MenuRow(Icons.Default.DeleteOutline, "Remove") {
                repos.prefs.launchUpdate {
                        it.copy(
                            folders = it.folders.filterNot { f -> f.id == folder.id },
                            favoritePackages = it.favoritePackages - folder.favoriteKey,
                        )
                    }
                onDismiss()
            }
            MenuRow(Icons.Default.Settings, "Diable settings") {
                onDismiss()
                onNavigate(Routes.DiableSettings)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    when (sub) {
        "edit" -> FolderEditSheet(folder, iconStyle, accent, onDismiss = { sub = null; onDismiss() })
        "rename" -> RenameDialog(
            current = folder.name,
            title = "Folder name",
            onDismiss = { sub = null },
            onRename = { name ->
                repos.prefs.launchUpdate {
                        it.copy(folders = it.folders.map { f -> if (f.id == folder.id) f.copy(name = name.ifBlank { f.name }) else f })
                    }
                sub = null
                onDismiss()
            },
        )
    }
}

@Composable
fun FolderEditSheet(folder: PopupFolder, iconStyle: IconStyle, accent: Color, onDismiss: () -> Unit) {
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val apps by repos.apps.apps.collectAsState()
    val scope = rememberCoroutineScope()
    val live = prefs.folders.firstOrNull { it.id == folder.id } ?: folder

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Edit ${live.name}", color = DiableText, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
            AppChecklist(apps = apps, selected = live.packages.toSet(), iconStyle = iconStyle, accent = accent) { pkg ->
                repos.prefs.launchUpdate { p ->
                        p.copy(
                            folders = p.folders.map { f ->
                                if (f.id != folder.id) f
                                else f.copy(packages = if (pkg in f.packages) f.packages - pkg else f.packages + pkg)
                            },
                        )
                    }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Done", color = DiableAccentText)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Diable button
// ---------------------------------------------------------------------------------------

/** Built-in actions the Diable button can run; apps are stored as `app:<package>`. */
enum class ButtonAction(val key: String, val label: String, val icon: ImageVector) {
    Search("search", "Search", Icons.Default.Search),
    Agenda("agenda", "Your agenda", Icons.Default.Event),
    Assistant("assistant", "Voice Assistant", Icons.Default.KeyboardVoice),
    Notifications("notifications", "Notification shade", Icons.Default.Notifications),
    LockScreen("lock", "Lock screen", Icons.Default.Lock),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
    }
}

fun buttonActionLabel(key: String?, apps: List<AppInfo>): String? {
    if (key == null) return null
    ButtonAction.fromKey(key)?.let { return it.label }
    val pkg = key.removePrefix("app:")
    return apps.firstOrNull { it.packageName == pkg }?.name
}

/** Long-press on the Diable button. */
@Composable
fun DiableButtonSheet(onNavigate: (String) -> Unit, onDismiss: () -> Unit) {
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val apps by repos.apps.apps.collectAsState()
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf<String?>(null) }

    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                "Diable Button",
                color = DiableText,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            MenuRowWithValue(Icons.Default.TouchApp, "Edit tap action", buttonActionLabel(prefs.buttonTapAction, apps)) {
                picking = "tap"
            }
            MenuRowWithValue(
                Icons.Default.SwipeUp,
                "Edit swipe up action",
                buttonActionLabel(prefs.buttonSwipeAction, apps),
            ) { picking = "swipe" }
            MenuRow(Icons.Default.Visibility, "Hide Diable Button") {
                repos.prefs.launchUpdate { it.copy(diableButton = false) }
                onDismiss()
            }
            MenuRow(Icons.Default.Settings, "Diable settings") {
                onDismiss()
                onNavigate(Routes.DiableSettings)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    picking?.let { which ->
        ButtonActionPicker(
            apps = apps,
            onDismiss = { picking = null },
            onPick = { key ->
                repos.prefs.launchUpdate {
                        if (which == "tap") it.copy(buttonTapAction = key) else it.copy(buttonSwipeAction = key)
                    }
                picking = null
            },
        )
    }
}

@Composable
private fun MenuRowWithValue(icon: ImageVector, label: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = DiableText, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(text = label, color = DiableText, fontSize = 16.sp)
            Text(text = value ?: "Not set", color = DiableTextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ButtonActionPicker(apps: List<AppInfo>, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
            Text(
                "Suggestions",
                color = DiableTextMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            ButtonAction.entries.forEach { action ->
                MenuRow(action.icon, action.label) { onPick(action.key) }
            }
            MenuRow(Icons.Default.Close, "No action") { onPick(null) }
            Text(
                "Apps",
                color = DiableTextMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            apps.forEach { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick("app:${app.packageName}") }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app = app, size = 32.dp)
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(app.name, color = DiableText, fontSize = 16.sp)
                }
            }
        }
    }
}
