package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.data.remote.model.DayJob
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobSlot
import com.srijeesolution.rojgaarwaala.utils.DayJobFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class DayJobFormatTest {

    private val today = LocalDate.of(2026, 10, 25)

    @Test fun whenTextNamesTodayAndTomorrow() {
        assertEquals("Today, 2:30 PM", DayJobFormat.whenText("2026-10-25", "14:30", today))
        assertEquals("Tomorrow, 8:00 AM", DayJobFormat.whenText("2026-10-26", "08:00", today))
        assertEquals("28 Oct, 8:00 AM", DayJobFormat.whenText("2026-10-28", "08:00:00", today))
    }

    @Test fun seatsAndDistance() {
        val job = DayJob(neededTotal = 3, filledTotal = 1, slots = listOf(DayJobSlot("mason", "Mason", 1, 1), DayJobSlot("helper", "Helper", 2, 0)))
        assertEquals("2 of 3 seats left", DayJobFormat.seatsLeftText(job))
        assertEquals("1/3 coming", DayJobFormat.comingText(job))
        assertEquals("Mason 1, Helper 2", DayJobFormat.slotsText(job))
        assertEquals("All seats taken", DayJobFormat.seatsLeftText(job.copy(filledTotal = 3)))
        assertEquals("2.4 km away", DayJobFormat.distanceText(2.43))
        assertEquals("400 m away", DayJobFormat.distanceText(0.4))
    }

    @Test fun closesInCountsDown() {
        val now = OffsetDateTime.parse("2026-10-25T09:00:00+05:30")
        assertEquals("Closes in 2h 10m", DayJobFormat.closesInText("2026-10-25T11:10:00+05:30", now))
        assertEquals("Closes in 25m", DayJobFormat.closesInText("2026-10-25T09:25:00+05:30", now))
        assertEquals("Closed", DayJobFormat.closesInText("2026-10-25T08:00:00+05:30", now))
    }

    @Test fun postFormListsWhatIsMissing() {
        val errors = DayJobFormat.postErrors("", mapOf("helper" to 0), 50, hasDate = true, hasTime = false, hasPin = false)
        assertEquals(listOf("Write what the work is", "Add at least one worker", "Wage must be at least ₹100 a day", "Choose the start time", "Pin the work site on the map"), errors)
        assertTrue(DayJobFormat.postErrors("Slab", mapOf("helper" to 2), 600, true, true, true).isEmpty())
    }
}
