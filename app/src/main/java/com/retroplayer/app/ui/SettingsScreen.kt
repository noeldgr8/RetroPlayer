package com.retroplayer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroplayer.app.R
import com.retroplayer.core.settings.AnimationLevel
import com.retroplayer.core.settings.AppSettings
import com.retroplayer.core.settings.LightDark
import com.retroplayer.core.settings.FoldedLayout
import com.retroplayer.core.settings.RecordMode
import com.retroplayer.core.settings.Thumb
import com.retroplayer.core.theme.VinylFinish
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.em
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.feature.player.RetroIcons

private enum class SettingsDialog { None, LightDark, Privacy, Licences, ClearCrate }

/** Settings (DESIGN.md 7.4). Every change is saved immediately. */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    theme: RetroTheme,
    sourceLabel: String,
    onBack: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenStyle: () -> Unit,
    onClearCrate: () -> Unit,
    onChooseSource: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf(SettingsDialog.None) }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
            BackTopBar(stringResource(R.string.settings), onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                SectionHeader(stringResource(R.string.section_appearance))
                NavRow(stringResource(R.string.theme_title), stringResource(theme.nameRes), onOpenTheme)
                NavRow(
                    stringResource(R.string.light_and_dark),
                    stringResource(settings.lightDark.labelRes()),
                    onClick = { dialog = SettingsDialog.LightDark },
                )
                NavRow(
                    stringResource(R.string.style_title),
                    styleSummary(settings),
                    onClick = onOpenStyle,
                    new = true,
                )
                SwitchRow(
                    stringResource(R.string.wallpaper_colours),
                    stringResource(R.string.wallpaper_colours_supporting),
                    checked = settings.dynamicColor,
                    onChange = { on -> onUpdate { it.copy(dynamicColor = on) } },
                )
                SwitchRow(
                    stringResource(R.string.tint_from_art),
                    stringResource(R.string.tint_from_art_supporting),
                    checked = settings.tintFromArt,
                    onChange = { on -> onUpdate { it.copy(tintFromArt = on) } },
                )
                SectionDivider()

                SectionHeader(stringResource(R.string.section_playback))
                NavRow(
                    stringResource(R.string.source_app),
                    stringResource(R.string.source_app_supporting, sourceLabel),
                    onChooseSource,
                )
                SwitchRow(
                    stringResource(R.string.keep_screen_on),
                    stringResource(R.string.keep_screen_on_supporting),
                    checked = settings.keepScreenOn,
                    onChange = { on -> onUpdate { it.copy(keepScreenOn = on) } },
                )
                SectionDivider()

                SectionHeader(stringResource(R.string.section_motion))
                AnimationRow(settings.animation) { level -> onUpdate { it.copy(animation = level) } }
                SwitchRow(
                    stringResource(R.string.sound_effects),
                    stringResource(R.string.sound_effects_supporting),
                    checked = settings.soundEffects,
                    onChange = { on -> onUpdate { it.copy(soundEffects = on) } },
                )
                SectionDivider()

                SectionHeader(stringResource(R.string.section_cover), new = true)
                SegmentRow(
                    stringResource(R.string.folded_layout),
                    listOf(FoldedLayout.Standard to R.string.folded_standard, FoldedLayout.MiniDeck to R.string.folded_mini),
                    settings.foldedLayout,
                ) { v -> onUpdate { it.copy(foldedLayout = v) } }
                SegmentRow(
                    stringResource(R.string.thumb),
                    listOf(Thumb.Left to R.string.thumb_left, Thumb.Right to R.string.thumb_right),
                    settings.thumb,
                ) { v -> onUpdate { it.copy(thumb = v) } }
                SectionDivider()

                SectionHeader(stringResource(R.string.section_crate), new = true)
                SwitchRow(
                    stringResource(R.string.keep_history),
                    stringResource(R.string.keep_history_sub),
                    checked = settings.keepHistory,
                    onChange = { on -> onUpdate { it.copy(keepHistory = on) } },
                )
                TextButton(
                    onClick = { dialog = SettingsDialog.ClearCrate },
                    modifier = Modifier.padding(horizontal = 12.dp).heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.clear_crate)) }
                SectionDivider()

                SectionHeader(stringResource(R.string.section_about))
                NavRow(
                    stringResource(R.string.privacy),
                    stringResource(R.string.privacy_supporting),
                    onClick = { dialog = SettingsDialog.Privacy },
                )
                NavRow(stringResource(R.string.licences), null, onClick = { dialog = SettingsDialog.Licences })
            }
        }
    }

    when (dialog) {
        SettingsDialog.None -> Unit
        SettingsDialog.LightDark -> ChoiceDialog(
            title = stringResource(R.string.light_and_dark),
            options = LightDark.entries.map { it to stringResource(it.labelRes()) },
            selected = settings.lightDark,
            onSelect = { mode -> onUpdate { it.copy(lightDark = mode) } },
            onDismiss = { dialog = SettingsDialog.None },
        )
        SettingsDialog.Privacy -> InfoDialog(
            stringResource(R.string.privacy),
            stringResource(R.string.privacy_body),
        ) { dialog = SettingsDialog.None }
        SettingsDialog.Licences -> InfoDialog(
            stringResource(R.string.licences),
            stringResource(R.string.licences_body),
        ) { dialog = SettingsDialog.None }
        SettingsDialog.ClearCrate -> AlertDialog(
            onDismissRequest = { dialog = SettingsDialog.None },
            title = { Text(stringResource(R.string.clear_crate_title)) },
            text = { Text(stringResource(R.string.clear_crate_body)) },
            confirmButton = {
                TextButton(onClick = { dialog = SettingsDialog.None; onClearCrate() }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = { TextButton(onClick = { dialog = SettingsDialog.None }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun LightDark.labelRes(): Int = when (this) {
    LightDark.FollowSystem -> R.string.follow_system
    LightDark.AlwaysLight -> R.string.always_light
    LightDark.AlwaysDark -> R.string.always_dark
}

@Composable
private fun SectionHeader(text: String, new: Boolean = false) {
    Row(
        Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
    Text(
        text,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { heading() },
    )
    if (new) NewBadge(Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** List item with a trailing chevron. */
@Composable
private fun NavRow(title: String, supporting: String?, onClick: () -> Unit, new: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                if (new) NewBadge(Modifier.padding(start = 8.dp))
            }
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(RetroIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Full / Reduced / Off segmented button (height 48, selected = primaryContainer + check). */
@Composable
private fun AnimationRow(level: AnimationLevel, onChange: (AnimationLevel) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
            stringResource(R.string.animation),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.animation_supporting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val labels = listOf(R.string.anim_full, R.string.anim_reduced, R.string.anim_off)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            AnimationLevel.entries.forEachIndexed { i, l ->
                SegmentedButton(
                    selected = l == level,
                    onClick = { onChange(l) },
                    shape = SegmentedButtonDefaults.itemShape(i, AnimationLevel.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.height(48.dp),
                ) { Text(stringResource(labels[i])) }
            }
        }
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** Small "NEW" badge for v1.1 rows (primaryContainer, 11 sp caps). */
@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.badge_new),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.08.em,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Headline with a 2-segment button below it (48 tall). */
@Composable
private fun <T> SegmentRow(title: String, options: List<Pair<T, Int>>, selected: T, onChange: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            options.forEachIndexed { i, (value, label) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.height(48.dp),
                ) { Text(stringResource(label)) }
            }
        }
    }
}

/** e.g. "Automatic · song name label off". */
@Composable
private fun styleSummary(s: AppSettings): String {
    val records = when (s.recordMode) {
        RecordMode.Automatic -> stringResource(R.string.style_auto)
        RecordMode.AlwaysClassic -> stringResource(R.string.style_always_classic)
        RecordMode.Favourite -> stringResource(
            when (s.favouriteFinish) {
                VinylFinish.Classic -> R.string.finish_classic
                VinylFinish.Translucent -> R.string.finish_translucent
                VinylFinish.Marble -> R.string.finish_marble
                VinylFinish.Splatter -> R.string.finish_splatter
                VinylFinish.Picture -> R.string.finish_picture
            },
        )
    }
    return stringResource(R.string.style_summary, records, stringResource(if (s.songLabel) R.string.on else R.string.off))
}
