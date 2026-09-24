package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import `in`.ankitsaroj.diable.DiableApplication
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.WidgetProviderEntry
import `in`.ankitsaroj.diable.data.WidgetRepository
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the next widget picked in [SelectWidgetScreen] goes. */
sealed interface WidgetTarget {
    /** Replace the home widget. */
    data object Home : WidgetTarget

    /** Add a page to the home widget stack. */
    data object Stack : WidgetTarget

    /** An app's swipe-right pop-up; only that app's widgets are offered. */
    data class Popup(val packageName: String) : WidgetTarget
}

object WidgetPick {
    @Volatile
    var next: WidgetTarget = WidgetTarget.Home
}

@Composable
fun SelectWidgetScreen(navController: NavController) {
    BackHandler { navController.popBackStack() }
    val context = LocalContext.current
    val widgetRepo = remember { WidgetRepository(context) }
    val prefsRepo = LocalDiableRepos.current.prefs
    val scope = rememberCoroutineScope()
    val appWidgetHost = (context.applicationContext as DiableApplication).appWidgetHost
    val target = remember { WidgetPick.next.also { WidgetPick.next = WidgetTarget.Home } }
    val prefs by prefsRepo.prefsFlow.collectAsState(initial = `in`.ankitsaroj.diable.data.DiablePrefs())
    val groups by produceState(initialValue = emptyList<`in`.ankitsaroj.diable.data.WidgetAppGroup>()) {
        val all = withContext(Dispatchers.IO) { widgetRepo.groupedProviders() }
        value = if (target is WidgetTarget.Popup) {
            all.filter { g -> g.widgets.any { it.info.provider.packageName == target.packageName } }
        } else {
            all
        }
    }
    var expanded by remember { mutableStateOf<String?>(null) }

    // The widget being bound: its id and provider, until the user finishes or backs out.
    var pendingWidgetId by remember { mutableIntStateOf(-1) }
    var pendingEntry by remember { mutableStateOf<WidgetProviderEntry?>(null) }

    /** Places [id] where the picker was opened for, then frees every id nothing uses. */
    fun commit(id: Int) {
        val keep = mutableSetOf(id)
        when (target) {
            WidgetTarget.Home -> {
                keep += prefs.widgetStackIds
                keep += prefs.popupWidgets.values
                prefsRepo.launchUpdate { it.copy(widgetAppWidgetId = id) }
            }
            WidgetTarget.Stack -> {
                keep += prefs.widgetStackIds + prefs.widgetAppWidgetId
                keep += prefs.popupWidgets.values
                prefsRepo.launchUpdate {
                    // An empty slot takes the widget directly; otherwise it joins the stack.
                    if (it.widgetAppWidgetId < 0) it.copy(widgetAppWidgetId = id)
                    else it.copy(widgetStackIds = it.widgetStackIds + id)
                }
            }
            is WidgetTarget.Popup -> {
                keep += prefs.widgetStackIds + prefs.widgetAppWidgetId
                keep += prefs.popupWidgets.filterKeys { it != target.packageName }.values
                prefsRepo.launchUpdate { it.copy(popupWidgets = it.popupWidgets + (target.packageName to id)) }
            }
        }
        // Replaced or abandoned widgets kept their ids allocated; release them.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appWidgetHost.appWidgetIds.filter { it !in keep }.forEach { appWidgetHost.deleteAppWidgetId(it) }
        }
        pendingWidgetId = -1
        navController.popBackStack()
    }

    fun abandon() {
        if (pendingWidgetId != -1) appWidgetHost.deleteAppWidgetId(pendingWidgetId)
        pendingWidgetId = -1
    }

    val configureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != -1) commit(pendingWidgetId) else abandon()
    }

    /** Widgets that need setting up (a photo, a city…) get their configure screen first. */
    fun configureOrCommit(id: Int) {
        val configure = pendingEntry?.info?.configure
        if (configure == null) {
            commit(id)
            return
        }
        // Launch it ourselves when we can, so backing out of setup cancels the widget.
        val direct = runCatching {
            configureLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                    .setComponent(configure)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            )
        }
        if (direct.isSuccess) return
        // Non-exported configure screens must be started by the host; its result goes to
        // the activity rather than here, so place the widget like Launcher3 does.
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appWidgetHost.startAppWidgetConfigureActivityForResult(
                    context as Activity, id, 0, REQUEST_CONFIGURE, null,
                )
            }
        }
        commit(id)
    }

    val bindLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != -1) {
            configureOrCommit(pendingWidgetId)
        } else {
            abandon()
        }
    }

    fun bindWidget(entry: WidgetProviderEntry) {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = appWidgetId
        pendingEntry = entry
        val allowed = AppWidgetManager.getInstance(context)
            .bindAppWidgetIdIfAllowed(appWidgetId, entry.info.provider)
        if (allowed) {
            configureOrCommit(appWidgetId)
        } else {
            bindLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, entry.info.provider),
            )
        }
    }

    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, end = 40.dp),
        ) {
            item {
                Text(
                    text = when (target) {
                        WidgetTarget.Stack -> "Add to widget stack"
                        is WidgetTarget.Popup -> "Pop-up widget"
                        WidgetTarget.Home -> "Select a widget"
                    },
                    color = DiableText,
                    fontSize = 40.sp,
                    modifier = Modifier.padding(top = 56.dp, bottom = 12.dp),
                )
            }
            item {
                Text(
                    text = "Get useful info at your fingertips without having to open apps.",
                    color = DiableTextMuted,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            var lastLetter: Char? = null
            groups.forEach { group ->
                val letter = group.appLabel.firstOrNull()?.uppercaseChar() ?: '#'
                // One header per letter, not one per app.
                if (letter != lastLetter) {
                    lastLetter = letter
                    item(key = "hdr_$letter") { WidgetGroupLabel(letter.toString()) }
                }
                item(key = "app_${group.appLabel}") {
                    val packageName = group.widgets.first().info.provider.packageName
                    WidgetAppRow(
                        packageName = packageName,
                        label = group.appLabel,
                        count = group.widgets.size,
                        onClick = { expanded = if (expanded == group.appLabel) null else group.appLabel },
                    )
                }
                if (expanded == group.appLabel) {
                    group.widgets.forEach { entry ->
                        item(key = "w_${entry.info.provider.flattenToString()}") {
                            WidgetPreviewRow(entry = entry, onClick = { bindWidget(entry) })
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}

private const val REQUEST_CONFIGURE = 4711

@Composable
private fun WidgetGroupLabel(text: String) {
    Text(
        text = text,
        color = DiableText,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun WidgetAppRow(packageName: String, label: String, count: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(rememberDrawablePainter(icon), contentDescription = null, modifier = Modifier.size(46.dp))
        } else {
            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(DiableCard))
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column {
            Text(label, color = DiableText, fontSize = 17.sp)
            Text(
                text = if (count == 1) "1 Widget" else "$count Widgets",
                color = DiableTextMuted,
                fontSize = 14.sp,
            )
        }
    }
}

/** A widget's own preview image with its name and size, the way launchers present them. */
@Composable
private fun WidgetPreviewRow(entry: WidgetProviderEntry, onClick: () -> Unit) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.densityDpi
    val preview = remember(entry.info.provider) {
        runCatching { entry.info.loadPreviewImage(context, density) ?: entry.info.loadIcon(context, density) }
            .getOrNull()
    }
    val cells = remember(entry.info.provider) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && entry.info.targetCellWidth > 0) {
            "${entry.info.targetCellWidth} × ${entry.info.targetCellHeight}"
        } else {
            val dp = context.resources.displayMetrics.density
            "${(entry.info.minWidth / dp / 70).toInt().coerceAtLeast(1)} × " +
                "${(entry.info.minHeight / dp / 70).toInt().coerceAtLeast(1)}"
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp, top = 6.dp, bottom = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DiableCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        if (preview != null) {
            Image(
                painter = rememberDrawablePainter(preview),
                contentDescription = entry.widgetLabel,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp),
            )
        }
        Text(entry.widgetLabel, color = DiableText, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
        Text(cells, color = DiableTextMuted, fontSize = 13.sp)
    }
}
