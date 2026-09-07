package com.srijeesolution.rojgaarwaala.utils

import java.util.Calendar

/**
 * Mirrors backend AttendanceShiftRules: punch-out before duty end is a half day
 * only after 4 hours, otherwise 0 hours.
 */
object PunchOutOutcome {

    const val MIN_HALF_DAY_MINUTES = 240
    const val DEFAULT_DUTY_END = "18:00"

    enum class Kind { FULL_DAY, HALF_DAY, ZERO_HOURS }

    fun evaluate(
        punchInAtMillis: Long?,
        nowMillis: Long,
        dutyEndHm: String?,
    ): Kind {
        val punchIn = punchInAtMillis ?: return Kind.FULL_DAY
        val dutyEnd = dutyEndMillis(nowMillis, dutyEndHm)
        val workedMinutes = ((nowMillis - punchIn) / 60_000L).coerceAtLeast(0L)

        return if (nowMillis < dutyEnd) {
            if (workedMinutes >= MIN_HALF_DAY_MINUTES) Kind.HALF_DAY else Kind.ZERO_HOURS
        } else {
            Kind.FULL_DAY
        }
    }

    private fun dutyEndMillis(nowMillis: Long, dutyEndHm: String?): Long {
        val parts = (dutyEndHm?.takeIf { it.contains(':') } ?: DEFAULT_DUTY_END).split(':')
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 18
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
