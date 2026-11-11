package com.retroplayer.app.demo

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * DEBUG ONLY. A real MediaSession playing silent placeholder tracks (DESIGN.md 12), so the
 * whole pipeline (notification listener -> MediaRepository -> player UI -> transport controls)
 * can be exercised on an emulator with no music app. Never ships: lives in src/debug.
 */
object DemoSession {

    private data class Track(
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val startMs: Long,
        val cover: (Int) -> Bitmap,
    )

    private val tracks = listOf(
        Track("Saffron Hours", "Mira Kale", "Daylight Index", 222_000, 78_000, DemoCovers::bright),
        Track("Low Tide Radio", "The Quiet Engines", "Night Shift", 297_000, 161_000, DemoCovers::dark),
        Track("Confetti Logic", "Ozo & Pell", "Paper Parade", 185_000, 47_000, DemoCovers::busy),
    )

    private const val ACTIONS = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
        PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or
        PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO

    private val handler = Handler(Looper.getMainLooper())
    private var session: MediaSession? = null
    private var index = 0
    private var playing = true
    private var positionMs = 0L
    private var positionAt = 0L
    private val endOfTrack = Runnable { skip(1) }

    fun start(context: Context) {
        if (session != null) return
        session = MediaSession(context.applicationContext, "RetroPlayerDemo").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = setPlaying(true)
                override fun onPause() = setPlaying(false)
                override fun onSkipToNext() = skip(1)
                override fun onSkipToPrevious() = skip(-1)
                override fun onSeekTo(pos: Long) {
                    positionMs = pos.coerceIn(0, tracks[index].durationMs)
                    positionAt = SystemClock.elapsedRealtime()
                    publishState()
                }
            }, handler)
            isActive = true
        }
        load(0, fromStart = false)
    }

    fun stop() {
        handler.removeCallbacks(endOfTrack)
        session?.release()
        session = null
    }

    private fun currentPosition(): Long {
        val elapsed = if (playing) SystemClock.elapsedRealtime() - positionAt else 0L
        return (positionMs + elapsed).coerceAtMost(tracks[index].durationMs)
    }

    private fun setPlaying(play: Boolean) {
        positionMs = currentPosition()
        positionAt = SystemClock.elapsedRealtime()
        playing = play
        publishState()
    }

    private fun skip(delta: Int) {
        // Like most players, "previous" restarts the track if more than 3 s in.
        if (delta < 0 && currentPosition() > 3_000) {
            positionMs = 0
            positionAt = SystemClock.elapsedRealtime()
            publishState()
            return
        }
        load(Math.floorMod(index + delta, tracks.size), fromStart = true)
    }

    private fun load(i: Int, fromStart: Boolean) {
        index = i
        val t = tracks[i]
        session?.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, t.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, t.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, t.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, t.durationMs)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, t.cover(512))
                .build(),
        )
        positionMs = if (fromStart) 0 else t.startMs
        positionAt = SystemClock.elapsedRealtime()
        publishState()
    }

    private fun publishState() {
        val s = session ?: return
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(ACTIONS)
                .setState(
                    if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    positionMs,
                    if (playing) 1f else 0f,
                    positionAt,
                )
                .build(),
        )
        handler.removeCallbacks(endOfTrack)
        if (playing) handler.postDelayed(endOfTrack, tracks[index].durationMs - positionMs)
    }
}
