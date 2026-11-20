package com.gmm.music.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gmm.music.R
import com.gmm.music.data.Track
import com.gmm.music.ui.components.*
import com.gmm.music.ui.screens.*
import com.gmm.music.ui.theme.*

private object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val PLAYLISTS = "playlists"
    const val NOW = "nowplaying"
    const val QUEUE = "queue"
    const val EQ = "equalizer"
    const val AUDIO = "audio"
    const val SETTINGS = "settings"
    const val SLEEP = "sleep"
}

private val mainRoutes = listOf(Routes.HOME, Routes.LIBRARY, Routes.SEARCH, Routes.PLAYLISTS)

private fun tabIndex(route: String?): Int = when (route?.substringBefore('?')?.substringBefore('/')) {
    Routes.HOME -> 0; Routes.LIBRARY -> 1; Routes.SEARCH -> 2; Routes.PLAYLISTS -> 3; else -> -1
}

private fun isSheet(route: String?): Boolean = route?.substringBefore('?') in listOf(Routes.NOW, Routes.QUEUE)

private val easing = FastOutSlowInEasing

/** Entering screen. Tabs use a quick fade-through with a small slide; other screens slide in. */
private fun enterT(from: String?, to: String?, pop: Boolean, reduced: Boolean): EnterTransition {
    if (reduced) return EnterTransition.None
    val ft = tabIndex(from); val tt = tabIndex(to)
    if (ft >= 0 && tt >= 0) {
        val dir = if (tt > ft) 1 else -1
        return fadeIn(tween(220, 90, easing)) + slideInHorizontally(tween(320, 90, easing)) { dir * it / 14 }
    }
    return if (!pop) {
        if (isSheet(to)) slideInVertically(tween(340, 0, easing)) { it } + fadeIn(tween(200))
        else slideInHorizontally(tween(320, 0, easing)) { it }
    } else {
        if (isSheet(from)) EnterTransition.None else slideInHorizontally(tween(320, 0, easing)) { -it / 5 }
    }
}

/** Leaving screen. For tabs it fades out first so the two pages never blend together. */
private fun exitT(from: String?, to: String?, pop: Boolean, reduced: Boolean): ExitTransition {
    if (reduced) return ExitTransition.None
    val ft = tabIndex(from); val tt = tabIndex(to)
    if (ft >= 0 && tt >= 0) {
        val dir = if (tt > ft) 1 else -1
        return fadeOut(tween(90)) + slideOutHorizontally(tween(90)) { -dir * it / 30 }
    }
    return if (!pop) {
        if (isSheet(to)) ExitTransition.None else slideOutHorizontally(tween(320, 0, easing)) { -it / 5 }
    } else {
        if (isSheet(from)) slideOutVertically(tween(300, 0, easing)) { it } + fadeOut(tween(220))
        else slideOutHorizontally(tween(320, 0, easing)) { it }
    }
}

/** Every screen paints its own opaque background so sliding pages cover each other cleanly. */
@Composable
private fun Screen(content: @Composable () -> Unit) {
    MusicBackground { content() }
}

