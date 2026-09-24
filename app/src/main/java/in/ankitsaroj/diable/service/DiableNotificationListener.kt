package `in`.ankitsaroj.diable.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** One notification as the home screen shows it under an app row. */
data class AppNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val contentIntent: PendingIntent?,
    val clearable: Boolean,
    /** The notification's inline-reply action (messaging apps), if it has one. */
    val replyAction: Notification.Action? = null,
) {
    /**
     * Sends [text] through the app's own reply action, exactly as the shade's reply box
     * does. Returns false when the app offers no reply or the send fails.
     */
    fun reply(context: android.content.Context, text: String): Boolean {
        val action = replyAction ?: return false
        val inputs = action.remoteInputs ?: return false
        val intent = android.content.Intent()
        val results = android.os.Bundle()
        inputs.forEach { results.putCharSequence(it.resultKey, text) }
        android.app.RemoteInput.addResultsToIntent(inputs, intent, results)
        return runCatching { action.actionIntent.send(context, 0, intent) }.isSuccess
    }
}

/**
 * Diable's notification dots, the swipe-an-app notification preview and the media
 * player all hang off notification access. Being the listener is also what authorises
 * [android.media.session.MediaSessionManager.getActiveSessions] for this package.
 *
 * State is process-wide and pushed by the system, so nothing here polls.
 */
class DiableNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        connected.value = true
        publish()
    }

    override fun onListenerDisconnected() {
        instance = null
        connected.value = false
        state.value = emptyMap()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null && summaryEnabled && NotificationSummary.shouldHold(sbn, packageName)) {
            // Snoozing is the system's own "remind me later": the notification comes back
            // by itself at the next delivery slot, so nothing is ever lost.
            runCatching { snoozeNotification(sbn.key, NotificationSummary.millisUntilNextSlot()) }
        }
        publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()

    private fun publish() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        state.value = active
            .filter { it.isVisibleOnHome() }
            .map { it.toAppNotification() }
            .sortedByDescending { it.postTime }
            .groupBy { it.packageName }
    }

    private fun StatusBarNotification.isVisibleOnHome(): Boolean {
        val n = notification ?: return false
        if (packageName == this@DiableNotificationListener.packageName) return false
        // Group summaries duplicate their children; ongoing ones are media/foreground noise.
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        if (isOngoing) return false
        val extras = n.extras
        return !extras.getCharSequence(Notification.EXTRA_TITLE).isNullOrBlank() ||
            !extras.getCharSequence(Notification.EXTRA_TEXT).isNullOrBlank()
    }

    private fun StatusBarNotification.toAppNotification(): AppNotification {
        val extras = notification.extras
        return AppNotification(
            key = key,
            packageName = packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = (
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                    ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                )?.toString().orEmpty(),
            postTime = postTime,
            contentIntent = notification.contentIntent,
            clearable = isClearable,
            replyAction = notification.actions?.firstOrNull { a ->
                a.remoteInputs?.any { it.allowFreeFormInput } == true
            },
        )
    }

    companion object {
        private val state = MutableStateFlow<Map<String, List<AppNotification>>>(emptyMap())
        private val connected = MutableStateFlow(false)

        @Volatile
        private var instance: DiableNotificationListener? = null

        /** Productivity → Notification Summary; set by the activity from prefs. */
        @Volatile
        var summaryEnabled: Boolean = false

        /** Active notifications keyed by package, newest first. */
        val notifications: StateFlow<Map<String, List<AppNotification>>> get() = state
        val isConnected: StateFlow<Boolean> get() = connected

        fun component(context: Context) =
            ComponentName(context, DiableNotificationListener::class.java)

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            return flat.split(':').any {
                ComponentName.unflattenFromString(it) == component(context)
            }
        }

        fun openAccessSettings(context: Context) {
            val intent = android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }

        fun dismiss(key: String) {
            runCatching { instance?.cancelNotification(key) }
        }

        fun dismissAll(packageName: String) {
            state.value[packageName].orEmpty().filter { it.clearable }.forEach { dismiss(it.key) }
        }
    }
}

/**
 * Notification Summary: non-urgent notifications are held and delivered together at
 * fixed times of day, the way Diable batches them. Calls, alarms, messages, ongoing
 * and system notifications always come straight through.
 */
object NotificationSummary {

    /** Delivery times, hours of the day. */
    private val SLOTS = listOf(8, 14, 20)

    /** Notifications re-posted within this long after a slot are the batch itself. */
    private const val DELIVERY_WINDOW_MS = 10 * 60 * 1000L

    private val PASS_THROUGH = setOf(
        Notification.CATEGORY_CALL,
        Notification.CATEGORY_ALARM,
        Notification.CATEGORY_MESSAGE,
        Notification.CATEGORY_REMINDER,
        Notification.CATEGORY_EVENT,
        Notification.CATEGORY_NAVIGATION,
        Notification.CATEGORY_TRANSPORT,
        Notification.CATEGORY_PROGRESS,
        Notification.CATEGORY_SERVICE,
        Notification.CATEGORY_SYSTEM,
        Notification.CATEGORY_ERROR,
        Notification.CATEGORY_STOPWATCH,
    )

    fun shouldHold(sbn: StatusBarNotification, ownPackage: String): Boolean {
        val n = sbn.notification ?: return false
        if (sbn.packageName == ownPackage || sbn.packageName == "android") return false
        // Never a group summary: snoozing one snoozes its whole group, which dragged
        // pass-through messages along with it (the system bundles an app's notifications
        // under an automatic summary as soon as there are two).
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        if (sbn.isOngoing || !sbn.isClearable) return false
        if (n.category in PASS_THROUGH) return false
        if (n.fullScreenIntent != null) return false
        return !inDeliveryWindow(System.currentTimeMillis())
    }

    fun millisUntilNextSlot(now: Long = System.currentTimeMillis()): Long =
        (nextSlot(now) - now).coerceAtLeast(60_000L)

    private fun inDeliveryWindow(now: Long): Boolean {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        return SLOTS.any { hour ->
            val slot = (cal.clone() as java.util.Calendar).apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            now in slot until slot + DELIVERY_WINDOW_MS
        }
    }

    private fun nextSlot(now: Long): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        for (dayOffset in 0..1) {
            for (hour in SLOTS) {
                val slot = (cal.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                if (slot > now) return slot
            }
        }
        return now + 6 * 60 * 60 * 1000L
    }
}
