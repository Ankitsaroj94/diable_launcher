package `in`.ankitsaroj.diable.ui.components

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted

/**
 * One row of a Diable settings sheet: lavender icon, title, optional subtitle, and an
 * optional trailing toggle. Each row is its own rounded card, matching Diable's sheets
 * (card inset 32dp, 32dp icon, 4dp between cards).
 */
@Composable
fun DiableSettingCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    linkText: String? = null,
    onLinkClick: (() -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    // Diable's sheet card: 621px wide at x=49, 96px tall, 6px gap -> 102px pitch, r=16.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .heightIn(min = 64.dp)
            .background(DiableCard, RoundedCornerShape(16.dp))
            // The whole card is the target: its action if it has one, else the switch.
            .then(
                when {
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    checked != null && onCheckedChange != null ->
                        Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                    else -> Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LavenderIconCircle(icon, size = 32.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DiableText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = DiableTextMuted,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (linkText != null) {
                Text(
                    text = linkText,
                    color = DiableAccentText,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .then(
                            if (onLinkClick != null) {
                                Modifier.clickable(onClick = onLinkClick)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
        if (checked != null && onCheckedChange != null) {
            Spacer(modifier = Modifier.width(12.dp))
            DiableToggle(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
