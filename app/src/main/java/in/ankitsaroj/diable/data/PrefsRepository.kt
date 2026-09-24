package `in`.ankitsaroj.diable.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "diable_prefs")

/** Matches Diable's Dark Theme radio list, in its display order. */
enum class DarkThemeMode(val label: String) {
    UseDeviceTheme("Use Device Theme"),
    DeviceThemeAndWallpaper("Based on Device Theme and Wallpaper"),
    AlwaysEnabled("Always Enabled"),
    AlwaysDisabled("Always Disabled"),
}

data class DiablePrefs(
    val webSearchSuggestions: Boolean = false,
    val contactSearch: Boolean = false,
    val smartSuggestions: Boolean = true,
    val swipeUpToSearch: Boolean = true,
    val calculatorInSearch: Boolean = true,
    val weatherEnabled: Boolean = false,
    val showMediaWidget: Boolean = false,
    val showBatteryPercentage: Boolean = false,
    val calendarPreview: Boolean = false,
    val quickReplies: Boolean = false,
    val weatherForecast: Boolean = false,
    val mediaPlayer: Boolean = false,
    val usageBreaker: Boolean = false,
    val notificationSummary: Boolean = false,
    val suggestOftenUsed: Boolean = true,
    val sortByUsage: Boolean = true,
    val diableButton: Boolean = true,
    val quickLock: Boolean = false,
    val allowRotation: Boolean = false,
    val allowHaptic: Boolean = true,
    val showUpcomingEvents: Boolean = false,
    val clockStyleId: Int = 0,
    val accentColorArgb: Int = 0xFFB4C5FF.toInt(),
    val wallpaperUri: String? = null,
    val favoritePackages: Set<String> = emptySet(),
    val hiddenPackages: Set<String> = emptySet(),
    val usageCounts: Map<String, Int> = emptyMap(),
    val widgetAppWidgetId: Int = -1,
    val replaceClockWithWidget: Boolean = false,
    val fillScreenWidget: Boolean = false,
    /** First-run favorites picker has been completed (or explicitly skipped). */
    val favoritesOnboarded: Boolean = false,
    /** Names of [`in`.ankitsaroj.diable.ui.home.HomePromo] rows the user has dismissed. */
    val dismissedPromos: Set<String> = emptySet(),
    /** Calendar sub-sheet: show the date line under the clock. */
    val showDate: Boolean = true,
    // Theme editor -> Global settings.
    val hideStatusBar: Boolean = false,
    val hideAlphabet: Boolean = false,
    val hideFavoriteNames: Boolean = false,
    val dimWallpaper: Boolean = true,
    val pitchBlackTheme: Boolean = false,
    val darkThemeMode: DarkThemeMode = DarkThemeMode.DeviceThemeAndWallpaper,
    // Theme identity: typeface, icon treatment, and which preset (if any) is applied.
    val fontKey: String = "quicksand",
    val iconStyleKey: String = "rounded",
    /** Installed third-party icon pack, or null for the built-in styles only. */
    val iconPackPackage: String? = null,
    val appliedPresetId: String? = null,
    /** Attribution for the current web wallpaper, shown in Themes. */
    val wallpaperCredit: String? = null,
    /**
     * Brightness (0..1) of the wallpaper strip behind the status bar, or -1 when unknown
     * (e.g. the wallpaper was set outside this app). Drives system-bar icon colour.
     */
    val wallpaperTopLuminance: Float = -1f,
    /** Apps surfaced on the home screen when audio output connects. */
    val musicAppPackages: Set<String> = emptySet(),
    /** User renames from the app long-press menu, keyed by package. */
    val appLabels: Map<String, String> = emptyMap(),
    /** Per-app icon override: the icon-pack package to take this app's icon from. */
    val appIconPacks: Map<String, String> = emptyMap(),
    /** Pop-up folders, which sit in favorites as `folder:<id>` entries. */
    val folders: List<PopupFolder> = emptyList(),
    /** Extra apps shown in an app's swipe-right pop-up, keyed by the owning package. */
    val popupApps: Map<String, List<String>> = emptyMap(),
    /** Diable button actions ([ButtonAction] keys); null = not set up yet. */
    val buttonTapAction: String? = null,
    val buttonSwipeAction: String? = null,
    /** The app most recently opened from search, shown as "Last result". */
    val lastSearchPackage: String? = null,
    /** Web suggestion provider key, or "none". */
    val webSearchEngine: String = "none",
    /** Theme editor: font size multiplier (0.8–1.4) and dark-text mode for light wallpapers. */
    val fontScale: Float = 1f,
    val darkText: Boolean = false,
    /** Usage Breaker: leisure apps that trigger a break reminder, and its interval. */
    val leisurePackages: Set<String> = emptySet(),
    val usageBreakerMinutes: Int = 15,
    /** "Move Widget": distance from the status bar to the clock/widget block, in dp. */
    val clockTopDp: Int = 125,
    /** "Move and resize": custom widget height in dp; 0 = the widget's own size. */
    val widgetHeightDp: Int = 0,
    /** Weather units; false = Fahrenheit. */
    val weatherCelsius: Boolean = false,
    /** Chosen weather city; null name = use the device's location. */
    val weatherCityName: String? = null,
    val weatherLat: Float = 0f,
    val weatherLon: Float = 0f,
    /** Pop-up widgets: an app's widget shown in its swipe-right pop-up, by package. */
    val popupWidgets: Map<String, Int> = emptyMap(),
    /** Widget stack: extra home widgets swiped through after [widgetAppWidgetId]. */
    val widgetStackIds: List<Int> = emptyList(),
    /** Calendar → Filter events: calendar ids whose events are left out. */
    val hiddenCalendarIds: Set<String> = emptySet(),
)

