package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.DayJob
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Short, plain words for 1 Day Job screens. Pure, so JVM tests cover it. */
object DayJobFormat {

    private val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    /** "Today, 8:00 AM", "Tomorrow, 2:30 PM" or "28 Oct, 8:00 AM". */
    fun whenText(workDate: String, startTime: String, today: LocalDate = LocalDate.now()): String {
        val date = runCatching { LocalDate.parse(workDate) }.getOrNull() ?: return "$workDate $startTime"
        val time = runCatching { LocalTime.parse(startTime.take(5)) }.getOrNull()
        val day = when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(dayFormat)
        }
        return if (time == null) day else "$day, ${time.format(timeFormat).uppercase(Locale.ENGLISH)}"
    }

    /** "Any time" or "6:00 AM – 8:00 PM". */
    fun alertHoursText(from: String?, to: String?): String {
        if (from.isNullOrBlank() || to.isNullOrBlank()) return "Any time"
        fun t(v: String) = runCatching { LocalTime.parse(v.take(5)).format(timeFormat).uppercase(Locale.ENGLISH) }.getOrDefault(v)
        return "${t(from)} – ${t(to)}"
    }

    /** "2 Nov" from an ISO date-time, or the text as is. */
    fun dayText(iso: String?): String =
        iso?.let { runCatching { OffsetDateTime.parse(it).format(dayFormat) }.getOrNull() ?: it }.orEmpty()

    /** "Helper usually gets ₹600–₹650 a day in Raipur" (or one figure when the range is flat). */
    fun wageHintText(label: String, low: Int, high: Int, district: String?): String {
        val range = if (low == high) "₹$low" else "₹$low–₹$high"
        val where = district?.let { " in $it" }.orEmpty()
        return "$label usually gets $range a day$where"
    }

    fun wageText(wage: Int): String = "₹$wage/day"

    fun distanceText(km: Double?): String = when {
        km == null -> ""
        km < 1 -> "${(km * 1000).toInt()} m away"
        else -> String.format(Locale.ENGLISH, "%.1f km away", km)
    }

    /** "Mason 1, Helper 2" */
    fun slotsText(job: DayJob): String = job.slots.joinToString(", ") { "${it.label} ${it.needed}" }

    /** "2 of 3 seats left" for a worker; "1/3 coming" for the company. */
    fun seatsLeftText(job: DayJob): String {
        val left = (job.neededTotal - job.filledTotal).coerceAtLeast(0)
        return if (left == 0) "All seats taken" else "$left of ${job.neededTotal} seats left"
    }

    fun comingText(job: DayJob): String = "${job.filledTotal}/${job.neededTotal} coming"

    fun statusText(status: String): String = when (status) {
        "in_review" -> "Waiting for verify"
        "open" -> "Open"
        "full" -> "Full"
        "closed" -> "Closed"
        "cancelled" -> "Cancelled"
        "expired" -> "No one came"
        "completed" -> "Done"
        else -> status.replaceFirstChar { it.uppercase() }
    }

    /** "Closes in 2h 10m", "Closes in 25m", or "Closed" once accepting has ended. */
    fun closesInText(acceptClosesAt: String?, now: OffsetDateTime = OffsetDateTime.now()): String {
        val closes = acceptClosesAt?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() } ?: return ""
        val left = Duration.between(now, closes)
        if (left.isNegative || left.isZero) return "Closed"
        val hours = left.toHours()
        val minutes = left.minusHours(hours).toMinutes()
        return when {
            hours >= 24 -> "Closes in ${hours / 24}d ${hours % 24}h"
            hours > 0 -> "Closes in ${hours}h ${minutes}m"
            else -> "Closes in ${minutes.coerceAtLeast(1)}m"
        }
    }

    /** Problems with a job form, in the order the fields appear. Empty means it can be posted. */
    fun postErrors(
        title: String,
        slots: Map<String, Int>,
        wage: Int?,
        hasDate: Boolean,
        hasTime: Boolean,
        hasPin: Boolean,
    ): List<String> = buildList {
        if (title.isBlank()) add("Write what the work is")
        if (slots.values.none { it > 0 }) add("Add at least one worker")
        if (wage == null || wage < 100) add("Wage must be at least ₹100 a day")
        if (!hasDate) add("Choose the date")
        if (!hasTime) add("Choose the start time")
        if (!hasPin) add("Pin the work site on the map")
    }
}
