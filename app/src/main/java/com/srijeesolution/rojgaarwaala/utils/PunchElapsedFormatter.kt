package com.srijeesolution.rojgaarwaala.utils

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Turns the dashboard's display clock ("09:12 AM") plus today's date into a live
 * Hindi elapsed string. The API only ships a minute-precision clock, so the timer
 * is accurate to the minute, then ticks locally every second.
 */
object PunchElapsedFormatter {

    fun parseClock(date: String?, clock: String?): Long? {
        val clockText = clock?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val dateText = date?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val combined = "$dateText $clockText"
        return parse(combined, "yyyy-MM-dd hh:mm a")
            ?: parse(combined, "yyyy-MM-dd h:mm a")
    }

    fun elapsedMillis(
        punchInAtMillis: Long,
        nowMillis: Long,
        punchOutAtMillis: Long? = null,
    ): Long {
        val end = punchOutAtMillis ?: nowMillis
        return (end - punchInAtMillis).coerceAtLeast(0L)
    }

    fun formatHindi(elapsedMillis: Long, running: Boolean): String {
        val totalSeconds = elapsedMillis / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val duration = buildString {
            if (hours > 0) append("$hours घंटे ")
            if (minutes > 0 || hours > 0) append("$minutes मिनट ")
            append("$seconds सेकंड")
        }.trim()
        return if (running) {
            "पंच इन लगे हुए $duration"
        } else {
            "कुल समय $duration"
        }
    }

    private fun parse(value: String, pattern: String): Long? {
        return try {
            val format = SimpleDateFormat(pattern, Locale.US)
            format.isLenient = false
            format.parse(value)?.time
        } catch (_: Exception) {
            null
        }
    }
}
