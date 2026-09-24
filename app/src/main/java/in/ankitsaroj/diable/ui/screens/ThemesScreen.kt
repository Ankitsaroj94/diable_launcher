package `in`.ankitsaroj.diable.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import `in`.ankitsaroj.diable.navigation.Routes
import `in`.ankitsaroj.diable.ui.components.DiableSettingsRow
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.components.ThemeScreenScaffold

@Composable
fun ThemesScreen(navController: NavController) {
    ThemeScreenScaffold(
        header = { PillHeader("Themes") },
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                DiableSettingsRow(
                    title = "Create Theme",
                    icon = Icons.Default.Add,
                    summary = "Build a new theme from scratch",
                    onClick = { navController.navigate(Routes.CreateTheme) },
                )
                DiableSettingsRow(
                    title = "Edit Current Theme",
                    icon = Icons.Default.Edit,
                    summary = "Fine-tune your setup",
                    onClick = { navController.navigate(Routes.ThemeEditor) },
                )
                DiableSettingsRow(
                    title = "My Themes",
                    icon = Icons.Default.Palette,
                    summary = "Your custom creations and imported themes",
                    onClick = { navController.navigate(Routes.MyThemes) },
                )
                DiableSettingsRow(
                    title = "Ready-Made Themes",
                    icon = Icons.Default.AutoAwesome,
                    summary = "Pick from handcrafted themes with just one tap",
                    onClick = { navController.navigate(Routes.ReadyMadeThemes) },
                )
                DiableSettingsRow(
                    title = "Community Themes",
                    icon = Icons.Default.Groups,
                    summary = "Discover themes made by our community",
                    onClick = { navController.navigate(Routes.CommunityThemes) },
                )
            }
        }
    }
}
