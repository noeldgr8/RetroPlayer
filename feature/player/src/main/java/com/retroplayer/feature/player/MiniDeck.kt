package com.retroplayer.feature.player

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.retroplayer.core.theme.LocalRetroStyle
import com.retroplayer.core.theme.PlayerStyle
import com.retroplayer.core.theme.RetroType

/*
 * EN-05 cover-screen mini deck (DESIGN.md 14.5): one-thumb layout for the folded phone.
 * Every interactive control sits in the bottom half; the object takes tap (play / pause)
 * and horizontal swipe (next / previous).
 */

@Composable
internal fun MiniDeckPortrait(state: PlayerUiState, actions: PlayerActions, motion: PlayerMotion, thumbRight: Boolean) {
    val retro = LocalRetroStyle.current
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        // Top half: object with hint chevrons and caption (chevrons are hints, not buttons).
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.weight(1f, fill = false).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val base = if (retro.style == PlayerStyle.Vinyl) 280.dp else 340.dp
                val objectWidth = min(base, maxWidth - 72.dp)
                DeckObject(state, actions, motion, objectWidth)
            }
            DeckHint(state.isPlaying, Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp))
        }
        // Bottom half: info, big transport, shortcut tiles on the thumb side.
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 20.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                state.title,
                style = RetroType.headline(retro.displayFont, 26, 32),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                state.artistLine,
                style = RetroType.artistCover,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Transport(state, actions, retro.style, big = true, modifier = Modifier.widthIn(max = 417.dp).fillMaxWidth())
            ShortcutRow(actions, thumbRight, Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun MiniDeckLandscape(state: PlayerUiState, actions: PlayerActions, motion: PlayerMotion, thumbRight: Boolean) {
    val retro = LocalRetroStyle.current
    val textAlign = if (thumbRight) TextAlign.End else TextAlign.Start
    val objectPane: @Composable () -> Unit = {
        Column(
            Modifier.width(370.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BoxWithConstraints(Modifier.weight(1f, fill = false), contentAlignment = Alignment.Center) {
                val base = if (retro.style == PlayerStyle.Vinyl) 250.dp else 290.dp
                DeckObject(state, actions, motion, min(base, min(maxWidth - 72.dp, maxHeight)))
            }
            DeckHint(state.isPlaying, Modifier.padding(top = 8.dp))
        }
    }
    val controls: @Composable () -> Unit = {
        Column(
            Modifier.width(336.dp).fillMaxHeight().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
            horizontalAlignment = if (thumbRight) Alignment.End else Alignment.Start,
        ) {
            Text(
                state.title,
                style = RetroType.headline(retro.displayFont, 26, 32),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                state.artistLine,
                style = RetroType.artistCover,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Transport(state, actions, retro.style, big = true, modifier = Modifier.fillMaxWidth())
            ShortcutRow(actions, thumbRight, Modifier.fillMaxWidth())
        }
    }
    Row(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Object on the side away from the thumb, controls on the thumb side.
        if (thumbRight) { objectPane(); controls() } else { controls(); objectPane() }
    }
}

/** The object with tap and swipe gestures (swipe > 64 dp or fling > 800 dp/s). */
@Composable
private fun DeckObject(state: PlayerUiState, actions: PlayerActions, motion: PlayerMotion, width: Dp) {
    val view = LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val current by rememberUpdatedState(state)
    var dragged by remember { mutableFloatStateOf(0f) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(RetroIcons.ChevronLeft, null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(28.dp))
        Box(
            Modifier
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        if (current.isPlaying) actions.onPause() else actions.onPlay()
                    })
                }
                .draggable(
                    state = rememberDraggableState { dragged += it },
                    orientation = Orientation.Horizontal,
                    onDragStarted = { dragged = 0f },
                    onDragStopped = { velocity ->
                        val minDistance = with(density) { 64.dp.toPx() }
                        val minVelocity = with(density) { 800.dp.toPx() }
                        when {
                            dragged < -minDistance || velocity < -minVelocity -> {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                actions.onNext()
                            }
                            dragged > minDistance || velocity > minVelocity -> {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                actions.onPrevious()
                            }
                        }
                    },
                ),
        ) {
            PlayerObject(state, motion, width)
        }
        Icon(RetroIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun DeckHint(playing: Boolean, modifier: Modifier = Modifier) {
    Text(
        stringResource(if (playing) R.string.mini_hint else R.string.mini_hint_paused).uppercase(),
        style = RetroType.source.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/** Three 56 dp tiles (Record crate, Share, Settings) aligned to the thumb side. */
@Composable
private fun ShortcutRow(actions: PlayerActions, thumbRight: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, if (thumbRight) Alignment.End else Alignment.Start),
    ) {
        Tile(RetroIcons.LibraryMusic, stringResource(R.string.cd_crate), actions.onCrate)
        Tile(RetroIcons.Share, stringResource(R.string.cd_share), actions.onShare)
        Tile(RetroIcons.Settings, stringResource(R.string.cd_settings), actions.onSettings)
    }
}

@Composable
private fun Tile(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(24.dp))
        }
    }
}
