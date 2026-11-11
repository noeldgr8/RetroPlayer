package com.retroplayer.feature.player

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.VinylTokens
import com.retroplayer.feature.player.draw.circlePath
import com.retroplayer.feature.player.draw.cssLinear
import com.retroplayer.feature.player.draw.cssRadialAt
import com.retroplayer.feature.player.draw.drawInsetEdge
import com.retroplayer.feature.player.draw.drawShadow

/*
 * Widget chassis and controls. All functions draw in dp units at the current
 * transform; the widget renderer scales by the bitmap density first.
 */

/** Wood plinth filling [size] (corners are clipped by the launcher's widget radius). */
fun DrawScope.drawPlinthChassis(t: VinylTokens, size: Size) {
    val bounds = Rect(Offset.Zero, size)
    drawRect(cssLinear(155f, bounds, listOf(t.plinthStart, t.plinthEnd)), size = size)
    rotate(3f, pivot = bounds.center) {
        var x = -size.height
        while (x < size.width + size.height) {
            drawRect(t.grain, Offset(x, -size.height), Size(2f, size.height * 3f))
            x += t.grainPeriod
        }
    }
    val shape = Path().apply { addRect(bounds) }
    drawInsetEdge(shape, 2f, t.plinthHighlight)
    drawInsetEdge(shape, -4f, t.plinthInnerShade)
}

/** Cassette player body filling [size] with four 8 dp screws inset 8 from each corner. */
fun DrawScope.drawCassetteChassis(t: CassetteTokens, size: Size) {
    drawRect(Brush.verticalGradient(listOf(t.bodyTop, t.bodyBottom), 0f, size.height), size = size)
    val shape = Path().apply { addRect(Rect(Offset.Zero, size)) }
    drawInsetEdge(shape, 2f, t.bodyHighlight)
    drawInsetEdge(shape, -4f, t.bodyInnerShade)
    for (c in listOf(
        Offset(12f, 12f), Offset(size.width - 12f, 12f),
        Offset(12f, size.height - 12f), Offset(size.width - 12f, size.height - 12f),
    )) {
        val r = Rect(c, 4f)
        drawCircle(cssRadialAt(r, 0.35f, 0.30f, 0f to t.screwLight, 1f to t.screwDark), 4f, c)
        drawLine(t.screwDark, c - Offset(2.3f, 0f), c + Offset(2.3f, 0f), strokeWidth = 1f)
    }
}

/** Indicator light, [d] dp, centred at [center]. On: indicatorOn + 0 0 8 2 glow. */
fun DrawScope.drawIndicatorLight(t: CassetteTokens, center: Offset, d: Float, on: Boolean) {
    val r = d / 2f
    if (on) {
        drawShadow(circlePath(center, r + 2f), t.indicatorOn, 8f)
        drawCircle(t.indicatorOn, r, center)
    } else {
        drawCircle(t.indicatorOff, r, center)
        drawInsetEdge(circlePath(center, r), 1.5f, Color.Black.copy(alpha = 0.35f))
    }
}

/** Key well: keyWell fill, corner 12, inset shadow. */
fun DrawScope.drawKeyWell(t: CassetteTokens, rect: Rect) {
    val shape = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(12f))) }
    drawPath(shape, t.keyWell)
    clipPath(shape) {
        drawRect(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent), rect.top, rect.top + 6f),
            topLeft = rect.topLeft,
            size = rect.size,
        )
    }
}

/**
 * Round metal knob of diameter [d] with its shadow, drawn so the knob's top-left is at
 * ([margin], [margin]). Pressed: sinks 2 dp and the icon turns [pressedInk].
 */
fun DrawScope.drawWidgetKnob(
    t: VinylTokens,
    d: Float,
    margin: Float,
    iconPath: String,
    pressed: Boolean,
    pressedInk: Color,
) {
    val dy = if (pressed) 2f else 0f
    val c = Offset(margin + d / 2f, margin + d / 2f + dy)
    val r = d / 2f
    if (pressed) {
        drawCircle(Color.Black.copy(alpha = 0.3f), r, c + Offset(0f, 1f))
    } else {
        drawShadow(circlePath(c, r), Color.Black.copy(alpha = 0.25f), 10f, dy = 5f)
        drawCircle(Color.Black.copy(alpha = 0.28f), r, c + Offset(0f, 3f))
    }
    val rect = Rect(c, r)
    drawCircle(
        cssRadialAt(rect, 0.35f, 0.30f, 0f to t.widgetKnob[0], 0.7f to t.widgetKnob[1], 1f to t.widgetKnob[2]),
        r,
        c,
    )
    if (pressed) drawInsetEdge(circlePath(c, r), 2f, Color.Black.copy(alpha = 0.3f))
    val icon = d * 0.46f
    drawIcon(iconPath, Offset(c.x - icon / 2f, c.y - icon / 2f), icon, if (pressed) pressedInk else t.widgetKnobInk)
}

/**
 * Mechanical key [w] x [h] (face only), drawn with its top-left at ([margin], [margin]).
 * Up: 0 5 0 keyEdge + 0 7 10 shadow. Pressed: sinks 4 dp, 0 1 0 keyEdge, ink [pressedInk].
 */
fun DrawScope.drawWidgetKey(
    t: CassetteTokens,
    w: Float,
    h: Float,
    margin: Float,
    iconPath: String,
    label: String,
    pressed: Boolean,
    pressedInk: Color,
    textMeasurer: TextMeasurer,
) {
    val dy = if (pressed) 4f else 0f
    val face = Rect(Offset(margin, margin + dy), Size(w, h))
    fun shape(r: Rect) = Path().apply {
        addRoundRect(
            RoundRect(
                r,
                topLeft = CornerRadius(8f), topRight = CornerRadius(8f),
                bottomRight = CornerRadius(6f), bottomLeft = CornerRadius(6f),
            ),
        )
    }
    val edge = if (pressed) 1f else 5f
    if (!pressed) drawShadow(shape(face), Color.Black.copy(alpha = 0.28f), 10f, dy = 7f)
    drawPath(shape(face.translate(0f, edge)), t.keyEdge)
    drawPath(shape(face), Brush.verticalGradient(listOf(t.keyFaceTop, t.keyFaceBottom), face.top, face.bottom))

    val ink = if (pressed) pressedInk else t.keyInk
    val text = textMeasurer.measure(
        label,
        TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.12.em),
        density = Density(1f),
    )
    val icon = 18f
    val gap = 2f
    val total = icon + gap + text.size.height
    val top = face.top + (h - total) / 2f
    drawIcon(iconPath, Offset(face.center.x - icon / 2f, top), icon, ink)
    drawText(text, color = ink, topLeft = Offset(face.center.x - text.size.width / 2f, top + icon + gap))
}

/** A 24-viewport icon path filled at [topLeft], [size] dp square. */
fun DrawScope.drawIcon(pathData: String, topLeft: Offset, size: Float, color: Color) {
    val path = PathParser().parsePathString(pathData).toPath()
    translate(topLeft.x, topLeft.y) {
        scale(size / 24f, pivot = Offset.Zero) { drawPath(path, color) }
    }
}
