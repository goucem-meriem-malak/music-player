package com.goucem.aurora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goucem.aurora.R
import com.goucem.aurora.ui.LocalContainer
import com.goucem.aurora.ui.components.*
import com.goucem.aurora.ui.theme.*

@Composable
fun AudioSettingsScreen(onBack: () -> Unit, onEqualizer: () -> Unit) {
    val c = LocalContainer.current
    val p = LocalPalette.current
    val caps by c.effects.caps.collectAsStateWithLifecycle()
    val s by c.settings.flow.collectAsStateWithLifecycle()
    val state by c.player.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.audio_settings_title), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            SettingGroup(stringResource(R.string.group_sound)) {
                SettingRow(stringResource(R.string.equalizer_title), if (s.eqEnabled) stringResource(R.string.on) else stringResource(R.string.off), onClick = onEqualizer)
                if (caps.bassBoost) SliderRow(stringResource(R.string.bass_title), "${s.bassPercent}%") {
                    GlassSlider(s.bassPercent.toFloat(), { v -> c.settings.update { it.copy(bassPercent = v.toInt()) } }, valueRange = 0f..100f, description = stringResource(R.string.bass_title))
                } else SettingRow(stringResource(R.string.bass_title), stringResource(R.string.not_supported))
                if (caps.loudness) SliderRow(stringResource(R.string.loudness_title), "+%.0f dB".format(s.loudnessDb)) {
                    GlassSlider(s.loudnessDb, { v -> c.settings.update { it.copy(loudnessDb = v) } }, valueRange = 0f..12f, description = stringResource(R.string.loudness_title))
                } else SettingRow(stringResource(R.string.loudness_title), stringResource(R.string.not_supported))
                SliderRow(stringResource(R.string.balance_title), when {
                    s.balance < -0.02f -> "L ${(-s.balance * 100).toInt()}%"; s.balance > 0.02f -> "R ${(s.balance * 100).toInt()}%"; else -> stringResource(R.string.balance_center)
                }) { GlassSlider(s.balance, { v -> c.settings.update { it.copy(balance = if (kotlin.math.abs(v) < 0.04f) 0f else v) } }, valueRange = -1f..1f, description = stringResource(R.string.balance_title)) }
            }
            SettingGroup(stringResource(R.string.group_playback)) {
                SliderRow(stringResource(R.string.speed_title), com.goucem.aurora.util.Format.speed(state.speed)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        items(listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)) { sp ->
                            GlassChip(com.goucem.aurora.util.Format.speed(sp), kotlin.math.abs(state.speed - sp) < 0.01f) { c.player.setSpeed(sp); c.settings.update { it.copy(playbackSpeed = sp) } }
                        }
                    }
                }
                SwitchRow(stringResource(R.string.fade_title), stringResource(R.string.fade_subtitle), s.crossfadeEnabled) { v -> c.settings.update { it.copy(crossfadeEnabled = v) } }
                if (s.crossfadeEnabled) SliderRow(stringResource(R.string.fade_length), "${s.crossfadeSeconds} s") {
                    GlassSlider(s.crossfadeSeconds.toFloat(), { v -> c.settings.update { it.copy(crossfadeSeconds = v.toInt().coerceIn(1, 12)) } }, valueRange = 1f..12f, description = stringResource(R.string.fade_length))
                }
                SettingRow(stringResource(R.string.gapless_title), stringResource(R.string.gapless_subtitle))
            }
            SettingGroup(stringResource(R.string.group_honesty)) {
                Text(stringResource(R.string.audio_honesty_text), style = AuroraType.bodySoft, color = p.textSoft, modifier = Modifier.padding(18.dp))
            }
        }
    }
}
