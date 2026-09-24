package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.navigation.Routes
import `in`.ankitsaroj.diable.ui.components.AppIcon
import `in`.ankitsaroj.diable.ui.home.ClockStylePreview
import `in`.ankitsaroj.diable.ui.components.FontSheet
import `in`.ankitsaroj.diable.ui.components.IconStyleSheet
import `in`.ankitsaroj.diable.ui.components.ThemeGlobalSettingsSheet
import `in`.ankitsaroj.diable.ui.screens.settings.ThemeColorSheet
import `in`.ankitsaroj.diable.ui.screens.settings.ThemeSnapshot
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import `in`.ankitsaroj.diable.ui.theme.EditorChrome
import `in`.ankitsaroj.diable.ui.theme.LocalHomeTextColor
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import `in`.ankitsaroj.diable.ui.theme.wallpaperBackground
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diable's theme editor: a framed phone preview of the live home screen with a floating
 * toolbar pill beneath it. Geometry from the reference app — 48dp chrome buttons at the
 * top, a 379x841dp preview frame, and a toolbar pill of 48dp buttons.
 */
/** Set by Create Theme so the editor's ✓ saves the result into My Themes. */
object ThemeEditorLaunch {
    @Volatile
    var saveAsNewTheme = false
}

@Composable
fun ThemeEditorScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val saveAsNew = remember {
        ThemeEditorLaunch.saveAsNewTheme.also { ThemeEditorLaunch.saveAsNewTheme = false }
    }
    var naming by remember { mutableStateOf(false) }
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val apps by repos.apps.apps.collectAsState()
    LaunchedEffect(Unit) { repos.apps.ensureLoaded() }
    var showGlobalSettings by remember { mutableStateOf(false) }
    var showIconStyles by remember { mutableStateOf(false) }
    var showFonts by remember { mutableStateOf(false) }
    var showThemeColors by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // The theme as it was on entry. Back restores it; ✓ keeps the edits. Read from the
    // stored prefs, not the collectAsState placeholder, or back would reset to defaults.
    var entryTheme by remember { mutableStateOf<ThemeSnapshot?>(null) }
    LaunchedEffect(Unit) {
        entryTheme = ThemeSnapshot.of(repos.prefs.prefsFlow.first())
    }
    val discardAndExit: () -> Unit = {
        val original = entryTheme
        scope.launch {
            if (original != null) repos.prefs.update { original.applyTo(it) }
            navController.popBackStack()
        }
    }
    BackHandler(onBack = discardAndExit)

    val favorites = remember(apps, prefs.favoritePackages) {
        val order = prefs.favoritePackages.toList()
        apps.filter { it.packageName in prefs.favoritePackages }
            .sortedBy { order.indexOf(it.packageName).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
    }

    // The preview must show the real wallpaper, which only exists behind the window. So
    // the editor paints its backdrop everywhere except the preview frame.
    var frameBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var rootOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val backdrop = DiableBg
    val frameRadius = with(androidx.compose.ui.platform.LocalDensity.current) { 19.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.positionInRoot() }
            .drawBehind {
                val hole = frameBounds?.translate(-rootOffset)
                if (hole == null) {
                    drawRect(backdrop)
                } else {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                hole,
                                androidx.compose.ui.geometry.CornerRadius(frameRadius),
                            ),
                        )
                    }
                    clipPath(path, androidx.compose.ui.graphics.ClipOp.Difference) { drawRect(backdrop) }
                }
            }
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ChromeButton(Icons.AutoMirrored.Filled.ArrowBack, CircleShape, label = "Discard changes", onClick = discardAndExit)
            ChromeButton(Icons.Default.Check, RoundedCornerShape(24.dp), width = 80.dp, label = "Save theme") {
                // Create Theme ends by naming the new theme, which lands in My Themes.
                if (saveAsNew) naming = true else navController.popBackStack()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val letters = remember(apps, prefs.hiddenPackages) {
                apps.filter { it.packageName !in prefs.hiddenPackages }
                    .map { `in`.ankitsaroj.diable.ui.components.getFirstLetter(it.name) }
                    .distinct()
                    .sorted()
            }
            ThemePreviewFrame(
                prefs = prefs,
                favorites = favorites,
                letters = letters,
                modifier = Modifier.onGloballyPositioned { frameBounds = it.boundsInRoot() },
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(EditorChrome)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarIcon(Icons.Default.Image, "Wallpaper") {
                navController.navigate(Routes.ChooseWallpaper)
            }
            ToolbarIcon(Icons.Default.AutoAwesome, "App icons") {
                showIconStyles = true
            }
            ToolbarIcon(Icons.Default.Schedule, "Clock style") {
                navController.navigate(Routes.ClockStyle)
            }
            ToolbarIcon(Icons.Default.TextFields, "Font") { showFonts = true }
            ToolbarIcon(Icons.Default.Palette, "Theme color") { showThemeColors = true }
            ToolbarIcon(Icons.Default.MoreHoriz, "Global settings") {
                showGlobalSettings = true
            }
        }
    }

    if (showGlobalSettings) {
        ThemeGlobalSettingsSheet(onDismiss = { showGlobalSettings = false })
    }
    if (showIconStyles) {
        IconStyleSheet(onDismiss = { showIconStyles = false })
    }
    if (naming) {
        `in`.ankitsaroj.diable.ui.home.RenameDialog(
            current = "My theme",
            title = "Name your theme",
            onDismiss = { naming = false },
            onRename = { name ->
                `in`.ankitsaroj.diable.ui.screens.settings.SavedThemes.save(
                    context,
                    name.ifBlank { "My theme" },
                    ThemeSnapshot.of(prefs),
                )
                android.widget.Toast.makeText(context, "Saved to My Themes", android.widget.Toast.LENGTH_SHORT).show()
                naming = false
                navController.popBackStack()
            },
        )
    }
    if (showFonts) {
        FontSheet(onDismiss = { showFonts = false })
    }
    if (showThemeColors) {
        ThemeColorSheet(onDismiss = { showThemeColors = false })
    }
}

