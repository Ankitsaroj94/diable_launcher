package `in`.ankitsaroj.diable.ui.components

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted

/**
 * A row in one of Diable's full-screen settings lists (not the card sheets).
 *
 * Geometry taken from the reference app: 56dp icon gutter starting at 24dp, so labels
 * land at 80dp; rows are 113px tall with a summary and 83px without.
 */
@Composable
fun DiableSettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    /**
     * Reserve the icon gutter even with no icon. Diable's Productivity rows are
     * iconless but still align at 80dp; its legal links sit flush at the margin.
     */
    iconGutter: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconGutter) {
            Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.CenterStart) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DiableAccentText,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = DiableText, fontSize = 17.sp)
            if (summary != null) {
                Text(
                    text = summary,
                    color = DiableTextMuted,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing?.invoke()
    }
}
