package com.retroplayer.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.retroplayer.core.theme.LocalRetroStyle
import com.retroplayer.core.theme.PlayerStyle
import com.retroplayer.core.theme.RetroType

/** Player layouts, plus two that are not designed yet. */
enum class PlayerLayout { Cover, InnerPortrait, InnerLandscape, Tabletop, CoverLandscape, Minimal, MiniDeckPortrait, MiniDeckLandscape }

/** Hinge band in this composable's coordinates, already including 12 dp clearance per side. */
data class HingeBand(val top: Dp, val bottom: Dp)

/** Selection order. Never keys off device model. */
fun selectPlayerLayout(width: Dp, height: Dp, tabletop: Boolean, miniDeck: Boolean = false): PlayerLayout = when {
    tabletop -> PlayerLayout.Tabletop
    width < 360.dp -> PlayerLayout.Minimal
    height < 480.dp && width > height -> if (miniDeck) PlayerLayout.MiniDeckLandscape else PlayerLayout.CoverLandscape
    width < 600.dp -> if (miniDeck) PlayerLayout.MiniDeckPortrait else PlayerLayout.Cover
    width < 840.dp -> PlayerLayout.InnerPortrait
    else -> PlayerLayout.InnerLandscape
}

@Composable
fun PlayerScreen(
    layout: PlayerLayout,
    state: PlayerUiState,
    actions: PlayerActions,
    motion: PlayerMotion,
    modifier: Modifier = Modifier,
    hinge: HingeBand? = null,
    /** EN-05: mini deck puts the controls on this side. */
    thumbRight: Boolean = true,
) {
    Box(modifier.fillMaxSize()) {
        when (layout) {
            PlayerLayout.Cover -> CoverLayout(state, actions, motion)
            PlayerLayout.InnerPortrait -> InnerPortraitLayout(state, actions, motion)
            PlayerLayout.InnerLandscape -> InnerLandscapeLayout(state, actions, motion)
            PlayerLayout.Tabletop -> TabletopLayout(state, actions, motion, hinge)
            PlayerLayout.CoverLandscape -> CoverLandscapeLayout(state, actions, motion)
            PlayerLayout.Minimal -> MinimalLayout(state, actions, motion)
            PlayerLayout.MiniDeckPortrait -> MiniDeckPortrait(state, actions, motion, thumbRight)
            PlayerLayout.MiniDeckLandscape -> MiniDeckLandscape(state, actions, motion, thumbRight)
        }
    }
}

private val TopAndSides = WindowInsetsSides.Top + WindowInsetsSides.Horizontal
private val BottomAndSides = WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal

// ---------------------------------------------------------------------------------------------
// A. Cover screen (465 x 735)

@Composable
private fun CoverLayout(state: PlayerUiState, actions: PlayerActions, motion: PlayerMotion) {
    val retro = LocalRetroStyle.current
    var sleeveOverlay by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(TopAndSides))) {
        RetroTopBar(actions.onSettings, onCrate = actions.onCrate, onShare = actions.onShare, startPadding = 24.dp, endPadding = 8.dp)
        ScaledStage(465f, 368f, Modifier.weight(1f).fillMaxWidth().heightIn(min = 220.dp)) {
            if (retro.style == PlayerStyle.Vinyl) {
                // EN-04: the sleeve peeks out; tapping brings it forward and flips it.
                val sleeveLabel = androidx.compose.ui.res.stringResource(R.string.cd_sleeve_front)
                AlbumArt(
                    state.art,
                    Modifier.at(20f, 14f).size(d(190))
                        .clickable(onClickLabel = sleeveLabel) { sleeveOverlay = true },
                    corner = 6.dp, rotation = -7f,
                )
                PlayerObject(state, motion, d(340), Modifier.at(95f, 18f))
            } else {
                PlayerObject(state, motion, d(401), Modifier.at(32f, 54f))
            }
        }
        TrackInfo(
            state,
            titleStyle = RetroType.titleCover(retro.displayFont),
            artistStyle = RetroType.artistCover,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 10.dp),
        )
        Transport(
            state, actions, retro.style,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(BottomAndSides))
                .padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 22.dp),
        )
    }
    if (sleeveOverlay) {
        SleeveOverlay(state, jCard = false, animate = motion.animate, onDismiss = { sleeveOverlay = false })
    }
    }
}

// ---------------------------------------------------------------------------------------------
// B. Inner portrait (730 x 968)

@Composable
private fun InnerPortraitLayout(state: PlayerUiState, actions: PlayerActions, motion: PlayerMotion) {
    val retro = LocalRetroStyle.current
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(TopAndSides)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RetroTopBar(actions.onSettings, onCrate = actions.onCrate, onShare = actions.onShare, inner = true, startPadding = 32.dp, endPadding = 16.dp)
        ScaledStage(730f, 470f, Modifier.weight(1f).fillMaxWidth().heightIn(min = 240.dp)) {
            if (retro.style == PlayerStyle.Vinyl) {
                FlippableSleeve(state, d(200), 6.dp, jCard = false, animate = motion.animate, modifier = Modifier.at(40f, 70f))
                Caption(stringResource(R.string.caption_sleeve, state.album), Modifier.at(40f, 286f).widthIn(max = d(220)))
                PlayerObject(state, motion, d(436), Modifier.at(262f, 14f))
            } else {
                FlippableSleeve(state, d(206), 4.dp, jCard = true, animate = motion.animate, modifier = Modifier.at(38f, 70f), rotation = -3f)
                Caption(stringResource(R.string.caption_jcard, state.album), Modifier.at(40f, 296f).widthIn(max = d(220)))
                PlayerObject(state, motion, d(428), Modifier.at(266f, 100f))
            }
        }
        TrackInfo(
            state,
            titleStyle = RetroType.titleInner(retro.displayFont),
            artistStyle = RetroType.artistInner,
            centered = true,
            modifier = Modifier.fillMaxWidth().padding(start = 80.dp, end = 80.dp, top = 16.dp),
        )
        Transport(
            state, actions, retro.style,
            modifier = Modifier
                .widthIn(max = 570.dp)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(BottomAndSides))
                .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 48.dp),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// C. Inner landscape (968 x 730), two panes

