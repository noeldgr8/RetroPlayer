package com.retroplayer.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.LocalRetroStyle
import com.retroplayer.core.theme.PlayerTokens
import com.retroplayer.core.theme.RetroMotion
import com.retroplayer.core.theme.RetroType
import com.retroplayer.core.theme.VinylTokens
import kotlin.math.min

/**
 * A fixed design-size canvas (e.g. 465 x 368 for the cover-screen object area) scaled
 * uniformly to fit, so the composition from the design frames never stretches.
 */
@Composable
fun ScaledStage(
    designWidth: Float,
    designHeight: Float,
    modifier: Modifier = Modifier,
    content: @Composable StageScope.() -> Unit,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val sw = maxWidth.value / designWidth
        val sh = if (constraints.hasBoundedHeight) maxHeight.value / designHeight else sw
        val s = min(sw, sh)
        Box(Modifier.requiredSize((designWidth * s).dp, (designHeight * s).dp)) {
            StageScope(s).content()
        }
    }
}

@Immutable
class StageScope(val scale: Float) {
    fun d(v: Float): Dp = (v * scale).dp
    fun d(v: Int): Dp = (v * scale).dp
    fun Modifier.at(x: Float, y: Float): Modifier = this.offset(d(x), d(y))
}

/** Top app bar for layouts A, B, C and the empty state. */
@Composable
fun RetroTopBar(
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    inner: Boolean = false,
    startPadding: Dp = 24.dp,
    endPadding: Dp = 8.dp,
    onCrate: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
) {
    val retro = LocalRetroStyle.current
    Row(
        modifier.fillMaxWidth().height(64.dp).padding(start = startPadding, end = endPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.retro_app_name),
            style = RetroType.appName(retro.displayFont, inner),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        // v1.1 order: Record crate, Share, Settings
        if (onCrate != null) {
            IconButton(onClick = onCrate, modifier = Modifier.size(48.dp)) {
                Icon(RetroIcons.LibraryMusic, stringResource(R.string.cd_crate), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onShare != null) {
            IconButton(onClick = onShare, modifier = Modifier.size(48.dp)) {
                Icon(RetroIcons.Share, stringResource(R.string.cd_share), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SourceLine(sourceLabel: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(RetroIcons.MusicNote, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            stringResource(R.string.playing_from, sourceLabel).uppercase(),
            style = RetroType.source,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun TrackInfo(
    state: PlayerUiState,
    titleStyle: TextStyle,
    artistStyle: TextStyle,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    singleLine: Boolean = false,
) {
    val align = if (centered) TextAlign.Center else TextAlign.Start
    Column(
        modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        SourceLine(state.sourceLabel)
        Text(
            state.title,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = align,
            maxLines = if (singleLine) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            state.artistLine,
            style = artistStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = align,
            maxLines = if (singleLine) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Square album art (sleeve, J-card or the landscape left pane). Falls back to a tinted tile. */
@Composable
fun AlbumArt(
    art: ImageBitmap?,
    modifier: Modifier = Modifier,
    corner: Dp = 6.dp,
    rotation: Float = 0f,
    elevation: Dp = 14.dp,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .rotate(rotation)
            .shadow(elevation, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) {
            Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(
                RetroIcons.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxSize(0.4f),
            )
        }
    }
}

/**
 * The turntable or cassette for the current theme at [width]. Switching style slides the
 * object out and the other in (~500 ms); light <-> dark of the same style just re-colours.
 */
@Composable
fun PlayerObject(
    state: PlayerUiState,
    motion: PlayerMotion,
    width: Dp,
    modifier: Modifier = Modifier,
    bare: Boolean = false,
    idle: Boolean = false,
) {
    val tokens = LocalRetroStyle.current.tokens
    AnimatedContent(
        targetState = tokens,
        contentKey = { it is VinylTokens },
        transitionSpec = {
            val d = RetroMotion.STYLE_SWAP_MS
            (slideInHorizontally(tween(d)) { it } + fadeIn(tween(d))) togetherWith
                (slideOutHorizontally(tween(d)) { -it } + fadeOut(tween(d)))
        },
        modifier = modifier,
        label = "styleSwap",
    ) { t: PlayerTokens ->
        when (t) {
            is VinylTokens -> Turntable(
                tokens = t,
                art = state.art,
                isPlaying = state.isPlaying,
                idle = idle,
                bare = bare,
                progress = state.progress,
                animate = motion.animate,
                spin = motion.spin,
                look = state.look,
                modifier = Modifier.width(width),
            )
            is CassetteTokens -> CassettePlayer(
                tokens = t,
                progress = state.progress,
                isPlaying = state.isPlaying,
                bare = bare,
                animate = motion.animate,
                reels = motion.reels,
                look = state.look,
                songTitle = state.title,
                modifier = Modifier.width(width),
            )
        }
    }
}

@Composable
internal fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text, style = RetroType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
internal fun VSpace(height: Dp) = Spacer(Modifier.height(height))
