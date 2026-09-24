package `in`.ankitsaroj.diable.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
    val firstInstallTime: Long = 0L,
    /** Launcher activity class — icon packs map icons per activity, not per package. */
    val className: String = "",
    /** The icon came from a third-party icon pack, which already has its own shape. */
    val fromIconPack: Boolean = false,
)
