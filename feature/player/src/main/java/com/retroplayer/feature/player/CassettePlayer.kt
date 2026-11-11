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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.RetroMotion
import com.retroplayer.core.theme.ObjectLook
import com.retroplayer.feature.player.draw.circlePath
import com.retroplayer.feature.player.draw.cssLinear
import com.retroplayer.feature.player.draw.cssRadialAt
import com.retroplayer.feature.player.draw.drawInsetEdge
import com.retroplayer.feature.player.draw.drawShadow
import com.retroplayer.feature.player.draw.roundRectPath

private const val W = 400f
private const val H = 260f
const val CASSETTE_ASPECT = W / H

private val WindowRect = Rect(36f, 34f, 364f, 178f)

/** Shell origin: window padding box (inside the 2u border) + (10, 8). */
internal val ShellOrigin = Offset(48f, 44f)
internal val PackLeft = Offset(82f, 62f)
internal val PackRight = Offset(222f, 62f)

/** Pack diameters for progress p (DESIGN.md 4.2). */
private fun packDiameters(p: Float): Pair<Float, Float> = (42f + 34f * (1f - p)) to (42f + 34f * p)

/**
 * Front-on cassette player (DESIGN.md 4.2), 400 x 260 base, height = width * 260 / 400.
 * No paper label and no art behind the window; tape winds from left reel to right reel as
 * [progress] (0..1) advances. Reel speeds follow 28 / r.
 */
@Composable
fun CassettePlayer(
    tokens: CassetteTokens,
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    bare: Boolean = false,
    animate: Boolean = true,
    reels: ReelState = rememberReelState(),
    look: ObjectLook = ObjectLook.Default,
    /** Shown on the song-name label when [look] has one. */
    songTitle: String? = null,
) {
    val shell = shellColors(tokens, look)
    val p = progress.coerceIn(0f, 1f)
    val (dL, dR) = packDiameters(p)
    ReelEffect(reels, isPlaying, animate, leftRadius = dL / 2f, rightRadius = dR / 2f)
    val indicator by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = if (animate) tween(RetroMotion.CASSETTE_START_MS) else snap(),
        label = "indicator",
    )
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier.aspectRatio(CASSETTE_ASPECT).clearAndSetSemantics { }) {
        val u = maxWidth / W

        // 1-5: body, screws, brand text, window, shell (static)
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val s = size.width / W
                val brand = textMeasurer.measure(
                    "RETROPLAYER",
                    TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.24.em),
                    density = Density(1f),
                )
                val stereo = textMeasurer.measure(
                    "STEREO",
                    TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.em),
                    density = Density(1f),
                )
                onDrawBehind { scale(s, Offset.Zero) { drawCassetteBase(tokens, bare, brand, stereo, shell.fill) } }
            },
        )

        // 6-9: tape packs, tape path, rollers, head (redrawn when progress changes)
        Spacer(
            Modifier.fillMaxSize().drawBehind {
                scale(size.width / W, Offset.Zero) { drawTape(tokens, p) }
            },
        )

        // EN-01 song-name label strip (off by default), above the tape packs
        val label = look.label
        if (label != null && !songTitle.isNullOrBlank()) {
            Spacer(
                Modifier.fillMaxSize().drawWithCache {
                    val text = measureSongLabel(textMeasurer, label, songTitle)
                    onDrawBehind {
                        scale(size.width / W, Offset.Zero) {
                            translate(ShellOrigin.x, ShellOrigin.y) { drawSongLabel(label, look.colors, text) }
                        }
                    }
                },
            )
        }

        // 10: reel hubs, each in its own rotating layer
        val hubTopLeft = { c: Offset -> ShellOrigin + c - Offset(20f, 20f) }
        val hubL = hubTopLeft(PackLeft)
        val hubR = hubTopLeft(PackRight)
        Spacer(
            Modifier
                .offset(u * hubL.x, u * hubL.y)
                .size(u * 40f)
                .graphicsLayer { rotationZ = reels.left }
                .drawWithCache { onDrawBehind { scale(size.width / 40f, Offset.Zero) { drawReelHub(tokens, shell.hub, shell.cogGap) } } },
        )
        Spacer(
            Modifier
                .offset(u * hubR.x, u * hubR.y)
                .size(u * 40f)
                .graphicsLayer { rotationZ = reels.right }
                .drawWithCache { onDrawBehind { scale(size.width / 40f, Offset.Zero) { drawReelHub(tokens, shell.hub, shell.cogGap) } } },
        )

        // 11-14: window gloss, speaker grille, indicator, volume wheel
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val s = size.width / W
                val play = textMeasurer.measure(
                    "PLAY",
                    TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.14.em),
                    density = Density(1f),
                )
                onDrawBehind { scale(s, Offset.Zero) { drawCassetteTop(tokens, bare, indicator, play) } }
            },
        )
    }
}

