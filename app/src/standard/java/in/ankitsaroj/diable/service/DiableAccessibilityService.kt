package `in`.ankitsaroj.diable.service

import android.content.Context

/**
 * Sideload build. Quick Lock needs an accessibility service, and declaring one makes
 * Play Protect block the install, so this variant cannot lock the screen.
 */
object DiableAccessibilityService {
    const val available: Boolean = false

    fun isEnabled(context: Context): Boolean = false

    fun lockScreen(context: Context): Boolean = false

    fun openSettings(context: Context) = Unit
}
