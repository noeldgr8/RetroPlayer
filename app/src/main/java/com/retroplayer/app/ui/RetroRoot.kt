package com.retroplayer.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.retroplayer.app.R
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.retroplayer.app.PlayerViewModel
import com.retroplayer.core.settings.AnimationLevel
import com.retroplayer.core.settings.AppSettings
import com.retroplayer.core.settings.LightDark
import com.retroplayer.core.media.NowPlaying
import com.retroplayer.core.media.SPOTIFY_PACKAGE
import com.retroplayer.core.theme.RetroPlayerTheme
import com.retroplayer.feature.player.HingeBand
import com.retroplayer.feature.player.PlayerActions
import com.retroplayer.feature.player.PlayerMotion
import com.retroplayer.feature.player.PlayerScreen
import com.retroplayer.feature.player.PlayerUiState
import com.retroplayer.feature.player.selectPlayerLayout
import kotlinx.coroutines.delay

/** Raw fold bounds in window pixels (the root fills the window edge to edge). */
data class HingePx(val top: Int, val bottom: Int)

/** Pages above the main (player / empty / error) content. Back goes to [parent]. */
private enum class Page(val parent: Page?) {
    Main(null),
    Settings(Main),
    Theme(Settings),
}

private enum class MainState { Access, Empty, Player }

@Composable
fun RetroRoot(vm: PlayerViewModel, hinge: HingePx?) {
    val loaded by vm.settings.collectAsStateWithLifecycle()
    val settings = loaded ?: AppSettings()
    val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()
    val access by vm.accessGranted.collectAsStateWithLifecycle()
    val accent by vm.accent.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val theme = settings.effectiveTheme(systemDark)
    val context = LocalContext.current
    val animate = rememberAnimationsEnabled(settings)

    var page by rememberSaveable { mutableStateOf(Page.Main) }
    var showSources by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = page.parent != null) { page = page.parent ?: Page.Main }

    SystemBarIcons(lightIcons = theme.isDark)

    RetroPlayerTheme(theme, dynamicChrome = settings.dynamicColor, accent = accent) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                // Wait for stored settings; the window background already matches the theme.
                loaded == null -> Unit
                !settings.onboardingDone -> OnboardingScreen(
                    current = theme,
                    accessGranted = access,
                    animate = animate,
                    onOpenAccess = { openAccessSettings(context, vm) },
                    onPickTheme = { vm.pickTheme(it, systemDark) },
                    onFinish = vm::completeOnboarding,
                )
                else -> AnimatedContent(
                    targetState = page,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "page",
                ) { p ->
                    when (p) {
                        Page.Main -> MainContent(
                            vm = vm,
                            settings = settings,
                            access = access,
                            nowPlaying = nowPlaying,
                            hinge = hinge,
                            animate = animate,
                            onSettings = { page = Page.Settings },
                            onChooseSource = { showSources = true },
                        )
                        Page.Settings -> SettingsScreen(
                            settings = settings,
                            theme = theme,
                            sourceLabel = sourceLabel(vm, settings.preferredSource),
                            onBack = { page = Page.Main },
                            onOpenTheme = { page = Page.Theme },
                            onChooseSource = { showSources = true },
                            onUpdate = vm::updateSettings,
                        )
                        Page.Theme -> ThemePickerScreen(
                            current = theme,
                            followSystem = settings.lightDark == LightDark.FollowSystem,
                            tintFromArt = settings.tintFromArt,
                            animate = animate,
                            onBack = { page = Page.Settings },
                            onPick = { vm.pickTheme(it, systemDark) },
                            onFollowSystem = { vm.setFollowSystem(it, theme) },
                            onTintFromArt = { on -> vm.updateSettings { it.copy(tintFromArt = on) } },
                        )
                    }
                }
            }
        }

        if (showSources) {
            SourceDialog(
                sources = remember { vm.activeSources() },
                selected = settings.preferredSource,
                onSelect = { pkg -> vm.updateSettings { it.copy(preferredSource = pkg) } },
                onDismiss = { showSources = false },
            )
        }
    }
}

