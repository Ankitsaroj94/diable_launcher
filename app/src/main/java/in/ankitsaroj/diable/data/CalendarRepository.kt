package `in`.ankitsaroj.diable.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class CalendarEventPreview(
    val title: String,
    val daysUntil: Int,
)

/** One entry in the agenda list. */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val location: String?,
    val calendarColor: Int,
)

/** A calendar account the user can include or exclude in Filter events. */
data class CalendarSource(val id: String, val name: String, val account: String, val color: Int)

class CalendarRepository(private val context: Context) {

    /** Every calendar visible on the device, for Filter events. */
    suspend fun listCalendars(): List<CalendarSource> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.ACCOUNT_NAME,
                    CalendarContract.Calendars.CALENDAR_COLOR,
                ),
                null,
                null,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            )?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(CalendarSource(c.getLong(0).toString(), c.getString(1) ?: "Calendar", c.getString(2) ?: "", c.getInt(3)))
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    /** SQL excluding [hidden] calendars; ids are numeric, so inlining them is safe. */
    private fun calendarFilter(hidden: Set<String>): String? {
        val ids = hidden.mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return null
        return "${CalendarContract.Instances.CALENDAR_ID} NOT IN (${ids.joinToString(",")})"
    }

    fun hasPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Upcoming events over the next [days], ordered soonest first. */
    suspend fun getUpcomingEvents(days: Int = 30, hidden: Set<String> = emptySet()): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext emptyList()
            try {
                val now = System.currentTimeMillis()
                val end = now + TimeUnit.DAYS.toMillis(days.toLong())
                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                ContentUris.appendId(builder, now)
                ContentUris.appendId(builder, end)
                val projection = arrayOf(
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.ALL_DAY,
                    CalendarContract.Instances.EVENT_LOCATION,
                    CalendarContract.Instances.CALENDAR_COLOR,
                )
                context.contentResolver.query(
                    builder.build(),
                    projection,
                    calendarFilter(hidden),
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC",
                )?.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(
                                CalendarEvent(
                                    id = cursor.getLong(0),
                                    title = cursor.getString(1)?.takeIf { t -> t.isNotBlank() }
                                        ?: "(No title)",
                                    beginMillis = cursor.getLong(2),
                                    endMillis = cursor.getLong(3),
                                    allDay = cursor.getInt(4) == 1,
                                    location = cursor.getString(5)?.takeIf { l -> l.isNotBlank() },
                                    calendarColor = cursor.getInt(6),
                                ),
                            )
                        }
                    }
                } ?: emptyList()
            } catch (_: SecurityException) {
                emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }

    suspend fun getNextUpcomingEvent(hidden: Set<String> = emptySet()): CalendarEventPreview? = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val end = now + TimeUnit.DAYS.toMillis(365)
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, now)
            ContentUris.appendId(builder, end)
            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
            )
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                calendarFilter(hidden),
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            ) ?: return@withContext null

            cursor.use {
                if (!it.moveToFirst()) return@withContext null
                val title = it.getString(0)?.takeIf { t -> t.isNotBlank() } ?: "Event"
                val begin = it.getLong(1)
                val days = ((begin - now) / TimeUnit.DAYS.toMillis(1)).toInt().coerceAtLeast(0)
                CalendarEventPreview(title = title, daysUntil = days)
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
