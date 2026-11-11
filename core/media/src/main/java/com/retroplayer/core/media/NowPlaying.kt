package com.retroplayer.core.media

import android.graphics.Bitmap
import android.os.SystemClock

/** Current track, as published by the source app's media session (PRD section 8). */
data class NowPlaying(
    val sourcePackage: String,
    val sourceLabel: String,
    val title: String,
    val artist: String,
    val album: String,
    val art: Bitmap?,
    val durationMs: Long,
    /** Position at [positionUpdatedAt]. */
    val positionMs: Long,
    /** [SystemClock.elapsedRealtime] timestamp of [positionMs]. */
    val positionUpdatedAt: Long,
    val playbackSpeed: Float,
    val isPlaying: Boolean,
    val canSkipNext: Boolean,
    val canSkipPrevious: Boolean,
    val canSeek: Boolean,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
    /** Album release year, when the source app publishes it. */
    val year: Int? = null,
    /** Track number on the album, when the source app publishes it. */
    val trackNumber: Int? = null,
) {
    /** Key for per-album data (palette, crate grouping). */
    val albumKey: String get() = "$artist|$album"

    /** Stable-enough identity for caching per track (widgets, tinting). */
    val trackId: String get() = "$sourcePackage|$title|$artist|$album|$durationMs"

    /** Position extrapolated from the last update, so progress moves without polling. */
    fun estimatedPositionMs(nowElapsed: Long = SystemClock.elapsedRealtime()): Long {
        val p = if (isPlaying) {
            positionMs + ((nowElapsed - positionUpdatedAt) * playbackSpeed).toLong()
        } else {
            positionMs
        }
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p.coerceAtLeast(0)
    }
}

/** An app that currently has a media session (for the source picker). */
data class SourceApp(val packageName: String, val label: String)

const val SPOTIFY_PACKAGE = "com.spotify.music"
