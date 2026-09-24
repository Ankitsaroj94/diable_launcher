package `in`.ankitsaroj.diable

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient

class DiableAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: android.appwidget.AppWidgetProviderInfo,
    ): AppWidgetHostView = `in`.ankitsaroj.diable.ui.home.LongPressWidgetHostView(context)
}

class DiableApplication : Application(), ImageLoaderFactory {
    lateinit var appWidgetHost: DiableAppWidgetHost
        private set

    override fun onCreate() {
        super.onCreate()
        appWidgetHost = DiableAppWidgetHost(this, HOST_ID)
    }

    /**
     * Coil's defaults keep no disk cache, so every wallpaper thumbnail was re-downloaded
     * each time a category opened. Caching to disk is what makes the picker feel instant
     * on a second visit; the memory cap keeps a grid of large photos from bloating RAM.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.10)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("wallpaper_cache"))
                .maxSizeBytes(64L * 1024 * 1024)
                .build()
        }
        .crossfade(true)
        .respectCacheHeaders(false)
        .okHttpClient {
            OkHttpClient.Builder()
                // Openverse rejects requests without a UA.
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "Diable/1.0")
                            .build(),
                    )
                }
                .build()
        }
        .build()

    companion object {
        const val HOST_ID = 1024
    }
}
