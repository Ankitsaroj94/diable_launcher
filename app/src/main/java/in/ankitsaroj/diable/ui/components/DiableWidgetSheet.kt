package `in`.ankitsaroj.diable.ui.components

import `in`.ankitsaroj.diable.ui.theme.DiableAccentText
import `in`.ankitsaroj.diable.ui.theme.DiableText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import `in`.ankitsaroj.diable.service.DiableNotificationListener
import `in`.ankitsaroj.diable.ui.theme.DiableAccent
import `in`.ankitsaroj.diable.ui.theme.DiableCard
import `in`.ankitsaroj.diable.ui.theme.DiableTextMuted
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import `in`.ankitsaroj.diable.data.CityResult
import `in`.ankitsaroj.diable.data.WeatherRepository
import kotlinx.coroutines.delay
import `in`.ankitsaroj.diable.data.LocalDiableRepos
import `in`.ankitsaroj.diable.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun DiableWidgetSheetContent(
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
    onShowEventsPromo: () -> Unit = {},
) {
    val repos = LocalDiableRepos.current
    val prefs by repos.prefs.prefsFlow.collectAsState(initial = `in`.ankitsaroj.diable.data.DiablePrefs())
    val scope = rememberCoroutineScope()
    var showCalendarSheet by remember { mutableStateOf(false) }
    var showWeatherSheet by remember { mutableStateOf(false) }
    var showCitySearch by remember { mutableStateOf(false) }
    var showAccessDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showAccessDialog) {
        AlertDialog(
            onDismissRequest = { showAccessDialog = false },
            containerColor = DiableCard,
            title = { Text("Allow notification access", color = DiableText) },
            text = {
                Text(
                    "The media player reads what's playing through notification access. " +
                        "Turn it on for this launcher on the next screen.",
                    color = DiableTextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAccessDialog = false
                    DiableNotificationListener.openAccessSettings(context)
                }) { Text("Open settings", color = DiableAccentText) }
            },
            dismissButton = {
                TextButton(onClick = { showAccessDialog = false }) { Text("Later", color = DiableTextMuted) }
            },
        )
    }

    if (showCitySearch) {
        BackHandler { showCitySearch = false }
        WeatherCityPicker(
            currentCity = prefs.weatherCityName,
            onPickDevice = {
                repos.prefs.launchUpdate { p -> p.copy(weatherCityName = null, weatherLat = 0f, weatherLon = 0f) }
                showCitySearch = false
            },
            onPickCity = { city ->
                repos.prefs.launchUpdate { p ->
                    p.copy(
                        weatherCityName = city.label,
                        weatherLat = city.lat.toFloat(),
                        weatherLon = city.lon.toFloat(),
                        weatherEnabled = true,
                    )
                }
                showCitySearch = false
            },
        )
        return
    }

    if (showWeatherSheet) {
        // BACK steps out to the widget sheet instead of closing everything.
        BackHandler { showWeatherSheet = false }
        // Weather options: on/off, units, and where the location comes from.
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
            Text(
                text = "Weather",
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                fontSize = 23.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            DiableSettingCard(
                icon = Icons.Default.WbSunny,
                title = "Show weather",
                checked = prefs.weatherEnabled,
                onCheckedChange = { repos.prefs.launchUpdate { p -> p.copy(weatherEnabled = it) } },
            )
            DiableSettingCard(
                icon = Icons.Default.Thermostat,
                title = "Temperature units",
                subtitle = if (prefs.weatherCelsius) "Celsius (°C)" else "Fahrenheit (°F)",
                onClick = { repos.prefs.launchUpdate { p -> p.copy(weatherCelsius = !p.weatherCelsius) } },
            )
            DiableSettingCard(
                icon = Icons.Default.LocationOn,
                title = "Location",
                subtitle = prefs.weatherCityName ?: "Device location",
                onClick = { showCitySearch = true },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        return
    }

    if (showCalendarSheet) {
        CalendarSettingsSheet(onDismiss = { showCalendarSheet = false })
        return
    }

    // Diable's sheet: seven widget rows, a 36px break, then two navigation rows.
    Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
        Text(
            text = "Diable widget",
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
            fontSize = 23.sp,
            color = DiableText,
        )
        DiableSettingCard(
            icon = Icons.Default.Schedule,
            title = "Clock style",
            onClick = {
                onDismiss()
                onNavigate(Routes.ClockStyle)
            },
        )
        DiableSettingCard(
            icon = Icons.Default.CalendarMonth,
            title = "Calendar",
            subtitle = if (prefs.showUpcomingEvents) {
                "Show upcoming events, Show date"
            } else {
                "Show date"
            },
            onClick = { showCalendarSheet = true },
        )
        DiableSettingCard(
            icon = Icons.Default.WbSunny,
            title = "Weather",
            subtitle = "Location, Temperature units",
            checked = prefs.weatherEnabled,
            onCheckedChange = {
                repos.prefs.launchUpdate { p -> p.copy(weatherEnabled = it) }
            },
            onClick = { showWeatherSheet = true },
        )
        DiableSettingCard(
            icon = Icons.Default.Add,
            title = "Add Custom Widget",
            onClick = {
                onDismiss()
                onNavigate(Routes.SelectWidget)
            },
        )
        DiableSettingCard(
            icon = Icons.Default.OpenWith,
            title = "Move Widget",
            onClick = {
                onDismiss()
                `in`.ankitsaroj.diable.ui.screens.HomeCommands.moveWidget.value = true
            },
        )
        DiableSettingCard(
            icon = Icons.Default.MusicNote,
            title = "Show Media Widget",
            subtitle = "While playing music",
            checked = prefs.showMediaWidget,
            onCheckedChange = {
                // Media sessions are only readable with notification access.
                if (it && !DiableNotificationListener.isEnabled(context)) showAccessDialog = true
                repos.prefs.launchUpdate { p -> p.copy(showMediaWidget = it, mediaPlayer = it) }
            },
        )
        DiableSettingCard(
            icon = Icons.Default.BatteryFull,
            title = "Show Battery Percentage",
            checked = prefs.showBatteryPercentage,
            onCheckedChange = {
                repos.prefs.launchUpdate { p -> p.copy(showBatteryPercentage = it) }
            },
        )
        Spacer(modifier = Modifier.height(24.dp))
        DiableSettingCard(
            icon = Icons.Default.StarOutline,
            title = "Edit favorites",
            onClick = {
                onDismiss()
                onNavigate(Routes.EditFavorites)
            },
        )
        DiableSettingCard(
            icon = Icons.Default.Settings,
            title = "Diable settings",
            onClick = {
                onDismiss()
                onNavigate(Routes.DiableSettings)
            },
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiableWidgetSheet(
    onDismissRequest: () -> Unit,
    onNavigate: (String) -> Unit,
    onShowEventsPromo: () -> Unit = {},
) {
    DiableBottomSheet(onDismissRequest = onDismissRequest) {
        DiableWidgetSheetContent(
            onNavigate = onNavigate,
            onDismiss = onDismissRequest,
            onShowEventsPromo = onShowEventsPromo,
        )
    }
}

/**
 * Weather location: the device's own location, or any city found through Open-Meteo's
 * geocoding search (debounced so each keystroke doesn't hit the network).
 */
@Composable
private fun WeatherCityPicker(
    currentCity: String?,
    onPickDevice: () -> Unit,
    onPickCity: (CityResult) -> Unit,
) {
    val repo = remember { WeatherRepository() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CityResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onPickDevice() }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        searching = true
        results = repo.searchCities(query)
        searching = false
    }

    Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)) {
        Text(
            text = "Weather location",
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            fontSize = 23.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        DiableSettingCard(
            icon = Icons.Default.MyLocation,
            title = "Use device location",
            subtitle = if (currentCity == null) "Selected" else null,
            onClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) onPickDevice() else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search for a city", color = DiableTextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DiableTextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = DiableAccentText,
                cursorColor = DiableAccentText,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
        )
        if (searching) {
            Text("Searching…", color = DiableTextMuted, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
        } else if (query.isNotBlank() && results.isEmpty()) {
            Text("No places found", color = DiableTextMuted, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
        }
        Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            results.forEach { city ->
                DiableSettingCard(
                    icon = Icons.Default.LocationOn,
                    title = city.name,
                    subtitle = listOfNotNull(city.admin1, city.country).joinToString(", ").ifBlank { null },
                    onClick = { onPickCity(city) },
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
