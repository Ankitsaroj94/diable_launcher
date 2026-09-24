package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.DiablePrefs
import `in`.ankitsaroj.diable.navigation.Routes
import `in`.ankitsaroj.diable.ui.components.DiableSettingsRow
import `in`.ankitsaroj.diable.ui.components.PillHeader
import `in`.ankitsaroj.diable.ui.components.ThemeScreenScaffold
import `in`.ankitsaroj.diable.ui.screens.settings.SavedTheme
import `in`.ankitsaroj.diable.ui.screens.settings.SavedThemes
import `in`.ankitsaroj.diable.ui.screens.settings.ThemeSnapshot
import `in`.ankitsaroj.diable.ui.screens.settings.shareText
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableOnAccent
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import `in`.ankitsaroj.diable.ui.theme.ThemeFont
import kotlinx.coroutines.launch

/** Create Theme: pick a wallpaper first, then land in the editor to style the rest. */
@Composable
fun CreateThemeScreen(navController: NavController) {
    ChooseWallpaperScreen(
        navController = navController,
        onApplied = {
            ThemeEditorLaunch.saveAsNewTheme = true
            navController.navigate(Routes.ThemeEditor) {
                popUpTo(Routes.CreateTheme) { inclusive = true }
            }
        },
    )
}

@Composable
fun EditThemeScreen(navController: NavController) {
    ThemeScreenScaffold(header = { PillHeader("Edit Theme") }) {
        DiableSettingsRow(
            title = "Open theme editor",
            onClick = { navController.navigate(Routes.ThemeEditor) },
        )
    }
}

