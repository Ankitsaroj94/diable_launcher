package `in`.ankitsaroj.diable.ui.components

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableHeaderPill

/**
 * A screen header. Diable draws this unfilled and centred while the list is at the top,
 * and only paints the pill once the content scrolls under it.
 */
@Composable
fun PillHeader(
    title: String,
    modifier: Modifier = Modifier,
    scrolled: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .then(
                    if (scrolled) {
                        Modifier.background(DiableHeaderPill, RoundedCornerShape(25.dp))
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 24.dp, vertical = 10.dp),
            fontSize = 23.sp,
            color = DiableAccentText,
        )
    }
}
