@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package `in`.ankitsaroj.diable.ui.home

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.CalendarEventPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClockWidget(
    clockStyleId: Int,
    showBattery: Boolean,
    showWeather: Boolean,
    showEvent: Boolean,
    weatherTempF: Float?,
    event: CalendarEventPreview?,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White,
    showDate: Boolean = true,
    weatherCelsius: Boolean = false,
    /** Diable: the time opens the clock app, the date the agenda, weather the forecast. */
    onTimeClick: (() -> Unit)? = null,
    onDateClick: (() -> Unit)? = null,
    onWeatherClick: (() -> Unit)? = null,
    /** Long-pressing any part of the clock opens the widget sheet. */
    onLongClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    fun Modifier.target(onClick: (() -> Unit)?) =
        if (onClick == null && onLongClick == null) {
            this
        } else {
            this.combinedClickable(
                interactionSource = null,
                indication = null,
                onLongClick = onLongClick,
                onClick = { onClick?.invoke() },
            )
        }
    // The face only shows h:mm, so waking once a second was 60x more work than needed.
    // Sleep to the next minute boundary instead, and only while actually visible.
    val lifecycleOwner = LocalLifecycleOwner.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                now = System.currentTimeMillis()
                delay(60_000L - (now % 60_000L))
            }
        }
    }
    val is24h = remember(context) { android.text.format.DateFormat.is24HourFormat(context) }
    val timeFmt = remember(is24h) {
        SimpleDateFormat(if (is24h) "H:mm" else "h:mm", Locale.getDefault())
    }
    val amPmFmt = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val timeStr = remember(now) { timeFmt.format(Date(now)) }
    val amPm = remember(now) { amPmFmt.format(Date(now)) }
    val dateStr = remember(now) { dateFmt.format(Date(now)) }
    val batteryPct = remember { readBatteryLevel(context) }

    val dayName = remember(dateStr) { dateStr.substringBefore(",").trim() + "." }

    // Horizontal inset comes from the caller so the clock lines up with the app rows.
    Column(modifier = modifier) {
        Box(modifier = Modifier.target(onTimeClick)) {
            if (isDayNameStyle(clockStyleId)) {
                ClockDayNameDisplay(clockStyleId, dayName, accentColor)
            } else {
                ClockTimeDisplay(
                    styleId = clockStyleId,
                    time = timeStr,
                    amPm = amPm,
                    accentColor = accentColor,
                )
            }
        }
        if (showDate && !isDayNameStyle(clockStyleId)) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(modifier = Modifier.target(onDateClick)) {
                ClockDateDisplay(styleId = clockStyleId, date = dateStr, accentColor = accentColor)
            }
        }
        // Each part of the meta line is its own target: time → clock, date → agenda,
        // weather → forecast. One shared target sent every tap to the weather.
        val metaParts = buildList<Pair<String, (() -> Unit)?>> {
            // A day-name face shows no clock of its own, so the time joins the meta line.
            if (isDayNameStyle(clockStyleId)) {
                add(timeStr to onTimeClick)
                if (showDate) add(dateStr.substringAfter(",").trim() to onDateClick)
            }
            if (showBattery && batteryPct != null) add("$batteryPct%" to onDateClick)
            if (showWeather && weatherTempF != null) {
                val temp = if (weatherCelsius) (weatherTempF - 32f) * 5f / 9f else weatherTempF
                add("${Math.round(temp)}°" to (onWeatherClick ?: onDateClick))
            }
        }
        if (metaParts.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = 6.dp)) {
                metaParts.forEachIndexed { i, (label, onClick) ->
                    if (i > 0) {
                        Text("  ·  ", color = accentColor.copy(alpha = 0.75f), fontSize = 13.sp)
                    }
                    Text(
                        text = label,
                        color = accentColor.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        modifier = Modifier.target(onClick),
                    )
                }
            }
        }
        if (showEvent && event != null) {
            val dayLabel = when (event.daysUntil) {
                0 -> "today"
                1 -> "1d"
                else -> "${event.daysUntil}d"
            }
            Text(
                text = "${event.title} · $dayLabel",
                color = accentColor.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp).target(onDateClick),
            )
        }
    }
}

