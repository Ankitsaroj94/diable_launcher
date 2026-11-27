package `in`.ankitsaroj.diable.service

import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sideload build. This is not a [android.service.notification.NotificationListenerService]
 * and nothing in this variant's manifest declares one, which is what lets Play Protect
 * install the APK. Dots, quick replies, media controls and Notification Summary stay in
 * the full flavor.
 */
object DiableNotificationListener {
    const val available: Boolean = false

    private val empty = MutableStateFlow<Map<String, List<AppNotification>>>(emptyMap())
    private val notConnected = MutableStateFlow(false)

    val notifications: StateFlow<Map<String, List<AppNotification>>> get() = empty
    val isConnected: StateFlow<Boolean> get() = notConnected

    @Volatile
    var summaryEnabled: Boolean = false

    fun component(context: Context): ComponentName =
        ComponentName(context, DiableNotificationListener::class.java)

    fun isEnabled(context: Context): Boolean = false

    fun openAccessSettings(context: Context) = Unit

    fun dismiss(key: String) = Unit

    fun dismissAll(packageName: String) = Unit
}
