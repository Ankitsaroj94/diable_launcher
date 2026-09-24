package `in`.ankitsaroj.diable.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

data class DiableRepos(
    val prefs: PrefsRepository,
    val apps: AppRepository,
    val contacts: ContactsRepository,
    val wallpapers: WallpaperRepository,
)

val LocalDiableRepos = staticCompositionLocalOf<DiableRepos> {
    error("DiableRepos not provided")
}

@Composable
fun rememberDiableRepos(): DiableRepos {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        DiableRepos(
            prefs = PrefsRepository(context),
            apps = AppRepository(context),
            contacts = ContactsRepository(context),
            wallpapers = WallpaperRepository(context),
        )
    }
}
