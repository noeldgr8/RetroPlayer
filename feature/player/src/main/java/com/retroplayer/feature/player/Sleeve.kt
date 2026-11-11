package com.retroplayer.feature.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retroplayer.core.theme.LocalRetroStyle
import com.retroplayer.core.theme.RetroFonts
import com.retroplayer.core.theme.RetroTheme
import kotlin.math.PI
import kotlin.math.sin

/*
 * EN-04 flip the sleeve: tap the sleeve or J-card to turn it over and see
 * album details; tap again to turn back. Missing year / track rows are omitted.
 */

private val FlipEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val FLIP_MS = 400

/** A sleeve (vinyl) or J-card (cassette) that flips on tap. Square, [size] wide. */
@Composable
fun FlippableSleeve(
    state: PlayerUiState,
    size: Dp,
    corner: Dp,
    jCard: Boolean,
    animate: Boolean,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    flipped: Boolean? = null,
    onToggle: (() -> Unit)? = null,
) {
    var own by rememberSaveable { mutableStateOf(false) }
    val isFlipped = flipped ?: own
    val toggle = onToggle ?: { own = !own }
    val description = sleeveDescription(state, isFlipped)

    Box(
        modifier
            .rotate(rotation)
            .size(size)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = toggle)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                liveRegion = LiveRegionMode.Polite
                onClick { toggle(); true }
            },
    ) {
        FlipFaces(isFlipped, animate, corner, front = {
            AlbumArt(state.art, Modifier.fillMaxSize(), corner = corner)
        }, back = {
            Box(Modifier.fillMaxSize().shadow(14.dp, RoundedCornerShape(corner)).clip(RoundedCornerShape(corner))) {
                if (jCard) JCardBack(state, size) else SleeveBack(state, size)
            }
        })
    }
}

@Composable
private fun FlipFaces(
    flipped: Boolean,
    animate: Boolean,
    corner: Dp,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
) {
    if (!animate) {
        // Reduced motion: cross-fade 150 ms instead of rotating.
        Crossfade(flipped, animationSpec = tween(150), label = "sleeveFade") { f -> if (f) back() else front() }
        return
    }
    val angle = remember { Animatable(if (flipped) 180f else 0f) }
    LaunchedEffect(flipped) { angle.animateTo(if (flipped) 180f else 0f, tween(FLIP_MS, easing = FlipEasing)) }
    val density = LocalDensity.current.density
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            rotationY = angle.value
            cameraDistance = 14f * density
            // Lift: scale 1 -> 1.04 at the midpoint -> 1.
            val s = 1f + 0.04f * sin(PI.toFloat() * (angle.value / 180f))
            scaleX = s
            scaleY = s
        },
    ) {
        if (angle.value <= 90f) {
            front()
        } else {
            // Mirror the back face so its text reads correctly.
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) { back() }
        }
    }
}

@Composable
private fun sleeveDescription(state: PlayerUiState, flipped: Boolean): String {
    if (!flipped) return stringResource(R.string.cd_sleeve_front)
    val parts = buildList {
        add(stringResource(R.string.cd_sleeve_back))
        add(state.album.ifBlank { stringResource(R.string.unknown_album) })
        add(state.artist.ifBlank { stringResource(R.string.unknown_artist) })
        add("${stringResource(R.string.sleeve_now_playing)}: ${state.title}")
        state.trackNumber?.let { add("${stringResource(R.string.sleeve_track)} $it") }
        state.year?.let { add("${stringResource(R.string.sleeve_released)} $it") }
    }
    return parts.joinToString(". ")
}

/** Sleeve back, laid out in 300 units (vinyl themes). */
@Composable
private fun SleeveBack(state: PlayerUiState, size: Dp) {
    val dark = LocalRetroStyle.current.isDark
    val paper = if (dark) Color(0xFF1D1915) else Color(0xFFF3ECDF)
    val ink = if (dark) Color(0xFFEEE0D3) else Color(0xFF2A1E12)
    FixedScale(size) { u ->
        fun d(v: Number) = (v.toFloat() * u).dp
        fun s(v: Number) = (v.toFloat() * u).sp
        Box(Modifier.fillMaxSize().background(paper)) {
            // Subtle top-left sheen
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = if (dark) 0.05f else 0.35f), Color.Transparent),
                        start = Offset.Zero, end = Offset(size.value * 1.2f, size.value * 1.2f),
                    ),
                ),
            )
            Column(Modifier.offset(d(24), d(24)).width(d(252))) {
                Text(
                    state.album.ifBlank { stringResource(R.string.unknown_album) },
                    style = TextStyle(fontFamily = RetroFonts.Fraunces, fontWeight = FontWeight.SemiBold, fontSize = s(26), lineHeight = s(30)),
                    color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.artist.ifBlank { stringResource(R.string.unknown_artist) },
                    style = TextStyle(fontSize = s(15)), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = d(4)),
                )
            }
            Box(Modifier.offset(d(24), d(124)).width(d(252)).size(d(252), d(1)).background(ink.copy(alpha = 0.25f)))
            Column(Modifier.offset(d(24), d(140)).width(d(160))) {
                Field(stringResource(R.string.sleeve_now_playing), state.title, ink, u)
                state.trackNumber?.let { Field(stringResource(R.string.sleeve_track), it.toString(), ink, u) }
                state.year?.let { Field(stringResource(R.string.sleeve_released), it.toString(), ink, u) }
            }
            Thumb(state, d(72), Modifier.offset(d(300 - 24 - 72), d(300 - 24 - 72)))
        }
    }
}

