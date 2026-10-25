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

    @Test fun whenTextSaysAajAndKal() {
        assertEquals("आज, दोपहर 2:30", DayJobFormat.whenText("2026-10-25", "14:30", today))
        assertEquals("कल, सुबह 8:00", DayJobFormat.whenText("2026-10-26", "08:00", today))
        assertEquals("28 अक्टूबर, सुबह 8:00", DayJobFormat.whenText("2026-10-28", "08:00:00", today))
        assertEquals("28 अक्टूबर, रात 9:15", DayJobFormat.whenText("2026-10-28", "21:15", today))
    }

    @Test fun seatsAndDistance() {
        val job = DayJob(neededTotal = 3, filledTotal = 1, slots = listOf(DayJobSlot("mason", "मिस्त्री", 1, 1), DayJobSlot("helper", "हेल्पर", 2, 0)))
        assertEquals("3 में से 2 जगह खाली", DayJobFormat.seatsLeftText(job))
        assertEquals("1/3 आ रहे हैं", DayJobFormat.comingText(job))
        assertEquals("मिस्त्री 1, हेल्पर 2", DayJobFormat.slotsText(job))
        assertEquals("सब जगह भर गईं", DayJobFormat.seatsLeftText(job.copy(filledTotal = 3)))
        assertEquals("2.4 km दूर", DayJobFormat.distanceText(2.43))
        assertEquals("400 मीटर दूर", DayJobFormat.distanceText(0.4))
    }

    @Test fun closesInCountsDown() {
        val now = OffsetDateTime.parse("2026-10-25T09:00:00+05:30")
        assertEquals("2 घंटे 10 मिनट में बंद", DayJobFormat.closesInText("2026-10-25T11:10:00+05:30", now))
        assertEquals("25 मिनट में बंद", DayJobFormat.closesInText("2026-10-25T09:25:00+05:30", now))
        assertEquals("बंद", DayJobFormat.closesInText("2026-10-25T08:00:00+05:30", now))
    }

    @Test fun postFormListsWhatIsMissing() {
        val errors = DayJobFormat.postErrors("", mapOf("helper" to 0), 50, hasDate = true, hasTime = false, hasPin = false)
        assertEquals(listOf("काम का नाम लिखें", "कम से कम एक मज़दूर जोड़ें", "मज़दूरी कम से कम ₹100/दिन हो", "काम शुरू होने का समय चुनें", "नक्शे पर काम की जगह लगाएँ"), errors)
        assertTrue(DayJobFormat.postErrors("Slab", mapOf("helper" to 2), 600, true, true, true).isEmpty())
    }

    @Test fun alertHoursAndDays() {
        assertEquals("कभी भी", DayJobFormat.alertHoursText(null, "20:00"))
        assertEquals("सुबह 6:00 – रात 8:00", DayJobFormat.alertHoursText("06:00", "20:00"))
        assertEquals("2 नवंबर", DayJobFormat.dayText("2026-11-02T09:00:00+05:30"))
    }

    @Test fun wageHintReadsPlainly() {
        assertEquals("Raipur में हेल्पर को आम तौर पर ₹600–₹650/दिन मिलते हैं", DayJobFormat.wageHintText("हेल्पर", 600, 650, "Raipur"))
        assertEquals("मिस्त्री को आम तौर पर ₹800/दिन मिलते हैं", DayJobFormat.wageHintText("मिस्त्री", 800, 800, null))
    }
}
