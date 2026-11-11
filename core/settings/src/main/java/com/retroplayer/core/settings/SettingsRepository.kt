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
import com.retroplayer.core.theme.AlbumPalette
import com.retroplayer.core.theme.CassetteShell
import com.retroplayer.core.theme.LabelStyle
import com.retroplayer.core.theme.ObjectLook
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.core.theme.VinylFinish
import com.retroplayer.core.theme.autoFinish
import com.retroplayer.core.theme.autoLabel
import com.retroplayer.core.theme.autoShell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class LightDark { FollowSystem, AlwaysLight, AlwaysDark }
enum class AnimationLevel { Full, Reduced, Off }

/** EN-01 settings. */
enum class RecordMode { Automatic, AlwaysClassic, Favourite }
enum class CassetteMode { Automatic, AlwaysClear, Favourite }
enum class LabelChoice { Automatic, Ruled, Printed, Handwritten }

/** EN-05 settings. */
enum class FoldedLayout { Standard, MiniDeck }
enum class Thumb { Left, Right }

/** All app settings with their defaults. */
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
    val recordMode: RecordMode = RecordMode.Automatic,
    val favouriteFinish: VinylFinish = VinylFinish.Translucent,
    val cassetteMode: CassetteMode = CassetteMode.Automatic,
    val favouriteShell: CassetteShell = CassetteShell.Clear,
    val songLabel: Boolean = false,
    val labelChoice: LabelChoice = LabelChoice.Automatic,
    val foldedLayout: FoldedLayout = FoldedLayout.Standard,
    val thumb: Thumb = Thumb.Right,
    val keepHistory: Boolean = true,
) {
    fun effectiveTheme(systemDark: Boolean): RetroTheme = when (lightDark) {
        LightDark.FollowSystem -> theme.withDark(systemDark)
        LightDark.AlwaysLight -> theme.withDark(false)
        LightDark.AlwaysDark -> theme.withDark(true)
    }

    /**
     * What the player objects draw for an album. Automatic choices come only from the art
     * and album key, so the same album looks the same everywhere. Picture disc is never
     * automatic (artwork rule still open).
     */
    fun lookFor(palette: AlbumPalette?): ObjectLook {
        val finish = when (recordMode) {
            RecordMode.AlwaysClassic -> VinylFinish.Classic
            RecordMode.Favourite -> favouriteFinish
            RecordMode.Automatic -> palette?.autoFinish() ?: VinylFinish.Classic
        }
        val shell = when (cassetteMode) {
            CassetteMode.AlwaysClear -> CassetteShell.Clear
            CassetteMode.Favourite -> favouriteShell
            CassetteMode.Automatic -> palette?.autoShell() ?: CassetteShell.Clear
        }
        val label = if (!songLabel) null else when (labelChoice) {
            LabelChoice.Ruled -> LabelStyle.Ruled
            LabelChoice.Printed -> LabelStyle.Printed
            LabelChoice.Handwritten -> LabelStyle.Handwritten
            LabelChoice.Automatic -> palette?.autoLabel() ?: LabelStyle.Ruled
        }
        return ObjectLook(finish, shell, label, palette)
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
        val RECORD_MODE = stringPreferencesKey("record_mode")
        val FAV_FINISH = stringPreferencesKey("favourite_finish")
        val CASSETTE_MODE = stringPreferencesKey("cassette_mode")
        val FAV_SHELL = stringPreferencesKey("favourite_shell")
        val SONG_LABEL = booleanPreferencesKey("song_label")
        val LABEL_CHOICE = stringPreferencesKey("label_choice")
        val FOLDED_LAYOUT = stringPreferencesKey("folded_layout")
        val THUMB = stringPreferencesKey("thumb")
        val KEEP_HISTORY = booleanPreferencesKey("keep_history")
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
            recordMode = enumOrDefault(this[Keys.RECORD_MODE], d.recordMode),
            favouriteFinish = enumOrDefault(this[Keys.FAV_FINISH], d.favouriteFinish),
            cassetteMode = enumOrDefault(this[Keys.CASSETTE_MODE], d.cassetteMode),
            favouriteShell = enumOrDefault(this[Keys.FAV_SHELL], d.favouriteShell),
            songLabel = this[Keys.SONG_LABEL] ?: d.songLabel,
            labelChoice = enumOrDefault(this[Keys.LABEL_CHOICE], d.labelChoice),
            foldedLayout = enumOrDefault(this[Keys.FOLDED_LAYOUT], d.foldedLayout),
            thumb = enumOrDefault(this[Keys.THUMB], d.thumb),
            keepHistory = this[Keys.KEEP_HISTORY] ?: d.keepHistory,
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
        this[Keys.RECORD_MODE] = s.recordMode.name
        this[Keys.FAV_FINISH] = s.favouriteFinish.name
        this[Keys.CASSETTE_MODE] = s.cassetteMode.name
        this[Keys.FAV_SHELL] = s.favouriteShell.name
        this[Keys.SONG_LABEL] = s.songLabel
        this[Keys.LABEL_CHOICE] = s.labelChoice.name
        this[Keys.FOLDED_LAYOUT] = s.foldedLayout.name
        this[Keys.THUMB] = s.thumb.name
        this[Keys.KEEP_HISTORY] = s.keepHistory
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    // --- Per-widget configuration, keyed by appWidgetId ---------------------

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