/** A Diable pop-up folder: a named group of apps that opens as a card from favorites. */
data class PopupFolder(
    val id: String,
    val name: String,
    val packages: List<String>,
) {
    val favoriteKey: String get() = FOLDER_PREFIX + id

    companion object {
        const val FOLDER_PREFIX = "folder:"
    }
}

class PrefsRepository(private val context: Context) {

    /** Single source of truth for defaults — read paths must never restate them. */
    private val defaults = DiablePrefs()

    val prefsFlow: Flow<DiablePrefs> = context.dataStore.data.map { p -> p.toDiablePrefs() }

    /**
     * Fire-and-forget [update] that outlives the caller. Menus and sheets close in the same
     * tap that saves; launching in their composition scope cancelled the write mid-flight.
     */
    fun launchUpdate(transform: (DiablePrefs) -> DiablePrefs) {
        writeScope.launch { update(transform) }
    }

    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun update(transform: (DiablePrefs) -> DiablePrefs) {
        context.dataStore.edit { prefs ->
            val current = prefs.toDiablePrefs()
            val updated = transform(current)
            prefs[Keys.WEB_SEARCH_SUGGESTIONS] = updated.webSearchSuggestions
            prefs[Keys.CONTACT_SEARCH] = updated.contactSearch
            prefs[Keys.SMART_SUGGESTIONS] = updated.smartSuggestions
            prefs[Keys.SWIPE_UP_TO_SEARCH] = updated.swipeUpToSearch
            prefs[Keys.CALCULATOR_IN_SEARCH] = updated.calculatorInSearch
            prefs[Keys.WEATHER_ENABLED] = updated.weatherEnabled
            prefs[Keys.SHOW_MEDIA_WIDGET] = updated.showMediaWidget
            prefs[Keys.SHOW_BATTERY_PERCENTAGE] = updated.showBatteryPercentage
            prefs[Keys.CALENDAR_PREVIEW] = updated.calendarPreview
            prefs[Keys.QUICK_REPLIES] = updated.quickReplies
            prefs[Keys.WEATHER_FORECAST] = updated.weatherForecast
            prefs[Keys.MEDIA_PLAYER] = updated.mediaPlayer
            prefs[Keys.USAGE_BREAKER] = updated.usageBreaker
            prefs[Keys.NOTIFICATION_SUMMARY] = updated.notificationSummary
            prefs[Keys.SUGGEST_OFTEN_USED] = updated.suggestOftenUsed
            prefs[Keys.SORT_BY_USAGE] = updated.sortByUsage
            prefs[Keys.DIABLE_BUTTON] = updated.diableButton
            prefs[Keys.QUICK_LOCK] = updated.quickLock
            prefs[Keys.ALLOW_ROTATION] = updated.allowRotation
            prefs[Keys.ALLOW_HAPTIC] = updated.allowHaptic
            prefs[Keys.SHOW_UPCOMING_EVENTS] = updated.showUpcomingEvents
            prefs[Keys.FAVORITES_ONBOARDED] = updated.favoritesOnboarded
            prefs[Keys.DISMISSED_PROMOS] = updated.dismissedPromos
            prefs[Keys.SHOW_DATE] = updated.showDate
            prefs[Keys.HIDE_STATUS_BAR] = updated.hideStatusBar
            prefs[Keys.HIDE_ALPHABET] = updated.hideAlphabet
            prefs[Keys.HIDE_FAVORITE_NAMES] = updated.hideFavoriteNames
            prefs[Keys.DIM_WALLPAPER] = updated.dimWallpaper
            prefs[Keys.PITCH_BLACK_THEME] = updated.pitchBlackTheme
            prefs[Keys.DARK_THEME_MODE] = updated.darkThemeMode.name
            prefs[Keys.FONT_KEY] = updated.fontKey
            prefs[Keys.ICON_STYLE_KEY] = updated.iconStyleKey
            updated.iconPackPackage?.let { prefs[Keys.ICON_PACK_PACKAGE] = it }
                ?: prefs.remove(Keys.ICON_PACK_PACKAGE)
            updated.appliedPresetId?.let { prefs[Keys.APPLIED_PRESET_ID] = it }
                ?: prefs.remove(Keys.APPLIED_PRESET_ID)
            updated.wallpaperCredit?.let { prefs[Keys.WALLPAPER_CREDIT] = it }
                ?: prefs.remove(Keys.WALLPAPER_CREDIT)
            prefs[Keys.WALLPAPER_TOP_LUMINANCE] = updated.wallpaperTopLuminance
            prefs[Keys.MUSIC_APP_PACKAGES] = updated.musicAppPackages
            prefs[Keys.CLOCK_STYLE_ID] = updated.clockStyleId
            prefs[Keys.FAVORITE_ORDER_JSON] = JSONArray(updated.favoritePackages.toList()).toString()
            prefs[Keys.APP_LABELS_JSON] = encodeStringMap(updated.appLabels)
            prefs[Keys.APP_ICON_PACKS_JSON] = encodeStringMap(updated.appIconPacks)
            prefs[Keys.FOLDERS_JSON] = encodeFolders(updated.folders)
            prefs[Keys.POPUP_APPS_JSON] = JSONObject().also { o ->
                updated.popupApps.forEach { (k, v) -> o.put(k, JSONArray(v)) }
            }.toString()
            updated.buttonTapAction?.let { prefs[Keys.BUTTON_TAP_ACTION] = it }
                ?: prefs.remove(Keys.BUTTON_TAP_ACTION)
            updated.buttonSwipeAction?.let { prefs[Keys.BUTTON_SWIPE_ACTION] = it }
                ?: prefs.remove(Keys.BUTTON_SWIPE_ACTION)
            updated.lastSearchPackage?.let { prefs[Keys.LAST_SEARCH_PACKAGE] = it }
                ?: prefs.remove(Keys.LAST_SEARCH_PACKAGE)
            prefs[Keys.WEB_SEARCH_ENGINE] = updated.webSearchEngine
            prefs[Keys.FONT_SCALE] = updated.fontScale
            prefs[Keys.DARK_TEXT] = updated.darkText
            prefs[Keys.LEISURE_PACKAGES] = updated.leisurePackages
            prefs[Keys.USAGE_BREAKER_MINUTES] = updated.usageBreakerMinutes
            prefs[Keys.CLOCK_TOP_DP] = updated.clockTopDp
            prefs[Keys.WIDGET_HEIGHT_DP] = updated.widgetHeightDp
            prefs[Keys.WEATHER_CELSIUS] = updated.weatherCelsius
            updated.weatherCityName?.let { prefs[Keys.WEATHER_CITY_NAME] = it }
                ?: prefs.remove(Keys.WEATHER_CITY_NAME)
            prefs[Keys.WEATHER_LAT] = updated.weatherLat
            prefs[Keys.WEATHER_LON] = updated.weatherLon
            prefs[Keys.POPUP_WIDGETS_JSON] = JSONObject().also { o ->
                updated.popupWidgets.forEach { (k, v) -> o.put(k, v) }
            }.toString()
            prefs[Keys.WIDGET_STACK_JSON] = JSONArray(updated.widgetStackIds).toString()
            prefs[Keys.HIDDEN_CALENDAR_IDS] = updated.hiddenCalendarIds
            prefs[Keys.ACCENT_COLOR_ARGB] = updated.accentColorArgb
            if (updated.wallpaperUri != null) {
                prefs[Keys.WALLPAPER_URI] = updated.wallpaperUri
            } else {
                prefs.remove(Keys.WALLPAPER_URI)
            }
            prefs[Keys.FAVORITE_PACKAGES] = updated.favoritePackages
            prefs[Keys.HIDDEN_PACKAGES] = updated.hiddenPackages
            prefs[Keys.USAGE_COUNTS_JSON] = encodeUsageCounts(updated.usageCounts)
            prefs[Keys.WIDGET_APP_WIDGET_ID] = updated.widgetAppWidgetId
            prefs[Keys.REPLACE_CLOCK_WITH_WIDGET] = updated.replaceClockWithWidget
            prefs[Keys.FILL_SCREEN_WIDGET] = updated.fillScreenWidget
        }
    }

