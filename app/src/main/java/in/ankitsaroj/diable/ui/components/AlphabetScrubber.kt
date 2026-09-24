package `in`.ankitsaroj.diable.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.PopupFolder
import `in`.ankitsaroj.diable.model.AppInfo
import `in`.ankitsaroj.diable.service.AppNotification
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp

fun getFirstLetter(name: String): Char {
    val firstChar = name.uppercase().firstOrNull() ?: return '#'
    return if (firstChar in 'A'..'Z') firstChar else '#'
}

fun calculateElasticOffset(touchY: Float, letterY: Float, maxInfluenceDistance: Float): Float {
    val distance = touchY - letterY
    val maxOffset = 120f
    val decayFactor = 10f
    val normalizedDistance = distance / maxInfluenceDistance
    val influence = exp(-normalizedDistance * normalizedDistance * decayFactor)
    return maxOffset * influence
}

private const val STAR = '☆'
private const val FOOTER = '°'

/** One entry in the favorites block: an app, or a pop-up folder. */
sealed interface FavoriteEntry {
    val key: String

    data class App(val app: AppInfo) : FavoriteEntry {
        override val key get() = "fav_${app.packageName}"
    }

    data class Folder(val folder: PopupFolder, val apps: List<AppInfo>) : FavoriteEntry {
        override val key get() = "fav_${folder.favoriteKey}"
    }
}

/** Everything the home list reports back. Y values are in root (window) pixels. */
class HomeListActions(
    val onLaunch: (AppInfo) -> Unit,
    val onLongPress: (AppInfo) -> Unit,
    val onSwipeRight: (AppInfo, Float) -> Unit,
    val onFolderOpen: (PopupFolder, Float) -> Unit,
    val onFolderLongPress: (PopupFolder) -> Unit,
    val onRecentlyInstalled: (Float) -> Unit,
    val onSettings: () -> Unit,
    val onSwipeUpAtTop: () -> Unit,
    val onSwipeDownAtTop: () -> Unit,
    val onStarLongPress: () -> Unit,
    val onAlphabetDoubleTap: () -> Unit,
)

