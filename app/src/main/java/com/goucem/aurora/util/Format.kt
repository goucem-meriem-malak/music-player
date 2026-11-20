package com.goucem.aurora.util

object Format {
    fun duration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val total = ms / 1000
        val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun speed(v: Float): String = if (v % 1f == 0f) "${v.toInt()}×" else "${v.toString().trimEnd('0')}×"

    fun sizeBytes(b: Long): String = when {
        b <= 0 -> ""
        b < 1024 * 1024 -> "%.0f KB".format(b / 1024.0)
        else -> "%.1f MB".format(b / 1024.0 / 1024.0)
    }

    fun extension(mime: String, path: String?): String {
        val ext = path?.substringAfterLast('.', "")?.uppercase().orEmpty()
        return if (ext.isNotEmpty() && ext.length <= 5) ext else mime.substringAfter('/').uppercase()
    }
}