/** My Themes: save the current look under a name, then apply or delete saved ones. */
@Composable
fun MyThemesScreen(navController: NavController) {
    val context = LocalContext.current
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val scope = rememberCoroutineScope()
    var themes by remember { mutableStateOf(SavedThemes.load(context)) }
    var naming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<SavedTheme?>(null) }

    ThemeScreenScaffold(header = { PillHeader("My Themes") }) {
        if (themes.isEmpty()) {
            MyThemesEmptyState(
                onSave = { naming = true },
                onReadyMade = { navController.navigate(Routes.ReadyMadeThemes) },
                onCreate = { navController.navigate(Routes.CreateTheme) },
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                item {
                    PrimaryButton("Save current theme", onClick = { naming = true })
                    Spacer(modifier = Modifier.height(12.dp))
                }
                items(themes, key = { it.id }) { theme ->
                    SavedThemeCard(
                        theme = theme,
                        onApply = {
                            scope.launch {
                                repos.prefs.update { theme.snapshot.applyTo(it) }
                                Toast.makeText(context, "\"${theme.name}\" applied", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShare = { shareText(context, SavedThemes.shareJson(theme.name, theme.snapshot)) },
                        onDelete = { deleting = theme },
                    )
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    if (naming) {
        NameThemeDialog(
            initial = "My theme ${themes.size + 1}",
            onDismiss = { naming = false },
            onSave = { name ->
                naming = false
                themes = SavedThemes.save(context, name, ThemeSnapshot.of(prefs))
            },
        )
    }
    deleting?.let { theme ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = DiableCard,
            title = { Text("Delete \"${theme.name}\"?", color = DiableText) },
            text = { Text("Your current home screen won't change.", color = DiableTextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    themes = SavedThemes.delete(context, theme.id)
                    deleting = null
                }) { Text("Delete", color = DiableAccentText) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel", color = DiableAccentText) }
            },
        )
    }
}

@Composable
private fun MyThemesEmptyState(onSave: () -> Unit, onReadyMade: () -> Unit, onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(0.82f, 1f, 0.82f).forEach { scale ->
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height((300 * scale).dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(DiableAccent.copy(alpha = 0.35f), DiableCard))),
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text("Nothing here yet", color = DiableText, fontSize = 23.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Save your current look, pick a ready-made theme, or create your own from scratch",
            color = DiableTextMuted,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton("Pick Ready-Made Theme", onClick = onReadyMade)
        TextAction("Save Current Theme", onClick = onSave)
        TextAction("Create Own Theme", onClick = onCreate)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SavedThemeCard(
    theme: SavedTheme,
    onApply: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val snap = theme.snapshot
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DiableCard)
            .clickable(onClick = onApply)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(snap.accentColorArgb)),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(theme.name, color = DiableText, fontSize = 17.sp)
            Text(
                "${ThemeFont.fromKey(snap.fontKey).label} · ${IconStyle.fromKey(snap.iconStyleKey).label} icons",
                color = DiableTextMuted,
                fontSize = 13.sp,
                fontFamily = ThemeFont.fromKey(snap.fontKey).family,
            )
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "Share", tint = DiableAccentText)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DiableTextMuted)
        }
    }
}

/** Community Themes: browse, share the current theme as text, or import one from the clipboard. */
@Composable
fun CommunityThemesScreen(navController: NavController) {
    val context = LocalContext.current
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = DiablePrefs())
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf<Pair<String, ThemeSnapshot>?>(null) }

    ThemeScreenScaffold(header = { PillHeader("Community Themes") }) {
        Column {
            DiableSettingsRow(
                title = "Browse",
                icon = Icons.Default.Explore,
                summary = "Themes shared by other users",
                // There is no online gallery for this launcher; themes travel as shared text.
                onClick = {
                    android.widget.Toast.makeText(
                        context,
                        "No online gallery yet. Ask a friend to Share Current, then use Import.",
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                },
            )
            DiableSettingsRow(
                title = "Share Current",
                icon = Icons.Default.Share,
                summary = "Send your theme to a friend or post it",
                onClick = { shareText(context, SavedThemes.shareJson("Shared theme", ThemeSnapshot.of(prefs))) },
            )
            DiableSettingsRow(
                title = "Import",
                icon = Icons.Default.ContentPaste,
                summary = "Copy a shared theme, then tap here",
                onClick = {
                    val text = clipboardText(context)
                    if (text.isNullOrBlank()) {
                        Toast.makeText(context, "Copy a theme first", Toast.LENGTH_SHORT).show()
                    } else {
                        runCatching { SavedThemes.parseShared(text, prefs) }
                            .onSuccess { importing = it }
                            .onFailure {
                                Toast.makeText(context, it.message ?: "Not a theme", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
            )
            DiableSettingsRow(
                title = "Need Help?",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                summary = "How sharing and importing themes works",
                onClick = { navController.navigate(Routes.HelpFeedback) },
            )
        }
    }

    importing?.let { (name, snapshot) ->
        AlertDialog(
            onDismissRequest = { importing = null },
            containerColor = DiableCard,
            title = { Text("Import \"$name\"?", color = DiableText) },
            text = {
                Text(
                    "It will be saved to My Themes. Apply it now as well?",
                    color = DiableTextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    importing = null
                    SavedThemes.save(context, name, snapshot)
                    repos.prefs.launchUpdate { snapshot.applyTo(it) }
                    Toast.makeText(context, "Theme imported and applied", Toast.LENGTH_SHORT).show()
                }) { Text("Import & apply", color = DiableAccentText) }
            },
            dismissButton = {
                TextButton(onClick = {
                    importing = null
                    SavedThemes.save(context, name, snapshot)
                    Toast.makeText(context, "Saved to My Themes", Toast.LENGTH_SHORT).show()
                }) { Text("Just save", color = DiableAccentText) }
            },
        )
    }
}

private fun clipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}

@Composable
private fun NameThemeDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DiableCard,
        title = { Text("Save theme", color = DiableText) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DiableText,
                    unfocusedTextColor = DiableText,
                    focusedBorderColor = DiableAccentText,
                    focusedLabelColor = DiableAccentText,
                    cursorColor = DiableAccentText,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) {
                Text("Save", color = DiableAccentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = DiableAccentText) }
        },
    )
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(DiableAccent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = DiableOnAccent, fontSize = 17.sp)
    }
}

@Composable
private fun TextAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = DiableAccentText, fontSize = 17.sp)
    }
}

/** Ready-Made is the carousel itself in Diable, not an intermediate list. */
@Composable
fun ReadyMadeThemesScreen(navController: NavController) = ThemeCarouselScreen(navController)
