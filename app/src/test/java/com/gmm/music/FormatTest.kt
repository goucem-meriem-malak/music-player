package com.gmm.music

import com.gmm.music.util.Format
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun durations() {
        assertEquals("0:00", Format.duration(0))
        assertEquals("3:05", Format.duration(185_000))
        assertEquals("1:02:03", Format.duration(3_723_000))
    }

    @Test fun speeds() {
        assertEquals("1×", Format.speed(1f))
        assertEquals("1.25×", Format.speed(1.25f))
        assertEquals("0.75×", Format.speed(0.75f))
    }
}