    private object Keys {
        val WEB_SEARCH_SUGGESTIONS = booleanPreferencesKey("web_search_suggestions")
        val CONTACT_SEARCH = booleanPreferencesKey("contact_search")
        val SMART_SUGGESTIONS = booleanPreferencesKey("smart_suggestions")
        val SWIPE_UP_TO_SEARCH = booleanPreferencesKey("swipe_up_to_search")
        val CALCULATOR_IN_SEARCH = booleanPreferencesKey("calculator_in_search")
        val WEATHER_ENABLED = booleanPreferencesKey("weather_enabled")
        val SHOW_MEDIA_WIDGET = booleanPreferencesKey("show_media_widget")
        val SHOW_BATTERY_PERCENTAGE = booleanPreferencesKey("show_battery_percentage")
        val CALENDAR_PREVIEW = booleanPreferencesKey("calendar_preview")
        val QUICK_REPLIES = booleanPreferencesKey("quick_replies")
        val WEATHER_FORECAST = booleanPreferencesKey("weather_forecast")
        val MEDIA_PLAYER = booleanPreferencesKey("media_player")
        val USAGE_BREAKER = booleanPreferencesKey("usage_breaker")
        val NOTIFICATION_SUMMARY = booleanPreferencesKey("notification_summary")
        val SUGGEST_OFTEN_USED = booleanPreferencesKey("suggest_often_used")
        val SORT_BY_USAGE = booleanPreferencesKey("sort_by_usage")
        val DIABLE_BUTTON = booleanPreferencesKey("diable_button")
        val QUICK_LOCK = booleanPreferencesKey("quick_lock")
        val ALLOW_ROTATION = booleanPreferencesKey("allow_rotation")
        val ALLOW_HAPTIC = booleanPreferencesKey("allow_haptic")
        val SHOW_UPCOMING_EVENTS = booleanPreferencesKey("show_upcoming_events")
        val FAVORITES_ONBOARDED = booleanPreferencesKey("favorites_onboarded")
        val DISMISSED_PROMOS = stringSetPreferencesKey("dismissed_promos")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val HIDE_STATUS_BAR = booleanPreferencesKey("hide_status_bar")
        val HIDE_ALPHABET = booleanPreferencesKey("hide_alphabet")
        val HIDE_FAVORITE_NAMES = booleanPreferencesKey("hide_favorite_names")
        val DIM_WALLPAPER = booleanPreferencesKey("dim_wallpaper")
        val PITCH_BLACK_THEME = booleanPreferencesKey("pitch_black_theme")
        val DARK_THEME_MODE = stringPreferencesKey("dark_theme_mode")
        val FONT_KEY = stringPreferencesKey("font_key")
        val ICON_STYLE_KEY = stringPreferencesKey("icon_style_key")
        val ICON_PACK_PACKAGE = stringPreferencesKey("icon_pack_package")
        val APPLIED_PRESET_ID = stringPreferencesKey("applied_preset_id")
        val WALLPAPER_CREDIT = stringPreferencesKey("wallpaper_credit")
        val WALLPAPER_TOP_LUMINANCE = floatPreferencesKey("wallpaper_top_luminance")
        val MUSIC_APP_PACKAGES = stringSetPreferencesKey("music_app_packages")
        val CLOCK_STYLE_ID = intPreferencesKey("clock_style_id")
        val FAVORITE_ORDER_JSON = stringPreferencesKey("favorite_order_json")
        val APP_LABELS_JSON = stringPreferencesKey("app_labels_json")
        val APP_ICON_PACKS_JSON = stringPreferencesKey("app_icon_packs_json")
        val FOLDERS_JSON = stringPreferencesKey("folders_json")
        val POPUP_APPS_JSON = stringPreferencesKey("popup_apps_json")
        val BUTTON_TAP_ACTION = stringPreferencesKey("button_tap_action")
        val BUTTON_SWIPE_ACTION = stringPreferencesKey("button_swipe_action")
        val LAST_SEARCH_PACKAGE = stringPreferencesKey("last_search_package")
        val WEB_SEARCH_ENGINE = stringPreferencesKey("web_search_engine")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val DARK_TEXT = booleanPreferencesKey("dark_text")
        val LEISURE_PACKAGES = stringSetPreferencesKey("leisure_packages")
        val USAGE_BREAKER_MINUTES = intPreferencesKey("usage_breaker_minutes")
        val CLOCK_TOP_DP = intPreferencesKey("clock_top_dp")
        val WIDGET_HEIGHT_DP = intPreferencesKey("widget_height_dp")
        val WEATHER_CELSIUS = booleanPreferencesKey("weather_celsius")
        val WEATHER_CITY_NAME = stringPreferencesKey("weather_city_name")
        val WEATHER_LAT = floatPreferencesKey("weather_lat")
        val WEATHER_LON = floatPreferencesKey("weather_lon")
        val POPUP_WIDGETS_JSON = stringPreferencesKey("popup_widgets_json")
        val WIDGET_STACK_JSON = stringPreferencesKey("widget_stack_json")
        val HIDDEN_CALENDAR_IDS = stringSetPreferencesKey("hidden_calendar_ids")
        val ACCENT_COLOR_ARGB = intPreferencesKey("accent_color_argb")
        val WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        val FAVORITE_PACKAGES = stringSetPreferencesKey("favorite_packages")
        val HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages")
        val USAGE_COUNTS_JSON = stringPreferencesKey("usage_counts_json")
        val WIDGET_APP_WIDGET_ID = intPreferencesKey("widget_app_widget_id")
        val REPLACE_CLOCK_WITH_WIDGET = booleanPreferencesKey("replace_clock_with_widget")
        val FILL_SCREEN_WIDGET = booleanPreferencesKey("fill_screen_widget")
    }

