package `in`.ankitsaroj.diable.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import `in`.ankitsaroj.diable.ui.screens.HomeCommands
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.ui.screens.AdvancedScreen
import `in`.ankitsaroj.diable.ui.screens.CalendarAgendaScreen
import `in`.ankitsaroj.diable.ui.screens.ChangelogScreen
import `in`.ankitsaroj.diable.ui.screens.CommunityScreen
import `in`.ankitsaroj.diable.ui.screens.EditFavoritesScreen
import `in`.ankitsaroj.diable.ui.screens.FavoritesOnboardingScreen
import `in`.ankitsaroj.diable.ui.screens.HelpFeedbackScreen
import `in`.ankitsaroj.diable.ui.screens.HideAppsScreen
import `in`.ankitsaroj.diable.ui.screens.ChooseWallpaperScreen
import `in`.ankitsaroj.diable.ui.screens.ClockStyleScreen
import `in`.ankitsaroj.diable.ui.screens.HomeScreen
import `in`.ankitsaroj.diable.ui.screens.MusicAppsScreen
import `in`.ankitsaroj.diable.ui.screens.DiableProScreen
import `in`.ankitsaroj.diable.ui.screens.DiableSettingsScreen
import `in`.ankitsaroj.diable.ui.screens.ProductivityScreen
import `in`.ankitsaroj.diable.ui.screens.SearchScreen
import `in`.ankitsaroj.diable.ui.screens.CommunityThemesScreen
import `in`.ankitsaroj.diable.ui.screens.CreateThemeScreen
import `in`.ankitsaroj.diable.ui.screens.EditThemeScreen
import `in`.ankitsaroj.diable.ui.screens.FoldersScreen
import `in`.ankitsaroj.diable.ui.screens.MyThemesScreen
import `in`.ankitsaroj.diable.ui.screens.ReadyMadeThemesScreen
import `in`.ankitsaroj.diable.ui.screens.SelectWidgetScreen
import `in`.ankitsaroj.diable.ui.screens.ThemeCarouselScreen
import `in`.ankitsaroj.diable.ui.screens.ThemeEditorScreen
import `in`.ankitsaroj.diable.ui.screens.ThemesScreen

@Composable
fun DiableNav(homeRequests: SharedFlow<Boolean>? = null) {
    val navController = rememberNavController()
    val repos = LocalDiableRepos.current

    // HOME already on the home screen is Diable's own gesture (scroll back / toggle
    // search), so it is forwarded instead of popping. Events, not state: a press must be
    // handled once, never replayed when home recomposes.
    val homeReselect = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    LaunchedEffect(homeRequests) {
        homeRequests?.collect { wasVisible ->
            val onHome = navController.currentDestination?.route == Routes.Home
            if (wasVisible && onHome) {
                homeReselect.tryEmit(Unit)
            } else {
                // Returning from an app, or HOME from a launcher screen: favorites at rest.
                if (!onHome) navController.popBackStack(Routes.Home, inclusive = false)
                HomeCommands.resetList.value = true
            }
        }
    }

    val activity = LocalContext.current as? android.app.Activity

    // No background here: Home and Search must let the system wallpaper through.
    // Screens that need a solid backdrop paint it themselves.
    NavHost(
        navController = navController,
        startDestination = Routes.Home,
    ) {
        composable(Routes.Home) {
            // Null until the stored prefs load: deciding on the defaults flashed the
            // first-run favorites screen on every cold start.
            val prefs = repos.prefs.prefsFlow.collectAsState(initial = null).value
            if (prefs == null) {
                // The wallpaper shows through for the few ms before prefs arrive.
            } else if (!prefs.favoritesOnboarded) {
                FavoritesOnboardingScreen(onDone = {})
            } else {
                HomeScreen(
                    onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                    homePresses = homeReselect,
                )
            }
        }
        composable(Routes.Search) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
            )
        }
        composable(Routes.DiableSettings) {
            DiableSettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
            )
        }
        composable(Routes.Productivity) {
            ProductivityScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
            )
        }
        composable(Routes.Advanced) {
            AdvancedScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                onRestart = { activity?.recreate() },
            )
        }
        composable(Routes.Themes) { ThemesScreen(navController) }
        composable(Routes.ChooseWallpaper) { ChooseWallpaperScreen(navController) }
        composable(Routes.ThemeEditor) { ThemeEditorScreen(navController) }
        composable(Routes.ThemeCarousel) { ThemeCarouselScreen(navController) }
        composable(Routes.ClockStyle) { ClockStyleScreen(navController) }
        composable(Routes.SelectWidget) { SelectWidgetScreen(navController) }
        composable(Routes.EditFavorites) {
            EditFavoritesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HideApps) {
            HideAppsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Folders) {
            FoldersScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MusicApps) {
            MusicAppsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CalendarAgenda) {
            CalendarAgendaScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Changelog) {
            ChangelogScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DiablePro) {
            DiableProScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HelpFeedback) {
            HelpFeedbackScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Community) {
            CommunityScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CreateTheme) { CreateThemeScreen(navController) }
        composable(Routes.EditTheme) { EditThemeScreen(navController) }
        composable(Routes.MyThemes) { MyThemesScreen(navController) }
        composable(Routes.ReadyMadeThemes) { ReadyMadeThemesScreen(navController) }
        composable(Routes.CommunityThemes) { CommunityThemesScreen(navController) }
    }
}