@Composable
private fun ThemePreviewFrame(
    prefs: DiablePrefs,
    favorites: List<`in`.ankitsaroj.diable.model.AppInfo>,
    letters: List<Char>,
    modifier: Modifier = Modifier,
) {
    val accent = Color(prefs.accentColorArgb)
    val now = remember { System.currentTimeMillis() }
    val liveTime = remember(now) {
        SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(now))
    }
    val liveDate = remember(now) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(now))
    }
    Box(
        modifier = modifier
            .width(284.dp)
            .height(630.dp)
            .clip(RoundedCornerShape(19.dp))
            .border(2.dp, Color.White, RoundedCornerShape(19.dp))
            .wallpaperBackground(prefs.wallpaperUri)
            // The same black dim home uses; a theme-coloured wash turned pale in light mode.
            .background(Color.Black.copy(alpha = if (prefs.dimWallpaper) 0.35f else 0f)),
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, top = 118.dp)) {
            // Mirror the real clock style and live time rather than a hardcoded sample.
            ClockStylePreview(
                styleId = prefs.clockStyleId,
                time = liveTime,
                date = liveDate,
                battery = "",
            )
            Spacer(modifier = Modifier.height(10.dp))
            favorites.take(4).forEach { app ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(
                        app = app,
                        size = 26.dp,
                        iconStyle = IconStyle.fromKey(prefs.iconStyleKey),
                        accentColor = accent,
                    )
                    if (!prefs.hideFavoriteNames) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(app.name, color = LocalHomeTextColor.current, fontSize = 12.sp)
                    }
                }
            }
        }
        if (!prefs.hideAlphabet) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp),
                horizontalAlignment = Alignment.End,
            ) {
                (listOf("☆") + letters.map { it.toString() } + listOf("°")).forEach {
                    Text(
                        text = it,
                        color = accent,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChromeButton(
    icon: ImageVector,
    shape: androidx.compose.ui.graphics.Shape,
    width: androidx.compose.ui.unit.Dp = 48.dp,
    label: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(48.dp)
            .clip(shape)
            .background(EditorChrome)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = DiableText)
    }
}

@Composable
private fun ToolbarIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = DiableText)
    }
}
