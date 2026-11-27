package `in`.ankitsaroj.diable.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Drawn clock faces. Each one is an original drawing of a well-known public idea
 * (analog hands, seven-segment digits, the 1975 Mengenlehreuhr lamp layout, spoken
 * time). Nothing here is copied from another project's artwork or source.
 *
 * Style ids continue after the type treatments in [ClockTimeDisplay], from 48.
 */
const val OPEN_CLOCK_FIRST = 48
const val OPEN_CLOCK_COUNT = 20

@Composable
fun OpenClockFace(
    styleId: Int,
    time: String,
    amPm: String,
    accentColor: Color,
) {
    val (hour, minute) = parseHourMinute(time)
    when (styleId) {
        48 -> AnalogFace(hour, minute, accentColor, AnalogMarks.Ticks)
        49 -> AnalogFace(hour, minute, accentColor, AnalogMarks.Cardinal)
        50 -> AnalogFace(hour, minute, accentColor, AnalogMarks.Hours)
        51 -> AnalogFace(hour, minute, accentColor, AnalogMarks.Roman)
        52 -> SevenSegmentFace(hour, minute, accentColor, stacked = false)
        53 -> BcdFace(hour, minute, accentColor)
        54 -> MengenlehreuhrFace(hour, minute, accentColor)
        55 -> SpokenFace(hour, minute, accentColor)
        56 -> MinuteRingFace(hour, minute, accentColor)
        57 -> DualArcFace(hour, minute, accentColor)
        58 -> HourDotsFace(hour, minute, accentColor)
        59 -> BarFace(hour, minute, accentColor)
        60 -> FlipCardFace(hour, minute, accentColor)
        61 -> MinuteHandFace(hour, minute, accentColor)
        62 -> DayRingFace(hour, minute, accentColor)
        63 -> GlowDigitFace(time, amPm, accentColor)
        64 -> BlockRowFace(hour, minute, accentColor)
        65 -> ConcentricFace(hour, minute, accentColor)
        66 -> SevenSegmentFace(hour, minute, accentColor, stacked = true)
        else -> OutlinedSplitFace(hour, minute, accentColor)
    }
}

private enum class AnalogMarks { Ticks, Cardinal, Hours, Roman }

@Composable
private fun AnalogFace(hour: Int, minute: Int, color: Color, marks: AnalogMarks) {
    Canvas(Modifier.size(156.dp)) {
        val radius = size.minDimension / 2f * 0.92f
        drawCircle(color.copy(alpha = 0.85f), radius = radius, style = Stroke(width = 2.dp.toPx()))
        val labelRadius = radius * 0.72f
        when (marks) {
            AnalogMarks.Ticks -> {
                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30 - 90).toDouble())
                    val inner = radius * if (i % 3 == 0) 0.78f else 0.86f
                    val outer = radius * 0.94f
                    drawLine(
                        color,
                        start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                        end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                        strokeWidth = if (i % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            AnalogMarks.Cardinal -> {
                listOf(0 to "12", 3 to "3", 6 to "6", 9 to "9").forEach { (index, label) ->
                    drawDialLabel(label, index * 30f - 90f, labelRadius, color, 18.sp.toPx())
                }
            }
            AnalogMarks.Hours -> {
                for (i in 1..12) {
                    drawDialLabel(i.toString(), i * 30f - 90f, labelRadius, color, 14.sp.toPx())
                }
            }
            AnalogMarks.Roman -> {
                val roman = arrayOf("XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI")
                roman.forEachIndexed { index, label ->
                    drawDialLabel(label, index * 30f - 90f, labelRadius, color, 13.sp.toPx())
                }
            }
        }
        drawHands(hour, minute, color, radius)
    }
}

@Composable
private fun SevenSegmentFace(hour: Int, minute: Int, color: Color, stacked: Boolean) {
    val text = "%02d%02d".format(hour, minute)
    if (stacked) {
        Column {
            SegmentRow(text.take(2), color)
            Spacer(Modifier.height(6.dp))
            SegmentRow(text.drop(2), color.copy(alpha = 0.7f))
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentRow(text.take(2), color)
            Text(":", color = color, fontSize = 28.sp, modifier = Modifier.padding(horizontal = 4.dp))
            SegmentRow(text.drop(2), color)
        }
    }
}

@Composable
private fun SegmentRow(digits: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        digits.forEach { char ->
            Canvas(Modifier.size(width = 36.dp, height = 60.dp)) {
                drawSegments(char.digitToInt(), color)
            }
        }
    }
}

@Composable
private fun BcdFace(hour: Int, minute: Int, color: Color) {
    val digits = listOf(hour / 10, hour % 10, minute / 10, minute % 10)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        digits.forEach { digit ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (bit in 3 downTo 0) {
                    val on = (digit shr bit) and 1 == 1
                    Canvas(Modifier.size(12.dp)) {
                        drawCircle(if (on) color else color.copy(alpha = 0.22f))
                    }
                }
            }
        }
    }
}

