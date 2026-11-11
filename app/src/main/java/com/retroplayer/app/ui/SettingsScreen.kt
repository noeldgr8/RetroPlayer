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
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.feature.player.RetroIcons

private enum class SettingsDialog { None, LightDark, Privacy, Licences }

/** Settings (DESIGN.md 7.4). Every change is saved immediately. */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    theme: RetroTheme,
    sourceLabel: String,
    onBack: () -> Unit,
    onOpenTheme: () -> Unit,
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
    }
}

private fun LightDark.labelRes(): Int = when (this) {
    LightDark.FollowSystem -> R.string.follow_system
    LightDark.AlwaysLight -> R.string.always_light
    LightDark.AlwaysDark -> R.string.always_dark
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp).semantics { heading() },
    )
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
private fun NavRow(title: String, supporting: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
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
