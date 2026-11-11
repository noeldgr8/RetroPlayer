package com.retroplayer.feature.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.retroplayer.core.theme.RetroMotion
import com.retroplayer.core.theme.ObjectLook
import com.retroplayer.core.theme.VinylTokens
import com.retroplayer.feature.player.draw.circlePath
import com.retroplayer.feature.player.draw.cssLinear
import com.retroplayer.feature.player.draw.cssRadialAt
import com.retroplayer.feature.player.draw.drawInsetEdge
import com.retroplayer.feature.player.draw.drawShadow
import com.retroplayer.feature.player.draw.roundRectPath

private const val BASE = 400f
internal val RecordCenter = Offset(170f, 190f)
internal val Pivot = Offset(350f, 60f)

/** Tonearm angle while playing: 22deg at the first groove, creeping to 30deg by the end. */
private fun playAngle(progress: Float) =
    RetroMotion.TONEARM_PLAY_DEG + 8f * progress.coerceIn(0f, 1f)

/**
 * Top-down turntable, drawn in a 400 x 400 base and scaled uniformly.
 * Width decides size; height = width.
 *
 * Layers: static base (cached) -> rotating record (graphicsLayer rotation only) -> static
 * sheen -> rotating tonearm -> static top details. Only the two rotating layers update per frame.
 *
 * Decorative: TalkBack skips it; title, artist and controls carry the information.
 */
@Composable
fun Turntable(
    tokens: VinylTokens,
    art: ImageBitmap?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    idle: Boolean = false,
    bare: Boolean = false,
    progress: Float = 0f,
    animate: Boolean = true,
    spin: SpinState = rememberSpinState(),
    labelColor: Color = MaterialTheme.colorScheme.primaryContainer,
    look: ObjectLook = ObjectLook.Default,
) {
    val playing = isPlaying && !idle
    VinylSpinEffect(spin, playing, animate)
    val armDown by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = if (animate) tween(RetroMotion.TONEARM_MOVE_MS, easing = TonearmEasing) else snap(),
        label = "tonearm",
    )
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier.aspectRatio(1f).clearAndSetSemantics { }) {
        val u = maxWidth / BASE

        // 1, 2, 5, 7, 8: plinth, platter, (idle mat), glow, tonearm base, counterweight
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val s = size.width / BASE
                onDrawBehind { scale(s, Offset.Zero) { drawTurntableBase(tokens, bare, idle) } }
            },
        )

        if (!idle) {
            // 3, 3a: record + centre label, rotating together
            Spacer(
                Modifier
                    .offset(u * 26f, u * 46f)
                    .size(u * 288f)
                    .graphicsLayer { rotationZ = spin.angle }
                    .drawWithCache {
                        val s = size.width / 288f
                        onDrawBehind { scale(s, Offset.Zero) { drawRecordLook(tokens, art, labelColor, look) } }
                    },
            )
            // 4: static sheen, which is what sells the rotation
            Spacer(
                Modifier
                    .offset(u * 26f, u * 46f)
                    .size(u * 288f)
                    .drawWithCache {
                        val brush = Brush.sweepGradient(*SheenStops, center = size.center)
                        onDrawBehind { drawCircle(brush, radius = size.minDimension / 2f) }
                    },
            )
        }

        // 9, 9a: tonearm and headshell, rotating about the pivot
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = armDown * playAngle(progress)
                    transformOrigin = TransformOrigin(Pivot.x / BASE, Pivot.y / BASE)
                }
                .drawWithCache {
                    val s = size.width / BASE
                    onDrawBehind { scale(s, Offset.Zero) { drawTonearm(tokens) } }
                },
        )

        // 6, 10, 11, 12: spindle, pivot cap, speed selector, start knob
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val s = size.width / BASE
                val label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val t33 = textMeasurer.measure("33", label, density = Density(1f))
                val t45 = textMeasurer.measure("45", label, density = Density(1f))
                onDrawBehind { scale(s, Offset.Zero) { drawTurntableTop(tokens, bare, t33, t45) } }
            },
        )
    }
}

