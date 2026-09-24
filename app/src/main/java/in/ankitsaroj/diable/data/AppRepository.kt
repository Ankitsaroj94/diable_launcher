package `in`.ankitsaroj.diable.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import `in`.ankitsaroj.diable.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The installed-app list, loaded once and kept warm.
 *
 * Loading walks every launcher activity and inflates an icon Drawable for each, so it is
 * far too expensive to repeat. Callers observe [apps]; the list refreshes only when
 * packages actually change, not when the UI recomposes.
 */
class AppRepository(private val context: Context) {

    private val appContext = context.applicationContext

    val apps: StateFlow<List<AppInfo>> get() = state

    /** Kicks off the initial load if it hasn't happened yet. Safe to call repeatedly. */
    fun ensureLoaded() {
        if (state.value.isNotEmpty() || loadStarted) return
        loadStarted = true
        scope.launch { refresh() }
        registerPackageCallback()
    }

    suspend fun refresh() {
        loadMutex.withLock {
            val loaded = withContext(Dispatchers.IO) { loadInstalledApps(appContext) }
            if (loaded.isNotEmpty()) {
                raw = loaded
                state.value = withContext(Dispatchers.IO) { decorate(appContext, loaded) }
            }
        }
    }

    /**
     * Switches the third-party icon pack (null = none) and reloads icons if it changed.
     * Icons are resolved at load time so every screen gets pack icons for free.
     */
    fun setIconPack(packageName: String?) {
        if (packageName == iconPackPackage) return
        iconPackPackage = packageName
        if (loadStarted) scope.launch { refresh() }
    }

    /**
     * Applies the user's renames and per-app icon choices from the long-press menu. Cheap:
     * it re-decorates the already-loaded list without walking packages again.
     */
    fun setOverrides(labels: Map<String, String>, iconPacks: Map<String, String>) {
        if (labels == labelOverrides && iconPacks == iconPackOverrides) return
        labelOverrides = labels
        iconPackOverrides = iconPacks
        scope.launch {
            loadMutex.withLock { state.value = decorate(appContext, raw) }
        }
    }

    suspend fun getInstalledApps(): List<AppInfo> {
        if (state.value.isEmpty()) refresh()
        return state.value
    }

    private fun registerPackageCallback() {
        if (callbackRegistered) return
        val launcherApps =
            appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return
        callbackRegistered = true
        launcherApps.registerCallback(
            object : LauncherApps.Callback() {
                private fun reload() {
                    scope.launch { refresh() }
                }

                override fun onPackageRemoved(packageName: String?, user: UserHandle?) = reload()
                override fun onPackageAdded(packageName: String?, user: UserHandle?) = reload()
                override fun onPackageChanged(packageName: String?, user: UserHandle?) = reload()
                override fun onPackagesAvailable(
                    packageNames: Array<out String>?,
                    user: UserHandle?,
                    replacing: Boolean,
                ) = reload()

                override fun onPackagesUnavailable(
                    packageNames: Array<out String>?,
                    user: UserHandle?,
                    replacing: Boolean,
                ) = reload()
            },
        )
    }

    companion object {
        private const val TAG = "AppRepository"

        // Process-wide: the app list is the same for every screen, and reloading it per
        // screen was the main source of launcher jank.
        private val state = MutableStateFlow<List<AppInfo>>(emptyList())
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val loadMutex = Mutex()

        @Volatile
        private var loadStarted = false

        @Volatile
        private var callbackRegistered = false

        @Volatile
        private var iconPackPackage: String? = null

        private const val PACK_ICON_PX = 192

        /** Apps as the system reports them, before icon packs and renames. */
        @Volatile
        private var raw: List<AppInfo> = emptyList()

        @Volatile
        private var labelOverrides: Map<String, String> = emptyMap()

        @Volatile
        private var iconPackOverrides: Map<String, String> = emptyMap()

        private fun decorate(context: Context, apps: List<AppInfo>): List<AppInfo> {
            val packed = applyIconPack(context, apps).map { app ->
                val perApp = iconPackOverrides[app.packageName] ?: return@map app
                val pack = IconPackRepository.load(context, perApp) ?: return@map app
                val original = apps.firstOrNull { it.packageName == app.packageName }?.icon
                runCatching { pack.iconFor(app.packageName, app.className, original, PACK_ICON_PX) }
                    .getOrNull()
                    ?.let { app.copy(icon = it, fromIconPack = true) }
                    ?: app
            }
            if (labelOverrides.isEmpty()) return packed
            return packed
                .map { app -> labelOverrides[app.packageName]?.let { app.copy(name = it) } ?: app }
                .sortedBy { it.name.uppercase() }
        }

        /** Swaps in the selected icon pack's artwork wherever the pack provides some. */
        private fun applyIconPack(context: Context, apps: List<AppInfo>): List<AppInfo> {
            val packName = iconPackPackage ?: return apps
            val pack = IconPackRepository.load(context, packName) ?: return apps
            return apps.map { app ->
                val themed = runCatching {
                    pack.iconFor(app.packageName, app.className, app.icon, PACK_ICON_PX)
                }.getOrNull()
                if (themed != null) app.copy(icon = themed, fromIconPack = true) else app
            }
        }

        private fun loadInstalledApps(context: Context): List<AppInfo> {
            val pm = context.packageManager
            val appsList = mutableListOf<AppInfo>()

            try {
                val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
                val launcherApps =
                    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

                if (userManager != null && launcherApps != null) {
                    // One bulk query instead of getPackageInfo() per app.
                    val installTimes = installTimes(pm)
                    for (profile in userManager.userProfiles) {
                        for (activityInfo in launcherApps.getActivityList(null, profile)) {
                            try {
                                val pkg = activityInfo.applicationInfo.packageName
                                appsList.add(
                                    AppInfo(
                                        packageName = pkg,
                                        name = activityInfo.label.toString(),
                                        icon = activityInfo.getIcon(0),
                                        firstInstallTime = installTimes[pkg] ?: 0L,
                                        className = activityInfo.componentName.className,
                                    ),
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error loading app from LauncherApps", e)
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val installTimes = installTimes(pm)
                    @Suppress("DEPRECATION")
                    for (info in pm.queryIntentActivities(intent, 0)) {
                        try {
                            val activityInfo = info.activityInfo
                            val pkg = activityInfo.packageName
                            appsList.add(
                                AppInfo(
                                    packageName = pkg,
                                    name = activityInfo.loadLabel(pm).toString(),
                                    icon = activityInfo.loadIcon(pm),
                                    firstInstallTime = installTimes[pkg] ?: 0L,
                                    className = activityInfo.name,
                                ),
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading app from queryIntentActivities", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting apps", e)
            }

            // Like Diable, the launcher never lists itself; its settings live in the ° footer.
            return appsList
                .filter { it.packageName != context.packageName }
                .distinctBy { it.packageName }
                .sortedBy { it.name.uppercase() }
        }

        private fun installTimes(pm: PackageManager): Map<String, Long> = try {
            @Suppress("DEPRECATION")
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getInstalledPackages(0)
            }
            packages.associate { it.packageName to it.firstInstallTime }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading install times", e)
            emptyMap()
        }
    }
}