/** Lamp rows of the Mengenlehreuhr in Berlin: five-hour, one-hour, five-minute, one-minute. */
@Composable
private fun MengenlehreuhrFace(hour: Int, minute: Int, color: Color) {
    val fiveHour = hour / 5
    val oneHour = hour % 5
    val fiveMinute = minute / 5
    val oneMinute = minute % 5
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LampRow(4, fiveHour, color)
        LampRow(4, oneHour, color)
        LampRow(11, fiveMinute, color)
        LampRow(4, oneMinute, color.copy(alpha = 0.75f))
    }
}

@Composable
private fun LampRow(count: Int, lit: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(count) { index ->
            Canvas(Modifier.size(if (count > 6) 10.dp else 14.dp)) {
                drawCircle(if (index < lit) color else color.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
private fun SpokenFace(hour: Int, minute: Int, color: Color) {
    Text(
        text = spokenTime(hour, minute),
        color = color,
        fontSize = 28.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 32.sp,
    )
}

@Composable
private fun MinuteRingFace(hour: Int, minute: Int, color: Color) {
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                color = color.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = minute / 60f * 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Text("%d".format(hour % 12).let { if (it == "0") "12" else it }, color = color, fontSize = 36.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun DualArcFace(hour: Int, minute: Int, color: Color) {
    Canvas(Modifier.size(130.dp)) {
        val hourSweep = ((hour % 12) + minute / 60f) / 12f * 360f
        drawArc(
            color,
            startAngle = -90f,
            sweepAngle = hourSweep.coerceAtLeast(2f),
            useCenter = false,
            topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
            size = Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()),
            style = Stroke(10.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color.copy(alpha = 0.55f),
            startAngle = -90f,
            sweepAngle = minute / 60f * 360f,
            useCenter = false,
            topLeft = Offset(28.dp.toPx(), 28.dp.toPx()),
            size = Size(size.width - 56.dp.toPx(), size.height - 56.dp.toPx()),
            style = Stroke(10.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun HourDotsFace(hour: Int, minute: Int, color: Color) {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(12) { index ->
                Canvas(Modifier.size(8.dp)) {
                    drawCircle(if (index < h12) color else color.copy(alpha = 0.25f))
                }
            }
        }
        Text("%02d".format(minute), color = color, fontSize = 40.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun BarFace(hour: Int, minute: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProportionBar((hour % 12) / 12f, color, 160.dp)
        ProportionBar(minute / 60f, color.copy(alpha = 0.6f), 160.dp)
    }
}

@Composable
private fun ProportionBar(fraction: Float, color: Color, width: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .width(width)
            .height(10.dp)
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(5.dp)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                .background(color, RoundedCornerShape(5.dp)),
        )
    }
}

@Composable
private fun FlipCardFace(hour: Int, minute: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FlipCard("%02d".format(hour), color)
        Text(":", color = color, fontSize = 28.sp)
        FlipCard("%02d".format(minute), color)
    }
}

@Composable
private fun FlipCard(value: String, color: Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(value, color = color, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun MinuteHandFace(hour: Int, minute: Int, color: Color) {
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(color.copy(alpha = 0.8f), style = Stroke(1.5.dp.toPx()))
            val angle = Math.toRadians((minute * 6 - 90).toDouble())
            val length = size.minDimension / 2f * 0.78f
            drawLine(
                color,
                center,
                Offset(center.x + cos(angle).toFloat() * length, center.y + sin(angle).toFloat() * length),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Text("%d".format(if (hour % 12 == 0) 12 else hour % 12), color = color, fontSize = 22.sp)
    }
}

@Composable
private fun DayRingFace(hour: Int, minute: Int, color: Color) {
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(color.copy(alpha = 0.35f), style = Stroke(2.dp.toPx()))
            val fraction = (hour + minute / 60f) / 24f
            val angle = Math.toRadians((fraction * 360f - 90f).toDouble())
            val length = size.minDimension / 2f * 0.82f
            drawCircle(
                color,
                radius = 6.dp.toPx(),
                center = Offset(center.x + cos(angle).toFloat() * length, center.y + sin(angle).toFloat() * length),
            )
        }
        Text("%02d:%02d".format(hour, minute), color = color, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun GlowDigitFace(time: String, amPm: String, color: Color) {
    Column {
        Text(
            text = time,
            color = color,
            fontSize = 56.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
        )
        Text(amPm, color = color.copy(alpha = 0.6f), fontSize = 14.sp, letterSpacing = 3.sp)
    }
}

@Composable
private fun BlockRowFace(hour: Int, minute: Int, color: Color) {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(12) { index ->
                Box(
                    Modifier
                        .size(width = 8.dp, height = if (index < h12) 22.dp else 8.dp)
                        .background(if (index < h12) color else color.copy(alpha = 0.25f), RoundedCornerShape(2.dp)),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(12) { index ->
                val filled = index < minute / 5
                Box(
                    Modifier
                        .size(width = 8.dp, height = if (filled) 14.dp else 6.dp)
                        .background(if (filled) color.copy(alpha = 0.7f) else color.copy(alpha = 0.2f), RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
private fun ConcentricFace(hour: Int, minute: Int, color: Color) {
    Canvas(Modifier.size(130.dp)) {
        drawCircle(color.copy(alpha = 0.3f), style = Stroke(8.dp.toPx()))
        drawArc(
            color,
            startAngle = -90f,
            sweepAngle = ((hour % 12) + minute / 60f) / 12f * 360f,
            useCenter = false,
            style = Stroke(8.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(color.copy(alpha = 0.25f), radius = size.minDimension * 0.28f, style = Stroke(8.dp.toPx()))
        drawArc(
            color.copy(alpha = 0.85f),
            startAngle = -90f,
            sweepAngle = minute / 60f * 360f,
            useCenter = false,
            topLeft = Offset(size.width * 0.22f, size.height * 0.22f),
            size = Size(size.width * 0.56f, size.height * 0.56f),
            style = Stroke(8.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun OutlinedSplitFace(hour: Int, minute: Int, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "%02d".format(hour),
            color = color,
            fontSize = 64.sp,
            fontWeight = FontWeight.Thin,
            fontFamily = FontFamily.SansSerif,
        )
        Text(
            "%02d".format(minute),
            color = color.copy(alpha = 0.55f),
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
    }
}

private fun DrawScope.drawHands(hour: Int, minute: Int, color: Color, radius: Float) {
    val hourAngle = Math.toRadians((((hour % 12) + minute / 60f) * 30f - 90f).toDouble())
    val minuteAngle = Math.toRadians((minute * 6f - 90f).toDouble())
    fun end(angle: Double, length: Float) = Offset(
        center.x + cos(angle).toFloat() * length,
        center.y + sin(angle).toFloat() * length,
    )
    drawLine(color, center, end(hourAngle, radius * 0.5f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color, center, end(minuteAngle, radius * 0.74f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color, radius = 4.dp.toPx())
}

private fun DrawScope.drawDialLabel(text: String, angleDeg: Float, radius: Float, color: Color, textSizePx: Float) {
    val angle = Math.toRadians(angleDeg.toDouble())
    val x = center.x + cos(angle).toFloat() * radius
    val y = center.y + sin(angle).toFloat() * radius
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color = color.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            this.textSize = textSizePx
            isAntiAlias = true
        }
        val baseline = y - (paint.ascent() + paint.descent()) / 2f
        canvas.nativeCanvas.drawText(text, x, baseline, paint)
    }
}

private fun DrawScope.drawSegments(digit: Int, color: Color) {
    val on = when (digit) {
        0 -> booleanArrayOf(true, true, true, true, true, true, false)
        1 -> booleanArrayOf(false, true, true, false, false, false, false)
        2 -> booleanArrayOf(true, true, false, true, true, false, true)
        3 -> booleanArrayOf(true, true, true, true, false, false, true)
        4 -> booleanArrayOf(false, true, true, false, false, true, true)
        5 -> booleanArrayOf(true, false, true, true, false, true, true)
        6 -> booleanArrayOf(true, false, true, true, true, true, true)
        7 -> booleanArrayOf(true, true, true, false, false, false, false)
        8 -> booleanArrayOf(true, true, true, true, true, true, true)
        else -> booleanArrayOf(true, true, true, true, false, true, true)
    }
    val w = size.width
    val h = size.height
    val t = w * 0.16f
    val gap = t * 0.45f
    fun horiz(y: Float, lit: Boolean) {
        if (!lit) return
        drawRoundRect(
            color,
            topLeft = Offset(t, y),
            size = Size(w - 2 * t, t),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(t / 2f, t / 2f),
        )
    }
    fun vert(x: Float, y: Float, lit: Boolean) {
        if (!lit) return
        drawRoundRect(
            color,
            topLeft = Offset(x, y),
            size = Size(t, h / 2f - t - gap),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(t / 2f, t / 2f),
        )
    }
    horiz(0f, on[0])
    vert(w - t, t * 0.6f, on[1])
    vert(w - t, h / 2f + gap * 0.2f, on[2])
    horiz(h - t, on[3])
    vert(0f, h / 2f + gap * 0.2f, on[4])
    vert(0f, t * 0.6f, on[5])
    horiz(h / 2f - t / 2f, on[6])
}

private val HOUR_WORDS = arrayOf(
    "twelve", "one", "two", "three", "four", "five",
    "six", "seven", "eight", "nine", "ten", "eleven",
)

private fun spokenTime(hour: Int, minute: Int): String {
    val name = HOUR_WORDS[hour % 12]
    val next = HOUR_WORDS[(hour + 1) % 12]
    return when (minute) {
        0 -> "$name o'clock"
        15 -> "quarter past $name"
        30 -> "half past $name"
        45 -> "quarter to $next"
        in 1..29 -> "$minute past $name"
        else -> "${60 - minute} to $next"
    }.replaceFirstChar { it.uppercase() }
}

private fun parseHourMinute(time: String): Pair<Int, Int> {
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.take(2)?.toIntOrNull() ?: 0
    return hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
}
