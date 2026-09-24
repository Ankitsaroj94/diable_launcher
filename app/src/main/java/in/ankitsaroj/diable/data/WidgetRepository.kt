package `in`.ankitsaroj.diable.data

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager

data class WidgetProviderEntry(
    val info: AppWidgetProviderInfo,
    val appLabel: String,
    val widgetLabel: String,
)

data class WidgetAppGroup(
    val appLabel: String,
    val widgets: List<WidgetProviderEntry>,
)

class WidgetRepository(private val context: Context) {

    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val packageManager: PackageManager = context.packageManager

    fun groupedProviders(): List<WidgetAppGroup> {
        val providers = appWidgetManager.installedProviders ?: emptyList()
        return providers
            .map { info -> info.toEntry() }
            .groupBy { it.appLabel }
            .map { (label, entries) ->
                WidgetAppGroup(
                    appLabel = label,
                    widgets = entries.sortedBy { it.widgetLabel },
                )
            }
            .sortedBy { it.appLabel }
    }

    fun suggestionProviders(): List<WidgetProviderEntry> =
        groupedProviders().flatMap { it.widgets }.take(5)

    private fun AppWidgetProviderInfo.toEntry(): WidgetProviderEntry {
        val appLabel = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(provider.packageName, 0),
            ).toString()
        } catch (_: Exception) {
            provider.packageName
        }
        val widgetLabel = loadLabel(packageManager)?.toString() ?: provider.className
        return WidgetProviderEntry(info = this, appLabel = appLabel, widgetLabel = widgetLabel)
    }
}
