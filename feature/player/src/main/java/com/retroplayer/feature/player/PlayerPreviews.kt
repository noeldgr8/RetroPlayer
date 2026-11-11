package com.retroplayer.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.retroplayer.core.theme.CassetteDarkTokens
import com.retroplayer.core.theme.CassetteLightTokens
import com.retroplayer.core.theme.RetroPlayerTheme
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.core.theme.VinylDarkTokens
import com.retroplayer.core.theme.VinylLightTokens

// Placeholder sample content from DESIGN.md 12. Previews only; never ship.
// TODO: add the three sample covers as vector drawables in a debug source set.
internal val SampleSaffron = PlayerUiState(
    title = "Saffron Hours",
    artist = "Mira Kale",
    album = "Daylight Index",
    sourceLabel = "Spotify",
    art = null,
    isPlaying = true,
    positionMs = 78_000,
    durationMs = 222_000,
)

private val NoActions = PlayerActions({}, {}, {}, {}, {}, {})

@Composable
private fun PreviewFrame(theme: RetroTheme, content: @Composable () -> Unit) {
    RetroPlayerTheme(theme) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}

@Preview(widthDp = 380, heightDp = 380)
@Composable
private fun TurntableLightPreview() = PreviewFrame(RetroTheme.VinylLight) {
    Box(Modifier.padding(20.dp)) {
        Turntable(VinylLightTokens, art = null, isPlaying = true, animate = false, modifier = Modifier.width(340.dp))
    }
}

@Preview(widthDp = 380, heightDp = 380)
@Composable
private fun TurntableDarkIdlePreview() = PreviewFrame(RetroTheme.VinylDark) {
    Box(Modifier.padding(20.dp)) {
        Turntable(VinylDarkTokens, art = null, isPlaying = false, idle = true, animate = false, modifier = Modifier.width(340.dp))
    }
}

@Preview(widthDp = 440, heightDp = 300)
@Composable
private fun CassetteLightPreview() = PreviewFrame(RetroTheme.CassetteLight) {
    Box(Modifier.padding(20.dp)) {
        CassettePlayer(CassetteLightTokens, progress = 0.35f, isPlaying = true, animate = false, modifier = Modifier.width(400.dp))
    }
}

@Preview(widthDp = 440, heightDp = 300)
@Composable
private fun CassetteDarkPreview() = PreviewFrame(RetroTheme.CassetteDark) {
    Box(Modifier.padding(20.dp)) {
        CassettePlayer(CassetteDarkTokens, progress = 0.8f, isPlaying = false, animate = false, modifier = Modifier.width(400.dp))
    }
}

@Preview(name = "A · Cover · Vinyl Light", widthDp = 465, heightDp = 735)
@Composable
private fun CoverVinylPreview() = PreviewFrame(RetroTheme.VinylLight) {
    PlayerScreen(PlayerLayout.Cover, SampleSaffron, NoActions, PlayerMotion(SpinState(), ReelState(), animate = false))
}

@Preview(name = "A · Cover · Cassette Dark", widthDp = 465, heightDp = 735)
@Composable
private fun CoverCassettePreview() = PreviewFrame(RetroTheme.CassetteDark) {
    PlayerScreen(PlayerLayout.Cover, SampleSaffron, NoActions, PlayerMotion(SpinState(), ReelState(), animate = false))
}

@Preview(name = "B · Inner portrait · Vinyl Dark", widthDp = 730, heightDp = 968)
@Composable
private fun InnerPortraitPreview() = PreviewFrame(RetroTheme.VinylDark) {
    PlayerScreen(PlayerLayout.InnerPortrait, SampleSaffron, NoActions, PlayerMotion(SpinState(), ReelState(), animate = false))
}

@Preview(name = "C · Inner landscape · Cassette Light", widthDp = 968, heightDp = 730)
@Composable
private fun InnerLandscapePreview() = PreviewFrame(RetroTheme.CassetteLight) {
    PlayerScreen(PlayerLayout.InnerLandscape, SampleSaffron, NoActions, PlayerMotion(SpinState(), ReelState(), animate = false))
}

@Preview(name = "D · Tabletop · Vinyl Light", widthDp = 968, heightDp = 730)
@Composable
private fun TabletopPreview() = PreviewFrame(RetroTheme.VinylLight) {
    PlayerScreen(PlayerLayout.Tabletop, SampleSaffron, NoActions, PlayerMotion(SpinState(), ReelState(), animate = false))
}
