package com.srijeesolution.rojgaarwaala.utils

import java.util.Calendar

/**
 * Factory duty clocks come as "09:00". The local reminder fires [LEAD_MINUTES]
 * before that clock so labour does not depend on the server cron.
 */
object PunchReminderTimes {
    const val LEAD_MINUTES = 5
    const val LATE_WINDOW_MINUTES = 2

    fun parseDutyHm(value: String?): Pair<Int, Int>? {
        val text = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val match = Regex("""^(\d{1,2}):(\d{2})(?::\d{2})?$""").find(text) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    /**
     * Next wall-clock millis for the reminder. If today's fire already passed
     * but duty has not (or just started), fire almost immediately so a late
     * login still gets today's alert.
     */
    fun nextFireMillis(
        dutyHm: String?,
        nowMillis: Long,
        alreadyDoneToday: Boolean,
        leadMinutes: Int = LEAD_MINUTES,
    ): Long? {
        val (hour, minute) = parseDutyHm(dutyHm) ?: return null
        val duty = calendarAt(nowMillis).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fire = (duty.clone() as Calendar).apply { add(Calendar.MINUTE, -leadMinutes) }
        val lateUntil = (duty.clone() as Calendar).apply { add(Calendar.MINUTE, LATE_WINDOW_MINUTES) }

        return when {
            alreadyDoneToday -> {
                fire.add(Calendar.DAY_OF_MONTH, 1)
                fire.timeInMillis
            }
            nowMillis < fire.timeInMillis -> fire.timeInMillis
            nowMillis < lateUntil.timeInMillis -> nowMillis + 1_500L
            else -> {
                fire.add(Calendar.DAY_OF_MONTH, 1)
                fire.timeInMillis
            }
        }
    }

    private fun calendarAt(nowMillis: Long): Calendar {
        return Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
}