    private fun Preferences.toDiablePrefs(): DiablePrefs = defaults.let { d ->
        DiablePrefs(
            webSearchSuggestions = this[Keys.WEB_SEARCH_SUGGESTIONS] ?: d.webSearchSuggestions,
            contactSearch = this[Keys.CONTACT_SEARCH] ?: d.contactSearch,
            smartSuggestions = this[Keys.SMART_SUGGESTIONS] ?: d.smartSuggestions,
            swipeUpToSearch = this[Keys.SWIPE_UP_TO_SEARCH] ?: d.swipeUpToSearch,
            calculatorInSearch = this[Keys.CALCULATOR_IN_SEARCH] ?: d.calculatorInSearch,
            weatherEnabled = this[Keys.WEATHER_ENABLED] ?: d.weatherEnabled,
            showMediaWidget = this[Keys.SHOW_MEDIA_WIDGET] ?: d.showMediaWidget,
            showBatteryPercentage = this[Keys.SHOW_BATTERY_PERCENTAGE] ?: d.showBatteryPercentage,
            calendarPreview = this[Keys.CALENDAR_PREVIEW] ?: d.calendarPreview,
            quickReplies = this[Keys.QUICK_REPLIES] ?: d.quickReplies,
            weatherForecast = this[Keys.WEATHER_FORECAST] ?: d.weatherForecast,
            mediaPlayer = this[Keys.MEDIA_PLAYER] ?: d.mediaPlayer,
            usageBreaker = this[Keys.USAGE_BREAKER] ?: d.usageBreaker,
            notificationSummary = this[Keys.NOTIFICATION_SUMMARY] ?: d.notificationSummary,
            suggestOftenUsed = this[Keys.SUGGEST_OFTEN_USED] ?: d.suggestOftenUsed,
            sortByUsage = this[Keys.SORT_BY_USAGE] ?: d.sortByUsage,
            diableButton = this[Keys.DIABLE_BUTTON] ?: d.diableButton,
            quickLock = this[Keys.QUICK_LOCK] ?: d.quickLock,
            allowRotation = this[Keys.ALLOW_ROTATION] ?: d.allowRotation,
            allowHaptic = this[Keys.ALLOW_HAPTIC] ?: d.allowHaptic,
            showUpcomingEvents = this[Keys.SHOW_UPCOMING_EVENTS] ?: d.showUpcomingEvents,
            favoritesOnboarded = this[Keys.FAVORITES_ONBOARDED] ?: d.favoritesOnboarded,
            dismissedPromos = this[Keys.DISMISSED_PROMOS] ?: d.dismissedPromos,
            showDate = this[Keys.SHOW_DATE] ?: d.showDate,
            hideStatusBar = this[Keys.HIDE_STATUS_BAR] ?: d.hideStatusBar,
            hideAlphabet = this[Keys.HIDE_ALPHABET] ?: d.hideAlphabet,
            hideFavoriteNames = this[Keys.HIDE_FAVORITE_NAMES] ?: d.hideFavoriteNames,
            dimWallpaper = this[Keys.DIM_WALLPAPER] ?: d.dimWallpaper,
            pitchBlackTheme = this[Keys.PITCH_BLACK_THEME] ?: d.pitchBlackTheme,
            darkThemeMode = this[Keys.DARK_THEME_MODE]
                ?.let { runCatching { DarkThemeMode.valueOf(it) }.getOrNull() }
                ?: d.darkThemeMode,
            fontKey = this[Keys.FONT_KEY] ?: d.fontKey,
            iconStyleKey = this[Keys.ICON_STYLE_KEY] ?: d.iconStyleKey,
            iconPackPackage = this[Keys.ICON_PACK_PACKAGE],
            appliedPresetId = this[Keys.APPLIED_PRESET_ID],
            wallpaperCredit = this[Keys.WALLPAPER_CREDIT],
            wallpaperTopLuminance = this[Keys.WALLPAPER_TOP_LUMINANCE] ?: d.wallpaperTopLuminance,
            musicAppPackages = this[Keys.MUSIC_APP_PACKAGES] ?: d.musicAppPackages,
            clockStyleId = this[Keys.CLOCK_STYLE_ID] ?: d.clockStyleId,
            accentColorArgb = this[Keys.ACCENT_COLOR_ARGB] ?: d.accentColorArgb,
            wallpaperUri = this[Keys.WALLPAPER_URI],
            favoritePackages = orderedFavorites(
                this[Keys.FAVORITE_PACKAGES] ?: d.favoritePackages,
                this[Keys.FAVORITE_ORDER_JSON],
            ),
            hiddenPackages = this[Keys.HIDDEN_PACKAGES] ?: d.hiddenPackages,
        usageCounts = decodeUsageCounts(this[Keys.USAGE_COUNTS_JSON]),
            widgetAppWidgetId = this[Keys.WIDGET_APP_WIDGET_ID] ?: d.widgetAppWidgetId,
            replaceClockWithWidget = this[Keys.REPLACE_CLOCK_WITH_WIDGET] ?: d.replaceClockWithWidget,
            fillScreenWidget = this[Keys.FILL_SCREEN_WIDGET] ?: d.fillScreenWidget,
            appLabels = decodeStringMap(this[Keys.APP_LABELS_JSON]),
            appIconPacks = decodeStringMap(this[Keys.APP_ICON_PACKS_JSON]),
            folders = decodeFolders(this[Keys.FOLDERS_JSON]),
            popupApps = decodePopupApps(this[Keys.POPUP_APPS_JSON]),
            buttonTapAction = this[Keys.BUTTON_TAP_ACTION],
            buttonSwipeAction = this[Keys.BUTTON_SWIPE_ACTION],
            lastSearchPackage = this[Keys.LAST_SEARCH_PACKAGE],
            webSearchEngine = this[Keys.WEB_SEARCH_ENGINE] ?: d.webSearchEngine,
            fontScale = this[Keys.FONT_SCALE] ?: d.fontScale,
            darkText = this[Keys.DARK_TEXT] ?: d.darkText,
            leisurePackages = this[Keys.LEISURE_PACKAGES] ?: d.leisurePackages,
            usageBreakerMinutes = this[Keys.USAGE_BREAKER_MINUTES] ?: d.usageBreakerMinutes,
            clockTopDp = this[Keys.CLOCK_TOP_DP] ?: d.clockTopDp,
            widgetHeightDp = this[Keys.WIDGET_HEIGHT_DP] ?: d.widgetHeightDp,
            weatherCelsius = this[Keys.WEATHER_CELSIUS] ?: d.weatherCelsius,
            weatherCityName = this[Keys.WEATHER_CITY_NAME],
            weatherLat = this[Keys.WEATHER_LAT] ?: d.weatherLat,
            weatherLon = this[Keys.WEATHER_LON] ?: d.weatherLon,
            popupWidgets = runCatching {
                val o = JSONObject(this[Keys.POPUP_WIDGETS_JSON] ?: "{}")
                buildMap { o.keys().forEach { put(it, o.getInt(it)) } }
            }.getOrDefault(emptyMap()),
            hiddenCalendarIds = this[Keys.HIDDEN_CALENDAR_IDS] ?: d.hiddenCalendarIds,
            widgetStackIds = runCatching {
                val a = JSONArray(this[Keys.WIDGET_STACK_JSON] ?: "[]")
                List(a.length()) { a.getInt(it) }
            }.getOrDefault(emptyList()),
        )
    }

