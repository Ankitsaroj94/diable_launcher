package `in`.ankitsaroj.diable.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.ui.components.LavenderIconCircle

/**
 * The first-run rows Diable stacks between the date and the favorites: an icon on the
 * left, a sentence, and a right-aligned underlined action beneath it. They are dismissed
 * individually once acted on.
 */
enum class HomePromo { Accessibility, Pro, Customize, Productivity }

private data class PromoSpec(
    val icon: ImageVector,
    val text: String,
    val action: String,
)

private fun specFor(promo: HomePromo) = when (promo) {
    HomePromo.Accessibility -> PromoSpec(
        Icons.Default.Info,
        "Some features use Accessibility access when turned on in Settings",
        "Learn more",
    )
    HomePromo.Pro -> PromoSpec(
        Icons.Default.AutoAwesome,
        "Make the most out of Diable Pro",
        "Show Diable Pro features",
    )
    HomePromo.Customize -> PromoSpec(
        Icons.Default.Brush,
        "Customize Your Home Screen",
        "Customize",
    )
    HomePromo.Productivity -> PromoSpec(
        Icons.Default.StarOutline,
        "Choose Your Productivity Features",
        "Choose",
    )
}

@Composable
fun HomePromoRow(
    promo: HomePromo,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = specFor(promo)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        LavenderIconCircle(spec.icon, size = 48.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spec.text,
                color = Color.White,
                fontSize = 17.sp,
                lineHeight = 23.sp,
            )
            Text(
                text = spec.action,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable(onClick = onAction),
            )
        }
    }
}
