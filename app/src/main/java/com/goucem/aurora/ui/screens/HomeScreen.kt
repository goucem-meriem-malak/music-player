package com.goucem.aurora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goucem.aurora.R
import com.goucem.aurora.data.Track
import com.goucem.aurora.ui.*
import com.goucem.aurora.ui.components.*
import com.goucem.aurora.ui.theme.*

@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Text(
        stringResource(R.string.wordmark), modifier,
        style = AuroraType.wordmark.merge(TextStyle(brush = Brush.horizontalGradient(listOf(p.text, p.accentSoft, p.accent)))),
    )
}

@Composable
fun TrackTile(track: Track, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.width(136.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)) {
        Artwork(track.art, Modifier.size(136.dp).glass(androidx.compose.foundation.shape.CircleShape), sizePx = 384, shape = androidx.compose.foundation.shape.CircleShape)
        Spacer(Modifier.height(8.dp))
        Text(track.title, style = AuroraType.body, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(artistLabel(track.artist), style = AuroraType.caption, color = p.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TileSection(title: String, tracks: List<Track>, seeAllType: String) {
    if (tracks.isEmpty()) return
    val ui = LocalUi.current
    val player = LocalContainer.current.player
    SectionHeader(title, actionLabel = stringResource(R.string.see_all)) { ui.openCollection(seeAllType, "") }
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(tracks, key = { _, t -> t.key }) { i, t -> TrackTile(t) { player.play(tracks, i) } }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
fun HomeScreen(onSearch: () -> Unit, onSettings: () -> Unit) {
    val c = LocalContainer.current
    val p = LocalPalette.current
    val bottom = LocalBottomPadding.current
    val tracks by c.library.tracks.collectAsStateWithLifecycle()
    val albums by c.library.albums.collectAsStateWithLifecycle()
    val index by c.library.index.collectAsStateWithLifecycle()
    val stats by c.userData.stats.collectAsStateWithLifecycle()
    val favs by c.userData.favorites.collectAsStateWithLifecycle()

    val recentlyAdded = remember(tracks) { tracks.sortedByDescending { it.dateAddedSec }.take(20) }
    val recentlyPlayed = remember(stats, index) { stats.entries.sortedByDescending { it.value.lastPlayedMs }.mapNotNull { index.byUri[it.key] }.take(20) }
    val mostPlayed = remember(stats, index) { stats.entries.filter { it.value.count >= 2 }.sortedByDescending { it.value.count }.mapNotNull { index.byUri[it.key] }.take(20) }
    val favorites = remember(favs, index) { favs.mapNotNull { index.byUri[it] }.take(20) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 14.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Wordmark(Modifier.weight(1f))
            GlassIconButton(Icons.Rounded.Settings, stringResource(R.string.settings_title), onClick = onSettings)
        }
        LibraryGate {
            LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
                item {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth().glass(RoundedCornerShape(20.dp), strong = true)
                            .clickable(onClick = onSearch).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = p.textSoft)
                        Text(stringResource(R.string.search_hint), style = AuroraType.body, color = p.textFaint)
                    }
                }
                item {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PrimaryButton(stringResource(R.string.shuffle_all), icon = Icons.Rounded.Shuffle) { c.player.shuffleAll(tracks) }
                        SecondaryButton(stringResource(R.string.play_all), icon = Icons.Rounded.PlayArrow) { c.player.play(tracks, 0) }
                    }
                    Text("${songsLabel(tracks.size)} · ${albumsLabel(albums.size)}", style = AuroraType.caption, color = p.textFaint, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
                item { TileSection(stringResource(R.string.section_recently_played), recentlyPlayed, "recent_played") }
                item { TileSection(stringResource(R.string.section_recently_added), recentlyAdded, "recent_added") }
                item { TileSection(stringResource(R.string.section_most_played), mostPlayed, "most_played") }
                item { TileSection(stringResource(R.string.section_favorites), favorites, "favorites") }
            }
        }
    }
}
