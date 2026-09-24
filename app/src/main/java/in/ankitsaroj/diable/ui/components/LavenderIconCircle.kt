package `in`.ankitsaroj.diable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.ankitsaroj.diable.ui.theme.DiableIconCircle
import `in`.ankitsaroj.diable.ui.theme.DiableIconGlyph

@Composable
fun LavenderIconCircle(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    // Diable fills the disc solid and draws the glyph in dark navy on top.
    iconTint: Color = DiableIconGlyph,
    backgroundColor: Color = DiableIconCircle,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}
