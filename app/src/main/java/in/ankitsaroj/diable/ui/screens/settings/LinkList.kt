package `in`.ankitsaroj.diable.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.ui.components.DiableSettingsRow
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted

/** One tappable row in a settings link list. */
data class LinkRow(
    val title: String,
    val icon: ImageVector? = null,
    val summary: String? = null,
    val onClick: (Context) -> Unit,
)

/** A full-screen Diable settings list whose rows open in-app pages or intents. */
@Composable
fun LinkListScreen(
    title: String,
    rows: List<LinkRow>,
    onBack: () -> Unit,
    intro: String? = null,
) {
    BackHandler(onBack = onBack)
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
    ) {
        PillHeader(title)
        if (intro != null) {
            Text(
                text = intro,
                color = DiableTextMuted,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
        }
        rows.forEach { row ->
            DiableSettingsRow(
                title = row.title,
                icon = row.icon,
                summary = row.summary,
                onClick = { row.onClick(context) },
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
    }
}

/** Opens a Play Store page, falling back to the web store when Play isn't installed. */
fun openStore(context: Context, packageName: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$packageName")
    }
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(
        Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/**
 * Feedback goes wherever the user chooses (mail, chat, notes) through the share sheet,
 * pre-filled with what a report needs. There is no fixed support address.
 */
fun sendFeedback(context: Context) {
    val body = buildString {
        appendLine("Feedback:")
        appendLine()
        appendLine()
        appendLine("---")
        appendLine("App version: ${appVersionName(context)}")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, "Launcher feedback")
        .putExtra(Intent.EXTRA_TEXT, body)
    try {
        context.startActivity(
            Intent.createChooser(send, "Send feedback").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can send feedback", Toast.LENGTH_SHORT).show()
    }
}

fun appVersionName(context: Context): String = try {
    @Suppress("DEPRECATION")
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.PackageInfoFlags.of(0),
        )
    } else {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    info.versionName ?: "1.0"
} catch (_: Exception) {
    "1.0"
}
