package com.retroplayer.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.retroplayer.core.media.SPOTIFY_PACKAGE
import com.retroplayer.core.theme.RetroTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class LightDark { FollowSystem, AlwaysLight, AlwaysDark }
enum class AnimationLevel { Full, Reduced, Off }

/** Settings with the defaults from DESIGN.md 7.4. */
data class AppSettings(
    val theme: RetroTheme = RetroTheme.VinylLight,
    val lightDark: LightDark = LightDark.FollowSystem,
    val dynamicColor: Boolean = false,
    val tintFromArt: Boolean = false,
    val preferredSource: String = SPOTIFY_PACKAGE,
    val keepScreenOn: Boolean = true,
    val animation: AnimationLevel = AnimationLevel.Full,
    val soundEffects: Boolean = false,
    val onboardingDone: Boolean = false,
) {
    fun effectiveTheme(systemDark: Boolean): RetroTheme = when (lightDark) {
        LightDark.FollowSystem -> theme.withDark(systemDark)
        LightDark.AlwaysLight -> theme.withDark(false)
        LightDark.AlwaysDark -> theme.withDark(true)
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LIGHT_DARK = stringPreferencesKey("light_dark")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val TINT = booleanPreferencesKey("tint_from_art")
        val SOURCE = stringPreferencesKey("preferred_source")
        val KEEP_ON = booleanPreferencesKey("keep_screen_on")
        val ANIMATION = stringPreferencesKey("animation")
        val SOUND = booleanPreferencesKey("sound_effects")
        val ONBOARDED = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            theme = RetroTheme.fromId(this[Keys.THEME]),
            lightDark = enumOrDefault(this[Keys.LIGHT_DARK], d.lightDark),
            dynamicColor = this[Keys.DYNAMIC] ?: d.dynamicColor,
            tintFromArt = this[Keys.TINT] ?: d.tintFromArt,
            preferredSource = this[Keys.SOURCE] ?: d.preferredSource,
            keepScreenOn = this[Keys.KEEP_ON] ?: d.keepScreenOn,
            animation = enumOrDefault(this[Keys.ANIMATION], d.animation),
            soundEffects = this[Keys.SOUND] ?: d.soundEffects,
            onboardingDone = this[Keys.ONBOARDED] ?: d.onboardingDone,
        )
    }

    private fun MutablePreferences.write(s: AppSettings) {
        this[Keys.THEME] = s.theme.id
        this[Keys.LIGHT_DARK] = s.lightDark.name
        this[Keys.DYNAMIC] = s.dynamicColor
        this[Keys.TINT] = s.tintFromArt
        this[Keys.SOURCE] = s.preferredSource
        this[Keys.KEEP_ON] = s.keepScreenOn
        this[Keys.ANIMATION] = s.animation.name
        this[Keys.SOUND] = s.soundEffects
        this[Keys.ONBOARDED] = s.onboardingDone
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    // --- Per-widget configuration (DESIGN.md 8.4), keyed by appWidgetId ---------------------

    private fun widgetThemeKey(id: Int) = stringPreferencesKey("widget_theme_$id")
    private fun widgetWallpaperKey(id: Int) = booleanPreferencesKey("widget_wallpaper_$id")

    fun widgetConfig(appWidgetId: Int): Flow<WidgetConfig> = context.dataStore.data.map { p ->
        WidgetConfig(
            theme = p[widgetThemeKey(appWidgetId)]?.takeIf { it != WidgetConfig.SAME_AS_APP }?.let(RetroTheme::fromId),
            followWallpaper = p[widgetWallpaperKey(appWidgetId)] ?: false,
        )
    }

    suspend fun setWidgetConfig(appWidgetId: Int, config: WidgetConfig) {
        context.dataStore.edit { p ->
            p[widgetThemeKey(appWidgetId)] = config.theme?.id ?: WidgetConfig.SAME_AS_APP
            p[widgetWallpaperKey(appWidgetId)] = config.followWallpaper && config.theme == null
        }
    }

    suspend fun hasWidgetConfig(appWidgetId: Int): Boolean =
        context.dataStore.data.first().contains(widgetThemeKey(appWidgetId))

    suspend fun clearWidgetConfig(appWidgetId: Int) {
        context.dataStore.edit { p ->
            p.remove(widgetThemeKey(appWidgetId))
            p.remove(widgetWallpaperKey(appWidgetId))
        }
    }
}

/** A widget's own theme. [theme] null = "Same as app"; [followWallpaper] only applies then. */
data class WidgetConfig(val theme: RetroTheme? = null, val followWallpaper: Boolean = false) {
    companion object {
        const val SAME_AS_APP = "app"
    }
}
