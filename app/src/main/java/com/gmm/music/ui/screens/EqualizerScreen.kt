package com.gmm.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmm.music.R
import com.gmm.music.audio.EqCurves
import com.gmm.music.audio.EqPresets
import com.gmm.music.data.settings.EqCustomPreset
import com.gmm.music.ui.LocalContainer
import com.gmm.music.ui.components.*
import com.gmm.music.ui.theme.*
import java.util.UUID

@Composable
fun presetName(id: String): String = stringResource(when (id) {
    "flat" -> R.string.preset_flat; "bass_boost" -> R.string.preset_bass_boost; "bass_reducer" -> R.string.preset_bass_reducer
    "treble_boost" -> R.string.preset_treble_boost; "vocal" -> R.string.preset_vocal; "rock" -> R.string.preset_rock
    "pop" -> R.string.preset_pop; "jazz" -> R.string.preset_jazz; "classical" -> R.string.preset_classical
    "electronic" -> R.string.preset_electronic; "hip_hop" -> R.string.preset_hip_hop; "acoustic" -> R.string.preset_acoustic
    "loudness" -> R.string.preset_loudness; else -> R.string.preset_custom
})

private fun freqLabel(hz: Int) = if (hz >= 1000) "%.1fk".format(hz / 1000f).replace(".0k", "k") else "$hz"

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val c = LocalContainer.current
    val p = LocalPalette.current
    val caps by c.effects.caps.collectAsStateWithLifecycle()
    val s by c.settings.flow.collectAsStateWithLifecycle()
    var saving by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    val freqs = caps.bands.map { it.freqHz }
    val gains = remember(s.eqFreqsHz, s.eqGainsDb, freqs) { EqCurves.resample(s.eqFreqsHz, s.eqGainsDb, freqs) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.equalizer_title), onBack) {
            GlassSwitch(s.eqEnabled) { v -> c.settings.update { it.copy(eqEnabled = v) } }
        }
        if (!caps.equalizer) {
            EmptyState(Icons.Rounded.Equalizer, stringResource(R.string.eq_unavailable_title), stringResource(R.string.eq_unavailable_message))
            return@Column
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(EqPresets.builtIn, key = { it.id }) { preset ->
                    GlassChip(presetName(preset.id), s.eqPresetId == preset.id) {
                        c.settings.update { it.copy(eqEnabled = true, eqPresetId = preset.id, eqFreqsHz = freqs, eqGainsDb = EqCurves.resample(EqCurves.REFERENCE_FREQS, preset.gainsDb, freqs)) }
                    }
                }
                items(s.customPresets, key = { it.id }) { preset ->
                    GlassChip(preset.name, s.eqPresetId == preset.id) {
                        c.settings.update { it.copy(eqEnabled = true, eqPresetId = preset.id, eqFreqsHz = freqs, eqGainsDb = EqCurves.resample(preset.freqsHz, preset.gainsDb, freqs)) }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().glass(RoundedCornerShape(28.dp)).padding(vertical = 18.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                caps.bands.forEachIndexed { i, band ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("%+.0f".format(gains.getOrElse(i) { 0f }), style = MusicType.caption, color = p.textSoft)
                        Spacer(Modifier.height(6.dp))
                        VerticalGlassSlider(
                            gains.getOrElse(i) { 0f }, range = caps.minDb..caps.maxDb, modifier = Modifier.height(210.dp), enabled = s.eqEnabled,
                            onValueChange = { v ->
                                val next = gains.toMutableList().also { it[i] = v.coerceIn(caps.minDb, caps.maxDb) }
                                c.settings.update { it.copy(eqFreqsHz = freqs, eqGainsDb = next, eqPresetId = "custom") }
                            },
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(freqLabel(band.freqHz), style = MusicType.caption, color = p.textSoft)
                    }
                }
            }
            Text(stringResource(R.string.eq_db_note, "%.0f".format(caps.minDb), "%.0f".format(caps.maxDb)), style = MusicType.caption, color = p.textFaint, modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(stringResource(R.string.eq_reset), icon = Icons.Rounded.Refresh) {
                    c.settings.update { it.copy(eqPresetId = "flat", eqFreqsHz = freqs, eqGainsDb = freqs.map { 0f }) }
                }
                SecondaryButton(stringResource(R.string.eq_save_preset), icon = Icons.Rounded.Save) { presetNameInput = ""; saving = true }
            }
            val custom = s.customPresets.firstOrNull { it.id == s.eqPresetId }
            if (custom != null) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton(stringResource(R.string.eq_delete_preset, custom.name), Modifier.padding(horizontal = 20.dp), icon = Icons.Rounded.Delete) {
                    c.settings.update { it.copy(customPresets = it.customPresets.filterNot { x -> x.id == custom.id }, eqPresetId = "custom") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (saving) GlassDialog(stringResource(R.string.eq_save_preset), { saving = false }, stringResource(R.string.save), onConfirm = {
        val name = presetNameInput.trim()
        if (name.isNotEmpty()) {
            val id = "custom-" + UUID.randomUUID().toString().take(8)
            c.settings.update { it.copy(customPresets = it.customPresets + EqCustomPreset(id, name, freqs, gains), eqPresetId = id) }
        }
        saving = false
    }) { GlassTextField(presetNameInput, { presetNameInput = it }, stringResource(R.string.playlist_name_hint), autoFocus = true) }
}