    /** DataStore string sets are unordered; the JSON list carries the user's order. */
    private fun orderedFavorites(members: Set<String>, orderJson: String?): Set<String> {
        val order = runCatching {
            val a = JSONArray(orderJson ?: return members)
            List(a.length()) { a.getString(it) }
        }.getOrDefault(emptyList())
        return LinkedHashSet<String>().apply {
            order.filterTo(this) { it in members }
            addAll(members)
        }
    }

    companion object {
        fun encodeStringMap(map: Map<String, String>): String =
            JSONObject().also { o -> map.forEach { (k, v) -> o.put(k, v) } }.toString()

        fun decodeStringMap(json: String?): Map<String, String> = runCatching {
            val o = JSONObject(json ?: return emptyMap())
            buildMap { o.keys().forEach { put(it, o.getString(it)) } }
        }.getOrDefault(emptyMap())

        fun encodeFolders(folders: List<PopupFolder>): String = JSONArray().also { a ->
            folders.forEach { f ->
                a.put(
                    JSONObject()
                        .put("id", f.id)
                        .put("name", f.name)
                        .put("packages", JSONArray(f.packages)),
                )
            }
        }.toString()

        fun decodeFolders(json: String?): List<PopupFolder> = runCatching {
            val a = JSONArray(json ?: return emptyList())
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                val pk = o.optJSONArray("packages") ?: JSONArray()
                PopupFolder(
                    id = o.getString("id"),
                    name = o.optString("name", "Folder"),
                    packages = List(pk.length()) { pk.getString(it) },
                )
            }
        }.getOrDefault(emptyList())

