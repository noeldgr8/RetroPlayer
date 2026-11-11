package com.retroplayer.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroplayer.app.R
import com.retroplayer.core.media.SPOTIFY_PACKAGE
import com.retroplayer.core.media.SourceApp
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.PlayerTokens
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.core.theme.VinylTokens
import com.retroplayer.feature.player.CassettePlayer
import com.retroplayer.feature.player.Turntable

/** Material screens are designed at cover width; wider windows centre a 560 dp column. */
internal val MaxContentWidth = 560.dp

/** Top app bar with a back arrow (Settings, Theme). */
@Composable
internal fun BackTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
        }
        Text(
            title,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() },
        )
    }
}

/** Turntable or cassette for any theme's tokens, at a given width (used by cards and previews). */
@Composable
internal fun ThemeObject(
    tokens: PlayerTokens,
    turntableWidth: Dp,
    cassetteWidth: Dp,
    playing: Boolean,
    animate: Boolean,
    labelColor: androidx.compose.ui.graphics.Color,
) {
    when (tokens) {
        is VinylTokens -> Turntable(
            tokens, art = null, isPlaying = playing, animate = animate,
            labelColor = labelColor,
            modifier = Modifier.width(turntableWidth),
        )
        is CassetteTokens -> CassettePlayer(
            tokens, progress = 0.35f, isPlaying = playing, animate = animate,
            modifier = Modifier.width(cassetteWidth),
        )
    }
}

/**
 * A theme card filled with that theme's background, showing its object (paused) and name.
 * Selected: 3 dp ring in the current primary; others 1 dp outlineVariant.
 */
@Composable
internal fun ThemeCard(
    theme: RetroTheme,
    selected: Boolean,
    onClick: () -> Unit,
    height: Dp,
    turntableWidth: Dp,
    cassetteWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val scheme = theme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.background,
        border = if (selected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = modifier
            .height(height)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                ThemeObject(
                    theme.playerTokens, turntableWidth, cassetteWidth,
                    playing = false, animate = false, labelColor = scheme.primaryContainer,
                )
            }
            Text(
                stringResource(theme.nameRes),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** 2 x 2 grid of the four theme cards. */
@Composable
internal fun ThemeGrid(
    current: RetroTheme,
    onPick: (RetroTheme) -> Unit,
    cardHeight: Dp,
    turntableWidth: Dp,
    cassetteWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in RetroTheme.entries.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (t in row) {
                    ThemeCard(
                        t, selected = t == current, onClick = { onPick(t) },
                        height = cardHeight, turntableWidth = turntableWidth, cassetteWidth = cassetteWidth,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** List item with a trailing switch; the whole row toggles (min height 72). */
@Composable
internal fun SwitchRow(
    title: String,
    supporting: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
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
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Single-choice dialog with radio rows (min 52). */
@Composable
internal fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                for ((value, label) in options) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .selectable(
                                selected = value == selected,
                                onClick = { onSelect(value); onDismiss() },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** "Choose source app": apps with an active media session, Spotify always offered. */
@Composable
fun SourceDialog(
    sources: List<SourceApp>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val spotifyLabel = stringResource(R.string.spotify)
    val options = (listOf(SourceApp(SPOTIFY_PACKAGE, spotifyLabel)) + sources)
        .distinctBy { it.packageName }
        .map { it.packageName to it.label }
    ChoiceDialog(stringResource(R.string.choose_source), options, selected, onSelect, onDismiss)
}
