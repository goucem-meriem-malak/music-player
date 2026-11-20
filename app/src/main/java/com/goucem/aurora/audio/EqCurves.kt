package com.goucem.aurora.audio

import kotlin.math.ln

/** Pure helpers for equalizer curves. Frequencies are in Hz, gains in dB. */
object EqCurves {
    val REFERENCE_FREQS = listOf(60, 230, 910, 3600, 14000)

    /** Interpolates a curve onto other frequencies using a logarithmic frequency axis. */
    fun resample(srcFreqs: List<Int>, srcGains: List<Float>, dstFreqs: List<Int>): List<Float> {
        if (srcFreqs.isEmpty() || srcFreqs.size != srcGains.size) return dstFreqs.map { 0f }
        return dstFreqs.map { f ->
            when {
                f <= srcFreqs.first() -> srcGains.first()
                f >= srcFreqs.last() -> srcGains.last()
                else -> {
                    var i = 0
                    while (i < srcFreqs.size - 2 && srcFreqs[i + 1] < f) i++
                    val lo = ln(srcFreqs[i].toDouble())
                    val hi = ln(srcFreqs[i + 1].toDouble())
                    val t = ((ln(f.toDouble()) - lo) / (hi - lo)).toFloat()
                    srcGains[i] + (srcGains[i + 1] - srcGains[i]) * t
                }
            }
        }
    }
}

data class BuiltInPreset(val id: String, val gainsDb: List<Float>)

object EqPresets {
    val builtIn: List<BuiltInPreset> = listOf(
        BuiltInPreset("flat", listOf(0f, 0f, 0f, 0f, 0f)),
        BuiltInPreset("bass_boost", listOf(6f, 4f, 0f, 0f, 0f)),
        BuiltInPreset("bass_reducer", listOf(-5f, -3f, 0f, 0f, 0f)),
        BuiltInPreset("treble_boost", listOf(0f, 0f, 0f, 3f, 6f)),
        BuiltInPreset("vocal", listOf(-3f, 0f, 4f, 3f, 0f)),
        BuiltInPreset("rock", listOf(5f, 3f, -2f, 3f, 5f)),
        BuiltInPreset("pop", listOf(-1f, 3f, 5f, 2f, -1f)),
        BuiltInPreset("jazz", listOf(4f, 2f, -2f, 2f, 5f)),
        BuiltInPreset("classical", listOf(5f, 3f, -2f, 3f, 4f)),
        BuiltInPreset("electronic", listOf(5f, 3f, 0f, 3f, 5f)),
        BuiltInPreset("hip_hop", listOf(6f, 4f, 0f, 1f, 3f)),
        BuiltInPreset("acoustic", listOf(4f, 3f, 1f, 3f, 3f)),
        BuiltInPreset("loudness", listOf(6f, 2f, -1f, 2f, 6f)),
    )

    fun find(id: String): BuiltInPreset? = builtIn.firstOrNull { it.id == id }
}
