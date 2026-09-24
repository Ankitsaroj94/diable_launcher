package `in`.ankitsaroj.diable.ui.components

import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted

enum class DiableRowTrailing {
    None,
    Chevron,
    Toggle,
}

@Composable
fun DiableListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailing: DiableRowTrailing = DiableRowTrailing.None,
    toggleChecked: Boolean = false,
    onToggleChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickable = onClick != null || trailing == DiableRowTrailing.Chevron
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (clickable && trailing != DiableRowTrailing.Toggle) {
                    Modifier.clickable { onClick?.invoke() }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = DiableText)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DiableTextMuted,
                )
            }
        }
        when (trailing) {
            DiableRowTrailing.Chevron -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DiableTextMuted,
                )
            }
            DiableRowTrailing.Toggle -> {
                DiableToggle(
                    checked = toggleChecked,
                    onCheckedChange = { onToggleChange?.invoke(it) },
                )
            }
            DiableRowTrailing.None -> Unit
        }
    }
}
