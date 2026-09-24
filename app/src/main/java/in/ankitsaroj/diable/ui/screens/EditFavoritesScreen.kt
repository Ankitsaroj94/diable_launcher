package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.ui.components.AppIcon
import `in`.ankitsaroj.diable.ui.components.DiableToggle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.model.AppInfo
import `in`.ankitsaroj.diable.ui.components.DiableListRow
import `in`.ankitsaroj.diable.ui.components.DiableRowTrailing
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import kotlinx.coroutines.launch

@Composable
fun EditFavoritesScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = `in`.ankitsaroj.diable.data.DiablePrefs())
    val apps by repos.apps.apps.collectAsState()
    LaunchedEffect(Unit) { repos.apps.ensureLoaded() }
    FavoritesPicker(onDone = onBack, showDragHandles = true)
}

/** Hide Apps: every app with its icon; checked apps leave the list, search and favorites. */
@Composable
fun HideAppsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = `in`.ankitsaroj.diable.data.DiablePrefs())
    val apps by repos.apps.apps.collectAsState()
    LaunchedEffect(Unit) { repos.apps.ensureLoaded() }
    val iconStyle = `in`.ankitsaroj.diable.data.IconStyle.fromKey(prefs.iconStyleKey)
    val accent = androidx.compose.ui.graphics.Color(prefs.accentColorArgb)
    // Hidden apps first, like Diable's "Selected" group.
    val ordered = remember(apps, prefs.hiddenPackages) {
        apps.sortedBy { it.packageName !in prefs.hiddenPackages }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        PillHeader("Hide apps")
        Text(
            text = "Hidden apps stay installed but leave your app list and search.",
            color = `in`.ankitsaroj.diable.ui.theme.DiableTextMuted,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            items(ordered, key = { it.packageName }) { app ->
                val hidden = app.packageName in prefs.hiddenPackages
                val toggle = {
                            repos.prefs.launchUpdate { p ->
                                p.copy(
                                    hiddenPackages = if (hidden) p.hiddenPackages - app.packageName
                                    else p.hiddenPackages + app.packageName,
                                    favoritePackages = if (hidden) p.favoritePackages
                                    else p.favoritePackages - app.packageName,
                                )
                            }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = toggle)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app = app, size = 40.dp, iconStyle = iconStyle, accentColor = accent)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(app.name, color = DiableText, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    DiableToggle(checked = hidden, onCheckedChange = { toggle() })
                }
            }
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}