internal val SheenStops: Array<Pair<Float, Color>> = run {
    // CSS conic stops (0deg = up) converted to sweepGradient (0 = 3 o'clock), wrapped at 360.
    val w = Color.White
    arrayOf(
        0f to w.copy(alpha = 0.016f),
        5f / 360f to w.copy(alpha = 0f),
        110f / 360f to w.copy(alpha = 0f),
        145f / 360f to w.copy(alpha = 0.09f),
        185f / 360f to w.copy(alpha = 0f),
        290f / 360f to w.copy(alpha = 0f),
        325f / 360f to w.copy(alpha = 0.13f),
        1f to w.copy(alpha = 0.016f),
    )
}

internal fun DrawScope.drawTurntableBase(t: VinylTokens, bare: Boolean, idle: Boolean) {
    if (!bare) {
        val bounds = Rect(0f, 0f, BASE, BASE)
        val plinth = roundRectPath(bounds, 22f)
        drawShadow(plinth, t.plinthDropShadow, t.plinthDropBlur, dy = 20f)
        drawPath(plinth, cssLinear(155f, bounds, listOf(t.plinthStart, t.plinthEnd)))
        // Wood grain: repeating-linear-gradient(93deg, grain 0 2, transparent 2 period)
        clipPath(plinth) {
            rotate(3f, pivot = bounds.center) {
                var x = -120f
                while (x < BASE + 120f) {
                    drawRect(t.grain, Offset(x, -120f), Size(2f, BASE + 240f))
                    x += t.grainPeriod
                }
            }
        }
        drawInsetEdge(plinth, 2f, t.plinthHighlight)
        drawInsetEdge(plinth, -4f, t.plinthInnerShade)
    }

    // Platter
    drawShadow(circlePath(RecordCenter, 150f), Color.Black.copy(alpha = 0.35f), 18f, dy = 8f)
    drawCircle(
        Brush.radialGradient(
            0f to t.platterCenter, 0.60f to t.platterCenter,
            0.61f to t.platterMid, 0.97f to t.platterMid,
            0.98f to t.platterEdge, 1f to t.platterEdge,
            center = RecordCenter, radius = 150f,
        ),
        radius = 150f,
        center = RecordCenter,
    )
    drawCircle(Color.White.copy(alpha = 0.16f), radius = 149f, center = RecordCenter, style = Stroke(2f))

    if (idle) {
        drawCircle(t.idleMat, radius = 126f, center = RecordCenter)
        var r = 6f
        while (r < 126f) {
            drawCircle(t.idleMatLine, radius = r, center = RecordCenter, style = Stroke(1f))
            r += 7f
        }
    } else if (t.grooveGlow != null) {
        drawShadow(circlePath(RecordCenter, 150f), t.grooveGlow!!, 40f)
    }

    // Tonearm base and counterweight (they never overlap the record)
    drawShadow(circlePath(Pivot, 28f), Color.Black.copy(alpha = 0.3f), 12f, dy = 6f)
    val baseRect = Rect(Pivot, 28f)
    drawCircle(
        cssRadialAt(baseRect, 0.35f, 0.30f, 0f to t.tonearmBase[0], 0.7f to t.tonearmBase[1], 1f to t.tonearmBase[2]),
        radius = 28f,
        center = Pivot,
    )
    val cw = Rect(339f, 12f, 361f, 42f)
    drawRoundRect(cssLinear(90f, cw, t.counterweight), cw.topLeft, cw.size, CornerRadius(5f))
}

