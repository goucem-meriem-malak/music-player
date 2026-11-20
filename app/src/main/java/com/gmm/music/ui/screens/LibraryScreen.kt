package com.gmm.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmm.music.R
import com.gmm.music.data.Album
import com.gmm.music.data.library.FolderTree
import com.gmm.music.ui.*
import com.gmm.music.ui.components.*
import com.gmm.music.ui.theme.*

private enum class LibTab(val labelRes: Int) {
    Songs(R.string.tab_songs), Albums(R.string.tab_albums), Artists(R.string.tab_artists),
    Genres(R.string.tab_genres), Folders(R.string.tab_folders), Favorites(R.string.tab_favorites)
}

private enum class SongSort(val labelRes: Int) { Title(R.string.sort_title), Artist(R.string.sort_artist), Added(R.string.sort_added), Duration(R.string.sort_duration) }

@Composable
fun LibraryScreen(initialTab: Int = 0) {
    val c = LocalContainer.current
    val ui = LocalUi.current
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, LibTab.values().lastIndex)) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle(stringResource(R.string.nav_library))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(LibTab.values().toList()) { t -> GlassChip(stringResource(t.labelRes), tab == t.ordinal) { tab = t.ordinal } }
        }
        Spacer(Modifier.height(8.dp))
        LibraryGate {
            when (LibTab.values()[tab]) {
                LibTab.Songs -> SongsTab()
                LibTab.Albums -> AlbumsTab()
                LibTab.Artists -> ArtistsTab()
                LibTab.Genres -> GenresTab()
                LibTab.Folders -> FoldersTab()
                LibTab.Favorites -> FavoritesTab()
            }
        }
    }
}

@Composable
private fun SongsTab() {
    val c = LocalContainer.current
    val bottom = LocalBottomPadding.current
    val tracks by c.library.tracks.collectAsStateWithLifecycle()
    val nowKey by rememberNowPlayingKey()
    var sort by rememberSaveable { mutableStateOf(SongSort.Title) }
    val sorted = remember(tracks, sort) {
        when (sort) {
            SongSort.Title -> tracks
            SongSort.Artist -> tracks.sortedWith(compareBy({ it.artist.lowercase() }, { it.album.lowercase() }, { it.trackNumber }))
            SongSort.Added -> tracks.sortedByDescending { it.dateAddedSec }
            SongSort.Duration -> tracks.sortedByDescending { it.durationMs }
        }
    }
    LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
        item {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(songsLabel(tracks.size), style = MusicType.caption, color = LocalPalette.current.textSoft, modifier = Modifier.weight(1f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(2f)) {
                    items(SongSort.values().toList()) { s -> GlassChip(stringResource(s.labelRes), sort == s) { sort = s } }
                }
            }
        }
        itemsIndexed(sorted, key = { _, t -> t.key }, contentType = { _, _ -> "track" }) { i, t ->
            TrackRow(t, nowKey == t.key, onClick = { c.player.play(sorted, i) })
        }
    }
}

@Composable
fun AlbumTile(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)) {
        Artwork(album.tracks.firstOrNull()?.art, Modifier.fillMaxWidth().aspectRatioSquare().glass(RoundedCornerShape(20.dp)), sizePx = 384, shape = RoundedCornerShape(20.dp))
        Spacer(Modifier.height(8.dp))
        Text(albumLabel(album.name), style = MusicType.body, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(artistLabel(album.artist), style = MusicType.caption, color = p.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun Modifier.aspectRatioSquare(): Modifier = this.then(Modifier.aspectRatio(1f))

@Composable
private fun AlbumsTab() {
    val c = LocalContainer.current
    val ui = LocalUi.current
    val bottom = LocalBottomPadding.current
    val albums by c.library.albums.collectAsStateWithLifecycle()
    if (albums.isEmpty()) { EmptyState(Icons.Rounded.Album, stringResource(R.string.no_albums_title), stringResource(R.string.no_albums_message)); return }
    LazyVerticalGrid(
        GridCells.Fixed(2), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottom),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        gridItems(albums, key = { it.key }) { a -> AlbumTile(a, onClick = { ui.openAlbum(a.key) }) }
    }
}

@Composable
private fun ArtistsTab() {
    val c = LocalContainer.current
    val ui = LocalUi.current
    val p = LocalPalette.current
    val bottom = LocalBottomPadding.current
    val artists by c.library.artists.collectAsStateWithLifecycle()
    if (artists.isEmpty()) { EmptyState(Icons.Rounded.Person, stringResource(R.string.no_artists_title), stringResource(R.string.no_artists_message)); return }
    LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
        items(artists, key = { it.key }) { a ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { ui.openArtist(a.key) }.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(a.tracks.first().art, Modifier.size(56.dp), sizePx = 128, shape = CircleShape)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.name, style = MusicType.body, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${albumsLabel(a.albumCount)} · ${songsLabel(a.tracks.size)}", style = MusicType.caption, color = p.textSoft)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = p.textFaint)
            }
        }
    }
}

