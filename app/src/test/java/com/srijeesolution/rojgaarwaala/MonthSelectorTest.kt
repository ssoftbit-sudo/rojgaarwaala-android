package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.MonthSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthSelectorTest {

    @Test
    fun `builds the API value with a zero padded month`() {
        assertEquals("2025-08", MonthSelector.of(2025, 8).value)
        assertEquals("2025-11", MonthSelector.of(2025, 11).value)
    }

    @Test
    fun `builds a readable label`() {
        assertEquals("Aug 2025", MonthSelector.of(2025, 8).label)
        assertEquals("Jan 2026", MonthSelector.of(2026, 1).label)
        assertEquals("Dec 2025", MonthSelector.of(2025, 12).label)
    }

    @Test
    fun `december rolls forward into january of the next year`() {
        val next = MonthSelector.shift(2025, 12, 1)
        assertEquals(2026, next.year)
        assertEquals(1, next.month)
        assertEquals("2026-01", next.value)
        assertEquals("Jan 2026", next.label)
    }

    @Test
    fun `january rolls back into december of the previous year`() {
        val previous = MonthSelector.shift(2026, 1, -1)
        assertEquals(2025, previous.year)
        assertEquals(12, previous.month)
        assertEquals("2025-12", previous.value)
        assertEquals("Dec 2025", previous.label)
    }

    @Test
    fun `shifting by a full year keeps the month`() {
        assertEquals("2024-03", MonthSelector.shift(2025, 3, -12).value)
        assertEquals("2026-03", MonthSelector.shift(2025, 3, 12).value)
    }

    @Test
    fun `shifting further than a year still lands correctly`() {
        assertEquals("2024-11", MonthSelector.shift(2026, 2, -15).value)
        assertEquals("2027-05", MonthSelector.shift(2026, 2, 15).value)
    }

    @Test
    fun `zero shift is a no op`() {
        assertEquals("2025-08", MonthSelector.shift(2025, 8, 0).value)
    }

    @Test
    fun `last twelve months walks backwards across the year boundary`() {
        val months = MonthSelector.lastMonths(2026, 2, 12)
        assertEquals(12, months.size)
        assertEquals("2026-02", months.first().value)
        assertEquals("2026-01", months[1].value)
        assertEquals("2025-12", months[2].value)
        assertEquals("2025-03", months.last().value)
    }

    @Test
    fun `last months with a non positive count is empty`() {
        assertEquals(emptyList<MonthSelector.MonthOption>(), MonthSelector.lastMonths(2025, 8, 0))
        assertEquals(emptyList<MonthSelector.MonthOption>(), MonthSelector.lastMonths(2025, 8, -3))
    }

    @Test
    fun `parses a well formed month value`() {
        val parsed = MonthSelector.parse("2025-12")
        assertEquals(2025, parsed?.year)
        assertEquals(12, parsed?.month)
        assertEquals("Dec 2025", parsed?.label)
    }

    @Test
    fun `rejects malformed month values`() {
        assertNull(MonthSelector.parse(null))
        assertNull(MonthSelector.parse(""))
        assertNull(MonthSelector.parse("2025"))
        assertNull(MonthSelector.parse("2025-13"))
        assertNull(MonthSelector.parse("2025-00"))
        assertNull(MonthSelector.parse("abcd-08"))
        assertNull(MonthSelector.parse("2025-08-14"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an out of range month`() {
        MonthSelector.of(2025, 13)
    }
}
