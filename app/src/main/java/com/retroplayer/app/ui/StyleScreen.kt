package com.retroplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroplayer.app.R
import com.retroplayer.core.settings.AppSettings
import com.retroplayer.core.settings.CassetteMode
import com.retroplayer.core.settings.LabelChoice
import com.retroplayer.core.settings.RecordMode
import com.retroplayer.core.theme.CassetteShell
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.ObjectLook
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.core.theme.VinylFinish
import com.retroplayer.core.theme.VinylTokens
import com.retroplayer.feature.player.CassettePlayer
import com.retroplayer.feature.player.Turntable
import com.retroplayer.feature.player.rememberReelState
import com.retroplayer.feature.player.rememberSpinState

/** EN-01 "Record and tape style". Changes apply live. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleScreen(
    settings: AppSettings,
    theme: RetroTheme,
    look: ObjectLook,
    art: ImageBitmap?,
    songTitle: String?,
    animate: Boolean,
    onBack: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
            BackTopBar(stringResource(R.string.style_title), onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                StylePreview(theme, look, art, songTitle, animate)

                GroupHeader(stringResource(R.string.style_records))
                Column(Modifier.selectableGroup()) {
                    Choice(R.string.style_auto, R.string.style_records_auto_sub, settings.recordMode == RecordMode.Automatic) {
                        onUpdate { it.copy(recordMode = RecordMode.Automatic) }
                    }
                    Choice(R.string.style_always_classic, R.string.style_always_classic_sub, settings.recordMode == RecordMode.AlwaysClassic) {
                        onUpdate { it.copy(recordMode = RecordMode.AlwaysClassic) }
                    }
                    Choice(R.string.style_fav_finish, R.string.style_fav_finish_sub, settings.recordMode == RecordMode.Favourite) {
                        onUpdate { it.copy(recordMode = RecordMode.Favourite) }
                    }
                }
                if (settings.recordMode == RecordMode.Favourite) {
                    ChipRow(
                        listOf(
                            VinylFinish.Translucent to R.string.finish_translucent,
                            VinylFinish.Marble to R.string.finish_marble,
                            VinylFinish.Splatter to R.string.finish_splatter,
                            VinylFinish.Picture to R.string.finish_picture,
                            VinylFinish.Classic to R.string.finish_classic,
                        ),
                        settings.favouriteFinish,
                    ) { f -> onUpdate { it.copy(favouriteFinish = f) } }
                }

                GroupHeader(stringResource(R.string.style_cassettes))
                Column(Modifier.selectableGroup()) {
                    Choice(R.string.style_auto, R.string.style_cassettes_auto_sub, settings.cassetteMode == CassetteMode.Automatic) {
                        onUpdate { it.copy(cassetteMode = CassetteMode.Automatic) }
                    }
                    Choice(R.string.style_always_clear, R.string.style_always_clear_sub, settings.cassetteMode == CassetteMode.AlwaysClear) {
                        onUpdate { it.copy(cassetteMode = CassetteMode.AlwaysClear) }
                    }
                    Choice(R.string.style_fav_shell, null, settings.cassetteMode == CassetteMode.Favourite) {
                        onUpdate { it.copy(cassetteMode = CassetteMode.Favourite) }
                    }
                }
                if (settings.cassetteMode == CassetteMode.Favourite) {
                    ChipRow(
                        listOf(
                            CassetteShell.Clear to R.string.shell_clear,
                            CassetteShell.Black to R.string.shell_black,
                            CassetteShell.White to R.string.shell_white,
                            CassetteShell.Tinted to R.string.shell_tinted,
                        ),
                        settings.favouriteShell,
                    ) { s -> onUpdate { it.copy(favouriteShell = s) } }
                }

                SwitchRow(
                    stringResource(R.string.song_label),
                    stringResource(R.string.song_label_sub),
                    checked = settings.songLabel,
                    onChange = { on -> onUpdate { it.copy(songLabel = on) } },
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (settings.songLabel) {
                    ChipRow(
                        listOf(
                            LabelChoice.Automatic to R.string.style_auto,
                            LabelChoice.Ruled to R.string.label_ruled,
                            LabelChoice.Printed to R.string.label_printed,
                            LabelChoice.Handwritten to R.string.label_handwritten,
                        ),
                        settings.labelChoice,
                    ) { c -> onUpdate { it.copy(labelChoice = c) } }
                }
            }
        }
    }
}

/** Preview card: turntable 168 and cassette 232 side by side, both playing, current album. */
@Composable
private fun StylePreview(theme: RetroTheme, look: ObjectLook, art: ImageBitmap?, songTitle: String?, animate: Boolean) {
    val vinyl = (if (theme.isDark) RetroTheme.VinylDark else RetroTheme.VinylLight)
    val cassette = (if (theme.isDark) RetroTheme.CassetteDark else RetroTheme.CassetteLight)
    val spin = rememberSpinState()
    val reels = rememberReelState()
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(28.dp))
            .padding(16.dp)
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(168f), contentAlignment = Alignment.Center) {
            Turntable(
                vinyl.playerTokens as VinylTokens, art, isPlaying = true, animate = animate, spin = spin,
                labelColor = vinyl.colorScheme.primaryContainer, look = look,
                modifier = Modifier.widthIn(max = 168.dp).fillMaxWidth(),
            )
        }
        Box(Modifier.weight(232f), contentAlignment = Alignment.Center) {
            CassettePlayer(
                cassette.playerTokens as CassetteTokens, progress = 0.35f, isPlaying = true, animate = animate,
                reels = reels, look = look, songTitle = songTitle ?: stringResource(R.string.style_sample_title),
                modifier = Modifier.widthIn(max = 232.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp).semantics { heading() },
    )
}

@Composable
private fun Choice(title: Int, supporting: Int?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 16.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    stringResource(supporting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Chips: height 36 (48 touch target), corner 8, 1 dp outline; selected primaryContainer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().padding(start = 64.dp, end = 24.dp, bottom = 8.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((value, label) in options) {
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(stringResource(label)) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = value == selected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.height(36.dp),
            )
        }
    }
}
