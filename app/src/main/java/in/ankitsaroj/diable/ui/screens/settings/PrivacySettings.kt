package `in`.ankitsaroj.diable.ui.screens.settings

import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import `in`.ankitsaroj.diable.ui.components.DiableBottomSheet
import `in`.ankitsaroj.diable.ui.components.DiableSettingCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted

/** One consent switch in Privacy Settings. */
private enum class PrivacyToggle(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val default: Boolean,
    val locked: Boolean = false,
) {
    Marketing("marketing", "Marketing", "Help us measure which features people discover", Icons.Default.Campaign, false),
    Functional("functional", "Functional", "Remember preferences that improve the experience", Icons.Default.Tune, true),
    Essential(
        "essential", "Essential", "Required for the launcher to work; always on",
        Icons.Default.Lock, true, locked = true,
    ),
    Wellbeing(
        "wellbeing", "Digital Wellbeing Initiative",
        "Share anonymous usage patterns to research healthier phone use",
        Icons.Default.SelfImprovement, false,
    ),
    Categorization(
        "categorization", "App Categorization",
        "Look up app categories to improve suggestions",
        Icons.Default.Apps, false,
    ),
}

/**
 * Consent choices. These are a few booleans that nothing observes as a stream, so plain
 * SharedPreferences is enough — no need for another DataStore collection.
 */
private object PrivacyPrefs {
    private const val FILE = "privacy_settings"

    fun get(context: Context, toggle: PrivacyToggle): Boolean =
        toggle.locked || context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(toggle.key, toggle.default)

    fun set(context: Context, toggle: PrivacyToggle, value: Boolean) {
        if (toggle.locked) return
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putBoolean(toggle.key, value) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state = remember {
        mutableStateMapOf<PrivacyToggle, Boolean>().apply {
            PrivacyToggle.entries.forEach { put(it, PrivacyPrefs.get(context, it)) }
        }
    }
    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Privacy Settings",
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                fontSize = 23.sp,
                color = DiableText,
            )
            Text(
                text = "Choose what the launcher may use. You can change this at any time.",
                color = DiableTextMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            PrivacyToggle.entries.forEach { toggle ->
                DiableSettingCard(
                    icon = toggle.icon,
                    title = toggle.title,
                    subtitle = toggle.subtitle,
                    checked = state[toggle] ?: toggle.default,
                    onCheckedChange = { value ->
                        if (!toggle.locked) {
                            state[toggle] = value
                            PrivacyPrefs.set(context, toggle, value)
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