@Composable
private fun Field(label: String, value: String, ink: Color, u: Float) {
    Column(Modifier.padding(bottom = (10 * u).dp)) {
        Text(
            label.uppercase(),
            style = TextStyle(fontSize = (11 * u).sp, letterSpacing = 0.12.em),
            color = ink.copy(alpha = 0.7f),
        )
        Text(
            value,
            style = TextStyle(fontSize = (14 * u).sp, lineHeight = (18 * u).sp, fontWeight = FontWeight.SemiBold),
            color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** J-card back, laid out in 300 units (cassette themes). */
@Composable
private fun JCardBack(state: PlayerUiState, size: Dp) {
    val retro = LocalRetroStyle.current
    val light = retro.theme == RetroTheme.CassetteLight
    val paper = if (light) Color(0xFFFBF7EC) else Color(0xFF202327)
    val rule = if (light) Color(46, 93, 168).copy(alpha = 0.25f) else Color(227, 226, 230).copy(alpha = 0.16f)
    val ink = if (light) Color(0xFF191C20) else Color(0xFFE3E2E6)
    val spine = MaterialTheme.colorScheme.primary
    val onSpine = MaterialTheme.colorScheme.onPrimary
    val album = state.album.ifBlank { stringResource(R.string.unknown_album) }
    val artist = state.artist.ifBlank { stringResource(R.string.unknown_artist) }
    FixedScale(size) { u ->
        fun d(v: Number) = (v.toFloat() * u).dp
        fun s(v: Number) = (v.toFloat() * u).sp
        Row(Modifier.fillMaxSize()) {
            // Spine: album and artist in caps, rotated -90deg
            BoxWithConstraints(Modifier.width(d(36)).fillMaxHeight().background(spine), contentAlignment = Alignment.Center) {
                Text(
                    "$album · $artist".uppercase(),
                    style = TextStyle(fontSize = s(12), fontWeight = FontWeight.Bold, letterSpacing = 0.16.em),
                    color = onSpine, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.requiredSize(maxHeight, d(36)).rotate(-90f).padding(top = d(9), start = d(12), end = d(12)),
                )
            }
            Box(Modifier.fillMaxSize().background(paper)) {
                Canvas(Modifier.fillMaxSize()) {
                    var y = 28f * u * density
                    while (y < size.toPx()) {
                        drawLine(rule, Offset(0f, y), Offset(this.size.width, y), strokeWidth = u * density)
                        y += 28f * u * density
                    }
                }
                val hand = TextStyle(fontFamily = RetroFonts.Caveat, fontWeight = FontWeight.Bold)
                Column(Modifier.padding(start = d(16), top = d(8), end = d(12))) {
                    Text(album, style = hand.copy(fontSize = s(30), lineHeight = s(34)), color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(artist, style = hand.copy(fontSize = s(22), lineHeight = s(28)), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.jcard_now, state.title),
                        style = hand.copy(fontSize = s(22), lineHeight = s(28)), color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = d(8)),
                    )
                    state.trackNumber?.let {
                        Text(stringResource(R.string.jcard_track, it), style = hand.copy(fontSize = s(22), lineHeight = s(28)), color = ink)
                    }
                    state.year?.let {
                        Text(it.toString(), style = hand.copy(fontSize = s(22), lineHeight = s(28)), color = ink)
                    }
                }
                Thumb(state, d(60), Modifier.align(Alignment.BottomEnd).padding(d(16)))
            }
        }
    }
}

@Composable
private fun Thumb(state: PlayerUiState, size: Dp, modifier: Modifier) {
    val art = state.art
    val shape = RoundedCornerShape(size / 16)
    if (art != null) {
        Image(art, null, contentScale = ContentScale.Crop, modifier = modifier.size(size).clip(shape))
    } else {
        Box(modifier.size(size).background(MaterialTheme.colorScheme.primaryContainer, shape))
    }
}

/** Lays out [content] in 300 design units across [size], with font scaling neutralised. */
@Composable
private fun FixedScale(size: Dp, content: @Composable (u: Float) -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
        Box(Modifier.size(size)) { content(size.value / 300f) }
    }
}

/**
 * Cover screen: the sleeve peeks from behind the turntable, so tapping it brings it forward
 * into a centred overlay (40% scrim) and flips it. Tap again or tap the scrim to return.
 */
@Composable
fun SleeveOverlay(state: PlayerUiState, jCard: Boolean, animate: Boolean, onDismiss: () -> Unit) {
    var flipped by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { flipped = true }
    LaunchedEffect(flipped) {
        if (!flipped) {
            kotlinx.coroutines.delay(if (animate) FLIP_MS.toLong() else 150L)
            onDismiss()
        }
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { flipped = false },
        contentAlignment = Alignment.Center,
    ) {
        val side = minOf(maxWidth, maxHeight) * 0.78f
        FlippableSleeve(
            state, size = side, corner = if (jCard) 4.dp else 6.dp, jCard = jCard, animate = animate,
            flipped = flipped, onToggle = { flipped = !flipped },
        )
    }
}
