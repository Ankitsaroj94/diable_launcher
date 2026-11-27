package `in`.ankitsaroj.diable.ui.permissions

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Whether this package is the home app Android will open for the Home button.
 * On Android 10+ that is the HOME role; earlier it is the preferred HOME activity.
 */
fun isDefaultHome(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roles = context.getSystemService(RoleManager::class.java)
        if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return roles.isRoleHeld(RoleManager.ROLE_HOME)
        }
    }
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.resolveActivity(
            home,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
    }
    return resolved?.activityInfo?.packageName == context.packageName
}

/**
 * System UI that asks the user to make this app the home app, or null when it already is.
 * Android 10+ shows the role dialog. Older releases open the home-app settings screen.
 */
fun requestDefaultHomeIntent(context: Context): Intent? {
    if (isDefaultHome(context)) return null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roles = context.getSystemService(RoleManager::class.java)
        if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
    }
    val settings = Intent(Settings.ACTION_HOME_SETTINGS)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.resolveActivity(settings, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.resolveActivity(settings, 0)
    }
    return if (resolved != null) settings else Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
}

/**
 * The first time this screen is shown in a launch, ask to become the default launcher
 * when something else still holds Home. A dismissal is remembered for this launch so
 * the dialog does not reopen on every resume.
 */
@Composable
fun RequestDefaultHome() {
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { }
    LaunchedEffect(asked) {
        if (asked) return@LaunchedEffect
        val intent = requestDefaultHomeIntent(context) ?: return@LaunchedEffect
        asked = true
        runCatching { launcher.launch(intent) }
    }
}
