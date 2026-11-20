package com.goucem.aurora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goucem.aurora.R
import com.goucem.aurora.data.ArtKey
import com.goucem.aurora.data.Track
import com.goucem.aurora.data.library.FolderTree
import com.goucem.aurora.ui.*
import com.goucem.aurora.ui.components.*
import com.goucem.aurora.ui.theme.*

/** One screen for album, artist, genre, folder and the "see all" lists from Home. */
@Composable
fun CollectionScreen(type: String, key: String, onBack: () -> Unit) {
    val c = LocalContainer.current
    val p = LocalPalette.current
    val bottom = LocalBottomPadding.current
    val tracksAll by c.library.tracks.collectAsStateWithLifecycle()
    val albums by c.library.albums.collectAsStateWithLifecycle()
    val artists by c.library.artists.collectAsStateWithLifecycle()
    val genres by c.library.genres.collectAsStateWithLifecycle()
    val index by c.library.index.collectAsStateWithLifecycle()
    val stats by c.userData.stats.collectAsStateWithLifecycle()
    val favs by c.userData.favorites.collectAsStateWithLifecycle()
    val nowKey by rememberNowPlayingKey()

    val title: String; val subtitle: String; val tracks: List<Track>; val art: ArtKey?
    when (type) {
        "album" -> {
            val a = albums.firstOrNull { it.key == key }
            title = if (a != null) albumLabel(a.name) else ""; subtitle = a?.artist?.let { artistLabel(it) }.orEmpty(); tracks = a?.tracks.orEmpty(); art = tracks.firstOrNull()?.art
        }
        "artist" -> {
            val a = artists.firstOrNull { it.key == key }
            title = a?.name.orEmpty(); subtitle = a?.let { "${albumsLabel(it.albumCount)} · ${songsLabel(it.tracks.size)}" }.orEmpty(); tracks = a?.tracks.orEmpty(); art = tracks.firstOrNull()?.art
        }
        "genre" -> { title = key; tracks = genres.firstOrNull { it.name == key }?.tracks.orEmpty(); subtitle = songsLabel(tracks.size); art = null }
        "folder" -> { title = key.substringAfterLast('/'); tracks = FolderTree.allTracksUnder(tracksAll, key); subtitle = songsLabel(tracks.size); art = null }
        "favorites" -> { title = stringResource(R.string.section_favorites); tracks = favs.mapNotNull { index.byUri[it] }.sortedBy { it.title.lowercase() }; subtitle = songsLabel(tracks.size); art = null }
        "recent_added" -> { title = stringResource(R.string.section_recently_added); tracks = tracksAll.sortedByDescending { it.dateAddedSec }.take(100); subtitle = songsLabel(tracks.size); art = null }
        "recent_played" -> { title = stringResource(R.string.section_recently_played); tracks = stats.entries.sortedByDescending { it.value.lastPlayedMs }.mapNotNull { index.byUri[it.key] }.take(100); subtitle = songsLabel(tracks.size); art = null }
        else -> { title = stringResource(R.string.section_most_played); tracks = stats.entries.filter { it.value.count >= 1 }.sortedByDescending { it.value.count }.mapNotNull { index.byUri[it.key] }.take(100); subtitle = songsLabel(tracks.size); art = null }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(if (art == null) title else "", onBack)
        if (tracks.isEmpty()) {
            EmptyState(Icons.Rounded.MusicOff, stringResource(R.string.collection_empty_title), stringResource(R.string.collection_empty_message))
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (art != null) {
                        Artwork(art, Modifier.size(220.dp).glass(RoundedCornerShape(28.dp)), sizePx = 640, shape = RoundedCornerShape(28.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(title, style = AuroraType.title, color = p.text, textAlign = TextAlign.Center, maxLines = 2)
                    }
                    Text(subtitle, style = AuroraType.bodySoft, color = p.textSoft, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton(stringResource(R.string.cd_play), icon = Icons.Rounded.PlayArrow) { c.player.play(tracks, 0) }
                        SecondaryButton(stringResource(R.string.cd_shuffle), icon = Icons.Rounded.Shuffle) { c.player.shuffleAll(tracks) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            itemsIndexed(tracks, key = { _, t -> t.key }) { i, t ->
                TrackRow(t, nowKey == t.key, onClick = { c.player.play(tracks, i) })
            }
        }
    }
}
