package com.retroplayer.feature.player

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.LocalRetroStyle
import com.retroplayer.core.theme.PlayerStyle
import com.retroplayer.core.theme.RetroMotion
import com.retroplayer.core.theme.RetroType
import com.retroplayer.feature.player.draw.drawShadow

/** Playback commands from the UI. */
class PlayerActions(
    val onSeek: (Long) -> Unit,
    val onPrevious: () -> Unit,
    val onPlay: () -> Unit,
    val onPause: () -> Unit,
    val onNext: () -> Unit,
    val onSettings: () -> Unit,
    /** v1.1: share card (EN-02) and record crate (EN-03). */
    val onShare: () -> Unit = {},
    val onCrate: () -> Unit = {},
)

/**
 * Progress row + vinyl buttons or cassette key bank.
 */
@Composable
fun Transport(
    state: PlayerUiState,
    actions: PlayerActions,
    style: PlayerStyle,
    modifier: Modifier = Modifier,
    big: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (big) 20.dp else 14.dp)) {
        ProgressRow(state, style, actions.onSeek)
        when (style) {
            PlayerStyle.Vinyl -> VinylButtons(state, actions, big)
            PlayerStyle.Cassette -> CassetteKeys(state, actions, big)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Progress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressRow(state: PlayerUiState, style: PlayerStyle, onSeek: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val duration = state.durationMs.coerceAtLeast(0L)
    var scrub by remember { mutableStateOf<Float?>(null) }
    var lastTick by remember { mutableIntStateOf(-1) }
    val fraction = scrub ?: state.progress
    val seekLabel = stringResource(R.string.cd_seek)
    val vinyl = style == PlayerStyle.Vinyl

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Slider(
            value = fraction,
            onValueChange = { v ->
                scrub = v
                val bucket = ((v * duration) / 10_000L).toInt()
                if (bucket != lastTick) {
                    if (lastTick >= 0) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    lastTick = bucket
                }
            },
            onValueChangeFinished = {
                scrub?.let { onSeek((it * duration).toLong()) }
                scrub = null
                lastTick = -1
            },
            enabled = state.canSeek && duration > 0,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = seekLabel },
            thumb = {
                Box(
                    Modifier
                        .size(if (vinyl) 4.dp else 14.dp, 20.dp)
                        .background(colors.primary, RoundedCornerShape(if (vinyl) 2.dp else 3.dp)),
                )
            },
            track = { sliderState -> ProgressTrack(sliderState.fraction(), vinyl) },
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatTime((fraction * duration).toLong()), style = RetroType.times, color = colors.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(formatTime(duration), style = RetroType.times, color = colors.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun SliderState.fraction(): Float {
    val range = valueRange.endInclusive - valueRange.start
    return if (range <= 0f) 0f else ((value - valueRange.start) / range).coerceIn(0f, 1f)
}

@Composable
private fun ProgressTrack(fraction: Float, rounded: Boolean) {
    val inactive = MaterialTheme.colorScheme.outlineVariant
    val active = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        val r = if (rounded) CornerRadius(2.dp.toPx()) else CornerRadius.Zero
        drawRoundRect(inactive, size = size, cornerRadius = r)
        drawRoundRect(active, size = Size(size.width * fraction, size.height), cornerRadius = r)
    }
}

internal fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

// ---------------------------------------------------------------------------------------------
// Vinyl: Material 3 expressive buttons with a morphing play button

@Composable
private fun VinylButtons(state: PlayerUiState, actions: PlayerActions, big: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (big) 36.dp else 24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(
            icon = RetroIcons.SkipPrevious,
            description = stringResource(R.string.cd_previous),
            size = if (big) 72.dp else 56.dp,
            iconSize = if (big) 32.dp else 26.dp,
            enabled = state.canPrevious,
            onClick = actions.onPrevious,
        )
        PlayMorphButton(state.isPlaying, big, onPlay = actions.onPlay, onPause = actions.onPause)
        RoundButton(
            icon = RetroIcons.SkipNext,
            description = stringResource(R.string.cd_next),
            size = if (big) 72.dp else 56.dp,
            iconSize = if (big) 32.dp else 26.dp,
            enabled = state.canNext,
            onClick = actions.onNext,
        )
    }
}

@Composable
private fun RoundButton(
    icon: ImageVector,
    description: String,
    size: Dp,
    iconSize: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(iconSize))
        }
    }
}

