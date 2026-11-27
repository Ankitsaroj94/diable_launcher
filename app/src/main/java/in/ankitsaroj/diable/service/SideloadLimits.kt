package `in`.ankitsaroj.diable.service

/**
 * Copy for features the sideload build cannot offer.
 *
 * Play Protect's enhanced fraud protection hard-blocks an APK installed from a browser,
 * messaging app or file manager when the manifest declares [android.permission.READ_SMS],
 * [android.permission.RECEIVE_SMS], a notification listener or an accessibility service.
 * The default build declares none of those. The full build keeps the listener and the
 * accessibility service, and Play Protect may block that APK in the regions where the
 * protection is on.
 */
object SideloadLimits {
    const val TITLE = "Not in this install"

    const val NOTIFICATION_BODY =
        "Notification dots, quick replies, the media player and Notification Summary need " +
            "a notification listener. This install leaves that out so Google Play Protect " +
            "can allow sideloading. The full build keeps them. Play Protect may block that " +
            "APK in India and other regions."

    const val LOCK_BODY =
        "Quick Lock needs an accessibility service, which this install leaves out so " +
            "Google Play Protect can allow sideloading. The full build keeps it. Play " +
            "Protect may block that APK in India and other regions."

    const val ROW_NOTE = "Not in this install. The full build keeps it."
}