/** Drawn in record-local units: 288 x 288, centre (144, 144). */
internal fun DrawScope.drawRecord(t: VinylTokens, art: ImageBitmap?, labelColor: Color) {
    val c = Offset(144f, 144f)
    drawCircle(t.record, radius = 144f, center = c)
    // Grooves: repeating rings grooveA 0..1.2, grooveB 1.7..3.2
    var r = 60f
    while (r < 143f) {
        drawCircle(t.grooveA, radius = r + 0.6f, center = c, style = Stroke(1.2f))
        drawCircle(t.grooveB, radius = r + 2.45f, center = c, style = Stroke(1.5f))
        r += 3.2f
    }

    // Centre label: album art cropped to a circle (fallback: solid tint with a subtle ring)
    val label = circlePath(c, 60f)
    if (art != null) {
        clipPath(label) { drawCenterCrop(art, Rect(c, 60f)) }
    } else {
        drawCircle(labelColor, radius = 60f, center = c)
        drawCircle(Color.Black.copy(alpha = 0.12f), radius = 40f, center = c, style = Stroke(1f))
    }
    drawCircle(
        Brush.radialGradient(
            0f to Color.Transparent, (57f / 60f) to Color.Transparent, 1f to Color.Black.copy(alpha = 0.28f),
            center = c, radius = 60f,
        ),
        radius = 60f,
        center = c,
    )
    drawCircle(Color.Black.copy(alpha = 0.4f), radius = 58.5f, center = c, style = Stroke(3f))
    drawCircle(Color(0xFF111111), radius = 5f, center = c)
    drawCircle(Color.White.copy(alpha = 0.55f), radius = 6.5f, center = c, style = Stroke(1f))
}

private fun DrawScope.drawCenterCrop(image: ImageBitmap, dst: Rect) {
    val side = minOf(image.width, image.height)
    drawImage(
        image = image,
        srcOffset = IntOffset((image.width - side) / 2, (image.height - side) / 2),
        srcSize = IntSize(side, side),
        dstOffset = IntOffset(dst.left.toInt(), dst.top.toInt()),
        dstSize = IntSize(dst.width.toInt(), dst.height.toInt()),
        filterQuality = FilterQuality.Medium,
    )
}

internal fun DrawScope.drawTonearm(t: VinylTokens) {
    val arm = Rect(347f, 60f, 353f, 300f)
    val armPath = roundRectPath(arm, 3f)
    val head = Rect(339f, 282f, 361f, 314f)
    val shadow = Color.Black.copy(alpha = 0.28f)

    drawShadow(armPath, shadow, 5f, dx = 3f, dy = 4f)
    rotate(-16f, pivot = head.center) { drawShadow(roundRectPath(head, 4f), shadow, 5f, dx = 3f, dy = 4f) }

    drawPath(
        armPath,
        Brush.linearGradient(
            0f to t.tonearm[0], 0.45f to t.tonearm[1], 1f to t.tonearm[2],
            start = Offset(arm.left, 0f), end = Offset(arm.right, 0f),
        ),
    )
    rotate(-16f, pivot = head.center) {
        drawRoundRect(t.headshell, head.topLeft, head.size, CornerRadius(4f))
    }
}

internal fun DrawScope.drawTurntableTop(
    t: VinylTokens,
    bare: Boolean,
    t33: TextLayoutResult,
    t45: TextLayoutResult,
) {
    // Spindle
    drawCircle(
        Brush.radialGradient(listOf(Color.White, Color(0xFF8E949B)), center = RecordCenter - Offset(2f, 2f), radius = 8f),
        radius = 6f,
        center = RecordCenter,
    )
    // Pivot cap
    drawCircle(t.pivotCap, radius = 8f, center = Pivot)

    if (bare) return

    // Speed selector: "33" selected chip, "45" at 60%
    val chipH = 20f
    val chip = Rect(26f, 358f, 26f + t33.size.width + 12f, 358f + chipH)
    drawRoundRect(Color.Black.copy(alpha = 0.12f), chip.topLeft, chip.size, CornerRadius(6f))
    drawText(t33, color = t.plinthText, topLeft = Offset(chip.left + 6f, chip.top + (chipH - t33.size.height) / 2f))
    drawText(
        t45,
        color = t.plinthText,
        alpha = 0.6f,
        topLeft = Offset(chip.right + 6f, chip.top + (chipH - t45.size.height) / 2f),
    )

    // Start knob
    val knob = Rect(334f, 342f, 376f, 384f)
    drawShadow(circlePath(knob.center, 21f), Color.Black.copy(alpha = 0.25f), 6f, dy = 3f)
    drawCircle(
        cssRadialAt(knob, 0.35f, 0.30f, 0f to t.startKnob[0], 1f to t.startKnob[1]),
        radius = 21f,
        center = knob.center,
    )
}
