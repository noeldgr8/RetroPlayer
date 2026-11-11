package com.retroplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroplayer.app.R
import com.retroplayer.core.theme.LocalRetroStyle
import com.retroplayer.core.theme.RetroFonts
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.core.theme.RetroType
import com.retroplayer.core.theme.VinylLightTokens
import com.retroplayer.feature.player.RetroIcons
import com.retroplayer.feature.player.Turntable
import com.retroplayer.feature.player.rememberReelState
import com.retroplayer.feature.player.rememberSpinState
import kotlinx.coroutines.launch

private const val PAGES = 4
private const val PAGE_ACCESS = 1

/**
 * Onboarding (DESIGN.md 7.1): welcome, notification access, theme, widget tip.
 * Picking a theme on step 3 re-themes this screen live.
 */
@Composable
fun OnboardingScreen(
    current: RetroTheme,
    accessGranted: Boolean,
    animate: Boolean,
    onOpenAccess: () -> Unit,
    onPickTheme: (RetroTheme) -> Unit,
    onFinish: () -> Unit,
) {
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage
    fun next() = scope.launch { pager.animateScrollToPage((page + 1).coerceAtMost(PAGES - 1)) }

    // Coming back from system settings with access granted moves on automatically.
    LaunchedEffect(accessGranted) {
        if (accessGranted && pager.currentPage == PAGE_ACCESS) pager.animateScrollToPage(PAGE_ACCESS + 1)
    }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(end = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (page < PAGES - 1) {
                    TextButton(onClick = onFinish) { Text(stringResource(R.string.skip)) }
                }
            }

            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { p ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (p) {
                        0 -> WelcomeStep(animate)
                        1 -> AccessStep()
                        2 -> ThemeStep(current, onPickTheme)
                        else -> WidgetStep()
                    }
                }
            }

            PageDots(page, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val label = when (page) {
                    0 -> R.string.get_started
                    PAGE_ACCESS -> if (accessGranted) R.string.continue_ else R.string.open_settings
                    2 -> R.string.continue_
                    else -> R.string.done
                }
                Button(
                    onClick = {
                        when {
                            page == PAGE_ACCESS && !accessGranted -> onOpenAccess()
                            page == PAGES - 1 -> onFinish()
                            else -> next()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(label)) }
                // Keep the primary button still between steps: reserve the secondary's space.
                Box(Modifier.heightIn(min = 48.dp).padding(top = 4.dp), contentAlignment = Alignment.Center) {
                    if (page == PAGE_ACCESS && !accessGranted) {
                        TextButton(onClick = { next() }) { Text(stringResource(R.string.not_now)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageDots(page: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_page_of, page + 1, PAGES)
    Row(
        modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PAGES) { i ->
            Box(
                Modifier
                    .size(if (i == page) 24.dp else 8.dp, 8.dp)
                    .background(
                        if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun Headline(text: String, size: Int, line: Int) {
    Text(
        text,
        style = RetroType.headline(RetroFonts.Fraunces, size, line),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().semantics { heading() },
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}

// --- Step 1: Welcome -------------------------------------------------------------------------

@Composable
private fun WelcomeStep(animate: Boolean) {
    val tokens = LocalRetroStyle.current.tokens
    BoxWithConstraints(Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
        val w = minOf(310.dp, maxWidth)
        ThemeObjectPlaying(tokens, w, animate)
    }
    Headline(stringResource(R.string.onb_welcome_title), 34, 40)
    Body(stringResource(R.string.onb_welcome_body))
}

@Composable
private fun ThemeObjectPlaying(tokens: com.retroplayer.core.theme.PlayerTokens, width: Dp, animate: Boolean) {
    val spin = rememberSpinState()
    val reels = rememberReelState()
    when (tokens) {
        is com.retroplayer.core.theme.VinylTokens -> Turntable(
            tokens, art = null, isPlaying = true, animate = animate, spin = spin,
            modifier = Modifier.width(width),
        )
        is com.retroplayer.core.theme.CassetteTokens -> com.retroplayer.feature.player.CassettePlayer(
            tokens, progress = 0.35f, isPlaying = true, animate = animate, reels = reels,
            modifier = Modifier.width(width),
        )
    }
}

// --- Step 2: Notification access -------------------------------------------------------------

@Composable
private fun AccessStep() {
    Column(
        Modifier.widthIn(max = 360.dp).fillMaxWidth().padding(top = 8.dp, bottom = 24.dp).clearAndSetSemantics { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MockNotification()
        Icon(
            RetroIcons.ArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        MockSettingsCard(header = stringResource(R.string.mock_settings_short), supporting = null, on = true)
    }
    Headline(stringResource(R.string.why_title), 28, 34)
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (line in listOf(R.string.why_1, R.string.why_2, R.string.why_3)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    RetroIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(line),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Illustration of a now-playing notification (design copy from DESIGN.md 7.1, step 2). */
@Composable
private fun MockNotification() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFD23F)),
            ) {
                Box(Modifier.size(26.dp).offset(4.dp, 6.dp).background(Color(0xFFFF8C42), CircleShape))
                Box(Modifier.size(22.dp).offset(18.dp, 18.dp).background(Color(0xFFF25F8C), CircleShape))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Saffron Hours", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Mira Kale · Spotify",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(RetroIcons.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// --- Step 3: Theme ---------------------------------------------------------------------------

@Composable
private fun ThemeStep(current: RetroTheme, onPick: (RetroTheme) -> Unit) {
    ThemeGrid(
        current = current,
        onPick = onPick,
        cardHeight = 176.dp,
        turntableWidth = 112.dp,
        cassetteWidth = 160.dp,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
    )
    Headline(stringResource(R.string.onb_theme_title), 34, 40)
    Body(stringResource(R.string.onb_theme_body))
}

// --- Step 4: Widget tip ----------------------------------------------------------------------

@Composable
private fun WidgetStep() {
    HomeScreenMock(Modifier.padding(top = 8.dp, bottom = 24.dp))
    Headline(stringResource(R.string.onb_widget_title), 30, 36)
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(R.string.onb_widget_1, R.string.onb_widget_2, R.string.onb_widget_3).forEachIndexed { i, res ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(26.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${i + 1}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(stringResource(res), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** 320 x 250 home screen with a Small vinyl widget, app icons and a dock. Decorative. */
@Composable
private fun HomeScreenMock(modifier: Modifier = Modifier) {
    Box(
        modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .aspectRatio(320f / 250f)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFA69A8B))
            .clearAndSetSemantics { },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val u = maxWidth / 320f
            // Small vinyl widget (a turntable is its own wooden chassis).
            val spin = remember { com.retroplayer.feature.player.SpinState() }
            Turntable(
                VinylLightTokens, art = null, isPlaying = true, animate = false, spin = spin,
                // The mock always shows a Vinyl Light widget, whatever theme is active.
                labelColor = RetroTheme.VinylLight.colorScheme.primaryContainer,
                modifier = Modifier.offset(u * 20, u * 20).width(u * 130),
            )
            // Four app icons
            for (i in 0 until 4) {
                val x = 184 + (i % 2) * 62
                val y = 26 + (i / 2) * 62
                Box(
                    Modifier
                        .offset(u * x, u * y)
                        .size(u * 46)
                        .background(Color.White.copy(alpha = 0.55f), CircleShape),
                )
            }
            // Dock
            Box(
                Modifier
                    .offset(u * 16, u * 180)
                    .size(u * 288, u * 54)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(u * 22)),
            )
            for (i in 0 until 4) {
                Box(
                    Modifier
                        .offset(u * (40 + i * 66), u * 189)
                        .size(u * 36)
                        .background(Color.White.copy(alpha = 0.7f), CircleShape),
                )
            }
        }
    }
}
