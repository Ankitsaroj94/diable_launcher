package `in`.ankitsaroj.diable.ui.screens

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.text.input.ImeAction
import `in`.ankitsaroj.diable.data.search.SearchEngine
import `in`.ankitsaroj.diable.data.search.primaryPhoneNumber
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import kotlinx.coroutines.delay
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.ankitsaroj.diable.data.ContactInfo
import `in`.ankitsaroj.diable.data.IconStyle
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.model.AppInfo
import `in`.ankitsaroj.diable.ui.components.AppListItem
import `in`.ankitsaroj.diable.ui.components.LavenderIconCircle
import `in`.ankitsaroj.diable.ui.components.DiableBottomSheet
import `in`.ankitsaroj.diable.ui.components.DiableSettingCard
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableNavy
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import `in`.ankitsaroj.diable.util.evaluateMath
import `in`.ankitsaroj.diable.util.looksLikeMath
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    BackHandler(onBack = onBack)
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = `in`.ankitsaroj.diable.data.DiablePrefs())
    val apps by repos.apps.apps.collectAsState()
    LaunchedEffect(Unit) { repos.apps.ensureLoaded() }
    var query by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    var webSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSettings by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Diable opens search straight into typing. The field isn't focusable until the
    // navigation transition has attached it, so wait a frame or two before asking.
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }

    // Held as state so the "Allow contact search" row disappears the moment access is
    // granted — here or in system settings — instead of on some later recomposition.
    var hasContactsPermission by remember { mutableStateOf(repos.contacts.hasPermission()) }
    LifecycleResumeEffect(Unit) {
        hasContactsPermission = repos.contacts.hasPermission()
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasContactsPermission = granted
        if (granted && query.isNotBlank()) {
            scope.launch {
                contacts = repos.contacts.searchByName(query)
            }
        }
    }

    LaunchedEffect(query, prefs.contactSearch, hasContactsPermission) {
        if (prefs.contactSearch && query.isNotBlank()) {
            if (hasContactsPermission) {
                contacts = repos.contacts.searchByName(query)
            } else {
                contacts = emptyList()
            }
        } else {
            contacts = emptyList()
        }
    }

    val engine = SearchEngine.fromKey(prefs.webSearchEngine)
    val webEnabled = prefs.webSearchSuggestions && engine != SearchEngine.None
    // Debounced: typing restarts the effect, so only a paused query hits the network.
    LaunchedEffect(query, engine, webEnabled) {
        webSuggestions = emptyList()
        if (!webEnabled || query.isBlank()) return@LaunchedEffect
        delay(250)
        webSuggestions = engine.suggestions(query)
    }

    val filteredApps = remember(apps, query) { matchApps(apps, query) }

    val lastResult = remember(apps, prefs.lastSearchPackage) {
        apps.firstOrNull { it.packageName == prefs.lastSearchPackage }
    }
    val recentApps = remember(apps, prefs.smartSuggestions, query, lastResult) {
        if (query.isNotBlank() || !prefs.smartSuggestions) emptyList()
        else {
            // Diable shows a handful of genuinely recent apps, not everything that
            // happens to be young on a fresh device.
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14)
            apps.filter { it.firstInstallTime >= cutoff && it.packageName != lastResult?.packageName }
                .sortedByDescending { it.firstInstallTime }
                .take(5)
        }
    }

    val iconStyle = IconStyle.fromKey(prefs.iconStyleKey)
    val accent = Color(prefs.accentColorArgb)

    val calcResult = remember(query, prefs.calculatorInSearch) {
        if (!prefs.calculatorInSearch || !looksLikeMath(query)) null
        else evaluateMath(query)
    }

    // Every launch from search is remembered as the "Last result" and counts as usage.
    val launchApp: (AppInfo) -> Unit = { app ->
        repos.prefs.launchUpdate { p ->
                val count = (p.usageCounts[app.packageName] ?: 0) + 1
                p.copy(
                    lastSearchPackage = app.packageName,
                    usageCounts = p.usageCounts + (app.packageName to count),
                )
            }
        runCatching {
            context.packageManager.getLaunchIntentForPackage(app.packageName)
                ?.let { context.startActivity(it) }
        }
        keyboard?.hide()
        onBack()
    }
    val openWeb: (String) -> Unit = { text ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(engine.resultsUrl(text))))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Search is a full content surface over the wallpaper, so it needs a
            // heavier scrim than the home screen to stay legible on busy images.
            .background(Color.Black.copy(alpha = if (prefs.dimWallpaper) 0.72f else 0.55f))
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                focusRequester = focusRequester,
                onMoreClick = { showSettings = true },
                // The keyboard's Search key opens the top hit, like Diable.
                onSubmit = {
                    when {
                        filteredApps.isNotEmpty() -> launchApp(filteredApps.first())
                        webEnabled && query.isNotBlank() -> openWeb(query)
                    }
                },
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    // Matches Diable: icon column at 48dp, labels landing at ~105dp.
                    .padding(start = 48.dp, end = 24.dp),
            ) {
                calcResult?.let { result ->
                    item(key = "calc") {
                        val text = formatCalc(result)
                        SearchResultRow(
                            icon = { LavenderIconCircle(Icons.Default.Calculate) },
                            title = "= $text",
                            subtitle = query.trim(),
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Result", text))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
                // Empty query: Diable's smart suggestions, each labelled inline.
                if (query.isBlank() && prefs.smartSuggestions && lastResult != null) {
                    item(key = "last_${lastResult.packageName}") {
                        AppListItem(
                            app = lastResult,
                            subtitle = "Last result",
                            iconStyle = iconStyle,
                            accentColor = accent,
                            onLaunch = launchApp,
                        )
                    }
                }
                if (recentApps.isNotEmpty()) {
                    items(recentApps, key = { "recent_${it.packageName}" }) { app ->
                        AppListItem(
                            app = app,
                            subtitle = "Recently installed",
                            iconStyle = iconStyle,
                            accentColor = accent,
                            onLaunch = launchApp,
                        )
                    }
                }
                if (filteredApps.isNotEmpty()) {
                    itemsIndexed(filteredApps, key = { _, a -> a.packageName }) { index, app ->
                        // The top hit is what the Search key launches, so Diable hints that inline.
                        AppListItem(
                            app = app,
                            subtitle = if (index == 0) "Tap Search to open" else null,
                            iconStyle = iconStyle,
                            accentColor = accent,
                            onLaunch = launchApp,
                        )
                    }
                }
                if (contacts.isNotEmpty()) {
                    item(key = "contacts_hdr") {
                        SectionLabel("Contacts")
                    }
                    items(contacts, key = { "contact_${it.id}" }) { contact ->
                        val openContact = {
                            contact.lookupKey?.let { key ->
                                val uri = Uri.withAppendedPath(
                                    ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                                    key,
                                )
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW).setData(uri))
                                }
                            }
                            Unit
                        }
                        SearchResultRow(
                            icon = { LavenderIconCircle(Icons.Default.ContactPage) },
                            title = contact.name,
                            onClick = openContact,
                            trailing = {
                                IconButton(onClick = {
                                    scope.launch {
                                        val number = primaryPhoneNumber(context, contact.id)
                                        if (number == null) {
                                            openContact()
                                        } else {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))),
                                                )
                                            }
                                        }
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.Call,
                                        contentDescription = "Call ${contact.name}",
                                        tint = Color.White.copy(alpha = 0.8f),
                                    )
                                }
                            },
                        )
                    }
                }
                if (prefs.contactSearch && query.isNotBlank() && !hasContactsPermission) {
                    item(key = "contacts_perm") {
                        SearchResultRow(
                            title = "Allow contact search",
                            subtitle = "Tap to grant contacts permission",
                            onClick = {
                                permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                            },
                        )
                    }
                }
                if (webEnabled && query.isNotBlank()) {
                    item(key = "web_hdr") { SectionLabel("Web suggestions") }
                    val rows = listOf(query.trim()) + webSuggestions
                    items(rows, key = { "web_$it" }) { suggestion ->
                        SearchResultRow(
                            icon = { LavenderIconCircle(Icons.Default.Search) },
                            title = suggestion,
                            onClick = { openWeb(suggestion) },
                        )
                    }
                }
                if (query.isNotBlank()) {
                    item(key = "more_apps") {
                        SearchResultRow(
                            icon = { PlayStoreDot() },
                            title = "Search for more apps",
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("market://search?q=${Uri.encode(query)}&c=apps"),
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showSettings) {
        SearchSettingsSheet(
            onDismiss = { showSettings = false },
        )
    }
}

/**
 * Diable matches the start of any word in the label: "ma" finds Maps, "mes" finds
 * Messages, but "e" finds neither. Labels that start with the query rank first.
 */
private val WORD_BREAK = Regex("[\\s\\-_.:&/()+,]+")

internal fun matchApps(apps: List<AppInfo>, query: String): List<AppInfo> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    return apps.mapNotNull { app ->
        when {
            app.name.startsWith(q, ignoreCase = true) -> 0 to app
            app.name.split(WORD_BREAK).any { it.startsWith(q, ignoreCase = true) } -> 1 to app
            else -> null
        }
    }.sortedWith(compareBy({ it.first }, { it.second.name.lowercase() })).map { it.second }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onMoreClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    // Diable's pill: no leading icon, 24dp side margins, fully rounded, overflow on the right.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            // Over the wallpaper in both modes, like home: the pill stays dark so its white
            // text reads. The light-theme pill left typed text white on pale.
            .background(DiableNavy.copy(alpha = 0.92f), RoundedCornerShape(percent = 50))
            .padding(start = 24.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 14.dp)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontSize = 19.sp,
            ),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search apps",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 19.sp,
                    )
                }
                inner()
            },
        )
        IconButton(onClick = onMoreClick) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Search settings",
                tint = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSettingsSheet(onDismiss: () -> Unit) {
    // "Learn more" is answered in the app; there is no external help site.
    var showCalcHelp by remember { mutableStateOf(false) }
    if (showCalcHelp) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCalcHelp = false },
            containerColor = `in`.ankitsaroj.diable.ui.theme.DiableCard,
            title = { Text("Calculator", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Text(
                    "Type a math expression in search to see the result; tap it to copy.\n\n" +
                        "Operators: + - * / ^ %, and ! for factorial. 2(3) and 2pi multiply.\n" +
                        "Constants: pi, e.\n" +
                        "Functions: abs, sqrt, log, ln, log10, fact, round, floor, ceil, " +
                        "sin, cos, tan, asin, acos, atan, max, min, random.",
                    color = DiableTextMuted,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showCalcHelp = false }) {
                    Text("Okay", color = DiableAccentText)
                }
            },
        )
    }
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = `in`.ankitsaroj.diable.data.DiablePrefs())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val engine = SearchEngine.fromKey(prefs.webSearchEngine)
    var showEngines by remember { mutableStateOf(false) }
    var consentFor by remember { mutableStateOf<SearchEngine?>(null) }
    var confirmContacts by remember { mutableStateOf(false) }

    val contactsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    fun applyEngine(choice: SearchEngine) {
        repos.prefs.launchUpdate { p ->
                p.copy(
                    webSearchEngine = choice.key,
                    webSearchSuggestions = choice != SearchEngine.None,
                )
            }
    }

    if (showEngines) {
        SearchEngineSheet(
            selected = if (prefs.webSearchSuggestions) engine else SearchEngine.None,
            onPick = { choice ->
                showEngines = false
                if (choice == SearchEngine.None) applyEngine(choice) else consentFor = choice
            },
            onDismiss = { showEngines = false },
        )
    }
    consentFor?.let { choice ->
        AlertDialog(
            onDismissRequest = { consentFor = null },
            containerColor = DiableCard,
            title = { Text("Web suggestions", color = DiableText) },
            text = {
                Text(
                    "Your search queries will be sent to ${choice.label} to fetch suggestions. " +
                        "Apps and contacts never leave your device.",
                    color = DiableTextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    applyEngine(choice)
                    consentFor = null
                }) { Text("Enable", color = DiableAccentText) }
            },
            dismissButton = {
                TextButton(onClick = { consentFor = null }) { Text("Cancel", color = DiableAccentText) }
            },
        )
    }
    if (confirmContacts) {
        AlertDialog(
            onDismissRequest = { confirmContacts = false },
            containerColor = DiableCard,
            title = { Text("Contact Search", color = DiableText) },
            text = {
                Text(
                    "Find your contacts right from the app search. Contacts stay on your device.",
                    color = DiableTextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmContacts = false
                    repos.prefs.launchUpdate { p -> p.copy(contactSearch = true) }
                    if (!repos.contacts.hasPermission()) {
                        contactsPermission.launch(android.Manifest.permission.READ_CONTACTS)
                    }
                }) { Text("Enable", color = DiableAccentText) }
            },
            dismissButton = {
                TextButton(onClick = { confirmContacts = false }) {
                    Text("Keep disabled", color = DiableAccentText)
                }
            },
        )
    }

    DiableBottomSheet(onDismissRequest = onDismiss) {
        // Diable insets this sheet's content by 32dp and gives every row its own card.
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
            Text(
                text = "Search",
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                fontSize = 23.sp,
                color = DiableText,
                fontWeight = FontWeight.Normal,
            )
            // Diable: tapping opens the provider list rather than flipping a switch.
            DiableSettingCard(
                icon = Icons.Default.Language,
                title = "Web Search Suggestions",
                subtitle = if (prefs.webSearchSuggestions && engine != SearchEngine.None) {
                    engine.label
                } else {
                    "Add search suggestions from the web to the app search"
                },
                onClick = { showEngines = true },
            )
            DiableSettingCard(
                icon = Icons.Default.ContactPage,
                title = "Contact Search",
                subtitle = "Add contacts to the app search",
                checked = prefs.contactSearch,
                onCheckedChange = { on ->
                    if (on) {
                        confirmContacts = true
                    } else {
                        repos.prefs.launchUpdate { p -> p.copy(contactSearch = false) }
                    }
                },
            )
            DiableSettingCard(
                icon = Icons.Default.AutoAwesome,
                title = "Smart Suggestions",
                subtitle = "Show smart suggestions when the search bar is empty",
                checked = prefs.smartSuggestions,
                onCheckedChange = {
                    repos.prefs.launchUpdate { p -> p.copy(smartSuggestions = it) }
                },
            )
            DiableSettingCard(
                icon = Icons.Default.Calculate,
                title = "Calculator",
                subtitle = "Enter math expressions to get instant results",
                linkText = "Learn more",
                onLinkClick = { showCalcHelp = true },
                checked = prefs.calculatorInSearch,
                onCheckedChange = {
                    repos.prefs.launchUpdate { p -> p.copy(calculatorInSearch = it) }
                },
            )
            DiableSettingCard(
                icon = Icons.Default.ArrowUpward,
                title = "Swipe up to search",
                subtitle = "Pull up your favorites for app search",
                checked = prefs.swipeUpToSearch,
                onCheckedChange = {
                    repos.prefs.launchUpdate { p -> p.copy(swipeUpToSearch = it) }
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Diable's provider picker: a radio list, "No suggestions" first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchEngineSheet(
    selected: SearchEngine,
    onPick: (SearchEngine) -> Unit,
    onDismiss: () -> Unit,
) {
    DiableBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
            Text(
                text = "Web Search Suggestions",
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                fontSize = 23.sp,
                color = DiableText,
            )
            SearchEngine.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPick(option) }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = option == selected,
                        onClick = { onPick(option) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = DiableAccentText,
                            unselectedColor = DiableTextMuted,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(option.label, color = DiableText, fontSize = 17.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        color = DiableTextMuted,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SearchResultRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.invoke()
        if (icon != null) Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, color = DiableTextMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        trailing?.invoke()
    }
}

private fun formatCalc(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString()
    else "%.4f".format(value).trimEnd('0').trimEnd('.')
}

/** The small dot Diable shows beside its "Search for more apps" row. */
@Composable
private fun PlayStoreDot() {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
