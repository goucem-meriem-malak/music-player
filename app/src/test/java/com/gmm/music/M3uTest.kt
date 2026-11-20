package com.gmm.music

import com.gmm.music.data.user.M3u
import com.gmm.music.data.user.M3uEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class M3uTest {
    @Test fun parsesExtinfAndPaths() {
        val text = "#EXTM3U\n#EXTINF:215,Daft Punk - Digital Love\n/storage/emulated/0/Music/digital.mp3\n\nC:\\Music\\b.flac\n"
        val e = M3u.parse(text)
        assertEquals(2, e.size)
        assertEquals("Daft Punk", e[0].artist)
        assertEquals("Digital Love", e[0].title)
        assertEquals(215, e[0].durationSec)
        assertEquals("C:/Music/b.flac", e[1].path)
    }

    @Test fun writeThenParseRoundTrips() {
        val out = M3u.write(listOf(M3uEntry("/a/b.mp3", "Song", "Artist", 100)))
        val back = M3u.parse(out)
        assertEquals("/a/b.mp3", back[0].path)
        assertEquals("Song", back[0].title)
        assertEquals("Artist", back[0].artist)
    }

    @Test fun ignoresBomAndComments() {
        val e = M3u.parse("\uFEFF#EXTM3U\n# comment\nsong.mp3")
        assertEquals(1, e.size)
        assertEquals("song.mp3", e[0].path)
    }
}
