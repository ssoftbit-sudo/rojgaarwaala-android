package com.srijeesolution.rojgaarwaala.utils

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Rupee formatting with Indian digit grouping (12,00,000 rather than 1,200,000).
 *
 * Grouping is done by hand instead of via [java.text.NumberFormat] so the output does
 * not shift with the device locale, and so the helper stays JVM-testable.
 */
object WageFormatter {

    private const val RUPEE = "\u20B9"

    /** 12000.0 -> "₹12,000", 450.5 -> "₹450.50", null -> "₹0". */
    fun format(amount: Double?): String {
        val value = amount ?: 0.0
        val sign = if (value < 0) "-" else ""
        return sign + RUPEE + formatAbsolute(abs(value))
    }

    /** Same as [format] but keeps an explicit "+" for credits, used for balances. */
    fun formatSigned(amount: Double?): String {
        val value = amount ?: 0.0
        if (value <= 0) return format(value)
        return "+" + format(value)
    }

    private fun formatAbsolute(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        val whole = rounded.toLong()
        val paise = ((rounded - whole) * 100.0).roundToLong()
        val grouped = groupIndian(whole)
        return if (paise == 0L) grouped else "$grouped.${paise.toString().padStart(2, '0')}"
    }

    /** Groups the last three digits, then in pairs: 1234567 -> "12,34,567". */
    private fun groupIndian(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        val head = digits.substring(0, digits.length - 3)
        val tail = digits.substring(digits.length - 3)
        val builder = StringBuilder()
        var index = head.length
        while (index > 2) {
            builder.insert(0, "," + head.substring(index - 2, index))
            index -= 2
        }
        builder.insert(0, head.substring(0, index))
        return "$builder,$tail"
    }
}
