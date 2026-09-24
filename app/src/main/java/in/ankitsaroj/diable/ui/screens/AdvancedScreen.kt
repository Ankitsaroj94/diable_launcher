package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.data.PrefsRepository
import `in`.ankitsaroj.diable.navigation.Routes
import `in`.ankitsaroj.diable.ui.components.DiableSettingsRow
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableBg
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import `in`.ankitsaroj.diable.ui.screens.settings.OpenSourceLicensesSheet
import `in`.ankitsaroj.diable.ui.screens.settings.PrivacySettingsSheet
import `in`.ankitsaroj.diable.ui.screens.settings.LegalDoc
import `in`.ankitsaroj.diable.ui.screens.settings.LegalSheet

@Composable
fun AdvancedScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onRestart: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = `in`.ankitsaroj.diable.data.DiablePrefs())
    val scope = rememberCoroutineScope()
    val versionName = rememberVersionName()
    // Diable exposes a single Backup & Restore row, so both directions live behind it.
    var showBackupChoice by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var legalDoc by remember { mutableStateOf<LegalDoc?>(null) }

    // Storage Access Framework: the user picks where the backup goes. Sharing a file://
    // URI instead throws FileUriExposedException on every supported Android version.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val json = PrefsRepository.diablePrefsToJson(prefs)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                        ?: error("Could not open $uri")
                }
            }.isSuccess
            Toast.makeText(
                context,
                if (ok) "Backup created" else "Backup failed",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                if (json.isNullOrBlank()) return@launch
                val imported = PrefsRepository.diablePrefsFromJson(json, prefs)
                repos.prefs.update { imported }
                Toast.makeText(context, "Settings restored", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiableBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(text = "Advanced", fontSize = 26.sp, color = DiableText)
        Spacer(modifier = Modifier.height(20.dp))
        Column(modifier = Modifier.fillMaxSize()) {
            DiableSettingsRow(
                title = "App info",
                summary = "Version $versionName",
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null)),
                        )
                    }
                },
            )
            DiableSettingsRow(
                title = "Backup & Restore (Beta)",
                onClick = { showBackupChoice = true },
            )
            DiableSettingsRow(
                title = "Changelog",
                onClick = { onNavigate(Routes.Changelog) },
            )
            DiableSettingsRow(
                title = "Change default launcher",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_HOME_SETTINGS),
                    )
                },
            )
            DiableSettingsRow(
                title = "Uninstall launcher",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.parse("package:${context.packageName}")
                        },
                    )
                },
            )
            DiableSettingsRow(
                title = "Restart launcher",
                summary = "Have you tried turning it off and on again?",
                onClick = onRestart,
            )

        }
        Column(modifier = Modifier.fillMaxSize()) {
            LinkText("Privacy policy") { legalDoc = LegalDoc.Privacy }
            LinkText("Terms of Service") { legalDoc = LegalDoc.Terms }
            LinkText("Privacy Settings") { showPrivacy = true }
            LinkText("Open source licenses") { showLicenses = true }
        }
    }

    if (showBackupChoice) {
        AlertDialog(
            onDismissRequest = { showBackupChoice = false },
            containerColor = DiableCard,
            title = { Text("Backup & Restore", color = DiableText) },
            text = {
                Text(
                    "Save your favorites, themes and settings to a file, or restore them from one.",
                    color = DiableTextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackupChoice = false
                    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportLauncher.launch("launcher-backup-$stamp.json")
                }) { Text("Create backup", color = DiableAccentText) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupChoice = false
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }) { Text("Restore backup", color = DiableAccentText) }
            },
        )
    }
    if (showPrivacy) {
        PrivacySettingsSheet(onDismiss = { showPrivacy = false })
    }
    if (showLicenses) {
        OpenSourceLicensesSheet(onDismiss = { showLicenses = false })
    }
    legalDoc?.let { doc -> LegalSheet(doc = doc, onDismiss = { legalDoc = null }) }
}

@Composable
private fun LinkText(label: String, onClick: () -> Unit) {
    DiableSettingsRow(title = label, iconGutter = false, onClick = onClick)
}

@Composable
private fun rememberVersionName(): String {
    val context = LocalContext.current
    return try {
        @Suppress("DEPRECATION")
        val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        info.versionName ?: "1.0"
    } catch (_: Exception) {
        "1.0"
    }
}