@Composable
private fun InnerLandscapeLayout(state: PlayerUiState, actions: PlayerActions, motion: PlayerMotion) {
    val retro = LocalRetroStyle.current
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        RetroTopBar(actions.onSettings, onCrate = actions.onCrate, onShare = actions.onShare, inner = true, startPadding = 40.dp, endPadding = 16.dp)
        Row(
            Modifier.weight(1f).fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 8.dp, bottom = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Column(Modifier.width(380.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                BoxWithConstraints(Modifier.weight(1f, fill = false).sizeIn(maxWidth = 220.dp, maxHeight = 220.dp).aspectRatio(1f)) {
                    val vinyl = retro.style == PlayerStyle.Vinyl
                    FlippableSleeve(
                        state, min(maxWidth, maxHeight), if (vinyl) 6.dp else 4.dp,
                        jCard = !vinyl, animate = motion.animate,
                    )
                }
                TrackInfo(
                    state,
                    titleStyle = RetroType.titleLandscape(retro.displayFont),
                    artistStyle = RetroType.artistInner,
                )
                Spacer(Modifier.weight(1f))
                Transport(state, actions, retro.style, Modifier.fillMaxWidth())
            }
            ObjectPane(state, motion, maxObjectWidth = 466.dp, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

/** Object centred and as large as fits, up to [maxObjectWidth]. */
@Composable
private fun ObjectPane(state: PlayerUiState, motion: PlayerMotion, maxObjectWidth: Dp, modifier: Modifier) {
    val aspect = if (LocalRetroStyle.current.style == PlayerStyle.Vinyl) 1f else CASSETTE_ASPECT
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val w = min(maxObjectWidth, min(maxWidth, maxHeight * aspect))
        PlayerObject(state, motion, w)
    }
}

// ---------------------------------------------------------------------------------------------
// D. Tabletop (hinge horizontal): immersive object on top, info + big controls below

@Composable
private fun TabletopLayout(state: PlayerUiState, actions: PlayerActions, motion: PlayerMotion, hinge: HingeBand?) {
    val retro = LocalRetroStyle.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val band = hinge ?: HingeBand(maxHeight / 2 - 12.dp, maxHeight / 2 + 12.dp)
        // Top half: perspective scene, no app bar
        Box(
            Modifier
                .fillMaxWidth()
                .height(band.top)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(TopAndSides)),
        ) {
            ScaledStage(
                968f, 353f,
                Modifier.fillMaxSize().graphicsLayer {
                    rotationX = 24f
                    // Tune on device; roughly CSS perspective(1000px).
                    cameraDistance = 16f * density
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
            ) {
                if (retro.style == PlayerStyle.Vinyl) {
                    FlippableSleeve(state, d(156), 6.dp, jCard = false, animate = motion.animate, modifier = Modifier.at(196f, 80f), rotation = -6f)
                    PlayerObject(state, motion, d(300), Modifier.at(334f, 10f))
                } else {
                    PlayerObject(state, motion, d(420), Modifier.at(274f, 30f))
                }
            }
        }
        // Bottom half: nothing inside the hinge band
        Row(
            Modifier
                .fillMaxWidth()
                .offset(y = band.bottom)
                .height((maxHeight - band.bottom).coerceAtLeast(0.dp))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(BottomAndSides))
                .padding(start = 56.dp, end = 56.dp, top = 28.dp, bottom = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            TrackInfo(
                state,
                titleStyle = RetroType.titleInner(retro.displayFont),
                artistStyle = RetroType.artistInner,
                modifier = Modifier.weight(340f).widthIn(max = 340.dp),
            )
            Transport(
                state, actions, retro.style, big = true,
                modifier = Modifier.weight(468f).widthIn(max = 468.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Not drawn yet: cover-screen landscape and the minimal split-screen view

@Composable
private fun CoverLandscapeLayout(state: PlayerUiState, actions: PlayerActions, motion: PlayerMotion) {
    val retro = LocalRetroStyle.current
    Row(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight().padding(bottom = 16.dp)) {
            RetroTopBar(actions.onSettings, onCrate = actions.onCrate, onShare = actions.onShare, startPadding = 24.dp, endPadding = 8.dp)
            Spacer(Modifier.weight(1f))
            TrackInfo(
                state,
                titleStyle = RetroType.titleCover(retro.displayFont),
                artistStyle = RetroType.artistCover,
                singleLine = true,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Transport(state, actions, retro.style, Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp))
        }
        ObjectPane(state, motion, maxObjectWidth = 466.dp, modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp))
    }
}

@Composable
private fun MinimalLayout(state: PlayerUiState, actions: PlayerActions, motion: PlayerMotion) {
    BoxWithConstraints(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val objectWidth = min(maxWidth, maxHeight * 0.55f).coerceAtMost(240.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlayerObject(state, motion, objectWidth, bare = true)
            Text(
                state.title,
                style = RetroType.titleCover(LocalRetroStyle.current.displayFont),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PlayMorphButton(state.isPlaying, big = false, onPlay = actions.onPlay, onPause = actions.onPause)
        }
    }
}
