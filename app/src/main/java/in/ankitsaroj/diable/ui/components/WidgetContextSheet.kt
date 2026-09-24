package `in`.ankitsaroj.diable.ui.components

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `in`.ankitsaroj.diable.DiableApplication
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun WidgetContextSheet(
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
    widgetProviderPackage: String? = null,
    /** The widget that was long-pressed: the main one or a page of the stack. */
    widgetId: Int = -1,
) {
    val context = LocalContext.current
    val prefsRepo = LocalDiableRepos.current.prefs
    val prefs by prefsRepo.prefsFlow.collectAsState(initial = `in`.ankitsaroj.diable.data.DiablePrefs())
    val scope = rememberCoroutineScope()
    val host = (context.applicationContext as DiableApplication).appWidgetHost

    Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
        DiableSettingCard(
            title = "Edit favorites",
            icon = Icons.Default.Star,
            onClick = {
                onDismiss()
                onNavigate(Routes.EditFavorites)
            },
        )
        DiableSettingCard(
            title = "App info",
            icon = Icons.Default.Info,
            onClick = {
                widgetProviderPackage?.let { pkg ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", pkg, null)
                    }
                    context.startActivity(intent)
                } ?: Toast.makeText(context, "No widget selected", Toast.LENGTH_SHORT).show()
                onDismiss()
            },
        )
        DiableSettingCard(
            title = "Move and resize",
            icon = Icons.Default.AspectRatio,
            onClick = {
                onDismiss()
                `in`.ankitsaroj.diable.ui.screens.HomeCommands.moveWidget.value = true
            },
        )
        DiableSettingCard(
            title = "Change widget settings",
            icon = Icons.Default.Tune,
            onClick = {
                val id = if (widgetId >= 0) widgetId else prefs.widgetAppWidgetId
                if (id != -1) {
                    val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)
                    if (info?.configure != null) {
                        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                            component = info.configure
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        }
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "No settings for this widget", Toast.LENGTH_SHORT).show()
                    }
                }
                onDismiss()
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        DiableSettingCard(
            title = "Replace clock with widget",
            icon = Icons.Default.SwapHoriz,
            checked = prefs.replaceClockWithWidget,
            onCheckedChange = { checked ->
                prefsRepo.launchUpdate { it.copy(replaceClockWithWidget = checked) }
            },
        )
        DiableSettingCard(
            title = "Fill screen",
            icon = Icons.Default.Fullscreen,
            checked = prefs.fillScreenWidget,
            onCheckedChange = { checked ->
                prefsRepo.launchUpdate { it.copy(fillScreenWidget = checked) }
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        DiableSettingCard(
            title = "Add Custom Widget",
            icon = Icons.Default.Add,
            onClick = {
                onDismiss()
                onNavigate(Routes.SelectWidget)
            },
        )
        DiableSettingCard(
            title = "Add to widget stack",
            subtitle = "Swipe sideways between stacked widgets",
            icon = Icons.Default.Layers,
            onClick = {
                onDismiss()
                `in`.ankitsaroj.diable.ui.screens.WidgetPick.next = `in`.ankitsaroj.diable.ui.screens.WidgetTarget.Stack
                onNavigate(Routes.SelectWidget)
            },
        )
        DiableSettingCard(
            title = "Remove",
            icon = Icons.Default.Delete,
            onClick = {
                val id = if (widgetId >= 0) widgetId else prefs.widgetAppWidgetId
                if (id != -1) {
                    host.deleteAppWidgetId(id)
                    prefsRepo.launchUpdate {
                        if (id == it.widgetAppWidgetId) {
                            // The next stacked widget moves up into the main slot.
                            it.copy(
                                widgetAppWidgetId = it.widgetStackIds.firstOrNull() ?: -1,
                                widgetStackIds = it.widgetStackIds.drop(1),
                            )
                        } else {
                            it.copy(widgetStackIds = it.widgetStackIds - id)
                        }
                    }
                }
                onDismiss()
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        DiableSettingCard(
            title = "Diable settings",
            icon = Icons.Default.Settings,
            onClick = {
                onDismiss()
                onNavigate(Routes.DiableSettings)
            },
        )
        DiableSettingCard(
            title = "Diable Pro",
            icon = Icons.Default.WorkspacePremium,
            onClick = {
                onDismiss()
                onNavigate(Routes.DiablePro)
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
