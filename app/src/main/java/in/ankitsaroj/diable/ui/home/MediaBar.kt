package `in`.ankitsaroj.diable.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.MediaInfo
import `in`.ankitsaroj.diable.ui.theme.DiableAccent

@Composable
fun MediaBar(
    media: MediaInfo,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            // Tapping the bar opens the player that owns the session.
            .clickable {
                context.packageManager.getLaunchIntentForPackage(media.packageName)
                    ?.let { runCatching { context.startActivity(it) } }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DiableAccent.copy(alpha = 0.25f)),
        ) {
            media.art?.let { art ->
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media.title,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = media.artist.ifBlank { "—" },
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { media.previous() }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
        }
        IconButton(onClick = { media.playPause() }, modifier = Modifier.size(32.dp)) {
            Icon(
                if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (media.isPlaying) "Pause" else "Play",
                tint = Color.White,
            )
        }
        IconButton(onClick = { media.next() }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
        }
    }
}
