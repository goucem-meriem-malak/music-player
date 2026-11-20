package com.goucem.aurora

import com.goucem.aurora.audio.EqCurves
import com.goucem.aurora.audio.EqPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqCurvesTest {
    @Test fun sameFrequenciesKeepGains() {
        val g = listOf(1f, 2f, 3f)
        assertEquals(g, EqCurves.resample(listOf(60, 910, 14000), g, listOf(60, 910, 14000)))
    }

    @Test fun clampsOutsideRange() {
        val r = EqCurves.resample(listOf(100, 1000), listOf(2f, 6f), listOf(20, 20000))
        assertEquals(listOf(2f, 6f), r)
    }

    @Test fun interpolatesInBetween() {
        val r = EqCurves.resample(listOf(100, 10000), listOf(0f, 10f), listOf(1000))
        assertEquals(5f, r[0], 0.01f)
    }

    @Test fun emptySourceGivesFlat() = assertEquals(listOf(0f, 0f), EqCurves.resample(emptyList(), emptyList(), listOf(50, 500)))

    @Test fun builtInPresetsMatchReferenceBands() {
        EqPresets.builtIn.forEach { assertEquals(EqCurves.REFERENCE_FREQS.size, it.gainsDb.size) }
        assertTrue(EqPresets.find("flat")!!.gainsDb.all { it == 0f })
    }
}