@Composable
private fun GenresTab() {
    val c = LocalContainer.current
    val ui = LocalUi.current
    val p = LocalPalette.current
    val bottom = LocalBottomPadding.current
    val genres by c.library.genres.collectAsStateWithLifecycle()
    if (genres.isEmpty()) { EmptyState(Icons.Rounded.Category, stringResource(R.string.no_genres_title), stringResource(R.string.no_genres_message)); return }
    LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
        items(genres, key = { it.name }) { g ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { ui.openCollection("genre", g.name) }.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(56.dp).glass(CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.GraphicEq, null, tint = p.accentSoft) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.name, style = MusicType.body, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(songsLabel(g.tracks.size), style = MusicType.caption, color = p.textSoft)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = p.textFaint)
            }
        }
    }
}

@Composable
private fun FavoritesTab() {
    val c = LocalContainer.current
    val bottom = LocalBottomPadding.current
    val favs by c.userData.favorites.collectAsStateWithLifecycle()
    val index by c.library.index.collectAsStateWithLifecycle()
    val nowKey by rememberNowPlayingKey()
    val list = remember(favs, index) { favs.mapNotNull { index.byUri[it] }.sortedBy { it.title.lowercase() } }
    if (list.isEmpty()) { EmptyState(Icons.Rounded.FavoriteBorder, stringResource(R.string.no_favorites_title), stringResource(R.string.no_favorites_message)); return }
    LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
        itemsIndexed(list, key = { _, t -> t.key }) { i, t -> TrackRow(t, nowKey == t.key, onClick = { c.player.play(list, i) }) }
    }
}

@Composable
private fun FoldersTab() {
    val c = LocalContainer.current
    val p = LocalPalette.current
    val bottom = LocalBottomPadding.current
    val tracks by c.library.tracks.collectAsStateWithLifecycle()
    val nowKey by rememberNowPlayingKey()
    val start = remember(tracks) { FolderTree.startPath(tracks) }
    var path by rememberSaveable { mutableStateOf<String?>(null) }
    val current = path ?: start
    val atTop = current == start || current.length <= start.length
    val listing = remember(tracks, current) { FolderTree.list(tracks, current) }
    BackHandler(enabled = !atTop) { path = FolderTree.parent(current) }
    LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!atTop) { BareIconButton(Icons.Rounded.ArrowUpward, stringResource(R.string.cd_folder_up), tint = p.text) { path = FolderTree.parent(current) } }
                Text(current.substringAfterLast('/').ifEmpty { stringResource(R.string.tab_folders) }, style = MusicType.heading, color = p.text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = if (atTop) 4.dp else 0.dp))
                val all = remember(tracks, current) { FolderTree.allTracksUnder(tracks, current) }
                if (all.isNotEmpty()) BareIconButton(Icons.Rounded.PlayArrow, stringResource(R.string.play_all), tint = p.text) { c.player.play(all, 0) }
            }
        }
        items(listing.folders, key = { "f:" + it.path }) { f ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { path = f.path }.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(52.dp).glass(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Folder, null, tint = p.accentSoft) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.name, style = MusicType.body, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(songsLabel(f.trackCount), style = MusicType.caption, color = p.textSoft)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = p.textFaint)
            }
        }
        itemsIndexed(listing.tracks, key = { _, t -> t.key }) { i, t -> TrackRow(t, nowKey == t.key, onClick = { c.player.play(listing.tracks, i) }) }
    }
}
