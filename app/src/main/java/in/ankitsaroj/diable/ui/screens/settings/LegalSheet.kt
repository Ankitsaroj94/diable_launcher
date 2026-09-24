package `in`.ankitsaroj.diable.ui.screens.settings

import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.ui.components.DiableBottomSheet
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted

enum class LegalDoc(val title: String, val sections: List<Pair<String, String>>) {
    Privacy(
        "Privacy policy",
        listOf(
            "No data collection" to
                "Diable has no account, no analytics and no ads. It does not collect, " +
                "sell or share personal data.",
            "What stays on your device" to
                "Your favorites, hidden apps, themes, usage counts, calendar events, contacts " +
                "and notifications are read and stored only on this device. Notification " +
                "access is used to show notifications and media controls on your home screen.",
            "Network requests" to
                "Only two features reach the internet: wallpapers are downloaded from the " +
                "Openverse API, and weather is fetched for your approximate location when you " +
                "turn weather on. Neither request includes anything that identifies you.",
            "Backups" to
                "Backups are files you create and keep. The launcher never uploads them.",
        ),
    ),
    Terms(
        "Terms of Service",
        listOf(
            "Use" to
                "Diable is a free, open home-screen launcher provided as is, without " +
                "warranty of any kind.",
            "Wallpapers" to
                "Wallpapers are openly licensed works from Openverse. Their creators and " +
                "licenses are shown in the picker; follow those licenses if you reuse them.",
            "Third-party apps" to
                "Icon packs, widgets and the apps you launch are provided by their own " +
                "developers under their own terms.",
            "Changes" to
                "These terms may change with app updates; the version in the app applies.",
        ),
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalSheet(doc: LegalDoc, onDismiss: () -> Unit) {
    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 8.dp),
        ) {
            Text(
                text = doc.title,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                fontSize = 23.sp,
                color = DiableText,
            )
            doc.sections.forEach { (heading, body) ->
                Text(heading, color = DiableText, fontSize = 17.sp, modifier = Modifier.padding(top = 12.dp))
                Text(
                    body,
                    color = DiableTextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
