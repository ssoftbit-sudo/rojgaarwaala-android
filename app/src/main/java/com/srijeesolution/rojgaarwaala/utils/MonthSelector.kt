package com.srijeesolution.rojgaarwaala.utils

import java.util.Calendar

/**
 * Month arithmetic for the attendance / wage month pickers.
 *
 * Everything except [currentMonth] is pure so year-boundary behaviour can be covered by
 * JVM unit tests. Month numbers are 1-based (January = 1) to match the `YYYY-MM` API value.
 */
object MonthSelector {

    private val MONTH_NAMES = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    /**
     * @param value the `YYYY-MM` string sent to the API.
     * @param label the human readable label, e.g. "Aug 2025".
     */
    data class MonthOption(
        val year: Int,
        val month: Int,
        val value: String,
        val label: String,
    )

    fun of(year: Int, month: Int): MonthOption {
        require(month in 1..12) { "month must be 1..12 but was $month" }
        return MonthOption(
            year = year,
            month = month,
            value = "$year-${month.toString().padStart(2, '0')}",
            label = "${MONTH_NAMES[month - 1]} $year",
        )
    }

    /** Shifts by [delta] months, rolling the year over in both directions. */
    fun shift(year: Int, month: Int, delta: Int): MonthOption {
        require(month in 1..12) { "month must be 1..12 but was $month" }
        val zeroBased = (year * 12) + (month - 1) + delta
        return of(Math.floorDiv(zeroBased, 12), Math.floorMod(zeroBased, 12) + 1)
    }

    fun shift(option: MonthOption, delta: Int): MonthOption = shift(option.year, option.month, delta)

    /** [count] months ending at the given month, newest first. */
    fun lastMonths(year: Int, month: Int, count: Int): List<MonthOption> {
        if (count <= 0) return emptyList()
        return (0 until count).map { shift(year, month, -it) }
    }

    /** Parses a `YYYY-MM` value, returning null when it is malformed. */
    fun parse(value: String?): MonthOption? {
        val parts = value?.split("-") ?: return null
        if (parts.size != 2) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        if (month !in 1..12) return null
        return of(year, month)
    }

    fun currentMonth(): MonthOption {
        val calendar = Calendar.getInstance()
        return of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }
}
