package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.DayJob
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.Locale

/**
 * Words for 1 Day Job screens, in plain everyday Hindi: many workers and thekedars
 * do not read English. Pure, so JVM tests cover it.
 */
object DayJobFormat {

    private val months = listOf("जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")

    /** "15 अगस्त" */
    fun dayMonth(date: LocalDate): String = "${date.dayOfMonth} ${months[date.monthValue - 1]}"

    /** "सुबह 8:00", "दोपहर 2:30", "शाम 5:00", "रात 9:00". */
    fun clock(time: LocalTime): String {
        val part = when (time.hour) {
            in 4..11 -> "सुबह"
            in 12..15 -> "दोपहर"
            in 16..18 -> "शाम"
            else -> "रात"
        }
        val h = if (time.hour % 12 == 0) 12 else time.hour % 12
        return "$part $h:%02d".format(time.minute)
    }

    /** "आज, सुबह 8:00", "कल, दोपहर 2:30" या "28 अक्टूबर, सुबह 8:00". */
    fun whenText(workDate: String, startTime: String, today: LocalDate = LocalDate.now()): String {
        val date = runCatching { LocalDate.parse(workDate) }.getOrNull() ?: return "$workDate $startTime"
        val time = runCatching { LocalTime.parse(startTime.take(5)) }.getOrNull()
        val day = when (date) {
            today -> "आज"
            today.plusDays(1) -> "कल"
            else -> dayMonth(date)
        }
        return if (time == null) day else "$day, ${clock(time)}"
    }

    fun wageText(wage: Int): String = "₹$wage/दिन"

    fun distanceText(km: Double?): String = when {
        km == null -> ""
        km < 1 -> "${(km * 1000).toInt()} मीटर दूर"
        else -> String.format(Locale.ENGLISH, "%.1f km दूर", km)
    }

    /** "मिस्त्री 1, हेल्पर 2" */
    fun slotsText(job: DayJob): String = job.slots.joinToString(", ") { "${it.label} ${it.needed}" }

    /** "3 में से 2 जगह खाली" for a worker; "1/3 आ रहे हैं" for the company. */
    fun seatsLeftText(job: DayJob): String {
        val left = (job.neededTotal - job.filledTotal).coerceAtLeast(0)
        return if (left == 0) "सब जगह भर गईं" else "${job.neededTotal} में से $left जगह खाली"
    }

    fun comingText(job: DayJob): String = "${job.filledTotal}/${job.neededTotal} आ रहे हैं"

    fun statusText(status: String): String = when (status) {
        "in_review" -> "जाँच बाकी"
        "open" -> "खुला है"
        "full" -> "सब मिल गए"
        "closed" -> "बंद (लोग आ रहे)"
        "cancelled" -> "रद्द"
        "expired" -> "कोई नहीं आया"
        "completed" -> "पूरा हुआ"
        else -> status
    }

    /** "2 घंटे 10 मिनट में बंद", "25 मिनट में बंद", या "बंद" once accepting has ended. */
    fun closesInText(acceptClosesAt: String?, now: OffsetDateTime = OffsetDateTime.now()): String {
        val closes = acceptClosesAt?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() } ?: return ""
        val left = Duration.between(now, closes)
        if (left.isNegative || left.isZero) return "बंद"
        val hours = left.toHours()
        val minutes = left.minusHours(hours).toMinutes()
        return when {
            hours >= 24 -> "${hours / 24} दिन ${hours % 24} घंटे में बंद"
            hours > 0 -> "$hours घंटे $minutes मिनट में बंद"
            else -> "${minutes.coerceAtLeast(1)} मिनट में बंद"
        }
    }

    /** "कभी भी" या "सुबह 6:00 – रात 8:00". */
    fun alertHoursText(from: String?, to: String?): String {
        if (from.isNullOrBlank() || to.isNullOrBlank()) return "कभी भी"
        fun t(v: String) = runCatching { clock(LocalTime.parse(v.take(5))) }.getOrDefault(v)
        return "${t(from)} – ${t(to)}"
    }

    /** "2 नवंबर" from an ISO date-time, or the text as is. */
    fun dayText(iso: String?): String =
        iso?.let { runCatching { dayMonth(OffsetDateTime.parse(it).toLocalDate()) }.getOrNull() ?: it }.orEmpty()

    /** "शाम 7:00" from an ISO date-time, in the phone's time zone; empty when unreadable. */
    fun clockText(iso: String?, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String =
        iso?.let { runCatching { clock(OffsetDateTime.parse(it).atZoneSameInstant(zone).toLocalTime()) }.getOrNull() }.orEmpty()

    /** "Raipur में हेल्पर को आम तौर पर ₹600–₹650/दिन मिलते हैं" */
    fun wageHintText(label: String, low: Int, high: Int, district: String?): String {
        val range = if (low == high) "₹$low" else "₹$low–₹$high"
        val where = district?.let { "$it में " }.orEmpty()
        return "$where$label को आम तौर पर $range/दिन मिलते हैं"
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
        if (title.isBlank()) add("काम का नाम लिखें")
        if (slots.values.none { it > 0 }) add("कम से कम एक मज़दूर जोड़ें")
        if (wage == null || wage < 100) add("मज़दूरी कम से कम ₹100/दिन हो")
        if (!hasDate) add("तारीख़ चुनें")
        if (!hasTime) add("काम शुरू होने का समय चुनें")
        if (!hasPin) add("नक्शे पर काम की जगह लगाएँ")
    }
}
