package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.PunchOutOutcome
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class PunchOutOutcomeTest {

    @Test
    fun `full day at or after 6pm`() {
        val punchIn = clock(9, 0)
        assertEquals(
            PunchOutOutcome.Kind.FULL_DAY,
            PunchOutOutcome.evaluate(punchIn, clock(18, 0), "18:00"),
        )
        assertEquals(
            PunchOutOutcome.Kind.FULL_DAY,
            PunchOutOutcome.evaluate(punchIn, clock(18, 5), "18:00"),
        )
    }

    @Test
    fun `half day before 6pm after 4 hours`() {
        val punchIn = clock(9, 0)
        assertEquals(
            PunchOutOutcome.Kind.HALF_DAY,
            PunchOutOutcome.evaluate(punchIn, clock(13, 0), "18:00"),
        )
        assertEquals(
            PunchOutOutcome.Kind.HALF_DAY,
            PunchOutOutcome.evaluate(punchIn, clock(17, 59), "18:00"),
        )
    }

    @Test
    fun `zero hours before 6pm under 4 hours`() {
        val punchIn = clock(9, 0)
        assertEquals(
            PunchOutOutcome.Kind.ZERO_HOURS,
            PunchOutOutcome.evaluate(punchIn, clock(12, 59), "18:00"),
        )
        assertEquals(
            PunchOutOutcome.Kind.ZERO_HOURS,
            PunchOutOutcome.evaluate(punchIn, clock(10, 0), "18:00"),
        )
    }

    private fun clock(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
