package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.PunchReminderTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PunchReminderTimesTest {

    @Test
    fun `parses factory duty clocks`() {
        assertEquals(9 to 0, PunchReminderTimes.parseDutyHm("09:00"))
        assertEquals(18 to 30, PunchReminderTimes.parseDutyHm("18:30:00"))
        assertNull(PunchReminderTimes.parseDutyHm(""))
        assertNull(PunchReminderTimes.parseDutyHm("25:00"))
    }

    @Test
    fun `reminders run every two minutes from ten before to twenty after duty`() {
        var now = at(7, 0)
        val fires = mutableListOf<Long>()
        while (true) {
            now = PunchReminderTimes.nextReminderMillis("09:00", now, doneToday = false)!!
            if (now >= at(9, 0) + 24 * 60 * 60_000L - 60 * 60_000L) break
            fires += now
        }
        // 08:50, 08:52 ... 09:20: half an hour, 16 reminders.
        assertEquals((0..15).map { at(8, 50) + it * 2 * 60_000L }, fires)
        assertEquals(at(9, 20), fires.last())
    }

    @Test
    fun `after the window closes the next reminder is tomorrow`() {
        assertEquals(
            at(8, 50) + 24 * 60 * 60_000L,
            PunchReminderTimes.nextReminderMillis("09:00", at(9, 20), doneToday = false),
        )
    }

    @Test
    fun `punch-out reminders use the same half-hour window around duty end`() {
        assertEquals(at(17, 50), PunchReminderTimes.nextReminderMillis("18:00", at(12, 0), doneToday = false))
        assertEquals(at(18, 20), PunchReminderTimes.nextReminderMillis("18:00", at(18, 18), doneToday = false))
    }

    @Test
    fun `restarting mid-way lands on the same slot so nothing doubles up`() {
        val restart = at(8, 53) + 17_000L
        assertEquals(at(8, 54), PunchReminderTimes.nextReminderMillis("09:00", restart, doneToday = false))
    }

    @Test
    fun `a recorded punch stops today's reminders`() {
        assertEquals(
            at(8, 50) + 24 * 60 * 60_000L,
            PunchReminderTimes.nextReminderMillis("09:00", at(8, 54), doneToday = true),
        )
    }

    @Test
    fun `knows when the reminder window is open`() {
        assertTrue(PunchReminderTimes.inReminderWindow("09:00", at(8, 50)))
        assertTrue(PunchReminderTimes.inReminderWindow("09:00", at(9, 20)))
        assertFalse(PunchReminderTimes.inReminderWindow("09:00", at(8, 40)))
        assertFalse(PunchReminderTimes.inReminderWindow("09:00", at(9, 25)))
        assertNull(PunchReminderTimes.nextReminderMillis("", 0L, false))
    }

    private fun at(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 26, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
