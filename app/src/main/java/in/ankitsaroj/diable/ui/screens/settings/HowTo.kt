package `in`.ankitsaroj.diable.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.ui.components.LavenderIconCircle
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted

private data class HowToTip(val icon: ImageVector, val title: String, val body: String)

private val Tips = listOf(
    HowToTip(
        Icons.Default.SwipeLeft,
        "Swipe left on an app",
        "Opens it, same as a tap.",
    ),
    HowToTip(
        Icons.Default.SwipeRight,
        "Swipe right on an app",
        "Opens its pop-up: shortcuts, waiting notifications (reply or dismiss) and any apps or widget you pinned to it.",
    ),
    HowToTip(
        Icons.AutoMirrored.Filled.Sort,
        "The alphabet",
        "Slide along the letters to preview each letter's apps and let go to jump there. Tap a letter to jump, ☆ to go back to favorites, ° for the launcher footer. With Quick Lock on, double-tap it to lock the phone.",
    ),
    HowToTip(
        Icons.Default.TouchApp,
        "Long-press",
        "On an app: favorite, rename, change icon, hide, categories, pop-up and uninstall. On the clock: the widget sheet. On a widget: move, resize, replace or remove it.",
    ),
    HowToTip(
        Icons.Default.AddCircle,
        "The Diable button",
        "Long-press it to choose what a tap and a swipe up do. While the list is scrolled it becomes a search button.",
    ),
    HowToTip(
        Icons.Default.Folder,
        "Pop-up folders",
        "Create one from an app's menu (Add to category) or from Folders & Pop-Ups. It sits in favorites; tap to open, long-press to edit.",
    ),
    HowToTip(
        Icons.Default.Widgets,
        "Widgets",
        "Long-press the clock, then Add Custom Widget. Add more widgets to the stack and swipe sideways between them.",
    ),
    HowToTip(
        Icons.Default.Search,
        "Search",
        "Swipe up on the home screen or press Home while already home. Type the start of any word in an app's name; Enter opens the top result. Math like 2+3*4 is calculated instantly.",
    ),
    HowToTip(
        Icons.Default.Apps,
        "Returning home",
        "Home from any app lands on your favorites. Back scrolls the list back to the top.",
    ),
)

/** In-app How-To's: the launcher's gestures, without leaving the app. */
@Composable
fun HowToScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
    ) {
        PillHeader("How-To's")
        Tips.forEach { tip ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 5.dp)
                    .fillMaxWidth()
                    .background(DiableCard, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                LavenderIconCircle(tip.icon, size = 36.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        tip.title,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp,
                    )
                    Text(
                        tip.body,
                        color = DiableTextMuted,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
