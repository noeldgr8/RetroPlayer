package com.retroplayer.widget

import com.retroplayer.core.theme.PlayerStyle
import kotlin.math.min

/** The three designed sizes (DESIGN.md 8), in dp. */
enum class WidgetSize(val w: Float, val h: Float) {
    Small(170f, 170f),
    Medium(360f, 170f),
    Large(360f, 360f);

    companion object {
        /** Nearest designed layout for the widget's real size. */
        fun forSize(wDp: Float, hDp: Float): WidgetSize = when {
            wDp < 250f -> Small
            hDp < 250f -> Medium
            else -> Large
        }
    }
}

enum class WidgetMode { Playing, Paused, Empty }

enum class Control(val vinylIcon: String, val labelRes: Int) {
    Previous(com.retroplayer.feature.player.IconPaths.SKIP_PREVIOUS, R.string.key_rew),
    PlayPause(com.retroplayer.feature.player.IconPaths.PLAY, R.string.key_play),
    Next(com.retroplayer.feature.player.IconPaths.SKIP_NEXT, R.string.key_ff),
    Play(com.retroplayer.feature.player.IconPaths.PLAY, R.string.key_play),
    Stop(com.retroplayer.feature.player.IconPaths.STOP, R.string.key_stop),
}

data class DpRect(val x: Float, val y: Float, val w: Float, val h: Float)

/** A control's face in widget dp, plus the transparent margin baked around it for shadows. */
data class ControlSpot(val control: Control, val face: DpRect, val margin: Float)

/**
 * Positions from DESIGN.md 8.1 / 8.2 at the design size, stretched to the real widget size:
 * positions scale per axis, the object scales uniformly, controls keep their dp size
 * (touch targets). Every coordinate below is in the real widget's dp.
 */
class WidgetLayout(
    val style: PlayerStyle,
    val size: WidgetSize,
    val wDp: Float,
    val hDp: Float,
    val mode: WidgetMode,
) {
    private val sx = wDp / size.w
    private val sy = hDp / size.h
    private val s = min(sx, sy)
    private fun x(v: Float) = v * sx
    private fun y(v: Float) = v * sy
    private val empty = mode == WidgetMode.Empty
    private val vinyl = style == PlayerStyle.Vinyl

    /** Object placement: x, y, width (height follows the object's aspect). */
    val obj: Triple<Float, Float, Float> = when {
        vinyl && empty && size == WidgetSize.Small -> Triple(x(30f), y(0f), 110f * s)
        vinyl -> when (size) {
            WidgetSize.Small -> Triple(x(4f), y(-2f), 150f * s)
            WidgetSize.Medium -> Triple(x(4f), y(-4f), 170f * s)
            WidgetSize.Large -> Triple(x(14f), y(-4f), 310f * s)
        }
        else -> when (size) {
            WidgetSize.Small -> Triple(x(0f), y(2f), 170f * s)
            WidgetSize.Medium -> Triple(x(0f), y(-2f), 226f * s)
            WidgetSize.Large -> Triple(x(0f), y(-4f), 380f * s)
        }
    }

    /** Title / artist column: left, top, right inset. Null = no text at this size. */
    val text: DpRect? = when (size) {
        WidgetSize.Small -> null
        WidgetSize.Medium -> if (vinyl) DpRect(x(180f), y(20f), wDp - x(180f) - 14f, 0f)
        else DpRect(x(216f), y(22f), wDp - x(216f) - 14f, 0f)
        WidgetSize.Large -> if (vinyl) DpRect(x(22f), y(270f), if (empty) x(150f) else x(150f), 0f)
        else DpRect(x(22f), y(180f), wDp - x(22f) - 60f, 0f)
    }

    /** Progress bar (Large only, not when empty). */
    val progress: DpRect? = if (size == WidgetSize.Large && !empty) {
        if (vinyl) DpRect(x(22f), y(338f), wDp - x(22f) - x(22f), 4f)
        else DpRect(x(22f), y(232f), wDp - x(22f) - x(22f), 4f)
    } else null

    /** Cassette indicator light centre and diameter. */
    val indicator: Triple<Float, Float, Float>? = if (vinyl) null else when (size) {
        WidgetSize.Small -> Triple(x(146f) + 5f, y(84f) + 5f, 10f)
        WidgetSize.Medium -> Triple(x(218f) + 5f, y(80f) + 5f, 10f)
        WidgetSize.Large -> Triple(x(324f) + 5f, y(190f) + 5f, 10f)
    }

    /** "Open Spotify" button in the empty state. */
    val openButton: DpRect? = if (!empty) null else when (size) {
        WidgetSize.Small -> DpRect(12f, y(if (vinyl) 110f else 104f), wDp - 24f, 48f)
        WidgetSize.Medium -> text!!.let { DpRect(it.x, hDp - 48f - 14f, it.w, 48f) }
        WidgetSize.Large -> if (vinyl) DpRect(x(190f), y(282f), wDp - x(190f) - 16f, 48f)
        else DpRect(x(80f), y(280f), wDp - x(80f) - x(80f), 48f)
    }

    /** Cassette key well. */
    val well: DpRect? = if (vinyl || empty) null else {
        val keyH = if (size == WidgetSize.Large) 72f else 48f
        when (size) {
            WidgetSize.Small -> DpRect(12f, y(98f), wDp - 24f, keyH + 12f)
            WidgetSize.Medium -> DpRect(12f, y(104f), wDp - 24f, keyH + 12f)
            WidgetSize.Large -> DpRect(14f, y(250f), wDp - 28f, keyH + 12f)
        }
    }

    val controls: List<ControlSpot> = when {
        empty -> emptyList()
        vinyl -> vinylControls()
        else -> cassetteKeys()
    }

    private fun vinylControls(): List<ControlSpot> {
        val m = 8f
        fun knob(c: Control, x: Float, y: Float, d: Float) = ControlSpot(c, DpRect(x, y, d, d), m)
        return when (size) {
            WidgetSize.Small -> listOf(knob(Control.PlayPause, x(110f), y(110f), 52f))
            WidgetSize.Medium, WidgetSize.Large -> {
                val startX = if (size == WidgetSize.Medium) x(176f) else wDp - 16f - (48f + 8f + 52f + 8f + 48f)
                val top = if (size == WidgetSize.Medium) y(104f) else y(266f)
                listOf(
                    knob(Control.Previous, startX, top + 2f, 48f),
                    knob(Control.PlayPause, startX + 56f, top, 52f),
                    knob(Control.Next, startX + 116f, top + 2f, 48f),
                )
            }
        }
    }

    private fun cassetteKeys(): List<ControlSpot> {
        val w = well!!
        val keys = if (size == WidgetSize.Small) {
            listOf(Control.Play, Control.Stop)
        } else {
            listOf(Control.Previous, Control.Play, Control.Stop, Control.Next)
        }
        val gap = 6f
        val kw = (w.w - 12f - gap * (keys.size - 1)) / keys.size
        val kh = w.h - 12f
        return keys.mapIndexed { i, c ->
            ControlSpot(c, DpRect(w.x + 6f + i * (kw + gap), w.y + 6f, kw, kh), margin = 8f)
        }
    }
}
