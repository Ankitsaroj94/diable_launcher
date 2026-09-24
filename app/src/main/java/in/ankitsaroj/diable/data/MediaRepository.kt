package `in`.ankitsaroj.diable.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import `in`.ankitsaroj.diable.service.DiableNotificationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

data class MediaInfo(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val packageName: String,
    val art: Bitmap? = null,
    /** Kept so the bar's buttons drive the same session it is showing. */
    val controller: MediaController? = null,
) {
    fun playPause() {
        val controls = controller?.transportControls ?: return
        if (isPlaying) controls.pause() else controls.play()
    }

    fun next() = controller?.transportControls?.skipToNext()

    fun previous() = controller?.transportControls?.skipToPrevious()
}

class MediaRepository(private val context: Context) {

    /**
     * Emits the current media session whenever sessions come and go, and whenever the
     * shown session's metadata or play state changes.
     *
     * Reading sessions is only allowed for an enabled notification listener, so the flow
     * registers against [DiableNotificationListener]. Everything is push-based; nothing polls.
     */
    fun activeMediaFlow(): Flow<MediaInfo?> = callbackFlow {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
            as? MediaSessionManager
        if (manager == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val component = DiableNotificationListener.component(context)

        var watched: MediaController? = null
        val controllerCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                trySend(watched?.toMediaInfo())
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                trySend(watched?.toMediaInfo())
            }

            override fun onSessionDestroyed() {
                trySend(null)
            }
        }

        fun watch(controllers: List<MediaController>?) {
            val pick = pickController(controllers.orEmpty())
            if (pick?.sessionToken != watched?.sessionToken) {
                watched?.unregisterCallback(controllerCallback)
                watched = pick
                pick?.registerCallback(controllerCallback)
            }
            trySend(pick?.toMediaInfo())
        }

        val listener = MediaSessionManager.OnActiveSessionsChangedListener { watch(it) }

        val registered = try {
            watch(manager.getActiveSessions(component))
            manager.addOnActiveSessionsChangedListener(listener, component)
            true
        } catch (_: SecurityException) {
            // Notification access not granted yet.
            trySend(null)
            false
        }

        awaitClose {
            watched?.unregisterCallback(controllerCallback)
            if (registered) {
                runCatching { manager.removeOnActiveSessionsChangedListener(listener) }
            }
        }
    }.flowOn(Dispatchers.Main)

    private fun pickController(controllers: List<MediaController>): MediaController? {
        val withMetadata = controllers.filter { it.metadata?.title() != null }
        return withMetadata.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: withMetadata.firstOrNull()
    }

    private fun MediaMetadata.title(): String? =
        (getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE))?.takeIf { it.isNotBlank() }

    private fun MediaController.toMediaInfo(): MediaInfo? {
        val metadata = metadata ?: return null
        val title = metadata.title() ?: return null
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val state = playbackState?.state
        return MediaInfo(
            title = title,
            artist = artist,
            isPlaying = state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_BUFFERING,
            packageName = packageName,
            art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART),
            controller = this,
        )
    }
}