/**
 * Diable's home: clock and favorites, then — below the fold — every app A–Z, then a
 * "Diable Launcher" footer. It is one continuous list; the alphabet on the right jumps
 * through it. At rest the list is pinned to the top, where vertical swipes are gestures
 * (up = search, down = notifications) rather than scrolling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlphabetScrubber(
    favorites: List<FavoriteEntry>,
    apps: List<AppInfo>,
    listState: LazyListState,
    actions: HomeListActions,
    modifier: Modifier = Modifier,
    notifications: Map<String, List<AppNotification>> = emptyMap(),
    listContentEndPadding: Dp = 100.dp,
    accentColor: Color = DiableAccent,
    textColor: Color = Color.White,
    hideAlphabet: Boolean = false,
    hideFavoriteNames: Boolean = false,
    iconStyle: IconStyle = IconStyle.Rounded,
    allowHaptic: Boolean = true,
    header: (@Composable () -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val currentActions by rememberUpdatedState(actions)

    val groupedApps = remember(apps) {
        apps.groupBy { getFirstLetter(it.name) }.toSortedMap()
    }
    // Diable only lists letters that actually have apps behind them, bookended by
    // the favorites star and the launcher footer.
    val alphabet = remember(groupedApps) {
        listOf(STAR) + groupedApps.keys.toList() + listOf(FOOTER)
    }
    // LazyColumn indices: 0 = clock + favorites, 1 = filler, then header + apps per letter.
    val letterIndex = remember(groupedApps) {
        val map = HashMap<Char, Int>()
        var index = 2
        groupedApps.forEach { (letter, sectionApps) ->
            map[letter] = index
            index += 1 + sectionApps.size
        }
        map[STAR] = 0
        map[FOOTER] = index
        map
    }

    var viewportHeight by remember { mutableIntStateOf(0) }
    var topBlockHeight by remember { mutableIntStateOf(0) }

    // --- Scrubbing -----------------------------------------------------------------------
    var touchY by remember { mutableStateOf<Float?>(null) }
    var dragBoxTop by remember { mutableFloatStateOf(0f) }
    var columnHeight by remember { mutableFloatStateOf(0f) }
    val letterPositions = remember { mutableStateMapOf<Char, Float>() }
    val letterXPositions = remember { mutableStateMapOf<Char, Float>() }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var cancelled by remember { mutableStateOf(false) }

    val touchRootY = touchY?.let { it + dragBoxTop }
    val letterOffsets = remember(touchRootY, columnHeight, letterPositions.toMap()) {
        if (touchRootY == null || columnHeight == 0f || letterPositions.isEmpty()) {
            alphabet.associateWith { 0f }
        } else {
            alphabet.associateWith { letter ->
                calculateElasticOffset(touchRootY, letterPositions[letter] ?: 0f, columnHeight)
            }
        }
    }
    val letterAtPeak = remember(letterOffsets) {
        letterOffsets.maxByOrNull { it.value }?.takeIf { it.value > 0f }?.key
    }
    LaunchedEffect(letterAtPeak, touchY) {
        val next = if (touchY == null) null else letterAtPeak ?: selectedLetter
        if (next != null && next != selectedLetter && allowHaptic) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        selectedLetter = next
    }

    fun nearestLetter(localY: Float): Char? {
        val y = localY + dragBoxTop
        return letterPositions.minByOrNull { abs(it.value - y) }?.key
    }

    fun jumpTo(letter: Char) {
        val index = letterIndex[letter] ?: return
        scope.launch { listState.scrollToItem(index) }
    }

    // --- Top-of-list gestures ------------------------------------------------------------
    val topGestures = remember(listState) {
        object : NestedScrollConnection {
            private var pull = 0f
            private var gestureStarted = false
            private var startedAtTop = false

            private fun atTop() =
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (!gestureStarted) {
                    gestureStarted = true
                    // Only a resting home that fits on screen turns swipes into gestures;
                    // with more favorites than fit, the list must scroll to reach them.
                    startedAtTop = atTop() && topBlockHeight <= viewportHeight
                    pull = 0f
                }
                if (!startedAtTop) return Offset.Zero
                pull += available.y
                // Swallow the drag: at rest the list does not scroll, the swipe is a gesture.
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val wasTop = startedAtTop
                val distance = pull
                gestureStarted = false
                startedAtTop = false
                pull = 0f
                if (!wasTop) return Velocity.Zero
                val threshold = with(density) { 48.dp.toPx() }
                when {
                    distance < -threshold || available.y < -1500f -> currentActions.onSwipeUpAtTop()
                    distance > threshold || available.y > 1500f -> currentActions.onSwipeDownAtTop()
                }
                return available
            }
        }
    }

    // Read the letter under the finger directly; selectedLetter settles a frame later
    // (it drives haptics and the jump on release), which showed the previous letter's apps.
    val shownLetter = if (touchY != null) letterAtPeak ?: selectedLetter else null
    val scrubbing = touchY != null && !cancelled && shownLetter != null
    var boxOrigin by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxSize().onGloballyPositioned { boxOrigin = it.positionInRoot() }) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewportHeight = it.height }
                .nestedScroll(topGestures)
                // While scrubbing, only the picked letter's apps are shown (overlay below).
                .alpha(if (scrubbing) 0f else 1f)
                .padding(start = 48.dp, end = listContentEndPadding),
        ) {
            item(key = "top") {
                Column(modifier = Modifier.onSizeChanged { topBlockHeight = it.height }) {
                    header?.invoke()
                    favorites.forEach { entry ->
                        when (entry) {
                            is FavoriteEntry.App -> HomeAppRow(
                                app = entry.app,
                                accentColor = accentColor,
                                textColor = textColor,
                                iconStyle = iconStyle,
                                showLabel = !hideFavoriteNames,
                                notifications = notifications[entry.app.packageName].orEmpty(),
                                actions = actions,
                            )
                            is FavoriteEntry.Folder -> FolderRow(
                                entry = entry,
                                accentColor = accentColor,
                                textColor = textColor,
                                iconStyle = iconStyle,
                                showLabel = !hideFavoriteNames,
                                actions = actions,
                            )
                        }
                    }
                }
            }
            // Pushes the A–Z list below the fold so the resting home shows favorites only.
            item(key = "filler") {
                val gap = (viewportHeight - topBlockHeight).coerceAtLeast(0)
                Spacer(modifier = Modifier.height(with(density) { gap.toDp() }))
            }
            groupedApps.forEach { (letter, sectionApps) ->
                item(key = "header_$letter") { LetterHeader(letter.toString(), textColor) }
                items(items = sectionApps, key = { "app_${it.packageName}" }) { app ->
                    HomeAppRow(
                        app = app,
                        accentColor = accentColor,
                        textColor = textColor,
                        iconStyle = iconStyle,
                        notifications = notifications[app.packageName].orEmpty(),
                        actions = actions,
                    )
                }
            }
            item(key = "footer") {
                Column(modifier = Modifier.padding(bottom = 96.dp)) {
                    LetterHeader("Diable Launcher", textColor)
                    FooterRow(Icons.Default.History, "Recently installed", accentColor, textColor) { y ->
                        actions.onRecentlyInstalled(y)
                    }
                    FooterRow(Icons.Default.Settings, "Diable settings", accentColor, textColor) {
                        actions.onSettings()
                    }
                }
            }
        }

        // The picked letter's apps, shown while the finger is on the alphabet.
        if (scrubbing) {
            Column(
                modifier = Modifier
                    .padding(start = 48.dp, end = listContentEndPadding, top = 140.dp),
            ) {
                when (val letter = shownLetter) {
                    STAR -> favorites.filterIsInstance<FavoriteEntry.App>().forEach {
                        key(it.app.packageName) {
                            HomeAppRow(it.app, accentColor, textColor, iconStyle, actions = actions)
                        }
                    }
                    FOOTER -> LetterHeader("Diable Launcher", textColor)
                    null -> Unit
                    else -> {
                        LetterHeader(letter.toString(), textColor)
                        groupedApps[letter].orEmpty().forEach { app ->
                            key(app.packageName) {
                                HomeAppRow(app, accentColor, textColor, iconStyle, actions = actions)
                            }
                        }
                    }
                }
            }
        }

        if (!hideAlphabet) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(72.dp)
                    .align(Alignment.CenterEnd)
                    .onGloballyPositioned { dragBoxTop = it.positionInRoot().y }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset -> nearestLetter(offset.y)?.let(::jumpTo) },
                            onDoubleTap = { currentActions.onAlphabetDoubleTap() },
                            onLongPress = { offset ->
                                when (val letter = nearestLetter(offset.y)) {
                                    STAR -> currentActions.onStarLongPress()
                                    null -> Unit
                                    else -> jumpTo(letter)
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        val cancelDistance = 160.dp.toPx()
                        detectDragGestures(
                            onDragStart = { start ->
                                cancelled = false
                                touchY = start.y
                            },
                            onDrag = { change, _ ->
                                touchY = change.position.y
                                // Dragging well away from the alphabet abandons the scrub.
                                cancelled = change.position.x < -cancelDistance
                            },
                            onDragEnd = {
                                val letter = selectedLetter
                                if (!cancelled && letter != null) jumpTo(letter)
                                touchY = null
                                cancelled = false
                            },
                            onDragCancel = {
                                touchY = null
                                cancelled = false
                            },
                        )
                    },
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .onGloballyPositioned { columnHeight = it.size.height.toFloat() },
                horizontalAlignment = Alignment.End,
            ) {
                alphabet.forEach { letter ->
                    DiableLetter(
                        letter = letter,
                        offset = if (cancelled) 0f else letterOffsets[letter] ?: 0f,
                        color = textColor,
                        onPositionUpdate = { x, y ->
                            letterPositions[letter] = y
                            letterXPositions[letter] = x
                        },
                    )
                }
            }
        }

        val peak = letterAtPeak
        if (scrubbing && peak != null && touchRootY != null) {
            val circleSize = 48.dp
            val radius = with(density) { (circleSize / 2f).toPx() }
            // Letter positions already include their bend, so sit just left of the letter.
            val centerX = (letterXPositions[peak] ?: 0f) - with(density) { 34.dp.toPx() }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = centerX - radius - boxOrigin.x
                        translationY = touchRootY - radius - boxOrigin.y
                    }
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = peak.toString(),
                    color = Color(0xFF132569),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun LetterHeader(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

/**
 * An app on the home screen. Tap launches, long-press opens the app menu, swiping left
 * also launches, swiping right opens the app's pop-up (shortcuts and notifications).
 * Waiting notifications show as a dot and a one-line preview, like Diable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeAppRow(
    app: AppInfo,
    accentColor: Color,
    textColor: Color,
    iconStyle: IconStyle,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    notifications: List<AppNotification> = emptyList(),
    actions: HomeListActions,
) {
    var rowBottom by remember { mutableFloatStateOf(0f) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val shift by animateFloatAsState(
        targetValue = dragX.coerceIn(-120f, 120f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "rowSwipe",
    )
    val currentActions by rememberUpdatedState(actions)
    val latest = notifications.firstOrNull()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowBottom = it.positionInRoot().y + it.size.height }
            .pointerInput(app.packageName) {
                val threshold = 56.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        dragX += delta
                    },
                    onDragEnd = {
                        when {
                            dragX < -threshold -> currentActions.onLaunch(app)
                            dragX > threshold -> currentActions.onSwipeRight(app, rowBottom)
                        }
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f },
                )
            }
            .combinedClickable(
                onClick = { actions.onLaunch(app) },
                onLongClick = { actions.onLongPress(app) },
            )
            .graphicsLayer { translationX = shift * 0.4f }
            // 48dp icon + 5dp above/below == Diable's 58dp row pitch.
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AppIcon(app = app, iconStyle = iconStyle, accentColor = accentColor)
            if (notifications.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                )
            }
        }
        if (!showLabel) return@Row
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (latest != null) {
                Text(
                    text = listOf(latest.title, latest.text).filter { it.isNotBlank() }.joinToString(": "),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A pop-up folder in favorites: a mini grid of its apps; tap opens the pop-up card. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRow(
    entry: FavoriteEntry.Folder,
    accentColor: Color,
    textColor: Color,
    iconStyle: IconStyle,
    showLabel: Boolean,
    actions: HomeListActions,
) {
    var rowBottom by remember { mutableFloatStateOf(0f) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowBottom = it.positionInRoot().y + it.size.height }
            .combinedClickable(
                onClick = { actions.onFolderOpen(entry.folder, rowBottom) },
                onLongClick = { actions.onFolderLongPress(entry.folder) },
            )
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolderIcon(entry.apps, accentColor, iconStyle)
        if (!showLabel) return@Row
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = entry.folder.name,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun FolderIcon(apps: List<AppInfo>, accentColor: Color, iconStyle: IconStyle, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size + 4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy((-4).dp)) {
            apps.take(4).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    row.forEach { app ->
                        AppIcon(app = app, size = size / 2 - 4.dp, iconStyle = iconStyle, accentColor = accentColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    textColor: Color,
    onClick: (Float) -> Unit,
) {
    var rowBottom by remember { mutableFloatStateOf(0f) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowBottom = it.positionInRoot().y + it.size.height }
            .combinedClickableCompat { onClick(rowBottom) }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit) =
    this.combinedClickable(onClick = onClick)

@Composable
fun DiableLetter(
    letter: Char,
    offset: Float,
    onPositionUpdate: (Float, Float) -> Unit,
    color: Color = Color.White,
) {
    val animatedOffset by animateFloatAsState(
        targetValue = offset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "letterOffset",
    )

    Text(
        text = letter.toString(),
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .offset(x = (-animatedOffset).dp)
            .onGloballyPositioned { coordinates ->
                val rect = coordinates.positionInRoot()
                onPositionUpdate(
                    rect.x + coordinates.size.width / 2f,
                    rect.y + coordinates.size.height / 2f,
                )
            },
    )
}
