package com.goucem.aurora.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import com.goucem.aurora.data.settings.AppSettings
import com.goucem.aurora.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class EqBand(val freqHz: Int)

data class EffectCaps(
    val equalizer: Boolean = false,
    val bands: List<EqBand> = emptyList(),
    val minDb: Float = -12f,
    val maxDb: Float = 12f,
    val bassBoost: Boolean = false,
    val loudness: Boolean = false,
)

/**
 * Owns the Android audio effects (Equalizer, BassBoost, LoudnessEnhancer) and the balance processor.
 * Effects attach to the player's audio session. Anything the device does not support is reported
 * honestly in [caps] and simply not shown as available.
 */
class AudioEffectsManager(private val settings: SettingsRepository, scope: CoroutineScope) {
    val balanceProcessor = BalanceAudioProcessor()

    private val _caps = MutableStateFlow(EffectCaps())
    val caps: StateFlow<EffectCaps> = _caps

    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var loud: LoudnessEnhancer? = null

    init {
        scope.launch {
            settings.flow.map { it.audioKey() }.distinctUntilChanged().collect { apply(settings.value) }
        }
    }

    private fun AppSettings.audioKey() = listOf(eqEnabled, eqFreqsHz, eqGainsDb, bassPercent, loudnessDb, balance)

    @Synchronized
    fun attach(sessionId: Int) {
        release()
        if (sessionId != 0) {
            eq = try { Equalizer(0, sessionId) } catch (_: Exception) { null }
            bass = try { BassBoost(0, sessionId).takeIf { it.strengthSupported } } catch (_: Exception) { null }
            loud = try { LoudnessEnhancer(sessionId) } catch (_: Exception) { null }
        }
        val e = eq
        _caps.value = if (e == null) EffectCaps(bassBoost = bass != null, loudness = loud != null) else try {
            val range = e.bandLevelRange
            EffectCaps(
                equalizer = true,
                bands = List(e.numberOfBands.toInt()) { EqBand(e.getCenterFreq(it.toShort()) / 1000) },
                minDb = range[0] / 100f, maxDb = range[1] / 100f,
                bassBoost = bass != null, loudness = loud != null,
            )
        } catch (_: Exception) { EffectCaps(bassBoost = bass != null, loudness = loud != null) }
        apply(settings.value)
    }

    @Synchronized
    fun release() {
        try { eq?.release() } catch (_: Exception) { }
        try { bass?.release() } catch (_: Exception) { }
        try { loud?.release() } catch (_: Exception) { }
        eq = null; bass = null; loud = null
    }

    @Synchronized
    private fun apply(s: AppSettings) {
        balanceProcessor.setBalance(s.balance)
        val caps = _caps.value
        try {
            eq?.let { e ->
                if (caps.bands.isNotEmpty() && s.eqEnabled) {
                    val gains = EqCurves.resample(s.eqFreqsHz, s.eqGainsDb, caps.bands.map { it.freqHz })
                    gains.forEachIndexed { i, g ->
                        e.setBandLevel(i.toShort(), (g.coerceIn(caps.minDb, caps.maxDb) * 100).toInt().toShort())
                    }
                }
                e.setEnabled(s.eqEnabled)
            }
        } catch (_: Exception) { }
        try {
            bass?.let { b ->
                b.setStrength((s.bassPercent * 10).coerceIn(0, 1000).toShort())
                b.setEnabled(s.bassPercent > 0)
            }
        } catch (_: Exception) { }
        try {
            loud?.let { l ->
                l.setTargetGain((s.loudnessDb * 100).toInt())
                l.setEnabled(s.loudnessDb > 0f)
            }
        } catch (_: Exception) { }
    }
}