internal fun DrawScope.drawCassetteBase(
    t: CassetteTokens,
    bare: Boolean,
    brand: TextLayoutResult,
    stereo: TextLayoutResult,
    shellFill: Color = t.shell,
) {
    if (!bare) {
        val bounds = Rect(0f, 0f, W, H)
        val body = roundRectPath(bounds, 24f)
        drawShadow(body, t.bodyDropShadow, t.bodyDropBlur, dy = 18f)
        drawPath(body, Brush.verticalGradient(listOf(t.bodyTop, t.bodyBottom), 0f, H))
        drawInsetEdge(body, 2f, t.bodyHighlight)
        drawInsetEdge(body, -4f, t.bodyInnerShade)

        for (o in listOf(Offset(12f, 12f), Offset(379f, 12f), Offset(12f, 239f), Offset(379f, 239f))) {
            val r = Rect(o, Size(9f, 9f))
            drawCircle(cssRadialAt(r, 0.35f, 0.30f, 0f to t.screwLight, 1f to t.screwDark), 4.5f, r.center)
            drawLine(t.screwDark, r.center - Offset(2.6f, 0f), r.center + Offset(2.6f, 0f), strokeWidth = 1f)
        }

        drawText(brand, color = t.bodyText, topLeft = Offset(36f, 12f))
        drawText(stereo, color = t.bodyText, topLeft = Offset(364f - stereo.size.width, 13f))
    }

    // Window: glass + 2u border, then the translucent shell
    val window = roundRectPath(WindowRect, 14f)
    drawPath(window, Brush.verticalGradient(listOf(t.glassTop, t.glassBottom), WindowRect.top, WindowRect.bottom))
    drawRoundRect(
        t.edge,
        topLeft = WindowRect.topLeft + Offset(1f, 1f),
        size = Size(WindowRect.width - 2f, WindowRect.height - 2f),
        cornerRadius = CornerRadius(13f),
        style = Stroke(2f),
    )
    translate(ShellOrigin.x, ShellOrigin.y) {
        drawRoundRect(shellFill, Offset.Zero, Size(304f, 124f), CornerRadius(8f))
    }
}

internal fun DrawScope.drawTape(t: CassetteTokens, p: Float) {
    val (dL, dR) = packDiameters(p)
    val rL = dL / 2f
    val rR = dR / 2f
    translate(ShellOrigin.x, ShellOrigin.y) {
        drawPack(t, PackLeft, rL)
        drawPack(t, PackRight, rR)

        val path = Path().apply {
            moveTo(PackLeft.x - 0.94f * rL, PackLeft.y + 0.34f * rL)
            lineTo(10f, 120f)
            lineTo(294f, 120f)
            lineTo(PackRight.x + 0.94f * rR, PackRight.y + 0.34f * rR)
        }
        drawPath(path, t.tapeLine, style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        for (c in listOf(Offset(14f, 118f), Offset(290f, 118f))) {
            drawCircle(t.roller, radius = 5f, center = c)
            drawCircle(t.rollerStroke, radius = 4.5f, center = c, style = Stroke(1f))
        }
        drawRoundRect(t.head, Offset(136f, 114f), Size(32f, 8f), CornerRadius(2f))
    }
}

private fun DrawScope.drawPack(t: CassetteTokens, c: Offset, r: Float) {
    drawCircle(t.tapeB, radius = r, center = c)
    var rr = r - 1f
    while (rr > 1f) {
        drawCircle(t.tapeA, radius = rr, center = c, style = Stroke(2f))
        rr -= 4f
    }
    drawCircle(Color.Black.copy(alpha = 0.35f), radius = r - 0.5f, center = c, style = Stroke(1f))
}

/** Hub-local 40 x 40. Six cog teeth: 14deg of hub colour per 60deg pitch. */
internal fun DrawScope.drawReelHub(t: CassetteTokens, hub: Color = t.reelHub, cog: Color = t.cogDark) {
    val c = Offset(20f, 20f)
    drawCircle(hub, radius = 20f, center = c)
    drawCircle(Color.Black.copy(alpha = 0.18f), radius = 19.5f, center = c, style = Stroke(1f))
    drawCircle(cog, radius = 12f, center = c)
    for (i in 0 until 6) {
        drawArc(
            color = hub,
            startAngle = i * 60f - 7f,
            sweepAngle = 14f,
            useCenter = true,
            topLeft = Offset(8f, 8f),
            size = Size(24f, 24f),
        )
    }
    drawCircle(cog, radius = 7f, center = c)
}

internal fun DrawScope.drawCassetteTop(
    t: CassetteTokens,
    bare: Boolean,
    indicator: Float,
    playLabel: TextLayoutResult,
) {
    // Window gloss: linear-gradient(160deg, white 22%, transparent 38%)
    clipPath(roundRectPath(WindowRect, 14f)) {
        drawRect(
            cssLinear(
                160f, WindowRect,
                0f to Color.White.copy(alpha = 0.22f),
                0.38f to Color.White.copy(alpha = 0f),
                1f to Color.White.copy(alpha = 0f),
            ),
            topLeft = WindowRect.topLeft,
            size = WindowRect.size,
        )
    }

    if (bare) return

    // Speaker grille: vertical stripes 5u, gap 5u
    val grille = Rect(36f, 194f, 266f, 240f)
    clipPath(roundRectPath(grille, 10f)) {
        var x = grille.left
        while (x < grille.right) {
            drawRect(t.grille, Offset(x, grille.top), Size(5f, grille.height))
            x += 10f
        }
    }

    // Indicator light + PLAY
    val lamp = Offset(297f, 205f)
    val lampPath = circlePath(lamp, 7f)
    drawCircle(t.indicatorOff, radius = 7f, center = lamp)
    drawInsetEdge(lampPath, 2f, Color.Black.copy(alpha = 0.35f))
    if (indicator > 0f) {
        drawShadow(circlePath(lamp, 10f), t.indicatorOn.copy(alpha = indicator), 10f)
        drawCircle(t.indicatorOn, radius = 7f, center = lamp, alpha = indicator)
    }
    drawText(playLabel, color = t.bodyText, topLeft = Offset(312f, 198f))

    // Volume wheel: stripes edge 2u / wheel 4u
    val wheel = Rect(290f, 224f, 364f, 238f)
    clipPath(roundRectPath(wheel, 7f)) {
        drawRect(t.wheel, wheel.topLeft, wheel.size)
        var x = wheel.left
        while (x < wheel.right) {
            drawRect(t.edge, Offset(x, wheel.top), Size(2f, wheel.height))
            x += 6f
        }
    }
}
