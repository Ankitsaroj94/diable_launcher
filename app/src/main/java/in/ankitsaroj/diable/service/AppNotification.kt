package `in`.ankitsaroj.diable.service

import android.app.Notification

/** One notification as the home screen shows it under an app row. */
data class AppNotification(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val contentIntent: android.app.PendingIntent?,
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
