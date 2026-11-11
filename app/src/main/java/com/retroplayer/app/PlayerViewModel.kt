package com.retroplayer.app

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retroplayer.core.settings.AppSettings
import com.retroplayer.core.settings.LightDark
import com.retroplayer.core.settings.SettingsRepository
import com.retroplayer.core.media.MediaRepository
import com.retroplayer.core.media.NowPlaying
import com.retroplayer.core.media.SourceApp
import com.retroplayer.core.theme.AlbumTint
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.feature.player.ReelState
import com.retroplayer.feature.player.SpinState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val media: MediaRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlaying?> = media.nowPlaying
    val accessGranted: StateFlow<Boolean> = media.listenerEnabled

    /** Null until DataStore has loaded, so returning users never see onboarding flash by. */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .map<AppSettings, AppSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Album-art accent, only when "Tint from album art" is on. */
    val accent: StateFlow<Color?> = combine(
        nowPlaying.map { it?.art }.distinctUntilChanged(),
        settingsRepository.settings.map { it.tintFromArt }.distinctUntilChanged(),
    ) { art, enabled ->
        if (enabled && art != null) withContext(Dispatchers.Default) { AlbumTint.extract(art) } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Rotation lives here so the record and reels keep their angle across fold / unfold. */
    val spin = SpinState()
    val reels = ReelState()

    init {
        viewModelScope.launch {
            settingsRepository.settings.map { it.preferredSource }.distinctUntilChanged()
                .collect { media.setPreferredPackage(it) }
        }
    }

    fun refreshAccess() = media.refreshAccess()
    fun accessSettingsIntent() = media.accessSettingsIntent()
    fun accessSettingsFallbackIntent() = media.accessSettingsFallbackIntent()
    fun activeSources(): List<SourceApp> = media.activeSources()
    fun appLabel(packageName: String): String = media.labelFor(packageName)

    fun play() = media.play()
    fun pause() = media.pause()
    fun next() = media.next()
    fun previous() = media.previous()
    fun seekTo(ms: Long) = media.seekTo(ms)

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun completeOnboarding() = updateSettings { it.copy(onboardingDone = true) }

    /**
     * Theme pick. With "match system" on, picking the variant that doesn't match the system
     * pins light or dark, so the user sees exactly what they tapped.
     */
    fun pickTheme(theme: RetroTheme, systemDark: Boolean) = updateSettings { s ->
        val pinned = if (theme.isDark) LightDark.AlwaysDark else LightDark.AlwaysLight
        val lightDark = when {
            s.lightDark != LightDark.FollowSystem -> pinned
            theme.isDark != systemDark -> pinned
            else -> s.lightDark
        }
        s.copy(theme = theme, lightDark = lightDark)
    }

    fun setFollowSystem(follow: Boolean, current: RetroTheme) = updateSettings { s ->
        s.copy(
            theme = current,
            lightDark = when {
                follow -> LightDark.FollowSystem
                current.isDark -> LightDark.AlwaysDark
                else -> LightDark.AlwaysLight
            },
        )
    }
}
