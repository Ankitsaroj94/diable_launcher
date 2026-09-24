package `in`.ankitsaroj.diable.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log

/** A launcher shortcut ("New message", "Scan QR"…) published by an app. */
data class AppShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    internal val info: ShortcutInfo?,
)

/**
 * The system actions behind an app's long-press menu. Shortcuts need the default-launcher
 * role, which this app has whenever the menu is reachable.
 */
object AppActions {

    private const val TAG = "AppActions"
    private const val MAX_SHORTCUTS = 4

    fun shortcuts(context: Context, packageName: String): List<AppShortcut> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return emptyList()
        return try {
            if (!launcherApps.hasShortcutHostPermission()) return emptyList()
            val query = LauncherApps.ShortcutQuery()
                .setPackage(packageName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            launcherApps.getShortcuts(query, Process.myUserHandle())
                .orEmpty()
                .filter { it.isEnabled }
                // Manifest shortcuts first, then by the rank the app asked for.
                .sortedWith(compareBy<ShortcutInfo>({ !it.isDeclaredInManifest }, { it.rank }))
                .take(MAX_SHORTCUTS)
                .map { info ->
                    AppShortcut(
                        id = info.id,
                        packageName = packageName,
                        label = (info.shortLabel ?: info.longLabel ?: info.id).toString(),
                        icon = runCatching {
                            launcherApps.getShortcutIconDrawable(info, context.resources.displayMetrics.densityDpi)
                        }.getOrNull(),
                        info = info,
                    )
                }
        } catch (e: Exception) {
            Log.w(TAG, "Shortcut query failed for $packageName", e)
            emptyList()
        }
    }

    fun launchShortcut(context: Context, shortcut: AppShortcut) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return
        runCatching {
            launcherApps.startShortcut(shortcut.packageName, shortcut.id, null, null, Process.myUserHandle())
        }.onFailure { Log.w(TAG, "Could not start shortcut ${shortcut.id}", it) }
    }

    fun openAppInfo(context: Context, packageName: String) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun uninstall(context: Context, packageName: String) {
        runCatching {
            @Suppress("DEPRECATION")
            context.startActivity(
                Intent(Intent.ACTION_UNINSTALL_PACKAGE)
                    .setData(Uri.fromParts("package", packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** System apps can only be disabled, not uninstalled, so the menu hides the option. */
    fun isUninstallable(context: Context, packageName: String): Boolean = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 ||
            info.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    } catch (_: Exception) {
        false
    }

    fun launch(context: Context, packageName: String): Boolean = try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } else {
            false
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error launching $packageName", e)
        false
    }
}
