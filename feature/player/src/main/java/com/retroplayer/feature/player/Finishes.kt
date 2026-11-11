package com.retroplayer.feature.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retroplayer.core.theme.AlbumPalette
import com.retroplayer.core.theme.CassetteShell
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.LabelStyle
import com.retroplayer.core.theme.ObjectLook
import com.retroplayer.core.theme.RetroFonts
import com.retroplayer.core.theme.VinylFinish
import com.retroplayer.core.theme.VinylTokens
import com.retroplayer.core.theme.printedInk
import com.retroplayer.feature.player.draw.circlePath
import com.retroplayer.feature.player.draw.cssRadialAt
import com.retroplayer.feature.player.draw.drawShadow
import com.retroplayer.feature.player.draw.roundRectPath

/*
 * EN-01 record finishes and cassette styles. Record functions draw in
 * record-local units (288 x 288, centre 144, 144); cassette functions in shell-local units.
 */

private val RC = Offset(144f, 144f)
private const val RR = 144f

/** The rotating record for [look]. Classic is exactly the v1 record. */
internal fun DrawScope.drawRecordLook(t: VinylTokens, art: ImageBitmap?, labelColor: Color, look: ObjectLook) {
    val p = look.colors
    when (look.finish) {
        VinylFinish.Classic -> drawRecord(t, art, labelColor)
        VinylFinish.Picture -> drawPictureDisc(art, labelColor)
        VinylFinish.Translucent -> {
            // radial(tint@70% 0-55%, tint@90% 100%), whole layer at 0.92 so the platter shows through
            drawCircle(
                Brush.radialGradient(
                    0f to p.tint.copy(alpha = 0.70f * 0.92f),
                    0.55f to p.tint.copy(alpha = 0.70f * 0.92f),
                    1f to p.tint.copy(alpha = 0.90f * 0.92f),
                    center = RC, radius = RR,
                ),
                RR, RC,
            )
            drawGrooveOverlay()
            drawRoundLabel(art, labelColor)
        }
        VinylFinish.Marble -> {
            clipPath(circlePath(RC, RR)) {
                val c = Offset(288f * 0.44f, 288f * 0.56f)
                // Conic "from 20deg" (CSS 0 = up) == sweep start at -70deg (sweep 0 = 3 o'clock).
                rotate(-70f, pivot = c) {
                    drawRect(
                        Brush.sweepGradient(
                            0f to p.tint, 0.14f to p.light, 0.27f to p.tint, 0.41f to p.dark,
                            0.55f to p.light, 0.68f to p.tint, 0.84f to p.dark, 1f to p.tint,
                            center = c,
                        ),
                        topLeft = Offset(-300f, -300f),
                        size = Size(900f, 900f),
                    )
                }
                drawRect(
                    cssRadialAt(
                        Rect(0f, 0f, 288f, 288f), 0.30f, 0.28f,
                        0f to Color.White.copy(alpha = 0.35f), 0.38f to Color.White.copy(alpha = 0f),
                    ),
                    size = Size(288f, 288f),
                )
            }
            drawGrooveOverlay()
            drawRoundLabel(art, labelColor)
        }
        VinylFinish.Splatter -> {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF1A1A1A), Color(0xFF101010)), RC, RR), RR, RC)
            clipPath(circlePath(RC, RR)) { drawSplatter(p) }
            drawGrooveOverlay()
            drawRoundLabel(art, labelColor)
        }
    }
}

private val SplatterSpots = listOf(
    Triple(22, 30, 9), Triple(70, 18, 6), Triple(80, 62, 11), Triple(35, 74, 7), Triple(58, 44, 4),
    Triple(14, 55, 5), Triple(66, 82, 6), Triple(44, 12, 5), Triple(88, 40, 4), Triple(28, 48, 3),
    Triple(52, 88, 4), Triple(76, 30, 3), Triple(18, 82, 4), Triple(60, 64, 3),
)

private fun DrawScope.drawSplatter(p: AlbumPalette) {
    val colors = listOf(p.light, p.tint, Color(0xFFF4F1EA))
    SplatterSpots.forEachIndexed { i, (x, y, r) ->
        // Jitter by the album hash so albums differ, deterministically.
        val jx = (((p.hash ushr (i * 2)) and 7) - 3.5f) * 2f
        val jy = (((p.hash ushr (i * 2 + 1)) and 7) - 3.5f) * 2f
        drawCircle(colors[i % 3], r.toFloat(), Offset(288f * x / 100f + jx, 288f * y / 100f + jy))
    }
}

/** repeating-radial(rgba(0,0,0,.22) 0-1.2u, rgba(255,255,255,.05) 1.7-3.2u) over the disc. */
private fun DrawScope.drawGrooveOverlay() {
    val a = Color.Black.copy(alpha = 0.22f)
    val b = Color.White.copy(alpha = 0.05f)
    var r = 60f
    while (r < 143f) {
        drawCircle(a, radius = r + 0.6f, center = RC, style = Stroke(1.2f))
        drawCircle(b, radius = r + 2.45f, center = RC, style = Stroke(1.5f))
        r += 3.2f
    }
}

