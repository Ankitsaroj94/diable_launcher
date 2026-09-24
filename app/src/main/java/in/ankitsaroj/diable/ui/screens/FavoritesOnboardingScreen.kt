package `in`.ankitsaroj.diable.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.model.AppInfo
import `in`.ankitsaroj.diable.ui.components.AppIcon
import `in`.ankitsaroj.diable.ui.components.LavenderIconCircle
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.launch

/**
 * Diable's first-run favorites picker. Chosen apps hop into a "Selected" group at the
 * top; everything else stays under "Suggestions". Done writes them to prefs, which is
 * what populates the home screen.
 */
@Composable
fun FavoritesOnboardingScreen(onDone: () -> Unit) = FavoritesPicker(onDone = onDone)

/**
 * Shared by first-run onboarding and Settings -> Edit favorites; Diable uses one screen
 * for both, the only difference being that already-chosen apps start in "Selected".
 */
@Composable
fun FavoritesPicker(
    onDone: () -> Unit,
    showDragHandles: Boolean = false,
) {
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val apps by repos.apps.apps.collectAsState()
    LaunchedEffect(Unit) { repos.apps.ensureLoaded() }
    val scope = rememberCoroutineScope()

    // Selection order is the home-screen order, so keep it as a list, not a set.
    var selected by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(prefs.favoritePackages) {
        if (selected == null && prefs.favoritePackages.isNotEmpty()) {
            selected = prefs.favoritePackages.toList()
        }
    }
    val selection = selected ?: emptyList()

    val byPackage = remember(apps) { apps.associateBy { it.packageName } }
    val selectedApps = remember(selection, byPackage) { selection.mapNotNull { byPackage[it] } }
    val suggestions = remember(apps, selection) {
        apps.filter { it.packageName !in selection }
    }

    fun save() {
        // Outlives this screen: saving and leaving happen in the same tap.
        repos.prefs.launchUpdate { p ->
            p.copy(favoritePackages = LinkedHashSet(selection), favoritesOnboarded = true)
        }
    }
    // Diable: leaving the editor with BACK keeps the changes.
    if (showDragHandles) {
        androidx.activity.compose.BackHandler {
            save()
            onDone()
        }
    }

    // Drag-to-reorder by the handle: the dragged row follows the finger and swaps with its
    // neighbour each time it crosses a row's height.
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val rowPx = with(androidx.compose.ui.platform.LocalDensity.current) { 58.dp.toPx() }
    val iconStyle = `in`.ankitsaroj.diable.data.IconStyle.fromKey(prefs.iconStyleKey)
    val accent = Color(prefs.accentColorArgb)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp)) {
            item {
                Text(
                    text = "Your favorites",
                    color = Color.White,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(top = 56.dp, bottom = 16.dp),
                )
            }
            item {
                OnboardingHint(
                    icon = Icons.Default.StarOutline,
                    text = "Favorites appear on your home screen for quick access",
                )
            }
            item {
                OnboardingHint(
                    icon = Icons.Default.CheckCircleOutline,
                    text = "Most people choose their 4 to 8 most-used apps",
                )
            }
            if (selectedApps.isNotEmpty()) {
                item { GroupLabel("Selected") }
                items(selectedApps, key = { "sel_${it.packageName}" }) { app ->
                    val dragging = draggingKey == app.packageName
                    FavoriteRow(
                        app = app,
                        checked = true,
                        iconStyle = iconStyle,
                        accent = accent,
                        showDragHandle = showDragHandles,
                        modifier = Modifier
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer { translationY = if (dragging) dragOffset else 0f },
                        onDragStart = {
                            draggingKey = app.packageName
                            dragOffset = 0f
                        },
                        onDrag = { dy ->
                            dragOffset += dy
                            val list = selection.toMutableList()
                            val from = list.indexOf(app.packageName)
                            val to = when {
                                dragOffset > rowPx / 2 && from < list.lastIndex -> from + 1
                                dragOffset < -rowPx / 2 && from > 0 -> from - 1
                                else -> from
                            }
                            if (to != from) {
                                list.add(to, list.removeAt(from))
                                selected = list
                                dragOffset -= if (to > from) rowPx else -rowPx
                            }
                        },
                        onDragEnd = {
                            draggingKey = null
                            dragOffset = 0f
                        },
                    ) {
                        selected = selection - app.packageName
                    }
                }
            }
            item { GroupLabel("Suggestions") }
            items(suggestions, key = { "sug_${it.packageName}" }) { app ->
                FavoriteRow(app = app, checked = false, iconStyle = iconStyle, accent = accent) {
                    selected = selection + app.packageName
                }
            }
            item { Spacer(modifier = Modifier.height(140.dp)) }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(DiableAccent)
                .clickable {
                    save()
                    onDone()
                }
                .padding(horizontal = 28.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Done", color = Color.Black, fontSize = 17.sp)
        }
    }
}

@Composable
private fun OnboardingHint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LavenderIconCircle(icon, size = 44.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = Color.White, fontSize = 17.sp, lineHeight = 23.sp)
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun FavoriteRow(
    app: AppInfo,
    checked: Boolean,
    iconStyle: `in`.ankitsaroj.diable.data.IconStyle,
    accent: Color,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = false,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onToggle: () -> Unit,
) {
    // The gesture outlives recompositions; always call the latest callbacks.
    val latestDragStart by androidx.compose.runtime.rememberUpdatedState(onDragStart)
    val latestDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
    val latestDragEnd by androidx.compose.runtime.rememberUpdatedState(onDragEnd)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) Color.White else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        AppIcon(app = app, size = 46.dp, iconStyle = iconStyle, accentColor = accent)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = app.name,
            color = Color.White,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
        )
        if (showDragHandle) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = DiableTextMuted,
                modifier = Modifier
                    .size(40.dp)
                    .pointerInput(app.packageName) {
                        detectDragGestures(
                            onDragStart = { latestDragStart() },
                            onDragEnd = { latestDragEnd() },
                            onDragCancel = { latestDragEnd() },
                            onDrag = { change, amount ->
                                change.consume()
                                latestDrag(amount.y)
                            },
                        )
                    }
                    .padding(8.dp),
            )
        }
    }
}
