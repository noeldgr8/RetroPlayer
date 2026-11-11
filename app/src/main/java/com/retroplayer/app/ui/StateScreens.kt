package com.retroplayer.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroplayer.app.R
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.LocalRetroStyle
import com.retroplayer.core.theme.RetroType
import com.retroplayer.core.theme.VinylTokens
import com.retroplayer.feature.player.CassettePlayer
import com.retroplayer.feature.player.PlayerMotion
import com.retroplayer.feature.player.RetroIcons
import com.retroplayer.feature.player.RetroTopBar
import com.retroplayer.feature.player.Turntable


// ---------------------------------------------------------------------------------------------
// 7.2 Empty state

@Composable
fun EmptyScreen(
    motion: PlayerMotion,
    onSettings: () -> Unit,
    onOpenSpotify: () -> Unit,
    onChooseSource: () -> Unit,
) {
    val retro = LocalRetroStyle.current
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = MaxContentWidth).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RetroTopBar(onSettings)
            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().heightIn(min = 200.dp, max = 380.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (val t = retro.tokens) {
                    is VinylTokens -> Turntable(
                        t, art = null, isPlaying = false, idle = true, animate = false,
                        spin = motion.spin,
                        modifier = Modifier.width(minOf(320.dp, maxWidth, maxHeight)),
                    )
                    is CassetteTokens -> CassettePlayer(
                        t, progress = 0f, isPlaying = false, animate = false,
                        reels = motion.reels,
                        modifier = Modifier.width(minOf(390.dp, maxWidth, maxHeight * 400f / 260f)),
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.empty_title),
                    style = RetroType.headline(retro.displayFont),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.empty_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onOpenSpotify, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.open_spotify))
                }
                TextButton(onClick = onChooseSource, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.choose_source))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 7.3 Error: access revoked / not yet granted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessScreen(onGrant: () -> Unit) {
    var showWhy by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 32.dp, end = 32.dp, top = 64.dp + 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Box(
                    Modifier
                        .size(112.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(36.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        RetroIcons.NotificationsOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.access_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.access_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                MockSettingsCard(
                    header = stringResource(R.string.mock_settings_header),
                    supporting = stringResource(R.string.mock_off),
                    on = false,
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, bottom = 28.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onGrant, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.grant_access))
                }
                TextButton(onClick = { showWhy = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.why_access))
                }
            }
        }
    }

    if (showWhy) {
        // Stand-in for onboarding step 2 as a sheet (onboarding is the next build pass).
        ModalBottomSheet(onDismissRequest = { showWhy = false }) {
            Column(
                Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.why_title),
                    style = RetroType.headline(LocalRetroStyle.current.displayFont, 28, 34),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                for (line in listOf(R.string.why_1, R.string.why_2, R.string.why_3)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            RetroIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(stringResource(line), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

/** Mock of the system notification-access row (used on the error screen and in onboarding). */
@Composable
fun MockSettingsCard(header: String, supporting: String?, on: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                header,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        RetroIcons.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.bodyLarge)
                    if (supporting != null) {
                        Text(
                            supporting,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(checked = on, onCheckedChange = null)
            }
        }
    }
}
