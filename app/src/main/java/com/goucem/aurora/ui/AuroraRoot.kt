package com.goucem.aurora.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
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
import com.goucem.aurora.R
import com.goucem.aurora.data.Track
import com.goucem.aurora.ui.components.*
import com.goucem.aurora.ui.screens.*
import com.goucem.aurora.ui.theme.*

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

@Composable
fun AuroraRoot() {
    val c = LocalContainer.current
    val p = LocalPalette.current
    val reduced = LocalReducedMotion.current
    val ctx = LocalContext.current
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

    val fullScreen = route in listOf(Routes.NOW, Routes.QUEUE, Routes.EQ, Routes.AUDIO, Routes.SETTINGS, Routes.SLEEP, "info")
    val showNav = route in mainRoutes || route == "collection" || route == "playlist"
    val showMini = hasTrack && route != Routes.NOW && route != Routes.QUEUE
    val navInset = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val bottomPad = navInset + (if (showNav) 82.dp else 0.dp) + (if (showMini) 78.dp else 0.dp) + 16.dp

    CompositionLocalProvider(LocalUi provides ui, LocalBottomPadding provides bottomPad) {
        AuroraBackground {
            val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { if (reduced) EnterTransition.None else fadeIn(tween(220)) + scaleIn(tween(220), 0.98f) }
            val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { if (reduced) ExitTransition.None else fadeOut(tween(120)) }
            val slideUp: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { if (reduced) EnterTransition.None else slideInVertically(tween(300)) { it } + fadeIn(tween(200)) }
            val slideDown: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { if (reduced) ExitTransition.None else slideOutVertically(tween(260)) { it } + fadeOut(tween(200)) }

            NavHost(nav, startDestination = Routes.HOME, enterTransition = enter, exitTransition = exit, popEnterTransition = enter, popExitTransition = exit) {
                composable(Routes.HOME) { HomeScreen(onSearch = { navMain(nav, Routes.SEARCH) }, onSettings = { nav.navigate(Routes.SETTINGS) }) }
                composable("library?tab={tab}", arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })) {
                    LibraryScreen(it.arguments?.getInt("tab") ?: 0)
                }
                composable(Routes.SEARCH) { SearchScreen() }
                composable(Routes.PLAYLISTS) { PlaylistsScreen(onOpenPlaylist = { id -> nav.navigate("playlist/${Uri.encode(id)}") }) }
                composable("playlist/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    PlaylistDetailScreen(it.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
                }
                composable("collection/{type}?key={key}", arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("key") { type = NavType.StringType; defaultValue = "" },
                )) {
                    CollectionScreen(it.arguments?.getString("type").orEmpty(), it.arguments?.getString("key").orEmpty(), onBack = { nav.popBackStack() })
                }
                composable(Routes.NOW, enterTransition = slideUp, exitTransition = exit, popEnterTransition = enter, popExitTransition = slideDown) {
                    NowPlayingScreen(onClose = { nav.popBackStack() }, onQueue = { nav.navigate(Routes.QUEUE) }, onEqualizer = { nav.navigate(Routes.EQ) }, onSleep = { nav.navigate(Routes.SLEEP) })
                }
                composable(Routes.QUEUE, enterTransition = slideUp, popExitTransition = slideDown) { QueueScreen(onBack = { nav.popBackStack() }) }
                composable(Routes.EQ) { EqualizerScreen(onBack = { nav.popBackStack() }) }
                composable(Routes.AUDIO) { AudioSettingsScreen(onBack = { nav.popBackStack() }, onEqualizer = { nav.navigate(Routes.EQ) }) }
                composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }, onAudio = { nav.navigate(Routes.AUDIO) }, onSleep = { nav.navigate(Routes.SLEEP) }) }
                composable(Routes.SLEEP) { SleepTimerScreen(onBack = { nav.popBackStack() }) }
                composable("info?key={key}", arguments = listOf(navArgument("key") { type = NavType.StringType; defaultValue = "" })) {
                    TrackInfoScreen(it.arguments?.getString("key").orEmpty(), onBack = { nav.popBackStack() })
                }
            }

            if (showMini || showNav) {
                Column(Modifier.align(Alignment.BottomCenter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth().height(24.dp).background(Brush.verticalGradient(listOf(Color.Transparent, p.bgBottom.copy(alpha = if (p.isDark) 0.55f else 0.35f)))))
                    if (showMini) MiniPlayer(onOpen = { nav.navigate(Routes.NOW) { launchSingleTop = true } })
                    if (showNav) AuroraBottomBar(
                        listOf(
                            NavItem(Routes.HOME, Icons.Rounded.Home, R.string.nav_home), NavItem(Routes.LIBRARY, Icons.Rounded.LibraryMusic, R.string.nav_library),
                            NavItem(Routes.SEARCH, Icons.Rounded.Search, R.string.nav_search), NavItem(Routes.PLAYLISTS, Icons.Rounded.QueueMusic, R.string.nav_playlists),
                        ),
                        route, onSelect = { navMain(nav, it) },
                    )
                    Spacer(Modifier.height(navInset + 4.dp))
                }
            }
        }

        menuTrack?.let { t ->
            TrackMenuSheet(t, onDismiss = { menuTrack = null })
        }
        pickerTracks?.let { list ->
            PlaylistPickerSheet(list, onDismiss = { pickerTracks = null })
        }
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
                Text(track.title, style = AuroraType.heading, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artistLabel(track.artist), style = AuroraType.caption, color = p.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Text(stringResource(R.string.pick_playlist_title), style = AuroraType.heading, color = p.text, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
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