        private fun decodePopupApps(json: String?): Map<String, List<String>> = runCatching {
            val o = JSONObject(json ?: return emptyMap())
            buildMap {
                o.keys().forEach { k ->
                    val a = o.getJSONArray(k)
                    put(k, List(a.length()) { a.getString(it) })
                }
            }
        }.getOrDefault(emptyMap())

        fun encodeUsageCounts(map: Map<String, Int>): String {
            val json = JSONObject()
            map.forEach { (k, v) -> json.put(k, v) }
            return json.toString()
        }

        fun diablePrefsToJson(prefs: DiablePrefs): String {
            val o = JSONObject()
            o.put("webSearchSuggestions", prefs.webSearchSuggestions)
            o.put("contactSearch", prefs.contactSearch)
            o.put("smartSuggestions", prefs.smartSuggestions)
            o.put("swipeUpToSearch", prefs.swipeUpToSearch)
            o.put("calculatorInSearch", prefs.calculatorInSearch)
            o.put("weatherEnabled", prefs.weatherEnabled)
            o.put("showMediaWidget", prefs.showMediaWidget)
            o.put("showBatteryPercentage", prefs.showBatteryPercentage)
            o.put("calendarPreview", prefs.calendarPreview)
            o.put("quickReplies", prefs.quickReplies)
            o.put("weatherForecast", prefs.weatherForecast)
            o.put("mediaPlayer", prefs.mediaPlayer)
            o.put("usageBreaker", prefs.usageBreaker)
            o.put("notificationSummary", prefs.notificationSummary)
            o.put("suggestOftenUsed", prefs.suggestOftenUsed)
            o.put("sortByUsage", prefs.sortByUsage)
            o.put("diableButton", prefs.diableButton)
            o.put("quickLock", prefs.quickLock)
            o.put("allowRotation", prefs.allowRotation)
            o.put("allowHaptic", prefs.allowHaptic)
            o.put("showUpcomingEvents", prefs.showUpcomingEvents)
            o.put("clockStyleId", prefs.clockStyleId)
            o.put("accentColorArgb", prefs.accentColorArgb)
            o.put("wallpaperUri", prefs.wallpaperUri)
            o.put("favoritePackages", JSONObject(prefs.favoritePackages.associateWith { true }))
            o.put("hiddenPackages", JSONObject(prefs.hiddenPackages.associateWith { true }))
            o.put("usageCounts", JSONObject(prefs.usageCounts.mapValues { it.value }))
            o.put("widgetAppWidgetId", prefs.widgetAppWidgetId)
            o.put("replaceClockWithWidget", prefs.replaceClockWithWidget)
            o.put("fillScreenWidget", prefs.fillScreenWidget)
            o.put("iconStyleKey", prefs.iconStyleKey)
            o.put("iconPackPackage", prefs.iconPackPackage)
            o.put("favoriteOrder", JSONArray(prefs.favoritePackages.toList()))
            o.put("appLabels", encodeStringMap(prefs.appLabels))
            o.put("folders", encodeFolders(prefs.folders))
            o.put("buttonTapAction", prefs.buttonTapAction)
            o.put("buttonSwipeAction", prefs.buttonSwipeAction)
            return o.toString()
        }

