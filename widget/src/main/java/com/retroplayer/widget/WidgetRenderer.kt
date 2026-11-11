package com.retroplayer.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.core.theme.VinylTokens
import com.retroplayer.feature.player.IconPaths
import com.retroplayer.feature.player.drawCassetteChassis
import com.retroplayer.feature.player.drawCassetteStill
import com.retroplayer.feature.player.drawIndicatorLight
import com.retroplayer.feature.player.drawKeyWell
import com.retroplayer.feature.player.drawPlinthChassis
import com.retroplayer.feature.player.drawTurntableStill
import com.retroplayer.feature.player.drawWidgetKey
import com.retroplayer.feature.player.drawWidgetKnob
import kotlin.math.roundToInt

/**
 * Pre-renders widget art with the app's own drawing code (DESIGN.md 8): one background
 * bitmap per widget (chassis + object + indicator + key well) and small up / pressed bitmaps
 * per knob or key. Text stays live in the Glance layout.
 *
 * Bitmaps render at up to 2x density to stay well under the RemoteViews bitmap budget.
 */
class WidgetRenderer(private val context: Context) {

    private val scale = context.resources.displayMetrics.density.coerceAtMost(2f)
    private val measurer = TextMeasurer(
        defaultFontFamilyResolver = createFontFamilyResolver(context),
        defaultDensity = Density(1f),
        defaultLayoutDirection = LayoutDirection.Ltr,
    )
    private val controlCache = LruCache<String, Bitmap>(48)

    fun background(
        layout: WidgetLayout,
        theme: RetroTheme,
        art: ImageBitmap?,
        progress: Float,
        labelColor: Color,
    ): Bitmap = render(layout.wDp, layout.hDp) {
        val playing = layout.mode == WidgetMode.Playing
        val idle = layout.mode == WidgetMode.Empty
        val (ox, oy, ow) = layout.obj
        when (val t = theme.playerTokens) {
            is VinylTokens -> {
                drawPlinthChassis(t, Size(layout.wDp, layout.hDp))
                translate(ox, oy) {
                    scale(ow / 400f, Offset.Zero) {
                        drawTurntableStill(
                            t, art, playing, labelColor, measurer,
                            idle = idle, bare = true, progress = progress,
                        )
                    }
                }
            }
            is CassetteTokens -> {
                drawCassetteChassis(t, Size(layout.wDp, layout.hDp))
                translate(ox, oy) {
                    scale(ow / 400f, Offset.Zero) {
                        drawCassetteStill(t, if (idle) 0f else progress, playing, measurer, bare = true)
                    }
                }
                layout.indicator?.let { (cx, cy, d) -> drawIndicatorLight(t, Offset(cx, cy), d, on = playing) }
                layout.well?.let { w ->
                    drawKeyWell(t, androidx.compose.ui.geometry.Rect(Offset(w.x, w.y), Size(w.w, w.h)))
                }
            }
        }
    }

    /** Knob or key bitmap including its shadow margin. */
    fun control(
        spot: ControlSpot,
        theme: RetroTheme,
        pressed: Boolean,
        playing: Boolean,
        pressedInk: Color,
        label: String,
    ): Bitmap {
        val key = "${theme.id}|${spot.control}|${spot.face.w.roundToInt()}x${spot.face.h.roundToInt()}|$pressed|$playing|${pressedInk.value}|$label"
        controlCache.get(key)?.let { return it }
        val m = spot.margin
        val bmp = render(spot.face.w + 2 * m, spot.face.h + 2 * m) {
            when (val t = theme.playerTokens) {
                is VinylTokens -> drawWidgetKnob(
                    t, spot.face.w, m,
                    iconPath = vinylIcon(spot.control, playing),
                    pressed = pressed,
                    pressedInk = pressedInk,
                )
                is CassetteTokens -> drawWidgetKey(
                    t, spot.face.w, spot.face.h, m,
                    iconPath = cassetteIcon(spot.control),
                    label = label,
                    pressed = pressed,
                    pressedInk = pressedInk,
                    textMeasurer = measurer,
                )
            }
        }
        controlCache.put(key, bmp)
        return bmp
    }

    private fun vinylIcon(c: Control, playing: Boolean) = when (c) {
        Control.PlayPause -> if (playing) IconPaths.PAUSE else IconPaths.PLAY
        Control.Previous -> IconPaths.SKIP_PREVIOUS
        Control.Next -> IconPaths.SKIP_NEXT
        Control.Play -> IconPaths.PLAY
        Control.Stop -> IconPaths.STOP
    }

    private fun cassetteIcon(c: Control) = when (c) {
        Control.Previous -> IconPaths.FAST_REWIND
        Control.Next -> IconPaths.FAST_FORWARD
        Control.Stop -> IconPaths.STOP
        Control.Play, Control.PlayPause -> IconPaths.PLAY
    }

    /** Renders [block] (in dp units) into a bitmap of [wDp] x [hDp] dp. */
    private fun render(wDp: Float, hDp: Float, block: DrawScope.() -> Unit): Bitmap {
        val w = (wDp * scale).roundToInt().coerceAtLeast(1)
        val h = (hDp * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(
            Density(1f),
            LayoutDirection.Ltr,
            Canvas(bitmap.asImageBitmap()),
            Size(w.toFloat(), h.toFloat()),
        ) {
            scale(scale, Offset.Zero) { block() }
        }
        return bitmap
    }
}
