package `in`.ankitsaroj.diable.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.PopupFolder
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.navigation.Routes
import `in`.ankitsaroj.diable.ui.components.DiableSettingCard
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.home.FolderMenuSheet
import `in`.ankitsaroj.diable.ui.home.RenameDialog
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Folders & Pop-Ups: pop-up folders live in favorites and open as a card of apps.
 * New folders are added to the end of favorites; each one is edited from its menu.
 */
@Composable
fun FoldersScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    LaunchedEffect(Unit) { repos.apps.ensureLoaded() }
    val scope = rememberCoroutineScope()
    var naming by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<PopupFolder?>(null) }
    val iconStyle = IconStyle.fromKey(prefs.iconStyleKey)
    val accent = Color(prefs.accentColorArgb)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
    ) {
        PillHeader("Folders & Pop-Ups")
        Text(
            text = "Pop-up folders sit in your favorites and open as a card of apps. " +
                "Tap one on the home screen to open it, long-press it to edit.",
            color = DiableTextMuted,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            DiableSettingCard(
                icon = Icons.Default.Add,
                title = "Create pop-up folder",
                onClick = { naming = true },
            )
            prefs.folders.forEach { folder ->
                DiableSettingCard(
                    icon = Icons.Default.Folder,
                    title = folder.name,
                    subtitle = "${folder.packages.size} apps",
                    onClick = { menuFor = folder },
                )
            }
        }
    }

    if (naming) {
        RenameDialog(
            current = "Folder",
            title = "New pop-up folder",
            onDismiss = { naming = false },
            onRename = { name ->
                val folder = PopupFolder(
                    id = UUID.randomUUID().toString().take(8),
                    name = name.ifBlank { "Folder" },
                    packages = emptyList(),
                )
                repos.prefs.launchUpdate {
                        it.copy(
                            folders = it.folders + folder,
                            favoritePackages = LinkedHashSet(it.favoritePackages + folder.favoriteKey),
                        )
                    }
                naming = false
                menuFor = folder
            },
        )
    }
    menuFor?.let { folder ->
        FolderMenuSheet(
            folder = folder,
            iconStyle = iconStyle,
            accent = accent,
            onNavigate = {},
            onDismiss = { menuFor = null },
        )
    }
}