        fun diablePrefsFromJson(json: String, fallback: DiablePrefs = DiablePrefs()): DiablePrefs {
            val o = JSONObject(json)
            return DiablePrefs(
                webSearchSuggestions = o.optBoolean("webSearchSuggestions", fallback.webSearchSuggestions),
                contactSearch = o.optBoolean("contactSearch", fallback.contactSearch),
                smartSuggestions = o.optBoolean("smartSuggestions", fallback.smartSuggestions),
                swipeUpToSearch = o.optBoolean("swipeUpToSearch", fallback.swipeUpToSearch),
                calculatorInSearch = o.optBoolean("calculatorInSearch", fallback.calculatorInSearch),
                weatherEnabled = o.optBoolean("weatherEnabled", fallback.weatherEnabled),
                showMediaWidget = o.optBoolean("showMediaWidget", fallback.showMediaWidget),
                showBatteryPercentage = o.optBoolean("showBatteryPercentage", fallback.showBatteryPercentage),
                calendarPreview = o.optBoolean("calendarPreview", fallback.calendarPreview),
                quickReplies = o.optBoolean("quickReplies", fallback.quickReplies),
                weatherForecast = o.optBoolean("weatherForecast", fallback.weatherForecast),
                mediaPlayer = o.optBoolean("mediaPlayer", fallback.mediaPlayer),
                usageBreaker = o.optBoolean("usageBreaker", fallback.usageBreaker),
                notificationSummary = o.optBoolean("notificationSummary", fallback.notificationSummary),
                suggestOftenUsed = o.optBoolean("suggestOftenUsed", fallback.suggestOftenUsed),
                sortByUsage = o.optBoolean("sortByUsage", fallback.sortByUsage),
                diableButton = o.optBoolean("diableButton", fallback.diableButton),
                quickLock = o.optBoolean("quickLock", fallback.quickLock),
                allowRotation = o.optBoolean("allowRotation", fallback.allowRotation),
                allowHaptic = o.optBoolean("allowHaptic", fallback.allowHaptic),
                showUpcomingEvents = o.optBoolean("showUpcomingEvents", fallback.showUpcomingEvents),
                clockStyleId = o.optInt("clockStyleId", fallback.clockStyleId),
                accentColorArgb = o.optInt("accentColorArgb", fallback.accentColorArgb),
                wallpaperUri = o.optString("wallpaperUri").takeIf { it.isNotBlank() && it != "null" },
                favoritePackages = jsonStringSet(o.optJSONObject("favoritePackages")),
                hiddenPackages = jsonStringSet(o.optJSONObject("hiddenPackages")),
                usageCounts = o.optJSONObject("usageCounts")?.let { u ->
                    buildMap {
                        u.keys().forEach { k -> put(k, u.optInt(k)) }
                    }
                } ?: fallback.usageCounts,
                widgetAppWidgetId = o.optInt("widgetAppWidgetId", fallback.widgetAppWidgetId),
                replaceClockWithWidget = o.optBoolean("replaceClockWithWidget", fallback.replaceClockWithWidget),
                fillScreenWidget = o.optBoolean("fillScreenWidget", fallback.fillScreenWidget),
                iconStyleKey = o.optString("iconStyleKey", fallback.iconStyleKey),
                iconPackPackage = o.optString("iconPackPackage").takeIf { it.isNotBlank() && it != "null" },
                appLabels = decodeStringMap(o.optString("appLabels").takeIf { it.isNotBlank() }),
                folders = decodeFolders(o.optString("folders").takeIf { it.isNotBlank() }),
                buttonTapAction = o.optString("buttonTapAction").takeIf { it.isNotBlank() && it != "null" },
                buttonSwipeAction = o.optString("buttonSwipeAction").takeIf { it.isNotBlank() && it != "null" },
            ).let { restored ->
                // Restore the saved favorites order, not the JSON object's key order.
                val order = o.optJSONArray("favoriteOrder") ?: return@let restored
                val ordered = LinkedHashSet<String>()
                for (i in 0 until order.length()) {
                    order.optString(i).takeIf { it in restored.favoritePackages }?.let(ordered::add)
                }
                restored.copy(favoritePackages = ordered + restored.favoritePackages)
            }
        }

        private fun jsonStringSet(obj: JSONObject?): Set<String> {
            if (obj == null) return emptySet()
            return buildSet {
                obj.keys().forEach { add(it) }
            }
        }

        fun decodeUsageCounts(json: String?): Map<String, Int> {
            if (json.isNullOrBlank()) return emptyMap()
            return try {
                val obj = JSONObject(json)
                buildMap {
                    obj.keys().forEach { key ->
                        put(key, obj.getInt(key))
                    }
                }
            } catch (_: Exception) {
                emptyMap()
            }
        }
    }
}
