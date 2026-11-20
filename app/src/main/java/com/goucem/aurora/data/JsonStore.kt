package com.goucem.aurora.data

import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Small JSON file store. Every write goes to a temp file first, is flushed to disk,
 * then atomically replaces the real file. The previous version is kept as ".bak".
 * If the main file is corrupt, the backup is used.
 */
class JsonStore(private val dir: File) {
    init { dir.mkdirs() }

    fun read(name: String): JSONObject? {
        for (file in listOf(File(dir, name), File(dir, "$name.bak"))) {
            if (!file.exists()) continue
            try {
                return JSONObject(file.readText(Charsets.UTF_8))
            } catch (_: Exception) {
                // try the backup next
            }
        }
        return null
    }

    @Synchronized
    fun write(name: String, json: JSONObject) {
        val target = File(dir, name)
        val tmp = File(dir, "$name.tmp")
        val bak = File(dir, "$name.bak")
        FileOutputStream(tmp).use { out ->
            out.write(json.toString().toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        if (target.exists()) {
            try { target.copyTo(bak, overwrite = true) } catch (_: Exception) { }
        }
        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
