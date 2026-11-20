package com.gmm.music.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmm.music.AppContainer
import com.gmm.music.R
import com.gmm.music.data.library.LibraryStatus
import com.gmm.music.data.settings.ExtraFolder
import com.gmm.music.ui.LocalContainer
import com.gmm.music.ui.components.*
import com.gmm.music.ui.theme.MusicType
import com.gmm.music.ui.theme.LocalPalette

@Composable
fun songsLabel(n: Int): String = LocalContext.current.resources.getQuantityString(R.plurals.songs_count, n, n)

@Composable
fun albumsLabel(n: Int): String = LocalContext.current.resources.getQuantityString(R.plurals.albums_count, n, n)

/** Back arrow + title row used by every secondary screen. */
@Composable
fun TopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back), onClick = onBack)
        Spacer(Modifier.width(14.dp))
        Text(title, style = MusicType.title, color = LocalPalette.current.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

fun addMusicFolder(ctx: Context, c: AppContainer, uri: Uri) {
    try {
        ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: Exception) { }
    var name = ""
    try {
        val doc = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        ctx.contentResolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) name = it.getString(0).orEmpty()
        }
    } catch (_: Exception) { }
    c.settings.update { s ->
        if (s.extraFolders.any { it.uri == uri.toString() }) s else s.copy(extraFolders = s.extraFolders + ExtraFolder(uri.toString(), name))
    }
}

@Composable
fun rememberAddFolderAction(): () -> Unit {
    val ctx = LocalContext.current
    val c = LocalContainer.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) addMusicFolder(ctx, c, uri) }
    return { launcher.launch(null) }
}

/** Shows loading / permission / empty / error states until the library has songs, then the content. */
@Composable
fun LibraryGate(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = LocalContainer.current
    val tracks by c.library.tracks.collectAsStateWithLifecycle()
    val status by c.library.status.collectAsStateWithLifecycle()
    val addFolder = rememberAddFolderAction()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) c.library.refresh() }
    if (tracks.isNotEmpty()) { content(); return }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (status) {
            LibraryStatus.Idle, LibraryStatus.Scanning -> LoadingState()
            LibraryStatus.NeedsPermission -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(Icons.Rounded.Lock, stringResource(R.string.perm_title), stringResource(R.string.perm_message),
                    actionLabel = stringResource(R.string.grant_access)) {
                    permission.launch(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                SecondaryButton(stringResource(R.string.add_folder), onClick = addFolder)
            }
            LibraryStatus.Failed -> EmptyState(Icons.Rounded.ErrorOutline, stringResource(R.string.scan_failed_title), stringResource(R.string.scan_failed_message),
                actionLabel = stringResource(R.string.retry)) { c.library.refresh() }
            LibraryStatus.Ready -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(Icons.Rounded.MusicOff, stringResource(R.string.empty_title), stringResource(R.string.empty_message),
                    actionLabel = stringResource(R.string.rescan)) { c.library.refresh() }
                SecondaryButton(stringResource(R.string.add_folder), onClick = addFolder)
            }
        }
    }
}
