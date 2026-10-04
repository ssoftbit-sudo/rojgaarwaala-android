package com.srijeesolution.rojgaarwaala.utils

import java.util.Calendar

/**
 * Factory duty clocks come as "09:00". Punch-in reminders run around duty start and
 * punch-out reminders around duty end, the same way for both: from
 * [LEAD_MINUTES] before the clock to [CUTOFF_MINUTES] after it (09:00 duty ->
 * 08:50 to 09:20), every [INTERVAL_MINUTES], until the punch is recorded.
 * They only ask the employee to open the app; GPS is checked there, never in the
 * background.
 */
object PunchReminderTimes {
    const val LEAD_MINUTES = 10
    const val INTERVAL_MINUTES = 2
    const val CUTOFF_MINUTES = 20

    private const val MINUTE_MILLIS = 60_000L

    fun parseDutyHm(value: String?): Pair<Int, Int>? {
        val text = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val match = Regex("""^(\d{1,2}):(\d{2})(?::\d{2})?$""").find(text) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    /**
     * Next reminder strictly after [nowMillis]: the first slot of today's window,
     * the next 2-minute slot inside it, or tomorrow's first slot once the punch is
     * recorded or the window has closed. Slots sit on a fixed grid from the window
     * start, so rescheduling on every login or app start lands on the same times
     * and never doubles them up.
     */
    fun nextReminderMillis(dutyHm: String?, nowMillis: Long, doneToday: Boolean): Long? {
        val duty = dutyMillis(dutyHm, nowMillis) ?: return null
        val start = duty - LEAD_MINUTES * MINUTE_MILLIS
        val end = duty + CUTOFF_MINUTES * MINUTE_MILLIS
        val tomorrowStart = calendarAt(start).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

        if (doneToday) return tomorrowStart
        if (nowMillis < start) return start

        val interval = INTERVAL_MINUTES * MINUTE_MILLIS
        val next = start + ((nowMillis - start) / interval + 1) * interval

        return if (next <= end) next else tomorrowStart
    }

    /** True while today's reminders for [dutyHm] are still running at [nowMillis]. */
    fun inReminderWindow(dutyHm: String?, nowMillis: Long): Boolean {
        val duty = dutyMillis(dutyHm, nowMillis) ?: return false
        // Small slack so a reminder delivered a few seconds early or late still shows.
        return nowMillis >= duty - LEAD_MINUTES * MINUTE_MILLIS - 30_000L &&
            nowMillis <= duty + CUTOFF_MINUTES * MINUTE_MILLIS + 90_000L
    }

    private fun dutyMillis(dutyHm: String?, nowMillis: Long): Long? {
        val (hour, minute) = parseDutyHm(dutyHm) ?: return null
        return calendarAt(nowMillis).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun calendarAt(nowMillis: Long): Calendar {
        return Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
}
