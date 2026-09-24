package `in`.ankitsaroj.diable.data.wellbeing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import `in`.ankitsaroj.diable.R
import `in`.ankitsaroj.diable.data.DiablePrefs

/**
 * Usage Breaker: after the user opens a leisure app, remind them to take a break once the
 * chosen interval has passed — unless they came back to the home screen first.
 *
 * The launcher process is always resident, so a main-thread delayed post is reliable
 * enough here and needs no receiver or alarm registration. Returning home cancels it.
 */
object UsageBreaker {

    private const val CHANNEL_ID = "usage_breaker"
    private const val NOTIFICATION_ID = 4242

    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    /** Call whenever the launcher opens an app. */
    fun onAppLaunched(context: Context, packageName: String, prefs: DiablePrefs) {
        cancelPending()
        if (!prefs.usageBreaker || packageName !in prefs.leisurePackages) return
        val appContext = context.applicationContext
        val minutes = prefs.usageBreakerMinutes.coerceAtLeast(1)
        val label = appLabel(appContext, packageName)
        val task = Runnable {
            pending = null
            notify(appContext, label, minutes)
        }
        pending = task
        handler.postDelayed(task, minutes * 60_000L)
    }

    /** Call when the home screen resumes: the user already took their break. */
    fun onLauncherResumed() = cancelPending()

    private fun cancelPending() {
        pending?.let(handler::removeCallbacks)
        pending = null
    }

    private fun notify(context: Context, appLabel: String, minutes: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Usage Breaker", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Reminders to take a break from leisure apps"
                },
            )
        }
        // Tapping the reminder brings the user home.
        val home = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = home?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Time for a break?")
            .setContentText("You've been on $appLabel for $minutes minutes.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    private fun appLabel(context: Context, packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault("this app")
}
