package `in`.ankitsaroj.diable.ui.home

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import `in`.ankitsaroj.diable.DiableApplication
import kotlin.math.abs

/**
 * A widget view that claims long-presses for the launcher, the way Launcher3 does.
 *
 * The widget's own RemoteViews consume every touch, so a long-press would otherwise just
 * trigger the widget's tap action. Intercepting only once the press has been held long
 * (and still) lets ordinary taps and scrolls reach the widget untouched.
 */
class LongPressWidgetHostView(context: Context) : AppWidgetHostView(context) {

    var onLongPress: (() -> Unit)? = null

    private var downX = 0f
    private var downY = 0f
    private var longPressed = false
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val downOnScreen = IntArray(2)
    private val nowOnScreen = IntArray(2)
    private val trigger = Runnable {
        // A pager or list that took over the gesture moves the view without always
        // cancelling it; a press that travelled with its parent was a swipe, not a hold.
        getLocationOnScreen(nowOnScreen)
        if (abs(nowOnScreen[0] - downOnScreen[0]) > slop || abs(nowOnScreen[1] - downOnScreen[1]) > slop) {
            return@Runnable
        }
        longPressed = true
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        // Cancel the press inside the widget so it doesn't also fire its tap.
        val cancel = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        for (i in 0 until childCount) getChildAt(i).dispatchTouchEvent(cancel)
        cancel.recycle()
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                getLocationOnScreen(downOnScreen)
                longPressed = false
                postDelayed(trigger, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE ->
                if (abs(ev.x - downX) > slop || abs(ev.y - downY) > slop) removeCallbacks(trigger)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(trigger)
        }
        return longPressed
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Seen before interception: a parent stealing the gesture sends CANCEL here.
        if (ev.actionMasked == MotionEvent.ACTION_CANCEL || ev.actionMasked == MotionEvent.ACTION_UP) {
            removeCallbacks(trigger)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(trigger)
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (longPressed) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                longPressed = false
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}

@Composable
fun WidgetHostSlot(
    appWidgetId: Int,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit = {},
) {
    if (appWidgetId < 0) return
    val latestLongPress by rememberUpdatedState(onLongPress)
    var hostView by remember { mutableStateOf<AppWidgetHostView?>(null) }

    // A different widget needs a new view; the factory only runs once per key.
    key(appWidgetId) {
    AndroidView(
        // Widgets pick their layout from the size the host reports (Launcher3 does this on
        // every resize). Never reporting one left size-aware widgets, like the clock,
        // drawing nothing at all.
        modifier = modifier.onSizeChanged { size ->
            val view = hostView ?: return@onSizeChanged
            reportWidgetSize(view, size.width, size.height)
        },
        factory = { ctx ->
            // The application's host is the one that listens for updates for the whole
            // process lifetime; a second host per slot stopped updates when it left.
            val host = (ctx.applicationContext as DiableApplication).appWidgetHost
            val manager = AppWidgetManager.getInstance(ctx)
            val view: AppWidgetHostView = try {
                host.createView(ctx, appWidgetId, manager.getAppWidgetInfo(appWidgetId))
            } catch (_: Exception) {
                LongPressWidgetHostView(ctx)
            }
            (view as? LongPressWidgetHostView)?.onLongPress = { latestLongPress() }
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            view.setPadding(0, 0, 0, 0)
            view.minimumHeight = ctx.resources.displayMetrics.density.times(80).toInt()
            hostView = view
            view
        },
    )
    }
}


/**
 * Diable Pro's widget stack: several widgets in one slot, swiped through sideways, with
 * page dots underneath. A single widget renders exactly like [WidgetHostSlot].
 */
@Composable
fun WidgetStack(
    ids: List<Int>,
    accent: Color,
    modifier: Modifier = Modifier,
    onLongPress: (Int) -> Unit,
) {
    if (ids.isEmpty()) return
    if (ids.size == 1) {
        WidgetHostSlot(appWidgetId = ids[0], modifier = modifier, onLongPress = { onLongPress(ids[0]) })
        return
    }
    val pager = rememberPagerState { ids.size }
    Column {
        // Keep every page composed: a widget view created only when swiped to missed the
        // host's first update and stayed blank.
        HorizontalPager(
            state = pager,
            key = { ids[it] },
            pageSpacing = 12.dp,
            beyondViewportPageCount = ids.size,
        ) { page ->
            WidgetHostSlot(appWidgetId = ids[page], modifier = modifier, onLongPress = { onLongPress(ids[page]) })
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(ids.size) { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (i == pager.currentPage) accent else Color.White.copy(alpha = 0.35f)),
                )
            }
        }
    }
}

private fun reportWidgetSize(view: AppWidgetHostView, widthPx: Int, heightPx: Int) {
    if (widthPx <= 0 || heightPx <= 0) return
    val density = view.resources.displayMetrics.density
    val w = (widthPx / density).toInt()
    val h = (heightPx / density).toInt()
    runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            view.updateAppWidgetSize(android.os.Bundle(), listOf(android.util.SizeF(w.toFloat(), h.toFloat())))
        } else {
            @Suppress("DEPRECATION")
            view.updateAppWidgetSize(null, w, h, w, h)
        }
    }
}
