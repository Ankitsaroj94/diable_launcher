package `in`.ankitsaroj.diable.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The floating launcher button.
 *
 * Diable puts a small persistent control at the bottom of the home screen: a tap runs
 * the primary action, a swipe up runs a second one, and a long-press opens its settings.
 */
@Composable
fun DiableButton(
    accentColor: Color,
    onTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    /** The glyph for the current tap action; the list being scrolled shows search instead. */
    icon: androidx.compose.ui.graphics.vector.ImageVector = androidx.compose.material.icons.Icons.Default.Add,
) {
    var dragTotal by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            // Diable's button: a 56dp solid accent disc at the bottom-right.
            .size(56.dp)
            .clip(CircleShape)
            .background(accentColor)
            .pointerInput(onTap, onLongPress) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() },
                )
            }
            .pointerInput(onSwipeUp) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragTotal < -60f) onSwipeUp()
                        dragTotal = 0f
                    },
                    onDragCancel = { dragTotal = 0f },
                    onDrag = { _, amount -> dragTotal += amount.y },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = "Diable Button",
            tint = Color(0xFF132569),
        )
    }
}
