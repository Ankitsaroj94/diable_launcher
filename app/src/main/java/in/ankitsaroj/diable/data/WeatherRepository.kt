package `in`.ankitsaroj.diable.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A place from the city search, as the weather sheet lists it. */
data class CityResult(
    val name: String,
    val admin1: String?,
    val country: String?,
    val lat: Double,
    val lon: Double,
) {
    /** "Pune, Maharashtra, India" — skipping parts the API left out. */
    val label: String get() = listOfNotNull(name, admin1, country).distinct().joinToString(", ")
}

/** Current temperature and city search, both from Open-Meteo (keyless). */
class WeatherRepository {

    /**
     * The temperature where the user wants it: the city they picked, otherwise the
     * device's last known location. Null when neither is available — never a stand-in city.
     */
    suspend fun currentTemperatureF(context: Context, prefs: DiablePrefs): Float? {
        val (lat, lon) = if (prefs.weatherCityName != null) {
            prefs.weatherLat.toDouble() to prefs.weatherLon.toDouble()
        } else {
            val here = lastKnownLocation(context) ?: return null
            here.latitude to here.longitude
        }
        return fetchTemperatureF(lat, lon)
    }

    @Deprecated(
        "Always fetched New York. Use currentTemperatureF(context, prefs).",
        ReplaceWith("currentTemperatureF(context, prefs)"),
    )
    suspend fun getCurrentTemperatureF(): Float? = null

    suspend fun searchCities(query: String): List<CityResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            val json = JSONObject(
                get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=en&format=json"),
            )
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            List(results.length()) { i ->
                val o = results.getJSONObject(i)
                CityResult(
                    name = o.optString("name"),
                    admin1 = o.optString("admin1").takeIf { it.isNotBlank() },
                    country = o.optString("country").takeIf { it.isNotBlank() },
                    lat = o.getDouble("latitude"),
                    lon = o.getDouble("longitude"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchTemperatureF(latitude: Double, longitude: Double): Float? =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject(
                    get(
                        "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=$latitude&longitude=$longitude" +
                            "&current=temperature_2m&temperature_unit=fahrenheit",
                    ),
                )
                val current = json.optJSONObject("current") ?: return@withContext null
                if (!current.has("temperature_2m")) return@withContext null
                current.getDouble("temperature_2m").toFloat()
            } catch (_: Exception) {
                null
            }
        }

    /** Freshest cached fix from any provider; no Play Services, no active GPS request. */
    private fun lastKnownLocation(context: Context): Location? {
        val granted = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!granted) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER,
        ).mapNotNull { provider ->
            try {
                @Suppress("MissingPermission")
                manager.getLastKnownLocation(provider)
            } catch (_: Exception) {
                null
            }
        }.maxByOrNull { it.time }
    }

    private fun get(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
