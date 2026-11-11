package com.retroplayer.feature.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import com.retroplayer.core.theme.RetroMotion
import kotlinx.coroutines.flow.first

/*
 * Rotation is driven by accumulating angle += dt * angularVelocity on each frame, where the
 * velocity itself is animated. That gives smooth spin-up / spin-down and lets reel rates change
 * continuously without the snap a restartable infiniteRepeatable would cause (DESIGN.md 9).
 *
 * Hold these states in a ViewModel so the angle survives fold / unfold and recreation.
 */

internal val TonearmEasing = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)
private val EaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
private const val VINYL_DEG_PER_MS = 360f / RetroMotion.VINYL_REVOLUTION_MS
private const val REEL_BASE_DEG_PER_MS = 360f / RetroMotion.REEL_BASE_REVOLUTION_MS

/** Record rotation. */
@Stable
class SpinState {
    var angle by mutableFloatStateOf(0f)
        internal set

    /** Degrees per millisecond. */
    internal val velocity = Animatable(0f)
}

/** Cassette reel rotation (both reels share one transport speed, each turns at 28 / r). */
@Stable
class ReelState {
    var left by mutableFloatStateOf(0f)
        internal set
    var right by mutableFloatStateOf(0f)
        internal set

    /** 0 = stopped, 1 = normal play speed. */
    internal val speed = Animatable(0f)
}

@Composable
fun rememberSpinState(): SpinState = remember { SpinState() }

@Composable
fun rememberReelState(): ReelState = remember { ReelState() }

@Composable
internal fun VinylSpinEffect(state: SpinState, playing: Boolean, animate: Boolean) {
    val target = if (playing && animate) VINYL_DEG_PER_MS else 0f
    val spec: AnimationSpec<Float> = when {
        !animate -> snap()
        target > 0f -> tween(RetroMotion.PLATTER_SPIN_UP_MS, easing = EaseIn)
        else -> tween(RetroMotion.PLATTER_SPIN_DOWN_MS, easing = LinearOutSlowInEasing)
    }
    LaunchedEffect(state, target, animate) { state.velocity.animateTo(target, spec) }
    LaunchedEffect(state) {
        frameLoop(isMoving = { state.velocity.value != 0f || state.velocity.isRunning }) { dt ->
            state.angle = (state.angle + dt * state.velocity.value) % 360f
        }
    }
}

@Composable
internal fun ReelEffect(
    state: ReelState,
    playing: Boolean,
    animate: Boolean,
    leftRadius: Float,
    rightRadius: Float,
) {
    val radii by rememberUpdatedState(leftRadius to rightRadius)
    val running = playing && animate
    LaunchedEffect(state, running, animate) {
        when {
            running -> state.speed.animateTo(
                1f, tween(RetroMotion.CASSETTE_START_MS, easing = FastOutSlowInEasing),
            )
            // Stop with a small overshoot, like a capstan settling.
            animate -> state.speed.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
            else -> state.speed.snapTo(0f)
        }
    }
    LaunchedEffect(state) {
        frameLoop(isMoving = { state.speed.value != 0f || state.speed.isRunning }) { dt ->
            val (rl, rr) = radii
            val base = dt * REEL_BASE_DEG_PER_MS * state.speed.value * RetroMotion.REEL_RATE_CONSTANT
            // Tape runs left to right along the bottom, so both reels turn counter-clockwise.
            state.left = (state.left - base / rl) % 360f
            state.right = (state.right - base / rr) % 360f
        }
    }
}

/** Runs [onFrame] with the frame delta only while [isMoving]; idles (no frames) otherwise. */
private suspend fun frameLoop(isMoving: () -> Boolean, onFrame: (dtMs: Float) -> Unit) {
    while (true) {
        snapshotFlow { isMoving() }.first { it }
        var last = -1L
        while (isMoving()) {
            withFrameMillis { now ->
                if (last >= 0) onFrame((now - last).toFloat())
                last = now
            }
        }
    }
}