/** The v1 round label: album art cropped to a circle, or a tint with a ring. */
private fun DrawScope.drawRoundLabel(art: ImageBitmap?, labelColor: Color) {
    if (art != null) {
        clipPath(circlePath(RC, 60f)) { drawArtCrop(art, Rect(RC, 60f)) }
    } else {
        drawCircle(labelColor, radius = 60f, center = RC)
        drawCircle(Color.Black.copy(alpha = 0.12f), radius = 40f, center = RC, style = Stroke(1f))
    }
    drawCircle(
        Brush.radialGradient(
            0f to Color.Transparent, (57f / 60f) to Color.Transparent, 1f to Color.Black.copy(alpha = 0.28f),
            center = RC, radius = 60f,
        ),
        radius = 60f, center = RC,
    )
    drawCircle(Color.Black.copy(alpha = 0.4f), radius = 58.5f, center = RC, style = Stroke(3f))
    drawCircle(Color(0xFF111111), radius = 5f, center = RC)
    drawCircle(Color.White.copy(alpha = 0.55f), radius = 6.5f, center = RC, style = Stroke(1f))
}

/** Picture disc: art across a d 260 circle, faint grooves, no label, spindle hole d 20. */
private fun DrawScope.drawPictureDisc(art: ImageBitmap?, fallback: Color) {
    drawCircle(Color(0xFF151515), RR, RC)
    clipPath(circlePath(RC, 130f)) {
        if (art != null) drawArtCrop(art, Rect(RC, 130f)) else drawCircle(fallback, 130f, RC)
    }
    var r = 4f
    while (r < 130f) {
        drawCircle(Color.Black.copy(alpha = 0.12f), radius = r, center = RC, style = Stroke(1f))
        r += 4f
    }
    drawCircle(Color.White.copy(alpha = 0.70f), radius = 9.5f, center = RC, style = Stroke(1f))
    drawCircle(Color(0xFF111111), radius = 5f, center = RC)
}

private fun DrawScope.drawArtCrop(image: ImageBitmap, dst: Rect) {
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

// ---------------------------------------------------------------------------------------------
// Cassette shells, reels and the song-name label

internal data class ShellColors(val fill: Color, val hub: Color, val cogGap: Color)

internal fun shellColors(t: CassetteTokens, look: ObjectLook): ShellColors = when (look.shell) {
    CassetteShell.Clear -> ShellColors(t.shell, t.reelHub, Color(0xFF1A1A1A))
    CassetteShell.Black -> ShellColors(Color(22, 22, 24).copy(alpha = 0.94f), Color(0xFFEDE6D6), Color(0xFF1A1A1A))
    CassetteShell.White -> ShellColors(Color(242, 240, 235).copy(alpha = 0.96f), Color(0xFF2A2A2C), Color(0xFFEDEDED))
    CassetteShell.Tinted -> ShellColors(look.colors.tint.copy(alpha = 0.55f), look.colors.light, Color(0xFF1A1A1A))
}

/** Measures the song title for the label strip (single line, ellipsis; 260u of text room). */
internal fun measureSongLabel(measurer: TextMeasurer, style: LabelStyle, title: String): TextLayoutResult {
    val ts = when (style) {
        LabelStyle.Printed -> TextStyle(
            fontFamily = RetroFonts.Ui, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.16.em,
        )
        LabelStyle.Ruled, LabelStyle.Handwritten -> TextStyle(
            fontFamily = RetroFonts.Caveat, fontSize = 19.sp, fontWeight = FontWeight.Bold,
        )
    }
    val text = if (style == LabelStyle.Printed) title.uppercase() else title
    return measurer.measure(
        text, ts,
        overflow = TextOverflow.Ellipsis, maxLines = 1, softWrap = false,
        constraints = Constraints(maxWidth = 260),
        density = Density(1f),
    )
}

/** Label strip at shell-local (12, 3), 280 x 22u, corner 3. Song title only. */
internal fun DrawScope.drawSongLabel(style: LabelStyle, palette: AlbumPalette, text: TextLayoutResult) {
    val rect = Rect(Offset(12f, 3f), Size(280f, 22f))
    val shape = roundRectPath(rect, 3f)
    drawShadow(shape, Color.Black.copy(alpha = 0.25f), 2f, dy = 1f)
    val (bg, ink) = when (style) {
        LabelStyle.Ruled -> Color(0xFFFBF7EC) to Color(0xFF2A2620)
        LabelStyle.Printed -> palette.tint to printedInk(palette.tint)
        LabelStyle.Handwritten -> Color.White to Color(0xFF2A2620)
    }
    drawPath(shape, bg)
    if (style == LabelStyle.Ruled) {
        clipPath(shape) {
            var y = rect.top + 7f
            while (y < rect.bottom) {
                drawRect(Color(70, 110, 170).copy(alpha = 0.35f), Offset(rect.left, y), Size(rect.width, 1f))
                y += 7f
            }
        }
    }
    translate(rect.left + 10f, rect.top + (rect.height - text.size.height) / 2f) {
        drawText(text, color = ink)
    }
}
