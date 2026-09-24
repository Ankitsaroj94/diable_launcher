package `in`.ankitsaroj.diable.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.model.AppInfo
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted

@Composable
fun AppListItem(
    app: AppInfo,
    modifier: Modifier = Modifier,
    accentColor: Color = DiableAccent,
    subtitle: String? = null,
    showLabel: Boolean = true,
    iconStyle: IconStyle = IconStyle.Rounded,
    onLaunch: ((AppInfo) -> Unit)? = null,
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            // 48dp icon + 5dp above/below == Diable's 58dp row pitch.
            .padding(vertical = 5.dp)
            .clickable {
                if (onLaunch != null) {
                    onLaunch(app)
                } else {
                    try {
                        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        intent?.let { context.startActivity(it) }
                    } catch (e: Exception) {
                        Log.e("AppLaunch", "Error launching app", e)
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app = app, iconStyle = iconStyle, accentColor = accentColor)

        if (!showLabel) return@Row
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DiableTextMuted,
                )
            }
        }
    }
}

/**
 * The app's icon in the theme's icon style (shape + colour treatment), or the selected
 * third-party icon pack's artwork. Rendering happens once in [IconRenderer].
 */
@Composable
fun AppIcon(
    app: AppInfo,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    iconStyle: IconStyle = IconStyle.Rounded,
    accentColor: Color = DiableAccent,
) {
    Box(
        modifier = modifier.size(size + 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        val drawable = app.icon
        val accentArgb = accentColor.toArgb()
        // Cached icons draw immediately; new ones render off the main thread so a jump to
        // an unseen letter never blocks input while dozens of icons rasterise.
        val bitmap by produceState(
            initialValue = drawable?.let { IconRenderer.peek(app.packageName, it, iconStyle, accentArgb) },
            app.packageName, drawable, iconStyle, accentArgb,
        ) {
            if (value == null && drawable != null) {
                value = withContext(IconRenderer.dispatcher) {
                    IconRenderer.get(app.packageName, drawable, iconStyle, accentArgb, app.fromIconPack)
                }
            }
        }
        val rendered = bitmap
        if (rendered == null) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(DiableCard),
            )
        } else {
            Image(
                bitmap = rendered.asImageBitmap(),
                contentDescription = app.name,
                modifier = Modifier.size(size),
            )
        }
    }
}