@Composable
fun ClockTimeDisplay(
    styleId: Int,
    time: String,
    amPm: String,
    accentColor: Color,
) {
    val hm = time.split(":")
    val hh = hm.getOrElse(0) { time }
    val mm = hm.getOrElse(1) { "" }
    when (styleId) {
        // Default style matches Diable: time only, no AM/PM marker.
        0 -> Text(time, fontSize = 52.sp, fontWeight = FontWeight.Normal, color = accentColor)
        1 -> Row(verticalAlignment = Alignment.Bottom) {
            Text(outlineZeros(time), fontSize = 52.sp, fontWeight = FontWeight.Black, color = accentColor)
            Text(amPm, fontSize = 16.sp, color = accentColor.copy(0.7f), modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
        }
        2 -> Column {
            Text(time, fontSize = 48.sp, fontStyle = FontStyle.Italic, color = accentColor)
            Text(amPm, fontSize = 14.sp, fontStyle = FontStyle.Italic, color = accentColor.copy(0.75f))
        }
        3 -> Text(time, fontSize = 64.sp, fontWeight = FontWeight.ExtraLight, color = accentColor, letterSpacing = 4.sp)
        4 -> Column(horizontalAlignment = Alignment.Start) {
            Text(hh, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = accentColor, lineHeight = 40.sp)
            Text(mm, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = accentColor, lineHeight = 40.sp)
            Text(amPm, fontSize = 14.sp, color = accentColor.copy(0.7f))
        }
        5 -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(time, fontSize = 42.sp, fontWeight = FontWeight.Medium, color = accentColor)
            Text(" $amPm", fontSize = 42.sp, fontWeight = FontWeight.Light, color = accentColor.copy(0.5f))
        }
        6 -> Text(time.uppercase(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = 2.sp)
        7 -> Text(time, fontSize = 50.sp, fontWeight = FontWeight.SemiBold, color = accentColor, textDecoration = TextDecoration.Underline)
        8 -> Column(horizontalAlignment = Alignment.End) {
            Text(time, fontSize = 54.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(amPm, fontSize = 12.sp, color = accentColor.copy(0.6f))
        }
        9 -> Text(time, fontSize = 58.sp, fontWeight = FontWeight.Thin, color = accentColor)
        10 -> Row(verticalAlignment = Alignment.Top) {
            Text(hh, fontSize = 72.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Column {
                Text(":", fontSize = 36.sp, color = accentColor.copy(0.5f))
                Text(mm, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }
        }
        11 -> Text(time, fontSize = 46.sp, color = accentColor, fontStyle = FontStyle.Italic)
        12 -> Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(time, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(amPm.lowercase(), fontSize = 20.sp, fontWeight = FontWeight.Light, color = accentColor.copy(0.65f), modifier = Modifier.padding(bottom = 6.dp))
        }
        13 -> Row(verticalAlignment = Alignment.Bottom) {
            Text(time, fontSize = 52.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(amPm, fontSize = 15.sp, color = accentColor.copy(0.7f), modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        }
        // 14 is the date-only style; it renders no time at all.
        14 -> Unit
        // Hour and minute on one baseline, minutes dimmed back.
        15 -> Row(verticalAlignment = Alignment.Bottom) {
            Text(hh, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(mm, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = accentColor.copy(0.55f), modifier = Modifier.padding(start = 4.dp, bottom = 5.dp))
        }
        // Wide-tracked uppercase, editorial feel.
        16 -> Text(time, fontSize = 38.sp, fontWeight = FontWeight.Medium, color = accentColor, letterSpacing = 10.sp)
        // Monospaced digits, terminal feel.
        17 -> Text(time, fontSize = 46.sp, fontWeight = FontWeight.Medium, color = accentColor, fontFamily = FontFamily.Monospace)
        // Serif, classic.
        18 -> Text(time, fontSize = 52.sp, color = accentColor, fontFamily = FontFamily.Serif)
        // Serif italic, editorial.
        19 -> Text(time, fontSize = 50.sp, color = accentColor, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
        // Stacked with the minutes outdented.
        20 -> Column {
            Text(hh, fontSize = 50.sp, fontWeight = FontWeight.Black, color = accentColor, lineHeight = 46.sp)
            Text(mm, fontSize = 50.sp, fontWeight = FontWeight.Thin, color = accentColor, lineHeight = 46.sp, modifier = Modifier.padding(start = 20.dp))
        }
        // Dot separator instead of a colon.
        21 -> Text("$hh · $mm", fontSize = 46.sp, fontWeight = FontWeight.Light, color = accentColor)
        // Zeros drawn as rings, no AM marker.
        22 -> Text(outlineZeros(time), fontSize = 54.sp, fontWeight = FontWeight.Medium, color = accentColor)
        // Very large and very light.
        23 -> Text(time, fontSize = 76.sp, fontWeight = FontWeight.Thin, color = accentColor, lineHeight = 76.sp)
        // Small and quiet.
        24 -> Text(time, fontSize = 30.sp, fontWeight = FontWeight.Medium, color = accentColor, letterSpacing = 2.sp)
        // Minutes raised like an exponent.
        25 -> Row(verticalAlignment = Alignment.Top) {
            Text(hh, fontSize = 58.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(mm, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accentColor.copy(0.8f), modifier = Modifier.padding(start = 3.dp, top = 6.dp))
        }
        // Monospace, stacked, wide-tracked.
        26 -> Column {
            Text(hh, fontSize = 40.sp, color = accentColor, fontFamily = FontFamily.Monospace, letterSpacing = 6.sp, lineHeight = 40.sp)
            Text(mm, fontSize = 40.sp, color = accentColor.copy(0.6f), fontFamily = FontFamily.Monospace, letterSpacing = 6.sp, lineHeight = 40.sp)
        }
        // Underlined serif with the marker trailing.
        27 -> Row(verticalAlignment = Alignment.Bottom) {
            Text(time, fontSize = 46.sp, color = accentColor, fontFamily = FontFamily.Serif, textDecoration = TextDecoration.Underline)
            Text(amPm.lowercase(), fontSize = 16.sp, color = accentColor.copy(0.6f), modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
        }
        // Minutes under the hour, both heavy, tight leading.
        28 -> Column {
            Text(hh, fontSize = 54.sp, fontWeight = FontWeight.Bold, color = accentColor, lineHeight = 48.sp)
            Text(mm, fontSize = 54.sp, fontWeight = FontWeight.Bold, color = accentColor.copy(0.45f), lineHeight = 48.sp)
        }
        // Serif, stacked, generous tracking.
        29 -> Column {
            Text(hh, fontSize = 44.sp, color = accentColor, fontFamily = FontFamily.Serif, letterSpacing = 4.sp, lineHeight = 44.sp)
            Text(mm, fontSize = 44.sp, color = accentColor, fontFamily = FontFamily.Serif, letterSpacing = 4.sp, lineHeight = 44.sp)
        }
        // Hour large, minutes small and baseline-aligned to the right.
        30 -> Row(verticalAlignment = Alignment.Bottom) {
            Text(hh, fontSize = 64.sp, fontWeight = FontWeight.Light, color = accentColor)
            Text(mm, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = accentColor.copy(0.7f), modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
        }
        // All-caps marker under a medium clock.
        31 -> Column {
            Text(time, fontSize = 44.sp, fontWeight = FontWeight.Medium, color = accentColor)
            Text(amPm.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor.copy(0.6f), letterSpacing = 4.sp)
        }
        // Monospace with a wide gap instead of a colon.
        32 -> Text("$hh $mm", fontSize = 44.sp, color = accentColor, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        // Thin serif, oversized.
        33 -> Text(time, fontSize = 68.sp, fontWeight = FontWeight.Light, color = accentColor, fontFamily = FontFamily.Serif)
        // Ring zeros, stacked.
        34 -> Column {
            Text(outlineZeros(hh), fontSize = 46.sp, fontWeight = FontWeight.Medium, color = accentColor, lineHeight = 44.sp)
            Text(outlineZeros(mm), fontSize = 46.sp, fontWeight = FontWeight.Medium, color = accentColor.copy(0.6f), lineHeight = 44.sp)
        }
        // Compact bold with a lowercase marker inline.
        35 -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(time, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(amPm.lowercase(), fontSize = 13.sp, color = accentColor.copy(0.55f), modifier = Modifier.padding(start = 6.dp))
        }
        // Very wide tracking, thin, no marker.
        36 -> Text(time, fontSize = 34.sp, fontWeight = FontWeight.Light, color = accentColor, letterSpacing = 14.sp)
        // Italic serif stacked.
        37 -> Column {
            Text(hh, fontSize = 48.sp, color = accentColor, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, lineHeight = 46.sp)
            Text(mm, fontSize = 48.sp, color = accentColor.copy(0.55f), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, lineHeight = 46.sp, modifier = Modifier.padding(start = 24.dp))
        }
        // Underlined monospace.
        38 -> Text(time, fontSize = 42.sp, color = accentColor, fontFamily = FontFamily.Monospace, textDecoration = TextDecoration.Underline)
        // Heavy hour, hairline minutes, same baseline.
        39 -> Row(verticalAlignment = Alignment.Bottom) {
            Text(hh, fontSize = 58.sp, fontWeight = FontWeight.Black, color = accentColor)
            Text(mm, fontSize = 58.sp, fontWeight = FontWeight.Thin, color = accentColor, modifier = Modifier.padding(start = 6.dp))
        }
        else -> Text(time, fontSize = 52.sp, fontWeight = FontWeight.Normal, color = accentColor)
    }
}

@Composable
private fun ClockDateDisplay(styleId: Int, date: String, accentColor: Color) {
    when (styleId) {
        2, 11, 19 -> Text(date, fontSize = 15.sp, fontStyle = FontStyle.Italic, color = accentColor.copy(0.85f))
        3, 9, 16, 23, 36 -> Text(date.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Light, letterSpacing = 3.sp, color = accentColor.copy(0.65f))
        4 -> Text(date, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor.copy(0.9f))
        6 -> Text(date.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor.copy(0.75f))
        17, 26, 32, 38 -> Text(date, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = accentColor.copy(0.8f))
        18, 27, 29, 33 -> Text(date, fontSize = 15.sp, fontFamily = FontFamily.Serif, color = accentColor.copy(0.85f))
        37 -> Text(date, fontSize = 15.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, color = accentColor.copy(0.85f))
        else -> Text(date, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, color = accentColor.copy(0.85f))
    }
}

private fun outlineZeros(time: String): String =
    time.replace('0', '○')

private fun readBatteryLevel(context: Context): Int? {
    return try {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) (level * 100 / scale) else null
    } catch (_: Exception) {
        null
    }
}

/** Style id that renders the date line only, with no time — Diable offers one. */
const val CLOCK_STYLE_DATE_ONLY = 14

/**
 * Styles whose hero is the weekday rather than the time. Diable demotes the clock to a
 * "4:00 · Aug 30 · 100%" meta line for these, so the face layout differs, not just the
 * type treatment.
 */
val DAY_NAME_STYLES = 40..47

fun isDayNameStyle(styleId: Int) = styleId in DAY_NAME_STYLES

/** Renders the weekday hero for [DAY_NAME_STYLES]. */
@Composable
fun ClockDayNameDisplay(styleId: Int, day: String, accentColor: Color) {
    when (styleId) {
        40 -> Text(day, fontSize = 60.sp, fontWeight = FontWeight.Black, color = accentColor)
        41 -> Text(day, fontSize = 58.sp, color = accentColor, fontFamily = FontFamily.Cursive)
        42 -> Text(day, fontSize = 56.sp, fontWeight = FontWeight.Light, color = accentColor)
        43 -> Text(day, fontSize = 54.sp, color = accentColor, fontFamily = FontFamily.Serif)
        44 -> Text(
            day.uppercase(),
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 6.sp,
        )
        45 -> Text(
            day.uppercase(),
            fontSize = 64.sp,
            fontWeight = FontWeight.Thin,
            color = accentColor.copy(alpha = 0.85f),
            letterSpacing = 10.sp,
        )
        46 -> Text(
            day,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            color = accentColor.copy(alpha = 0.45f),
        )
        else -> Text(
            day.uppercase(),
            fontSize = 50.sp,
            color = accentColor,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 4.sp,
        )
    }
}

/** Total selectable clock styles, including [CLOCK_STYLE_DATE_ONLY]. */
const val CLOCK_STYLE_COUNT = 48

/**
 * A clock rendered with fixed demo values, for the style-picker tiles. Diable previews
 * every style against the same 4:00 / Thu, Aug 30 / 100% sample rather than live data.
 */
@Composable
fun ClockStylePreview(
    styleId: Int,
    modifier: Modifier = Modifier,
    time: String = "4:00",
    amPm: String = "AM",
    date: String = "Thu, Aug 30",
    battery: String = "100%",
) {
    // Scaling the density shrinks every sp and dp inside a style by the same factor, so a
    // tile stays faithful to the full-size clock instead of needing its own sizes.
    val scaled = Density(
        density = LocalDensity.current.density * 0.46f,
        fontScale = LocalDensity.current.fontScale,
    )
    val dayName = remember(date) { date.substringBefore(",").trim() + "." }
    Column(modifier = modifier) {
        CompositionLocalProvider(LocalDensity provides scaled) {
            when {
                isDayNameStyle(styleId) ->
                    ClockDayNameDisplay(styleId, dayName, Color.White)
                styleId != CLOCK_STYLE_DATE_ONLY ->
                    ClockTimeDisplay(styleId, time, amPm, Color.White)
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        // Day-name styles carry the time in the meta line, joined by middots.
        val meta = if (isDayNameStyle(styleId)) {
            listOfNotNull(time, date.substringAfter(",").trim(), battery.ifBlank { null })
        } else {
            listOfNotNull(date, battery.ifBlank { null })
        }
        Text(
            text = meta.joinToString(if (isDayNameStyle(styleId)) " · " else "  "),
            fontSize = 10.sp,
            color = Color.White,
        )
    }
}
