package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.PunchElapsedFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PunchElapsedFormatterTest {

    @Test
    fun `formats a running shift in Hindi`() {
        val punchIn = PunchElapsedFormatter.parseClock("2026-09-07", "09:12 AM")!!
        val now = punchIn + ((2 * 3600) + (15 * 60) + 8) * 1000L

        assertEquals(
            "पंच इन लगे हुए 2 घंटे 15 मिनट 8 सेकंड",
            PunchElapsedFormatter.formatHindi(
                PunchElapsedFormatter.elapsedMillis(punchIn, now),
                running = true,
            ),
        )
    }

    @Test
    fun `formats seconds only before the first minute`() {
        val punchIn = PunchElapsedFormatter.parseClock("2026-09-07", "09:12 AM")!!
        val now = punchIn + 45_000L

        assertEquals(
            "पंच इन लगे हुए 45 सेकंड",
            PunchElapsedFormatter.formatHindi(
                PunchElapsedFormatter.elapsedMillis(punchIn, now),
                running = true,
            ),
        )
    }

    @Test
    fun `freezes the duration once punched out`() {
        val punchIn = PunchElapsedFormatter.parseClock("2026-09-07", "09:00 AM")!!
        val punchOut = PunchElapsedFormatter.parseClock("2026-09-07", "06:30 PM")!!
        val later = punchOut + 3_600_000L

        assertEquals(
            "कुल समय 9 घंटे 30 मिनट 0 सेकंड",
            PunchElapsedFormatter.formatHindi(
                PunchElapsedFormatter.elapsedMillis(punchIn, later, punchOut),
                running = false,
            ),
        )
    }

    @Test
    fun `blank clocks do not parse`() {
        assertNull(PunchElapsedFormatter.parseClock("2026-09-07", null))
        assertNull(PunchElapsedFormatter.parseClock(null, "09:12 AM"))
        assertNull(PunchElapsedFormatter.parseClock("2026-09-07", "  "))
    }
}
