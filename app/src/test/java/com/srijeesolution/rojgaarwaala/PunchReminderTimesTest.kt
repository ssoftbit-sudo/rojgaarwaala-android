package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.PunchReminderTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PunchReminderTimesTest {

    @Test
    fun `parses factory duty clocks`() {
        assertEquals(9 to 0, PunchReminderTimes.parseDutyHm("09:00"))
        assertEquals(18 to 0, PunchReminderTimes.parseDutyHm("18:00:00"))
        assertEquals(9 to 5, PunchReminderTimes.parseDutyHm("9:05"))
        assertNull(PunchReminderTimes.parseDutyHm("09:12 AM"))
        assertNull(PunchReminderTimes.parseDutyHm(""))
    }

    @Test
    fun `fires five minutes before duty when the morning is still ahead`() {
        val now = at(2026, Calendar.SEPTEMBER, 26, 8, 0)
        val fire = PunchReminderTimes.nextFireMillis("09:00", now, alreadyDoneToday = false)
        assertEquals(at(2026, Calendar.SEPTEMBER, 26, 8, 55), fire)
    }

    @Test
    fun `still notifies today if the user opens the app inside the lead window`() {
        val now = at(2026, Calendar.SEPTEMBER, 26, 8, 57)
        val fire = PunchReminderTimes.nextFireMillis("09:00", now, alreadyDoneToday = false)!!
        assertTrue(fire in now until now + 5_000)
    }

    @Test
    fun `rolls to tomorrow after duty has started and the late window closed`() {
        val now = at(2026, Calendar.SEPTEMBER, 26, 10, 0)
        val fire = PunchReminderTimes.nextFireMillis("09:00", now, alreadyDoneToday = false)
        assertEquals(at(2026, Calendar.SEPTEMBER, 27, 8, 55), fire)
    }

    @Test
    fun `skips today when punch in is already done`() {
        val now = at(2026, Calendar.SEPTEMBER, 26, 8, 0)
        val fire = PunchReminderTimes.nextFireMillis("09:00", now, alreadyDoneToday = true)
        assertEquals(at(2026, Calendar.SEPTEMBER, 27, 8, 55), fire)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