@Composable
fun MusicRoot() {
    val c = LocalContainer.current
    val reduced = LocalReducedMotion.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route?.substringBefore('?')?.substringBefore('/')
    val playerState by c.player.state.collectAsStateWithLifecycle()
    val hasTrack = playerState.nowPlaying != null

    var menuTrack by remember { mutableStateOf<Track?>(null) }
    var pickerTracks by remember { mutableStateOf<List<Track>?>(null) }

    val ui = remember(nav) {
        UiActions(
            openTrackMenu = { menuTrack = it },
            pickPlaylist = { pickerTracks = it },
            openAlbum = { nav.navigate("collection/album?key=${Uri.encode(it)}") },
            openArtist = { nav.navigate("collection/artist?key=${Uri.encode(it)}") },
            openTrackInfo = { nav.navigate("info?key=${Uri.encode(it)}") },
            openNowPlaying = { nav.navigate(Routes.NOW) { launchSingleTop = true } },
            openCollection = { type, key -> nav.navigate("collection/$type?key=${Uri.encode(key)}") },
        )
    }

    val showNav = route in mainRoutes || route == "collection" || route == "playlist"
    val showMini = hasTrack && route != Routes.NOW && route != Routes.QUEUE
    val docked = showNav || showMini

    CompositionLocalProvider(LocalUi provides ui, LocalBottomPadding provides 12.dp) {
        MusicBackground {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().then(if (docked) Modifier else Modifier.navigationBarsPadding())) {
                    NavHost(
                        nav, startDestination = Routes.HOME,
                        enterTransition = { enterT(initialState.destination.route, targetState.destination.route, false, reduced) },
                        exitTransition = { exitT(initialState.destination.route, targetState.destination.route, false, reduced) },
                        popEnterTransition = { enterT(initialState.destination.route, targetState.destination.route, true, reduced) },
                        popExitTransition = { exitT(initialState.destination.route, targetState.destination.route, true, reduced) },
                    ) {
                        composable(Routes.HOME) { Screen { HomeScreen(onSearch = { navMain(nav, Routes.SEARCH) }, onSettings = { nav.navigate(Routes.SETTINGS) }) } }
                        composable("library?tab={tab}", arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })) {
                            Screen { LibraryScreen(it.arguments?.getInt("tab") ?: 0) }
                        }
                        composable(Routes.SEARCH) { Screen { SearchScreen() } }
                        composable(Routes.PLAYLISTS) { Screen { PlaylistsScreen(onOpenPlaylist = { id -> nav.navigate("playlist/${Uri.encode(id)}") }) } }
                        composable("playlist/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                            Screen { PlaylistDetailScreen(it.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }) }
                        }
                        composable("collection/{type}?key={key}", arguments = listOf(
                            navArgument("type") { type = NavType.StringType },
                            navArgument("key") { type = NavType.StringType; defaultValue = "" },
                        )) {
                            Screen { CollectionScreen(it.arguments?.getString("type").orEmpty(), it.arguments?.getString("key").orEmpty(), onBack = { nav.popBackStack() }) }
                        }
                        composable(Routes.NOW) {
                            Screen { NowPlayingScreen(onClose = { nav.popBackStack() }, onQueue = { nav.navigate(Routes.QUEUE) }, onEqualizer = { nav.navigate(Routes.EQ) }, onSleep = { nav.navigate(Routes.SLEEP) }) }
                        }
                        composable(Routes.QUEUE) { Screen { QueueScreen(onBack = { nav.popBackStack() }) } }
                        composable(Routes.EQ) { Screen { EqualizerScreen(onBack = { nav.popBackStack() }) } }
                        composable(Routes.AUDIO) { Screen { AudioSettingsScreen(onBack = { nav.popBackStack() }, onEqualizer = { nav.navigate(Routes.EQ) }) } }
                        composable(Routes.SETTINGS) { Screen { SettingsScreen(onBack = { nav.popBackStack() }, onAudio = { nav.navigate(Routes.AUDIO) }, onSleep = { nav.navigate(Routes.SLEEP) }) } }
                        composable(Routes.SLEEP) { Screen { SleepTimerScreen(onBack = { nav.popBackStack() }) } }
                        composable("info?key={key}", arguments = listOf(navArgument("key") { type = NavType.StringType; defaultValue = "" })) {
                            Screen { TrackInfoScreen(it.arguments?.getString("key").orEmpty(), onBack = { nav.popBackStack() }) }
                        }
                    }
                }
                if (docked) DockedBar(
                    showMini = showMini, showNav = showNav,
                    items = listOf(
                        NavItem(Routes.HOME, Icons.Rounded.Home, R.string.nav_home), NavItem(Routes.LIBRARY, Icons.Rounded.LibraryMusic, R.string.nav_library),
                        NavItem(Routes.SEARCH, Icons.Rounded.Search, R.string.nav_search), NavItem(Routes.PLAYLISTS, Icons.Rounded.QueueMusic, R.string.nav_playlists),
                    ),
                    currentRoute = route,
                    onOpenNow = { nav.navigate(Routes.NOW) { launchSingleTop = true } },
                    onSelect = { navMain(nav, it) },
                )
            }
        }

        menuTrack?.let { t -> TrackMenuSheet(t, onDismiss = { menuTrack = null }) }
        pickerTracks?.let { list -> PlaylistPickerSheet(list, onDismiss = { pickerTracks = null }) }
    }
}

