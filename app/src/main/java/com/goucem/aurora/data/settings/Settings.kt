package com.goucem.aurora.data.settings

import com.goucem.aurora.data.JsonStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { System, Dark, Light }
enum class GlassQuality { Full, Lite }

data class EqCustomPreset(val id: String, val name: String, val freqsHz: List<Int>, val gainsDb: List<Float>)
data class ExtraFolder(val uri: String, val name: String)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.Dark,
    val glassQuality: GlassQuality = GlassQuality.Full,
    val ambientBackground: Boolean = true,
    val reducedMotion: Boolean = false,
    val haptics: Boolean = true,
    val crossfadeEnabled: Boolean = false,
    val crossfadeSeconds: Int = 4,
    val playbackSpeed: Float = 1f,
    val minTrackSeconds: Int = 15,
    val eqEnabled: Boolean = false,
    val eqPresetId: String = "flat",
    val eqFreqsHz: List<Int> = emptyList(),
    val eqGainsDb: List<Float> = emptyList(),
    val bassPercent: Int = 0,
    val loudnessDb: Float = 0f,
    val balance: Float = 0f,
    val customPresets: List<EqCustomPreset> = emptyList(),
    val extraFolders: List<ExtraFolder> = emptyList(),
)

object SettingsJson {
    fun toJson(s: AppSettings): JSONObject = JSONObject().apply {
        put("version", 1)
        put("themeMode", s.themeMode.name)
        put("glassQuality", s.glassQuality.name)
        put("ambientBackground", s.ambientBackground)
        put("reducedMotion", s.reducedMotion)
        put("haptics", s.haptics)
        put("crossfadeEnabled", s.crossfadeEnabled)
        put("crossfadeSeconds", s.crossfadeSeconds)
        put("playbackSpeed", s.playbackSpeed.toDouble())
        put("minTrackSeconds", s.minTrackSeconds)
        put("eqEnabled", s.eqEnabled)
        put("eqPresetId", s.eqPresetId)
        put("eqFreqsHz", JSONArray(s.eqFreqsHz))
        put("eqGainsDb", JSONArray(s.eqGainsDb.map { it.toDouble() }))
        put("bassPercent", s.bassPercent)
        put("loudnessDb", s.loudnessDb.toDouble())
        put("balance", s.balance.toDouble())
        put("customPresets", JSONArray().also { arr ->
            s.customPresets.forEach { p ->
                arr.put(JSONObject().apply {
                    put("id", p.id); put("name", p.name)
                    put("freqsHz", JSONArray(p.freqsHz))
                    put("gainsDb", JSONArray(p.gainsDb.map { it.toDouble() }))
                })
            }
        })
        put("extraFolders", JSONArray().also { arr ->
            s.extraFolders.forEach { f -> arr.put(JSONObject().apply { put("uri", f.uri); put("name", f.name) }) }
        })
    }

    private fun ints(a: JSONArray?): List<Int> = if (a == null) emptyList() else List(a.length()) { a.getInt(it) }
    private fun floats(a: JSONArray?): List<Float> = if (a == null) emptyList() else List(a.length()) { a.getDouble(it).toFloat() }

    fun fromJson(j: JSONObject): AppSettings {
        val d = AppSettings()
        fun <T : Enum<T>> enumOf(values: Array<T>, name: String?, fallback: T): T =
            values.firstOrNull { it.name == name } ?: fallback
        val presets = j.optJSONArray("customPresets")
        val folders = j.optJSONArray("extraFolders")
        val freqs = ints(j.optJSONArray("eqFreqsHz"))
        val gains = floats(j.optJSONArray("eqGainsDb"))
        return AppSettings(
            themeMode = enumOf(ThemeMode.values(), j.optString("themeMode"), d.themeMode),
            glassQuality = enumOf(GlassQuality.values(), j.optString("glassQuality"), d.glassQuality),
            ambientBackground = j.optBoolean("ambientBackground", d.ambientBackground),
            reducedMotion = j.optBoolean("reducedMotion", d.reducedMotion),
            haptics = j.optBoolean("haptics", d.haptics),
            crossfadeEnabled = j.optBoolean("crossfadeEnabled", d.crossfadeEnabled),
            crossfadeSeconds = j.optInt("crossfadeSeconds", d.crossfadeSeconds).coerceIn(1, 12),
            playbackSpeed = j.optDouble("playbackSpeed", 1.0).toFloat().coerceIn(0.5f, 2f),
            minTrackSeconds = j.optInt("minTrackSeconds", d.minTrackSeconds).coerceIn(0, 600),
            eqEnabled = j.optBoolean("eqEnabled", d.eqEnabled),
            eqPresetId = j.optString("eqPresetId", d.eqPresetId),
            eqFreqsHz = if (freqs.size == gains.size) freqs else emptyList(),
            eqGainsDb = if (freqs.size == gains.size) gains else emptyList(),
            bassPercent = j.optInt("bassPercent", 0).coerceIn(0, 100),
            loudnessDb = j.optDouble("loudnessDb", 0.0).toFloat().coerceIn(0f, 12f),
            balance = j.optDouble("balance", 0.0).toFloat().coerceIn(-1f, 1f),
            customPresets = if (presets == null) emptyList() else List(presets.length()) { i ->
                val p = presets.getJSONObject(i)
                EqCustomPreset(p.getString("id"), p.getString("name"), ints(p.optJSONArray("freqsHz")), floats(p.optJSONArray("gainsDb")))
            },
            extraFolders = if (folders == null) emptyList() else List(folders.length()) { i ->
                val f = folders.getJSONObject(i)
                ExtraFolder(f.getString("uri"), f.optString("name", ""))
            },
        )
    }
}

class SettingsRepository(private val store: JsonStore, private val scope: CoroutineScope) {
    private val file = "settings.json"
    private val _flow = MutableStateFlow(
        try { store.read(file)?.let { SettingsJson.fromJson(it) } ?: AppSettings() } catch (_: Exception) { AppSettings() }
    )
    val flow: StateFlow<AppSettings> = _flow
    val value: AppSettings get() = _flow.value
    private var saveJob: Job? = null

    fun update(transform: (AppSettings) -> AppSettings) {
        _flow.value = transform(_flow.value)
        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.IO) {
            delay(400)
            try { store.write(file, SettingsJson.toJson(_flow.value)) } catch (_: Exception) { }
        }
    }
}
