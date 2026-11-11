package com.retroplayer.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.retroplayer.app.R
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.LocalRetroStyle
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.core.theme.VinylTokens
import com.retroplayer.feature.player.CassettePlayer
import com.retroplayer.feature.player.Turntable
import com.retroplayer.feature.player.rememberReelState
import com.retroplayer.feature.player.rememberSpinState

/** Theme picker with live preview. Tapping a card re-themes the whole screen. */
@Composable
fun ThemePickerScreen(
    current: RetroTheme,
    followSystem: Boolean,
    tintFromArt: Boolean,
    animate: Boolean,
    onBack: () -> Unit,
    onPick: (RetroTheme) -> Unit,
    onFollowSystem: (Boolean) -> Unit,
    onTintFromArt: (Boolean) -> Unit,
) {
    val spin = rememberSpinState()
    val reels = rememberReelState()
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
            BackTopBar(stringResource(R.string.theme_title), onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                // Preview: the selected object, playing
                Box(
                    Modifier.fillMaxWidth().height(226.dp).clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) {
                    when (val t = LocalRetroStyle.current.tokens) {
                        is VinylTokens -> Turntable(
                            t, art = null, isPlaying = true, animate = animate, spin = spin,
                            labelColor = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.width(216.dp),
                        )
                        is CassetteTokens -> CassettePlayer(
                            t, progress = 0.35f, isPlaying = true, animate = animate, reels = reels,
                            modifier = Modifier.width(320.dp),
                        )
                    }
                }
                ThemeGrid(
                    current = current,
                    onPick = onPick,
                    cardHeight = 144.dp,
                    turntableWidth = 96.dp,
                    cassetteWidth = 140.dp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                )
                SwitchRow(
                    stringResource(R.string.match_system),
                    stringResource(R.string.match_system_supporting),
                    checked = followSystem,
                    onChange = onFollowSystem,
                )
                SwitchRow(
                    stringResource(R.string.tint_from_art),
                    stringResource(R.string.tint_from_art_supporting),
                    checked = tintFromArt,
                    onChange = onTintFromArt,
                )
            }
        }
    }
}
