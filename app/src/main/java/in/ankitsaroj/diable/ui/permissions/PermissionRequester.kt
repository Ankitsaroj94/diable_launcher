package `in`.ankitsaroj.diable.ui.permissions

import android.Manifest

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs

private fun missing(context: android.content.Context, perms: List<String>) = perms.filter {
    ContextCompat.checkSelfPermission(context, it) !=
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

/**
 * Requests [permissions] once, the first time [enabled] becomes true.
 *
 * Diable never throws a permission wall at first launch — it asks only when an enabled
 * feature actually needs the data. Callers scope each request to the surface that uses it.
 */
@Composable
fun RequestPermissionsWhen(enabled: Boolean, vararg permissions: String) {
    val context = LocalContext.current
    // Remembered across launches: a launcher restarts often (theme switches, rotation),
    // and re-asking every time after the user declined is nagging.
    val store = remember { context.getSharedPreferences("permission_asks", android.content.Context.MODE_PRIVATE) }
    val askKey = remember { permissions.sorted().joinToString(",") }
    var asked by remember { mutableStateOf(store.getBoolean(askKey, false)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    LaunchedEffect(enabled, asked) {
        if (!enabled || asked) return@LaunchedEffect
        val fresh = missing(context, permissions.toList())
        if (fresh.isNotEmpty()) {
            asked = true
            store.edit().putBoolean(askKey, true).apply()
            launcher.launch(fresh.toTypedArray())
        }
    }
}

/**
 * Startup permissions — only what the home surface itself renders. Contacts are requested
 * by the search screen, calendar/location by the features that turn them on.
 */
@Composable
fun RequestLauncherPermissions() {
    val context = LocalContext.current
    val prefsRepo = LocalDiableRepos.current.prefs
    val prefs by prefsRepo.prefsFlow.collectAsState(initial = DiablePrefs())

    // Media reading goes through MediaSessionManager + notification-listener access, a
    // special access granted in Settings — not a runtime permission, so nothing to ask here.
    RequestPermissionsWhen(
        enabled = prefs.showUpcomingEvents && prefs.calendarPreview,
        Manifest.permission.READ_CALENDAR,
    )
    RequestPermissionsWhen(
        enabled = prefs.weatherEnabled,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
}
