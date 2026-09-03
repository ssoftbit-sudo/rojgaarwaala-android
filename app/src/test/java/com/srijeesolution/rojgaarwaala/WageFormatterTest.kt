package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.WageFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class WageFormatterTest {

    @Test
    fun `formats a whole thousand with a group separator`() {
        assertEquals("\u20B912,000", WageFormatter.format(12000.0))
    }

    @Test
    fun `formats small amounts without a separator`() {
        assertEquals("\u20B90", WageFormatter.format(0.0))
        assertEquals("\u20B9450", WageFormatter.format(450.0))
        assertEquals("\u20B9999", WageFormatter.format(999.0))
    }

    @Test
    fun `groups lakhs the Indian way`() {
        assertEquals("\u20B91,000", WageFormatter.format(1000.0))
        assertEquals("\u20B91,23,456", WageFormatter.format(123456.0))
        assertEquals("\u20B912,34,567", WageFormatter.format(1234567.0))
        assertEquals("\u20B91,23,45,678", WageFormatter.format(12345678.0))
    }

    @Test
    fun `keeps paise only when they are non zero`() {
        assertEquals("\u20B9450.50", WageFormatter.format(450.5))
        assertEquals("\u20B9450.05", WageFormatter.format(450.05))
        assertEquals("\u20B9450", WageFormatter.format(450.0))
    }

    @Test
    fun `rounds to two decimal places`() {
        assertEquals("\u20B9450.13", WageFormatter.format(450.126))
        assertEquals("\u20B9451", WageFormatter.format(450.999))
    }

    @Test
    fun `null is rendered as zero`() {
        assertEquals("\u20B90", WageFormatter.format(null))
    }

    @Test
    fun `negative amounts keep the sign outside the symbol`() {
        assertEquals("-\u20B92,500", WageFormatter.format(-2500.0))
    }

    @Test
    fun `signed formatting marks credits with a plus`() {
        assertEquals("+\u20B92,500", WageFormatter.formatSigned(2500.0))
        assertEquals("-\u20B92,500", WageFormatter.formatSigned(-2500.0))
        assertEquals("\u20B90", WageFormatter.formatSigned(0.0))
    }
}
