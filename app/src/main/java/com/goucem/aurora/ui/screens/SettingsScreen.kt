package com.goucem.aurora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goucem.aurora.BuildConfig
import com.goucem.aurora.R
import com.goucem.aurora.data.settings.GlassQuality
import com.goucem.aurora.data.settings.ThemeMode
import com.goucem.aurora.ui.LocalContainer
import com.goucem.aurora.ui.components.*
import com.goucem.aurora.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit, onAudio: () -> Unit, onSleep: () -> Unit) {
    val c = LocalContainer.current
    val p = LocalPalette.current
    val s by c.settings.flow.collectAsStateWithLifecycle()
    val tracks by c.library.tracks.collectAsStateWithLifecycle()
    val addFolder = rememberAddFolderAction()
    var showPrivacy by remember { mutableStateOf(false) }
    var showNotices by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.settings_title), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            SettingGroup(stringResource(R.string.group_sound)) {
                SettingRow(stringResource(R.string.audio_settings_title), stringResource(R.string.audio_settings_subtitle), onClick = onAudio)
                SettingRow(stringResource(R.string.sleep_title), null, onClick = onSleep)
            }
            SettingGroup(stringResource(R.string.group_appearance)) {
                SettingRow(stringResource(R.string.theme_title)) {}
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ThemeMode.values().toList()) { m ->
                        GlassChip(stringResource(when (m) { ThemeMode.System -> R.string.theme_system; ThemeMode.Dark -> R.string.theme_dark; ThemeMode.Light -> R.string.theme_light }), s.themeMode == m) { c.settings.update { it.copy(themeMode = m) } }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SettingRow(stringResource(R.string.glass_title), stringResource(R.string.glass_subtitle)) {}
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GlassQuality.values().toList()) { q ->
                        GlassChip(stringResource(if (q == GlassQuality.Full) R.string.glass_full else R.string.glass_lite), s.glassQuality == q) { c.settings.update { it.copy(glassQuality = q) } }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SwitchRow(stringResource(R.string.ambient_title), stringResource(R.string.ambient_subtitle), s.ambientBackground) { v -> c.settings.update { it.copy(ambientBackground = v) } }
                SwitchRow(stringResource(R.string.reduced_motion_title), stringResource(R.string.reduced_motion_subtitle), s.reducedMotion) { v -> c.settings.update { it.copy(reducedMotion = v) } }
                SwitchRow(stringResource(R.string.haptics_title), null, s.haptics) { v -> c.settings.update { it.copy(haptics = v) } }
            }
            SettingGroup(stringResource(R.string.group_library)) {
                SettingRow(stringResource(R.string.min_length_title), stringResource(R.string.min_length_subtitle)) {}
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(0, 15, 30, 60)) { sec ->
                        GlassChip(if (sec == 0) stringResource(R.string.off) else "$sec s", s.minTrackSeconds == sec) { c.settings.update { it.copy(minTrackSeconds = sec) } }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SettingRow(stringResource(R.string.rescan), songsLabel(tracks.size), onClick = { c.library.refresh() })
                SettingRow(stringResource(R.string.add_folder), stringResource(R.string.add_folder_subtitle), onClick = addFolder)
                s.extraFolders.forEach { f ->
                    SettingRow(f.name.ifBlank { f.uri }) { BareIconButton(Icons.Rounded.Close, stringResource(R.string.remove_folder)) { c.settings.update { st -> st.copy(extraFolders = st.extraFolders.filterNot { it.uri == f.uri }) } } }
                }
                SettingRow(stringResource(R.string.clear_art_cache), stringResource(R.string.clear_art_cache_subtitle), onClick = { c.artwork.clearDiskCache() })
            }
            SettingGroup(stringResource(R.string.group_about)) {
                SettingRow(stringResource(R.string.app_name), stringResource(R.string.version_fmt, BuildConfig.VERSION_NAME))
                SettingRow(stringResource(R.string.language_title), stringResource(R.string.language_subtitle))
                SettingRow(stringResource(R.string.privacy_title), stringResource(R.string.privacy_subtitle), onClick = { showPrivacy = true })
                SettingRow(stringResource(R.string.notices_title), null, onClick = { showNotices = true })
                SettingRow(stringResource(R.string.contact_title), stringResource(R.string.contact_email))
            }
        }
    }
    if (showPrivacy) GlassDialog(stringResource(R.string.privacy_title), { showPrivacy = false }, stringResource(R.string.close), onConfirm = { showPrivacy = false }) {
        Text(stringResource(R.string.privacy_text), style = AuroraType.bodySoft, color = p.textSoft)
    }
    if (showNotices) GlassDialog(stringResource(R.string.notices_title), { showNotices = false }, stringResource(R.string.close), onConfirm = { showNotices = false }) {
        Text(stringResource(R.string.notices_text), style = AuroraType.bodySoft, color = p.textSoft)
    }
}
