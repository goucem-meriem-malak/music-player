package com.goucem.aurora.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goucem.aurora.R
import com.goucem.aurora.data.Playlist
import com.goucem.aurora.ui.*
import com.goucem.aurora.ui.components.*
import com.goucem.aurora.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlaylistsScreen(onOpenPlaylist: (String) -> Unit) {
    val c = LocalContainer.current
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val bottom = LocalBottomPadding.current
    val playlists by c.userData.playlists.collectAsStateWithLifecycle()
    val favs by c.userData.favorites.collectAsStateWithLifecycle()
    val index by c.library.index.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val failed = stringResource(R.string.import_failed)

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val (text, display) = withContext(Dispatchers.IO) {
                    val t = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                    var n = ""
                    ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cur -> if (cur.moveToFirst()) n = cur.getString(0).orEmpty() }
                    t to n.substringBeforeLast('.').ifBlank { "Playlist" }
                }
                val pl = c.userData.importM3u(display, text, c.library.index.value)
                onOpenPlaylist(pl.id)
            } catch (_: Exception) { Toast.makeText(ctx, failed, Toast.LENGTH_SHORT).show() }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle(stringResource(R.string.nav_playlists)) {
            GlassIconButton(Icons.Rounded.FileUpload, stringResource(R.string.import_m3u)) { importer.launch(arrayOf("*/*")) }
            Spacer(Modifier.width(10.dp))
            GlassIconButton(Icons.Rounded.Add, stringResource(R.string.new_playlist)) { name = ""; creating = true }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
            item {
                PlaylistRow(stringResource(R.string.section_favorites), songsLabel(favs.count { it in index.byUri }), Icons.Rounded.Favorite) { ui.openCollection("favorites", "") }
            }
            if (playlists.isEmpty()) item {
                EmptyState(Icons.Rounded.QueueMusic, stringResource(R.string.no_playlists_title), stringResource(R.string.no_playlists_message),
                    actionLabel = stringResource(R.string.new_playlist)) { name = ""; creating = true }
            }
            items(playlists, key = { it.id }) { pl ->
                PlaylistRow(pl.name, songsLabel(pl.items.size), Icons.Rounded.QueueMusic) { onOpenPlaylist(pl.id) }
            }
        }
    }
    if (creating) {
        GlassDialog(stringResource(R.string.new_playlist), { creating = false }, stringResource(R.string.create), onConfirm = {
            val pl = c.userData.createPlaylist(name); creating = false; onOpenPlaylist(pl.id)
        }) { GlassTextField(name, { name = it }, stringResource(R.string.playlist_name_hint), autoFocus = true) }
    }
}

@Composable
private fun PlaylistRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).glass(RoundedCornerShape(16.dp), strong = true), contentAlignment = Alignment.Center) { Icon(icon, null, tint = p.accentSoft) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = AuroraType.body, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = AuroraType.caption, color = p.textSoft)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = p.textFaint)
    }
}

@Composable
fun PlaylistDetailScreen(id: String, onBack: () -> Unit) {
    val c = LocalContainer.current
    val ctx = LocalContext.current
    val p = LocalPalette.current
    val bottom = LocalBottomPadding.current
    val scope = rememberCoroutineScope()
    val playlists by c.userData.playlists.collectAsStateWithLifecycle()
    val index by c.library.index.collectAsStateWithLifecycle()
    val nowKey by rememberNowPlayingKey()
    val pl: Playlist? = playlists.firstOrNull { it.id == id }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var newName by remember(pl?.name) { mutableStateOf(pl?.name.orEmpty()) }
    val exportFailed = stringResource(R.string.export_failed)
    val exported = stringResource(R.string.export_done)

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri: Uri? ->
        if (uri == null || pl == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val text = c.userData.exportM3u(pl, c.library.index.value)
                    ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }
                Toast.makeText(ctx, exported, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(ctx, exportFailed, Toast.LENGTH_SHORT).show() }
        }
    }

    if (pl == null) { Column(Modifier.fillMaxSize().statusBarsPadding()) { TopBar("", onBack); EmptyState(Icons.Rounded.ErrorOutline, stringResource(R.string.playlist_missing), "") }; return }

    val resolved = remember(pl, index) { pl.items.map { index.resolve(it) } }
    val playable = remember(resolved) { resolved.filterNotNull() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(pl.name, onBack) {
            BareIconButton(Icons.Rounded.Edit, stringResource(R.string.rename), tint = p.text) { renaming = true }
            BareIconButton(Icons.Rounded.FileDownload, stringResource(R.string.export_m3u), tint = p.text) { exporter.launch(pl.name.replace('/', '-') + ".m3u8") }
            BareIconButton(Icons.Rounded.Delete, stringResource(R.string.delete), tint = p.text) { deleting = true }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(songsLabel(pl.items.size), style = AuroraType.bodySoft, color = p.textSoft)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton(stringResource(R.string.cd_play), icon = Icons.Rounded.PlayArrow) { if (playable.isNotEmpty()) c.player.play(playable, 0) }
                        SecondaryButton(stringResource(R.string.cd_shuffle), icon = Icons.Rounded.Shuffle) { if (playable.isNotEmpty()) c.player.shuffleAll(playable) }
                    }
                }
            }
            if (pl.items.isEmpty()) item { EmptyState(Icons.Rounded.PlaylistAdd, stringResource(R.string.playlist_empty_title), stringResource(R.string.playlist_empty_message)) }
            itemsIndexed(pl.items, key = { i, x -> "$i:${x.uri}:${x.path}" }) { i, item ->
                val t = resolved[i]
                if (t != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TrackRow(t, nowKey == t.key, onClick = { c.player.play(playable, playable.indexOf(t).coerceAtLeast(0)) }, modifier = Modifier.weight(1f))
                        BareIconButton(Icons.Rounded.RemoveCircleOutline, stringResource(R.string.remove_from_playlist)) { c.userData.removeFromPlaylist(pl.id, i) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = p.textFaint)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title.ifBlank { item.path.orEmpty() }, style = AuroraType.body, color = p.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.file_not_found), style = AuroraType.caption, color = p.textFaint)
                        }
                        BareIconButton(Icons.Rounded.RemoveCircleOutline, stringResource(R.string.remove_from_playlist)) { c.userData.removeFromPlaylist(pl.id, i) }
                    }
                }
            }
        }
    }
    if (renaming) GlassDialog(stringResource(R.string.rename), { renaming = false }, stringResource(R.string.save), onConfirm = { c.userData.renamePlaylist(pl.id, newName); renaming = false }) {
        GlassTextField(newName, { newName = it }, stringResource(R.string.playlist_name_hint), autoFocus = true)
    }
    if (deleting) GlassDialog(stringResource(R.string.delete_playlist_title), { deleting = false }, stringResource(R.string.delete),
        message = stringResource(R.string.delete_playlist_message), destructive = true,
        onConfirm = { deleting = false; c.userData.deletePlaylist(pl.id); onBack() })
}
