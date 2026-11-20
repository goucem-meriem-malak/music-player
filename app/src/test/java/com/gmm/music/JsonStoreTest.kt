package com.gmm.music

import com.gmm.music.data.JsonStore
import com.gmm.music.data.settings.AppSettings
import com.gmm.music.data.settings.SettingsJson
import com.gmm.music.data.settings.ThemeMode
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class JsonStoreTest {
    private fun dir(): File = Files.createTempDirectory("music-test").toFile()

    @Test fun writesAndReads() {
        val s = JsonStore(dir())
        s.write("a.json", JSONObject().put("x", 1))
        assertEquals(1, s.read("a.json")!!.getInt("x"))
    }

    @Test fun fallsBackToBackupWhenMainIsCorrupt() {
        val d = dir(); val s = JsonStore(d)
        s.write("a.json", JSONObject().put("x", 1))
        s.write("a.json", JSONObject().put("x", 2)) // creates .bak with x=1
        File(d, "a.json").writeText("{broken")
        assertEquals(1, s.read("a.json")!!.getInt("x"))
    }

    @Test fun missingFileIsNull() = assertNull(JsonStore(dir()).read("nope.json"))

    @Test fun settingsRoundTrip() {
        val a = AppSettings(themeMode = ThemeMode.Light, crossfadeEnabled = true, crossfadeSeconds = 7, balance = -0.5f,
            eqFreqsHz = listOf(60, 230), eqGainsDb = listOf(1f, -2f))
        val b = SettingsJson.fromJson(SettingsJson.toJson(a))
        assertEquals(a, b)
    }

    @Test fun settingsClampBadValues() {
        val b = SettingsJson.fromJson(JSONObject().put("crossfadeSeconds", 99).put("balance", 5.0))
        assertEquals(12, b.crossfadeSeconds)
        assertEquals(1f, b.balance, 0f)
    }
}
