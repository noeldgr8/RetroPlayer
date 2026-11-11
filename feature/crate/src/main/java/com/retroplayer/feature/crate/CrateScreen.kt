package com.retroplayer.feature.crate

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.retroplayer.core.history.PlayEntry
import com.retroplayer.core.theme.LocalRetroStyle
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.core.theme.RetroType
import com.retroplayer.feature.player.FlippableSleeve
import com.retroplayer.feature.player.PlayerUiState
import com.retroplayer.feature.player.RetroIcons
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/** Callbacks from the crate to the app. */
class CrateActions(
    val onBack: () -> Unit,
    val onToggleLogging: () -> Unit,
    val onClear: () -> Unit,
    val onDelete: (List<PlayEntry>) -> Unit,
    val onRestore: (List<PlayEntry>) -> Unit,
)

/**
 * EN-03 record crate (DESIGN.md 14.3). Compact (cover screen): Shelf | Stats tabs with a
 * fanned shelf and a detail view. Expanded (inner screen): shelves left, detail / stats right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrateScreen(
    entries: List<PlayEntry>,
    keepHistory: Boolean,
    animate: Boolean,
    actions: CrateActions,
) {
    val groups = remember(entries) { groupAlbums(entries) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var detailOpen by rememberSaveable { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedLabel = stringResource(R.string.crate_deleted, "%s")
    val undo = stringResource(R.string.crate_undo)

    fun delete(items: List<PlayEntry>, name: String) {
        actions.onDelete(items)
        scope.launch {
            val r = snackbar.showSnackbar(deletedLabel.replace("%s", name), actionLabel = undo, withDismissAction = true)
            if (r == SnackbarResult.ActionPerformed) actions.onRestore(items)
        }
    }

    val selected = groups.firstOrNull { it.key == selectedKey } ?: groups.firstOrNull()
    // System Back closes the album detail first (compact), then leaves the crate.
    androidx.activity.compose.BackHandler(enabled = detailOpen) { detailOpen = false }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val expanded = maxWidth >= 600.dp
            Column(Modifier.fillMaxSize()) {
                CrateTopBar(
                    keepHistory,
                    onBack = { if (detailOpen && !expanded) detailOpen = false else actions.onBack() },
                    onToggleLogging = actions.onToggleLogging,
                    onClear = { confirmClear = true },
                )
                if (!keepHistory) {
                    Text(
                        stringResource(R.string.crate_paused_banner),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
                Box(Modifier.weight(1f)) {
                    when {
                        groups.isEmpty() -> EmptyCrate(expanded, actions.onBack)
                        expanded -> ExpandedCrate(groups, entries, selected, tab, { tab = it }, { selectedKey = it.key }, animate, ::delete, actions)
                        detailOpen && selected != null -> AlbumDetail(selected, animate, sleeve = 150.dp, ::delete, actions)
                        else -> Column {
                            CrateTabs(tab, { tab = it }, Modifier.fillMaxWidth())
                            if (tab == 0) {
                                CompactShelf(groups, selected) { g, open ->
                                    selectedKey = g.key
                                    if (open) detailOpen = true
                                }
                            } else {
                                StatsPanel(entries, wide = false)
                            }
                        }
                    }
                }
                PrivacyFooter()
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.crate_clear_title)) },
            text = { Text(stringResource(R.string.crate_clear_body)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; actions.onClear() }) { Text(stringResource(R.string.crate_clear_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.crate_cancel)) } },
        )
    }
}

@Composable
private fun CrateTopBar(keepHistory: Boolean, onBack: () -> Unit, onToggleLogging: () -> Unit, onClear: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
        }
        Text(
            stringResource(R.string.crate_title),
            style = RetroType.appName(LocalRetroStyle.current.displayFont, inner = false),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 4.dp).semantics { heading() },
        )
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.cd_more)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (keepHistory) R.string.crate_pause else R.string.crate_resume)) },
                    onClick = { menu = false; onToggleLogging() },
                )
                DropdownMenuItem(text = { Text(stringResource(R.string.crate_clear)) }, onClick = { menu = false; onClear() })
            }
        }
    }
}

@Composable
private fun CrateTabs(tab: Int, onTab: (Int) -> Unit, modifier: Modifier) {
    PrimaryTabRow(selectedTabIndex = tab, modifier = modifier) {
        val on = MaterialTheme.colorScheme.primary
        val off = MaterialTheme.colorScheme.onSurfaceVariant
        Tab(tab == 0, { onTab(0) }, Modifier.height(48.dp), text = { Text(stringResource(R.string.crate_tab_shelf)) }, selectedContentColor = on, unselectedContentColor = off)
        Tab(tab == 1, { onTab(1) }, Modifier.height(48.dp), text = { Text(stringResource(R.string.crate_tab_stats)) }, selectedContentColor = on, unselectedContentColor = off)
    }
}

// ---------------------------------------------------------------------------------------------
// Compact shelf: fanned sleeves + shelf strip

@Composable
private fun CompactShelf(groups: List<AlbumGroup>, selected: AlbumGroup?, onSelect: (AlbumGroup, Boolean) -> Unit) {
    val start = groups.indexOf(selected).coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = start) { groups.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { groups.getOrNull(pager.currentPage)?.let { onSelect(it, false) } }
    val current = groups.getOrNull(pager.currentPage) ?: return
    val density = LocalDensity.current.density

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(280.dp)) {
            val pageW = 130.dp
            HorizontalPager(
                state = pager,
                pageSize = PageSize.Fixed(pageW),
                contentPadding = PaddingValues(horizontal = (maxWidth - pageW) / 2),
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                val a = offset.absoluteValue.coerceAtMost(2f)
                val g = groups[page]
                Box(Modifier.fillMaxSize().zIndex(-a), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .requiredSize(220.dp)
                            .graphicsLayer {
                                // Centre 220; neighbours 172 at +-48deg; outer 140 at +-58deg.
                                val scale = if (a <= 1f) 1f - a * (48f / 220f) else (172f - (a - 1f) * 32f) / 220f
                                scaleX = scale
                                scaleY = scale
                                rotationY = offset.coerceIn(-2f, 2f).let { if (a <= 1f) it * 48f else (if (it > 0) 1 else -1) * (48f + (a - 1f) * 10f) }
                                cameraDistance = 12f * density
                            }
                            .drawWithContent {
                                drawContent()
                                val dim = if (a <= 1f) a * 0.15f else 0.15f + (a - 1f) * 0.15f
                                drawRect(Color.Black.copy(alpha = dim))
                            }
                            .clickable {
                                if (page == pager.currentPage) onSelect(g, true) else scope.launch { pager.animateScrollToPage(page) }
                            }
                            .semantics { contentDescription = "${g.album}, ${g.artist}" },
                    ) { SleeveImage(g.thumbPath, 220.dp, 6.dp) }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.crate_hint).uppercase(),
                style = RetroType.source.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                current.album.ifBlank { current.artist },
                style = RetroType.headline(LocalRetroStyle.current.displayFont, 26, 32),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                stringResource(
                    R.string.crate_meta, current.artist,
                    pluralStringResource(R.plurals.crate_plays, current.plays, current.plays),
                    relative(current.lastPlayed),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        ShelfStrip(groups, current, Modifier.padding(top = 16.dp)) { i -> scope.launch { pager.animateScrollToPage(i) } }
    }
}

/** Album spines standing on a plank; the selected spine lifts and gets a primary outline. */
@Composable
private fun ShelfStrip(groups: List<AlbumGroup>, selected: AlbumGroup, modifier: Modifier, onTap: (Int) -> Unit) {
    val list = rememberLazyListState()
    val index = groups.indexOf(selected)
    LaunchedEffect(index) { if (index >= 0) list.animateScrollToItem((index - 3).coerceAtLeast(0)) }
    Box(modifier.fillMaxWidth().height(128.dp)) {
        Plank(Modifier.align(Alignment.BottomCenter).fillMaxWidth())
        LazyRow(
            state = list,
            modifier = Modifier.fillMaxSize().padding(bottom = 14.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            items(groups.size) { i ->
                val g = groups[i]
                val tint by rememberTint(g)
                val isSelected = g.key == selected.key
                val h = 92 + (g.key.hashCode().absoluteValue % 24)
                Box(
                    Modifier
                        .offset(y = if (isSelected) (-14).dp else 0.dp)
                        .width(26.dp)
                        .height(h.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(tint, tint.copy(alpha = 0.85f), tint)))
                        .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)) else Modifier)
                        .clickable { onTap(i) }
                        .semantics { contentDescription = "${g.album}, ${g.artist}" },
                )
            }
        }
    }
}

