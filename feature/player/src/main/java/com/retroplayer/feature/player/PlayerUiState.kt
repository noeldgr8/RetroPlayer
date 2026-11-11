package com.retroplayer.feature.player

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/** What the player UI renders. Mapped from core:media's NowPlaying in :app. */
@Immutable
data class PlayerUiState(
    val title: String,
    val artist: String,
    val album: String,
    val sourceLabel: String,
    val art: ImageBitmap?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val canSeek: Boolean = true,
    val canPrevious: Boolean = true,
    val canNext: Boolean = true,
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    /** "{artist} · {album}" */
    val artistLine: String
        get() = listOf(artist, album).filter { it.isNotBlank() }.joinToString(" · ")
}

/** Motion state owned by a ViewModel so rotation survives fold / unfold. */
class PlayerMotion(
    val spin: SpinState,
    val reels: ReelState,
    /** False for reduced motion (system or in-app) or when the app is not visible. */
    val animate: Boolean,
)
