package `in`.ankitsaroj.diable

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.data.wellbeing.UsageBreaker
import `in`.ankitsaroj.diable.data.rememberDiableRepos
import `in`.ankitsaroj.diable.navigation.DiableNav
import `in`.ankitsaroj.diable.ui.permissions.RequestLauncherPermissions
import `in`.ankitsaroj.diable.ui.theme.SystemBarAppearance
import `in`.ankitsaroj.diable.ui.theme.ThemeFont
import `in`.ankitsaroj.diable.ui.theme.DiableTheme
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {

    /** Bumped whenever a HOME intent arrives so the nav graph can reset to the home screen. */
    /**
     * HOME presses, tagged with whether the launcher was already on screen. Only a press
     * while visible is Diable's toggle gesture; coming back from an app just returns home.
     */
    private val homeRequests = MutableSharedFlow<Boolean>(extraBufferCapacity = 4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // One set of repositories for the whole app, provided above everything that
            // needs them so no screen constructs its own.
            val repos = rememberDiableRepos()
            CompositionLocalProvider(LocalDiableRepos provides repos) {
                // The theme's typeface comes from prefs, so a preset restyles the app.
                val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
                // Icons are resolved when apps load, so a pack change reloads the list.
                LaunchedEffect(prefs.iconPackPackage) { repos.apps.setIconPack(prefs.iconPackPackage) }
                LaunchedEffect(prefs.notificationSummary) {
                    `in`.ankitsaroj.diable.service.DiableNotificationListener.summaryEnabled = prefs.notificationSummary
                }
                LaunchedEffect(prefs.appLabels, prefs.appIconPacks) {
                    repos.apps.setOverrides(prefs.appLabels, prefs.appIconPacks)
                }
                // Productivity → "Allow Home Screen Rotation": portrait unless opted in.
                LaunchedEffect(prefs.allowRotation) {
                    requestedOrientation = if (prefs.allowRotation) {
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
                DiableTheme(font = ThemeFont.fromKey(prefs.fontKey)) {
                    SystemBarAppearance(measuredTopLuminance = prefs.wallpaperTopLuminance)
                    RequestLauncherPermissions()
                    DiableNav(homeRequests = homeRequests)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Tapping HOME while a settings screen is open should come back to the launcher.
        if (intent.hasCategory(android.content.Intent.CATEGORY_HOME)) {
            // Android pauses even a visible activity to deliver the intent, so "resumed"
            // can't tell the cases apart; being stopped (covered by an app) can.
            homeRequests.tryEmit(!stoppedSinceResume)
        }
    }

    /** True once another app covered the launcher, until it's back on screen. */
    private var stoppedSinceResume = false

    override fun onResume() {
        super.onResume()
        stoppedSinceResume = false
        // Back on the launcher: any pending Usage Breaker reminder is moot.
        UsageBreaker.onLauncherResumed()
    }

    override fun onStart() {
        super.onStart()
        (application as DiableApplication).appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        stoppedSinceResume = true
        (application as DiableApplication).appWidgetHost.stopListening()
    }
}
