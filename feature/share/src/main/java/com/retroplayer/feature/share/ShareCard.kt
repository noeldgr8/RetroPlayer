package com.retroplayer.feature.share

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.LocalRetroStyle
import com.retroplayer.core.theme.VinylTokens
import com.retroplayer.feature.player.CassettePlayer
import com.retroplayer.feature.player.PlayerUiState
import com.retroplayer.feature.player.ReelState
import com.retroplayer.feature.player.SpinState
import com.retroplayer.feature.player.Turntable

/** EN-02 card formats; design units are half the export size (DESIGN.md 14.2). */
enum class ShareFormat(val designW: Int, val designH: Int) {
    Story(540, 960),
    Post(540, 540);

    val exportW: Int get() = designW * 2
    val exportH: Int get() = designH * 2
}

/**
 * The "Now Spinning" card, laid out at exactly [ShareFormat.exportW] x [ShareFormat.exportH]
 * pixels whatever the screen density (one design unit = 2 px). Font scaling is neutralised so
 * the image is identical on every phone. Static frame: tonearm down / PLAY lit, no rotation.
 */
@Composable
fun ShareCard(format: ShareFormat, state: PlayerUiState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    // dp per design unit, and the card's size in dp so that it lays out at the export pixels.
    val u = 2f / density.density
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
        Box(modifier.requiredSize((format.exportW / density.density).dp, (format.exportH / density.density).dp)) {
            CardContent(format, state, u)
        }
    }
}

@Composable
private fun CardContent(format: ShareFormat, state: PlayerUiState, u: Float) {
    fun d(v: Number): Dp = (v.toFloat() * u).dp
    fun s(v: Number) = (v.toFloat() * u).sp
    val colors = MaterialTheme.colorScheme
    val retro = LocalRetroStyle.current
    val story = format == ShareFormat.Story

    Box(Modifier.fillMaxWidth().background(colors.background).requiredSize(d(format.designW), d(format.designH))) {
        // Soft circle behind the object
        Box(
            Modifier
                .offset(d(if (story) 20 else -20), d(if (story) 110 else 80))
                .size(d(if (story) 500 else 380))
                .background(colors.surfaceContainer, CircleShape),
        )

        // Object (static frame)
        when (val t = retro.tokens) {
            is VinylTokens -> Turntable(
                t, state.art, isPlaying = true, animate = false, spin = SpinState(),
                look = state.look, progress = state.progress,
                modifier = Modifier.offset(d(if (story) 60 else 32), d(if (story) 150 else 125)).width(d(if (story) 420 else 290)),
            )
            is CassetteTokens -> CassettePlayer(
                t, state.progress, isPlaying = true, animate = false, reels = ReelState(),
                look = state.look, songTitle = state.title,
                modifier = Modifier.offset(d(if (story) 40 else 24), d(if (story) 250 else 172)).width(d(if (story) 460 else 300)),
            )
        }

        val kicker = stringResource(R.string.share_kicker).uppercase()
        val titleStyle = TextStyle(
            fontFamily = retro.displayFont, fontWeight = FontWeight.SemiBold,
            fontSize = s(if (story) 40 else 28), lineHeight = s(if (story) 46 else 32),
        )
        val artistStyle = TextStyle(fontSize = s(if (story) 22 else 16), lineHeight = s(if (story) 28 else 22))

        if (story) {
            Text(
                kicker,
                style = TextStyle(fontSize = s(15), fontWeight = FontWeight.Bold, letterSpacing = 0.32.em),
                color = colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().offset(y = d(64)),
            )
            Row(Modifier.offset(d(48), d(640)).width(d(540 - 96)), verticalAlignment = Alignment.CenterVertically) {
                CardArt(state, d(112), d(8))
                Column(Modifier.padding(start = d(20))) {
                    Text(state.title, style = titleStyle, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(state.artist, style = artistStyle, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(
                Modifier.fillMaxWidth().offset(y = d(960 - 56 - 30)),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) { AppMark(u, retro.displayFont) }
        } else {
            Column(Modifier.offset(d(344), d(48)).width(d(164))) {
                Text(
                    kicker,
                    style = TextStyle(fontSize = s(12), fontWeight = FontWeight.Bold, letterSpacing = 0.26.em),
                    color = colors.primary,
                )
                Box(Modifier.padding(top = d(20))) { CardArt(state, d(84), d(6)) }
                Text(
                    state.title, style = titleStyle, color = colors.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = d(14)),
                )
                Text(
                    state.artist, style = artistStyle, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = d(4)),
                )
            }
            Row(Modifier.offset(d(344), d(540 - 36 - 30)), verticalAlignment = Alignment.CenterVertically) {
                AppMark(u, retro.displayFont)
            }
        }
    }
}

/** Album art, unaltered (cropped square only). */
@Composable
private fun CardArt(state: PlayerUiState, size: Dp, corner: Dp) {
    val shape = RoundedCornerShape(corner)
    val art = state.art
    if (art != null) {
        Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(shape))
    } else {
        Box(Modifier.size(size).background(MaterialTheme.colorScheme.primaryContainer, shape))
    }
}

/** Placeholder app mark (30 x 30 units) + wordmark. Never the Spotify logo. */
@Composable
private fun AppMark(u: Float, displayFont: androidx.compose.ui.text.font.FontFamily) {
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.size((30 * u).dp)) {
        val k = size.width / 30f
        drawCircle(colors.onSurface, 12f * k, Offset(13f * k, 16f * k))
        drawCircle(colors.primary, 4.5f * k, Offset(13f * k, 16f * k))
        drawCircle(colors.background, 1.4f * k, Offset(13f * k, 16f * k))
        drawLine(colors.onSurfaceVariant, Offset(26f * k, 3f * k), Offset(20f * k, 15f * k), strokeWidth = 2.6f * k, cap = StrokeCap.Round)
    }
    Text(
        stringResource(R.string.share_wordmark),
        style = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, fontSize = (18 * u).sp),
        color = colors.onSurface,
        modifier = Modifier.padding(start = (8 * u).dp),
    )
}
