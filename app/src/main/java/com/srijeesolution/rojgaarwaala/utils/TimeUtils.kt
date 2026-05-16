package com.srijeesolution.rojgaarwaala.utils

import android.content.Context
import android.os.Build
import java.text.DateFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {

    /**
     * Common API / backend date-time patterns. Tried in order; lenient parsing enabled.
     */
    private val DATE_PATTERNS = listOf(
        // ISO-8601 variants
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        // SQL / Laravel style
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd",
        // Other common separators
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd",
        "dd/MM/yyyy HH:mm:ss",
        "dd/MM/yyyy",
        "dd-MM-yyyy HH:mm:ss",
        "dd-MM-yyyy",
        "MM/dd/yyyy HH:mm:ss",
        "MM/dd/yyyy",
        "dd.MM.yyyy HH:mm:ss",
        "dd.MM.yyyy",
        "yyyy.MM.dd HH:mm:ss",
        "yyyy.MM.dd",
        // Human-readable
        "dd MMM yyyy HH:mm:ss",
        "dd MMM yyyy",
        "MMM dd, yyyy HH:mm:ss",
        "MMM dd, yyyy",
        "EEEE, dd MMM yyyy HH:mm:ss",
        "EEEE, dd MMM yyyy",
    )

    private fun parseDate(timeString: String?): Date? {
        if (timeString.isNullOrBlank()) return null

        val trimmed = timeString.trim()
        if (trimmed.equals("null", ignoreCase = true)) return null

        parseEpochMillis(trimmed)?.let { return it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            parseWithJavaTime(trimmed)?.let { return it }
        }

        for (candidate in buildInputVariants(trimmed)) {
            for (pattern in DATE_PATTERNS) {
                parseWithPattern(candidate, pattern)?.let { return it }
            }
        }

        parseWithDateFormatStyles(trimmed)?.let { return it }

        return null
    }

    /** Unix timestamp in seconds (10 digits) or milliseconds (13+ digits). */
    private fun parseEpochMillis(value: String): Date? {
        val digits = value.filter { it.isDigit() }
        if (digits.length < 10 || digits.length != value.length) return null
        val epoch = digits.toLongOrNull() ?: return null
        return when {
            digits.length <= 10 -> Date(epoch * 1000L)
            else -> Date(epoch)
        }
    }

    @Suppress("NewApi")
    private fun parseWithJavaTime(value: String): Date? {
        return try {
            java.time.Instant.parse(value).let { Date.from(it) }
        } catch (_: Exception) {
            try {
                java.time.OffsetDateTime.parse(value).toInstant().let { Date.from(it) }
            } catch (_: Exception) {
                try {
                    java.time.LocalDateTime.parse(value)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .let { Date.from(it) }
                } catch (_: Exception) {
                    try {
                        java.time.LocalDate.parse(value)
                            .atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .let { Date.from(it) }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }

    /** Generate normalized variants (T→space, strip Z, trim fractional seconds). */
    private fun buildInputVariants(raw: String): List<String> {
        val out = linkedSetOf(raw)

        if (raw.contains('T')) {
            out.add(raw.replace('T', ' '))
        }
        if (raw.endsWith("Z", ignoreCase = true)) {
            out.add(raw.dropLast(1).trim())
        }
        val withoutFraction = raw.replace(Regex("\\.\\d{1,9}"), "")
        if (withoutFraction != raw) {
            out.add(withoutFraction)
            if (withoutFraction.contains('T')) {
                out.add(withoutFraction.replace('T', ' '))
            }
        }
        // "+00:00" style — keep as-is; XXX patterns handle it

        return out.toList()
    }

    private fun parseWithPattern(value: String, pattern: String): Date? {
        return try {
            val locale = Locale.US
            val sdf = SimpleDateFormat(pattern, locale)
            sdf.isLenient = true
            when {
                pattern.contains("'Z'") || value.endsWith('Z') -> {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                pattern.contains("XXX") -> {
                    // offset included in value
                }
                pattern.contains("HH") && !pattern.contains("XXX") -> {
                    sdf.timeZone = TimeZone.getDefault()
                }
                else -> sdf.timeZone = TimeZone.getDefault()
            }
            val pos = ParsePosition(0)
            val parsed = sdf.parse(value, pos)
            if (parsed != null && pos.index > 0) parsed else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseWithDateFormatStyles(value: String): Date? {
        val locales = listOf(Locale.getDefault(), Locale.US, Locale.UK)
        val styles = intArrayOf(
            DateFormat.FULL,
            DateFormat.LONG,
            DateFormat.MEDIUM,
            DateFormat.SHORT,
        )
        for (locale in locales) {
            for (style in styles) {
                try {
                    DateFormat.getDateTimeInstance(style, style, locale)
                        .parse(value)?.let { return it }
                } catch (_: Exception) { /* next */ }
                try {
                    DateFormat.getDateInstance(style, locale)
                        .parse(value)?.let { return it }
                } catch (_: Exception) { /* next */ }
                try {
                    DateFormat.getTimeInstance(style, locale)
                        .parse(value)?.let { return it }
                } catch (_: Exception) { /* next */ }
            }
        }
        return null
    }

    fun getRelativeTimeSpanString(context: Context, timeString: String?): String {
        val date = parseDate(timeString) ?: return ""
        val now = Date()
        val diffInMillis = (now.time - date.time).coerceAtLeast(0)

        val seconds = diffInMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val months = days / 30
        val years = days / 365

        return when {
            years > 0 -> "$years year${if (years > 1) "s" else ""} ago"
            months > 0 -> "$months month${if (months > 1) "s" else ""} ago"
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }

    fun formatDisplayDate(timeString: String?): String {
        val date = parseDate(timeString) ?: return ""
        return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
    }

    fun formatPublishMeta(context: Context, publishDate: String?, createdAt: String?): String {
        val raw = createdAt?.takeIf { it.isNotBlank() } ?: publishDate
        val relative = getRelativeTimeSpanString(context, raw)
        if (relative.isNotEmpty()) return relative
        return formatDisplayDate(raw)
    }
}
