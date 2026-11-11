package com.retroplayer.feature.player.draw

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * Helpers that translate the CSS-style tokens (gradients, box-shadows) into Compose drawing.
 * All player drawing happens in base units (turntable 400 x 400, cassette 400 x 260) inside a
 * uniform scale() transform, so values below are in base units too.
 */

/** CSS linear-gradient(angle, ...) over [rect]. 0deg points up, 90deg points right. */
internal fun cssLinear(angleDeg: Float, rect: Rect, vararg stops: Pair<Float, Color>): Brush {
    val rad = Math.toRadians(angleDeg.toDouble())
    val dx = sin(rad).toFloat()
    val dy = -cos(rad).toFloat()
    val half = (abs(rect.width * dx) + abs(rect.height * dy)) / 2f
    val c = rect.center
    return Brush.linearGradient(
        *stops,
        start = Offset(c.x - dx * half, c.y - dy * half),
        end = Offset(c.x + dx * half, c.y + dy * half),
    )
}

/** Evenly spaced stops for a CSS gradient written without explicit positions. */
internal fun cssLinear(angleDeg: Float, rect: Rect, colors: List<Color>): Brush {
    val n = (colors.size - 1).coerceAtLeast(1)
    return cssLinear(angleDeg, rect, *colors.mapIndexed { i, c -> i.toFloat() / n to c }.toTypedArray())
}

/**
 * CSS radial(at x% y%) with the default farthest-corner extent, over [rect].
 */
internal fun cssRadialAt(
    rect: Rect,
    xFraction: Float,
    yFraction: Float,
    vararg stops: Pair<Float, Color>,
): Brush {
    val cx = rect.left + rect.width * xFraction
    val cy = rect.top + rect.height * yFraction
    val fx = maxOf(cx - rect.left, rect.right - cx)
    val fy = maxOf(cy - rect.top, rect.bottom - cy)
    val r = kotlin.math.sqrt(fx * fx + fy * fy)
    return Brush.radialGradient(*stops, center = Offset(cx, cy), radius = r)
}

internal fun roundRectPath(rect: Rect, radius: Float): Path =
    Path().apply { addRoundRect(RoundRect(rect, CornerRadius(radius))) }

internal fun circlePath(center: Offset, radius: Float): Path =
    Path().apply { addOval(Rect(center, radius)) }

/**
 * Blurred drop shadow (CSS box-shadow dx dy blur [spread] color). Uses a native BlurMaskFilter,
 * which respects the current transform, so [blur] is in base units.
 */
internal fun DrawScope.drawShadow(
    shape: Path,
    color: Color,
    blur: Float,
    dx: Float = 0f,
    dy: Float = 0f,
) {
    if (color.alpha == 0f) return
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            // CSS blur radius ~ 2 sigma; Android radius r gives sigma ~ 0.577 r + 0.5.
            if (blur > 0f) maskFilter = BlurMaskFilter(blur * 0.85f, BlurMaskFilter.Blur.NORMAL)
        }
        val native = canvas.nativeCanvas
        native.save()
        native.translate(dx, dy)
        native.drawPath(shape.asAndroidPath(), paint)
        native.restore()
    }
}

/**
 * CSS "inset 0 dy 0 color": paints the sliver of [shape] not covered by itself shifted by [dy].
 * Positive dy gives a top edge (highlight), negative a bottom edge (shade).
 */
internal fun DrawScope.drawInsetEdge(shape: Path, dy: Float, color: Color) {
    val shifted = Path().apply { addPath(shape, Offset(0f, dy)) }
    val edge = Path().apply { op(shape, shifted, PathOperation.Difference) }
    drawPath(edge, color)
}