private fun navMain(nav: NavHostController, route: String) {
    nav.navigate(if (route == Routes.LIBRARY) "library?tab=0" else route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun TrackMenuSheet(track: Track, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    val ui = LocalUi.current
    val p = LocalPalette.current
    val favs by c.userData.favorites.collectAsStateWithLifecycle()
    val isFav = track.key in favs
    GlassSheet(onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(track.art, Modifier.size(56.dp), sizePx = 128)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(track.title, style = MusicType.heading, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artistLabel(track.artist), style = MusicType.caption, color = p.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(6.dp))
        SheetAction(Icons.Rounded.SkipNext, stringResource(R.string.menu_play_next)) { c.player.playNext(track); onDismiss() }
        SheetAction(Icons.Rounded.QueueMusic, stringResource(R.string.menu_add_queue)) { c.player.addToQueue(track); onDismiss() }
        SheetAction(Icons.Rounded.PlaylistAdd, stringResource(R.string.menu_add_playlist)) { onDismiss(); ui.pickPlaylist(listOf(track)) }
        SheetAction(if (isFav) Icons.Rounded.FavoriteBorder else Icons.Rounded.Favorite, stringResource(if (isFav) R.string.menu_unfavorite else R.string.menu_favorite)) { c.userData.toggleFavorite(track.key); onDismiss() }
        if (track.album.isNotBlank()) {
            val album = c.library.albums.value.firstOrNull { a -> a.tracks.any { it.key == track.key } }
            if (album != null) SheetAction(Icons.Rounded.Album, stringResource(R.string.menu_go_album)) { onDismiss(); ui.openAlbum(album.key) }
        }
        if (track.artist.isNotBlank()) SheetAction(Icons.Rounded.Person, stringResource(R.string.menu_go_artist)) { onDismiss(); ui.openArtist(track.artist.lowercase()) }
        SheetAction(Icons.Rounded.Info, stringResource(R.string.menu_track_info)) { onDismiss(); ui.openTrackInfo(track.key) }
    }
}

@Composable
private fun PlaylistPickerSheet(tracks: List<Track>, onDismiss: () -> Unit) {
    val c = LocalContainer.current
    val ctx = LocalContext.current
    val p = LocalPalette.current
    val playlists by c.userData.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val added = stringResource(R.string.added_to_playlist)
    GlassSheet(onDismiss) {
        Text(stringResource(R.string.pick_playlist_title), style = MusicType.heading, color = p.text, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
        SheetAction(Icons.Rounded.Add, stringResource(R.string.new_playlist)) { creating = true }
        LazyColumn(Modifier.weight(1f, fill = false)) {
            items(playlists, key = { it.id }) { pl ->
                SheetAction(Icons.Rounded.QueueMusic, pl.name) { c.userData.addToPlaylist(pl.id, tracks); Toast.makeText(ctx, added, Toast.LENGTH_SHORT).show(); onDismiss() }
            }
        }
    }
    if (creating) GlassDialog(stringResource(R.string.new_playlist), { creating = false }, stringResource(R.string.create), onConfirm = {
        val pl = c.userData.createPlaylist(name); c.userData.addToPlaylist(pl.id, tracks)
        Toast.makeText(ctx, added, Toast.LENGTH_SHORT).show(); creating = false; onDismiss()
    }) { GlassTextField(name, { name = it }, stringResource(R.string.playlist_name_hint), autoFocus = true) }
}