/** App name for the preferred source; "Spotify" even when it isn't installed yet. */
@Composable
private fun sourceLabel(vm: PlayerViewModel, packageName: String): String {
    val label = remember(packageName) { vm.appLabel(packageName) }
    return if (label == packageName && packageName == SPOTIFY_PACKAGE) stringResource(R.string.spotify) else label
}

@Composable
private fun MainContent(
    vm: PlayerViewModel,
    settings: AppSettings,
    access: Boolean,
    nowPlaying: NowPlaying?,
    hinge: HingePx?,
    animate: Boolean,
    onSettings: () -> Unit,
    onChooseSource: () -> Unit,
) {
    val context = LocalContext.current
    val state = when {
        !access -> MainState.Access
        nowPlaying == null -> MainState.Empty
        else -> MainState.Player
    }
    Crossfade(state, label = "main") { s ->
        when (s) {
            MainState.Access -> AccessScreen(onGrant = { openAccessSettings(context, vm) })
            MainState.Empty -> EmptyScreen(
                motion = PlayerMotion(vm.spin, vm.reels, animate = false),
                onSettings = onSettings,
                onOpenSpotify = { openSpotify(context) },
                onChooseSource = onChooseSource,
            )
            MainState.Player -> nowPlaying?.let { np ->
                PlayerRoute(np, vm, settings, hinge, animate, onSettings)
            }
        }
    }
}

@Composable
private fun PlayerRoute(
    np: NowPlaying,
    vm: PlayerViewModel,
    settings: AppSettings,
    hinge: HingePx?,
    animate: Boolean,
    onSettings: () -> Unit,
) {
    // Position is extrapolated between session updates; 4 Hz is plenty for text and tape packs.
    val position by produceState(np.estimatedPositionMs(), np) {
        value = np.estimatedPositionMs()
        while (np.isPlaying) {
            delay(250)
            value = np.estimatedPositionMs()
        }
    }
    val art = remember(np.art) { np.art?.asImageBitmap() }
    val state = PlayerUiState(
        title = np.title,
        artist = np.artist,
        album = np.album,
        sourceLabel = np.sourceLabel,
        art = art,
        isPlaying = np.isPlaying,
        positionMs = position,
        durationMs = np.durationMs,
        canSeek = np.canSeek,
        canPrevious = np.canSkipPrevious,
        canNext = np.canSkipNext,
    )
    val actions = remember(vm, onSettings) {
        PlayerActions(
            onSeek = vm::seekTo,
            onPrevious = vm::previous,
            onPlay = vm::play,
            onPause = vm::pause,
            onNext = vm::next,
            onSettings = onSettings,
        )
    }

    KeepScreenOn(settings.keepScreenOn)

    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val band = hinge?.let {
            with(density) { HingeBand(it.top.toDp() - 12.dp, it.bottom.toDp() + 12.dp) }
        }
        val layout = selectPlayerLayout(maxWidth, maxHeight, tabletop = band != null)
        PlayerScreen(
            layout = layout,
            state = state,
            actions = actions,
            motion = PlayerMotion(vm.spin, vm.reels, animate),
            hinge = band,
        )
    }
}

/**
 * Full motion only with the in-app level at Full, system animations on, and the app visible
 * (frame loops stop in the background / screen off, NFR-02).
 */
@Composable
private fun rememberAnimationsEnabled(settings: AppSettings): Boolean {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val context = LocalContext.current
    val systemOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    return settings.animation == AnimationLevel.Full && !systemOff && lifecycle.isAtLeast(Lifecycle.State.STARTED)
}

/** Status / nav bar icon colour follows the app theme, not the system theme. */
@Composable
private fun SystemBarIcons(lightIcons: Boolean) {
    val view = LocalView.current
    // Reading the configuration makes this recompose (and the SideEffect re-run) on each change.
    LocalConfiguration.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !lightIcons
            isAppearanceLightNavigationBars = !lightIcons
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

private fun openAccessSettings(context: Context, vm: PlayerViewModel) {
    try {
        context.startActivity(vm.accessSettingsIntent())
    } catch (e: ActivityNotFoundException) {
        context.startActivity(vm.accessSettingsFallbackIntent())
    }
}

/** Launches Spotify if installed, else its Play Store page. */
internal fun openSpotify(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE)
    val intent = launch ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SPOTIFY_PACKAGE"))
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$SPOTIFY_PACKAGE"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