/** Playing = rounded square, paused = circle; 300 ms morph. */
@Composable
internal fun PlayMorphButton(isPlaying: Boolean, big: Boolean, onPlay: () -> Unit, onPause: () -> Unit) {
    val view = LocalView.current
    val size = if (big) 104.dp else 80.dp
    val corner by animateDpAsState(
        targetValue = if (isPlaying) (if (big) 34.dp else 26.dp) else size / 2,
        animationSpec = tween(RetroMotion.PLAY_BUTTON_MORPH_MS),
        label = "playMorph",
    )
    Surface(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            if (isPlaying) onPause() else onPlay()
        },
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (isPlaying) RetroIcons.Pause else RetroIcons.Play,
                contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                modifier = Modifier.size(if (big) 44.dp else 34.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Cassette: mechanical key bank

@Composable
private fun CassetteKeys(state: PlayerUiState, actions: PlayerActions, big: Boolean) {
    val tokens = LocalRetroStyle.current.tokens as? CassetteTokens ?: return
    val view = LocalView.current
    val wellShape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(wellShape)
            .background(tokens.keyWell)
            .drawBehind {
                // Approximates inset 0 2 6 rgba(0,0,0,.25)
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent),
                        endY = 8.dp.toPx(),
                    ),
                )
            }
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val keyHeight = if (big) 96.dp else 68.dp
        val key = Modifier.weight(1f).height(keyHeight)
        MechanicalKey(
            label = stringResource(R.string.key_rew),
            icon = RetroIcons.FastRewind,
            description = stringResource(R.string.cd_key_rew),
            latched = false,
            enabled = state.canPrevious,
            tokens = tokens,
            big = big,
            onClick = actions.onPrevious,
            modifier = key,
        )
        MechanicalKey(
            label = stringResource(R.string.key_play),
            icon = RetroIcons.Play,
            description = stringResource(R.string.cd_play),
            latched = state.isPlaying,
            stateDescription = stringResource(if (state.isPlaying) R.string.state_pressed else R.string.state_up),
            tokens = tokens,
            big = big,
            onClick = {
                if (!state.isPlaying) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    actions.onPlay()
                }
            },
            modifier = key,
        )
        MechanicalKey(
            label = stringResource(R.string.key_stop),
            icon = RetroIcons.Stop,
            description = stringResource(R.string.cd_pause),
            latched = false,
            tokens = tokens,
            big = big,
            onClick = {
                if (state.isPlaying) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    actions.onPause()
                }
            },
            modifier = key,
        )
        MechanicalKey(
            label = stringResource(R.string.key_ff),
            icon = RetroIcons.FastForward,
            description = stringResource(R.string.cd_key_ff),
            latched = false,
            enabled = state.canNext,
            tokens = tokens,
            big = big,
            onClick = actions.onNext,
            modifier = key,
        )
    }
}

@Composable
private fun MechanicalKey(
    label: String,
    icon: ImageVector,
    description: String,
    latched: Boolean,
    tokens: CassetteTokens,
    big: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    stateDescription: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val down = latched || pressed
    val travel by animateFloatAsState(
        targetValue = if (down) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "keyTravel",
    )
    val ink by animateColorAsState(
        if (down) MaterialTheme.colorScheme.primary else tokens.keyInk,
        label = "keyInk",
    )
    val shape = RoundedCornerShape(10.dp, 10.dp, 8.dp, 8.dp)

    Box(
        modifier
            .graphicsLayer { translationY = travel * 5.dp.toPx() }
            .drawBehind {
                val outline = Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(Offset.Zero, size),
                            topLeft = CornerRadius(10.dp.toPx()),
                            topRight = CornerRadius(10.dp.toPx()),
                            bottomRight = CornerRadius(8.dp.toPx()),
                            bottomLeft = CornerRadius(8.dp.toPx()),
                        ),
                    )
                }
                // Up: 0 7 0 keyEdge + 0 10 14 shadow. Pressed: 0 2 0 keyEdge + 0 3 4 shadow.
                drawShadow(
                    outline,
                    Color.Black.copy(alpha = 0.28f),
                    blur = lerp(14.dp.toPx(), 4.dp.toPx(), travel),
                    dy = lerp(10.dp.toPx(), 3.dp.toPx(), travel),
                )
                val edge = lerp(7.dp.toPx(), 2.dp.toPx(), travel)
                translateOutline(outline, edge) { drawPath(it, tokens.keyEdge) }
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tokens.keyFaceTop, tokens.keyFaceBottom)))
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = description
                if (stateDescription != null) this.stateDescription = stateDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(if (big) 32.dp else 26.dp))
            Text(
                label,
                style = RetroType.keyLabel(big),
                color = ink,
                modifier = Modifier.padding(top = 2.dp).clearAndSetSemantics { },
            )
        }
    }
}

private inline fun translateOutline(path: Path, dy: Float, block: (Path) -> Unit) {
    block(Path().apply { addPath(path, Offset(0f, dy)) })
}
