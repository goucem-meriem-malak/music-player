package com.gmm.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Search
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
import com.gmm.music.data.Artist
import com.gmm.music.data.Track
import com.gmm.music.ui.*
import com.gmm.music.ui.components.*
import com.gmm.music.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class SearchResult(val songs: List<Track>, val albums: List<Album>, val artists: List<Artist>) {
    val isEmpty get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
}

@Composable
fun SearchScreen() {
    val c = LocalContainer.current
    val ui = LocalUi.current
    val p = LocalPalette.current
    val bottom = LocalBottomPadding.current
    val tracks by c.library.tracks.collectAsStateWithLifecycle()
    val albums by c.library.albums.collectAsStateWithLifecycle()
    val artists by c.library.artists.collectAsStateWithLifecycle()
    val nowKey by rememberNowPlayingKey()
    var query by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf(SearchResult(emptyList(), emptyList(), emptyList())) }

    LaunchedEffect(query, tracks, albums, artists) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) { result = SearchResult(emptyList(), emptyList(), emptyList()); return@LaunchedEffect }
        delay(120) // debounce typing
        result = withContext(Dispatchers.Default) {
            SearchResult(
                songs = tracks.filter { it.searchText.contains(q) }.take(100),
                albums = albums.filter { it.name.lowercase().contains(q) || it.artist.lowercase().contains(q) }.take(30),
                artists = artists.filter { it.name.lowercase().contains(q) }.take(30),
            )
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle(stringResource(R.string.nav_search))
        GlassTextField(query, { query = it }, stringResource(R.string.search_hint), Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        LibraryGate {
            when {
                query.isBlank() -> EmptyState(Icons.Rounded.Search, stringResource(R.string.search_idle_title), stringResource(R.string.search_idle_message))
                result.isEmpty -> EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.search_none_title), stringResource(R.string.search_none_message))
                else -> LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
                    if (result.artists.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.tab_artists)) }
                        item {
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(result.artists, key = { it.key }) { a ->
                                    Column(Modifier.width(92.dp).clip(RoundedCornerShape(16.dp)).clickable { ui.openArtist(a.key) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Artwork(a.tracks.first().art, Modifier.size(80.dp), sizePx = 192, shape = CircleShape)
                                        Spacer(Modifier.height(6.dp))
                                        Text(a.name, style = MusicType.caption, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                    if (result.albums.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.tab_albums)) }
                        item {
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(result.albums, key = { it.key }) { a -> AlbumTile(a, { ui.openAlbum(a.key) }, Modifier.width(128.dp)) }
                            }
                        }
                    }
                    if (result.songs.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.tab_songs)) }
                        itemsIndexed(result.songs, key = { _, t -> t.key }) { i, t -> TrackRow(t, nowKey == t.key, onClick = { c.player.play(result.songs, i) }) }
                    }
                }
            }
        }
    }
}
