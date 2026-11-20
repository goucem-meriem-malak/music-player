package com.gmm.music

import com.gmm.music.data.Track
import com.gmm.music.data.library.FolderTree
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderTreeTest {
    private fun t(name: String, folder: String) = Track(1, "uri:$name", name, "", "", -1, -1, 1000, null, folder, 0, 0, 0, "", "audio/mpeg", 0)
    private val tracks = listOf(
        t("a", "/storage/emulated/0/Music/Rock"), t("b", "/storage/emulated/0/Music/Rock"),
        t("c", "/storage/emulated/0/Music/Jazz/Live"), t("d", "/storage/emulated/0/Music"),
    )

    @Test fun startSkipsSingleChildChain() = assertEquals("storage/emulated/0/Music", FolderTree.startPath(tracks))

    @Test fun listsSubfoldersAndDirectTracks() {
        val l = FolderTree.list(tracks, "storage/emulated/0/Music")
        assertEquals(listOf("Jazz", "Rock"), l.folders.map { it.name })
        assertEquals(2, l.folders.first { it.name == "Rock" }.trackCount)
        assertEquals(listOf("d"), l.tracks.map { it.title })
    }

    @Test fun allTracksUnderIncludesNested() = assertEquals(4, FolderTree.allTracksUnder(tracks, "storage/emulated/0/Music").size)

    @Test fun parentOfPath() = assertEquals("a/b", FolderTree.parent("a/b/c"))
}
