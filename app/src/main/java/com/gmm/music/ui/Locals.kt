package com.gmm.music.ui

import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmm.music.AppContainer
import com.gmm.music.data.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

/** Bottom space screens must leave free for the floating mini-player and navigation bar. */
val LocalBottomPadding = compositionLocalOf<Dp> { 0.dp }

/** Actions that open app-wide sheets and screens from anywhere. */
class UiActions(
    val openTrackMenu: (Track) -> Unit,
    val pickPlaylist: (List<Track>) -> Unit,
    val openAlbum: (String) -> Unit,
    val openArtist: (String) -> Unit,
    val openTrackInfo: (String) -> Unit,
    val openNowPlaying: () -> Unit,
    val openCollection: (type: String, key: String) -> Unit,
)

val LocalUi = staticCompositionLocalOf<UiActions> { error("UiActions not provided") }

@Composable
fun rememberNowPlayingKey(): State<String?> {
    val c = LocalContainer.current
    val flow = remember(c) { c.player.state.map { it.nowPlaying?.key }.distinctUntilChanged() }
    return flow.collectAsStateWithLifecycle(null)
}

/** Playback position, polled only while the caller is composed and audio is playing. */
@Composable
fun rememberPlaybackPosition(intervalMs: Long = 250L): State<Long> {
    val player = LocalContainer.current.player
    val state by player.state.collectAsStateWithLifecycle()
    val pos = remember { mutableLongStateOf(player.currentPositionMs()) }
    LaunchedEffect(state.isPlaying, state.nowPlaying?.uid) {
        pos.longValue = player.currentPositionMs()
        while (state.isPlaying) {
            delay(intervalMs)
            pos.longValue = player.currentPositionMs()
        }
    }
    return pos
}
