package com.retroplayer.core.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the active media session (Spotify preferred) via MediaSessionManager, which requires
 * this app's [MediaListenerService] to be an enabled notification listener.
 * All work happens on the main thread; callbacks are delivered on the main looper.
 */
@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _listenerEnabled = MutableStateFlow(isListenerEnabled())
    val listenerEnabled: StateFlow<Boolean> = _listenerEnabled.asStateFlow()

    private var preferredPackage: String = SPOTIFY_PACKAGE
    private var controller: MediaController? = null
    private var tracking = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        selectController(list.orEmpty())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() {
            selectController(activeControllers())
        }
    }

    /** Re-check notification access (call from onResume and from the listener service). */
    fun refreshAccess() {
        val enabled = isListenerEnabled()
        _listenerEnabled.value = enabled
        if (enabled) startTracking() else stopTracking()
    }

    fun setPreferredPackage(packageName: String) {
        if (packageName == preferredPackage) return
        preferredPackage = packageName
        if (tracking) selectController(activeControllers())
    }

    fun activeSources(): List<SourceApp> = activeControllers()
        .map { it.packageName }
        .distinct()
        .map { SourceApp(it, labelFor(it)) }

    fun play() { controller?.transportControls?.play() }
    fun pause() { controller?.transportControls?.pause() }
    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }
    fun seekTo(positionMs: Long) { controller?.transportControls?.seekTo(positionMs) }

    /**
     * Intent for the system page where the user grants notification access to RetroPlayer.
     * Start it from an Activity (no NEW_TASK) so Back returns to the app.
     */
    fun accessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                listenerComponent.flattenToString(),
            )

    fun accessSettingsFallbackIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    // ---------------------------------------------------------------------------------------

    private fun isListenerEnabled(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    private fun startTracking() {
        if (tracking) return
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, mainHandler)
            tracking = true
            selectController(activeControllers())
        } catch (e: SecurityException) {
            _listenerEnabled.value = false
        }
    }

    private fun stopTracking() {
        if (tracking) {
            sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
            tracking = false
        }
        controller?.unregisterCallback(controllerCallback)
        controller = null
        _nowPlaying.value = null
    }

    private fun activeControllers(): List<MediaController> = try {
        sessionManager.getActiveSessions(listenerComponent)
    } catch (e: SecurityException) {
        emptyList()
    }

    /** Preferred app if it is playing, else any playing app, else preferred, else most recent. */
    private fun selectController(controllers: List<MediaController>) {
        val playing = controllers.filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val chosen = playing.firstOrNull { it.packageName == preferredPackage }
            ?: playing.firstOrNull()
            ?: controllers.firstOrNull { it.packageName == preferredPackage }
            ?: controllers.firstOrNull()

        if (chosen?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(controllerCallback)
            controller = chosen
            chosen?.registerCallback(controllerCallback, mainHandler)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        val metadata = c?.metadata
        val state = c?.playbackState
        if (c == null || (metadata == null && state == null)) {
            _nowPlaying.value = null
            return
        }
        val actions = state?.actions ?: 0L
        // Some apps publish no action flags at all; assume the basics work in that case.
        fun can(flag: Long) = actions == 0L || actions and flag != 0L

        _nowPlaying.value = NowPlaying(
            sourcePackage = c.packageName,
            sourceLabel = labelFor(c.packageName),
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = (metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty(),
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            art = metadata?.artBitmap(),
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0) ?: 0L,
            positionMs = state?.position ?: 0L,
            positionUpdatedAt = state?.lastPositionUpdateTime ?: android.os.SystemClock.elapsedRealtime(),
            playbackSpeed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            canSkipNext = can(PlaybackState.ACTION_SKIP_TO_NEXT),
            canSkipPrevious = can(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
            canSeek = actions and PlaybackState.ACTION_SEEK_TO != 0L,
        )
    }

    private fun MediaMetadata.artBitmap(): Bitmap? =
        getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

    /** User-visible app name for [packageName], or the package name if it isn't installed. */
    fun labelFor(packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }
}