@Composable
private fun Plank(modifier: Modifier) {
    val (top, bottom) = plankColors(LocalRetroStyle.current.theme)
    Box(modifier.height(14.dp).background(Brush.verticalGradient(listOf(top, bottom))))
}

private fun plankColors(theme: RetroTheme): Pair<Color, Color> = when (theme) {
    RetroTheme.VinylLight -> Color(0xFFD8BE96) to Color(0xFFB8956A)
    RetroTheme.VinylDark -> Color(0xFF5A3824) to Color(0xFF3A2416)
    RetroTheme.CassetteLight -> Color(0xFFD2D6DC) to Color(0xFFA9AFB8)
    RetroTheme.CassetteDark -> Color(0xFF3A3D43) to Color(0xFF202226)
}

// ---------------------------------------------------------------------------------------------
// Album detail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumDetail(
    group: AlbumGroup,
    animate: Boolean,
    sleeve: Dp,
    onDelete: (List<PlayEntry>, String) -> Unit,
    actions: CrateActions,
    factsOnBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val thumb by rememberThumb(group.thumbPath)
    val jCard = LocalRetroStyle.current.style == com.retroplayer.core.theme.PlayerStyle.Cassette
    val sleeveState = PlayerUiState(
        title = group.tracks.firstOrNull()?.title.orEmpty(),
        artist = group.artist, album = group.album, sourceLabel = "",
        art = thumb, isPlaying = false, positionMs = 0, durationMs = 0,
    )
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlippableSleeve(sleeveState, sleeve, if (jCard) 4.dp else 6.dp, jCard = jCard, animate = animate)
                Column(Modifier.padding(start = 16.dp)) {
                    Text(
                        group.album.ifBlank { group.artist },
                        style = RetroType.headline(LocalRetroStyle.current.displayFont, 24, 30),
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                    Text(group.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            val bg = if (factsOnBackground) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainer
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Fact(stringResource(R.string.crate_fact_plays), group.plays.toString(), bg, Modifier.weight(1f))
                    Fact(stringResource(R.string.crate_fact_last), relative(group.lastPlayed), bg, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Fact(stringResource(R.string.crate_fact_time), formatDuration(group.listenedMs), bg, Modifier.weight(1f))
                    Fact(stringResource(R.string.crate_fact_first), relative(group.firstPlayed), bg, Modifier.weight(1f))
                }
            }
        }
        item {
            Text(
                stringResource(R.string.crate_played_from),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp).semantics { heading() },
            )
        }
        items(group.tracks, key = { it.title }) { t ->
            val dismiss = rememberSwipeToDismissBoxState()
            LaunchedEffect(dismiss.currentValue) {
                if (dismiss.currentValue != SwipeToDismissBoxValue.Settled) onDelete(t.entries, t.title)
            }
            SwipeToDismissBox(dismiss, backgroundContent = {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer))
            }) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).background(MaterialTheme.colorScheme.background),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(
                        pluralStringResource(R.plurals.crate_plays, t.plays, t.plays),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            TextButton(onClick = { onDelete(group.entries, group.album.ifBlank { group.artist }) }, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.crate_remove_album))
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String, bg: Color, modifier: Modifier) {
    Column(modifier.background(bg, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------------------------------------
// Stats

@Composable
private fun StatsPanel(entries: List<PlayEntry>, wide: Boolean, modifier: Modifier = Modifier) {
    var range by rememberSaveable { mutableStateOf(StatsRange.Week) }
    val s = remember(entries, range) { stats(entries, range) }
    val display = LocalRetroStyle.current.displayFont
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().widthIn(max = 360.dp)) {
            StatsRange.entries.forEachIndexed { i, r ->
                SegmentedButton(
                    selected = r == range,
                    onClick = { range = r },
                    shape = SegmentedButtonDefaults.itemShape(i, StatsRange.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) { Text(stringResource(if (r == StatsRange.Week) R.string.crate_this_week else R.string.crate_this_month)) }
            }
        }
        val totals: @Composable () -> Unit = {
            Column(Modifier.padding(top = 20.dp)) {
                Text(
                    formatDuration(s.totalMs),
                    style = RetroType.headline(display, if (wide) 64 else 44, if (wide) 70 else 50),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(if (range == StatsRange.Week) R.string.crate_listened_week else R.string.crate_listened_month, s.tracks),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val cards: @Composable (Modifier) -> Unit = { m ->
            Column(m.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (s.tracks == 0) {
                    Text(stringResource(R.string.crate_no_stats), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                s.topAlbum?.let {
                    StatCard(stringResource(R.string.crate_top_album), it.album.ifBlank { it.artist }, it.plays, it.thumbPath, round = false)
                }
                s.topArtist?.let { (name, plays) ->
                    StatCard(stringResource(R.string.crate_top_artist), name, plays, s.topArtistThumb, round = true)
                }
                s.topTrack?.let { (title, _, plays) ->
                    StatCard(stringResource(R.string.crate_top_track), title, plays, s.topTrackThumb, round = false)
                }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Box(Modifier.weight(1f)) { totals() }
                cards(Modifier.weight(1f))
            }
        } else {
            totals()
            cards(Modifier)
        }
    }
}

@Composable
private fun StatCard(label: String, name: String, plays: Int, thumb: String?, round: Boolean) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SleeveImage(thumb, 64.dp, if (round) 32.dp else 6.dp)
        Column(Modifier.padding(start = 14.dp)) {
            Text(label.uppercase(), style = RetroType.source, color = MaterialTheme.colorScheme.primary)
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(R.plurals.crate_plays, plays, plays),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Expanded (inner screen): shelves left, detail / stats right

@Composable
private fun ExpandedCrate(
    groups: List<AlbumGroup>,
    entries: List<PlayEntry>,
    selected: AlbumGroup?,
    tab: Int,
    onTab: (Int) -> Unit,
    onSelect: (AlbumGroup) -> Unit,
    animate: Boolean,
    onDelete: (List<PlayEntry>, String) -> Unit,
    actions: CrateActions,
) {
    if (tab == 1) {
        Column(Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
            CrateTabs(tab, onTab, Modifier.width(280.dp))
            StatsPanel(entries, wide = true)
        }
        return
    }
    Row(Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            CrateTabs(tab, onTab, Modifier.width(280.dp))
            LazyColumn(Modifier.weight(1f).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                items(groups.chunked(4)) { row ->
                    Box(Modifier.fillMaxWidth().height(176.dp)) {
                        Plank(Modifier.align(Alignment.BottomCenter).fillMaxWidth())
                        Row(
                            Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            for (g in row) {
                                val isSel = g.key == selected?.key
                                Box(
                                    Modifier
                                        .offset(y = if (isSel) (-8).dp else 0.dp)
                                        .then(if (isSel) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(8.dp)).padding(3.dp) else Modifier)
                                        .clickable { onSelect(g) }
                                        .semantics { contentDescription = "${g.album}, ${g.artist}" },
                                ) { SleeveImage(g.thumbPath, 112.dp, 6.dp) }
                            }
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .width(340.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(28.dp))
                .padding(vertical = 16.dp),
        ) {
            if (selected != null) AlbumDetail(selected, animate, sleeve = 130.dp, onDelete, actions, factsOnBackground = true)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Empty state, footer, helpers

@Composable
private fun EmptyCrate(expanded: Boolean, onBack: () -> Unit) {
    val text: @Composable (Modifier) -> Unit = { m ->
        Column(m, horizontalAlignment = if (expanded) Alignment.Start else Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.crate_empty_title),
                style = RetroType.headline(LocalRetroStyle.current.displayFont, 28, 34),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = if (expanded) TextAlign.Start else TextAlign.Center,
            )
            Text(
                stringResource(R.string.crate_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (expanded) TextAlign.Start else TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp).height(52.dp).then(if (expanded) Modifier else Modifier.fillMaxWidth())) {
                Text(stringResource(R.string.crate_back_to_player))
            }
        }
    }
    val shelf: @Composable (Modifier) -> Unit = { m ->
        Box(m.height(200.dp)) {
            Plank(Modifier.align(Alignment.BottomCenter).fillMaxWidth())
            DashedSleeve(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).size(150.dp))
        }
    }
    if (expanded) {
        Row(Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            shelf(Modifier.weight(1f))
            text(Modifier.weight(1f))
        }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp), verticalArrangement = Arrangement.Center) {
            shelf(Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            text(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DashedSleeve(modifier: Modifier) {
    val color = MaterialTheme.colorScheme.outline
    Canvas(modifier) {
        drawRoundRect(
            color, cornerRadius = CornerRadius(6.dp.toPx()),
            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))),
        )
    }
}

@Composable
private fun PrivacyFooter() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(RetroIcons.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.crate_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SleeveImage(path: String?, size: Dp, corner: Dp) {
    val img by rememberThumb(path)
    val shape = RoundedCornerShape(corner)
    val i = img
    if (i != null) {
        Image(i, null, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(shape))
    } else {
        Box(
            Modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(RetroIcons.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(size * 0.35f))
        }
    }
}

private fun relative(time: Long): String =
    DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
