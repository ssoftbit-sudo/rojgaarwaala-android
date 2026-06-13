package com.srijeesolution.rojgaarwaala.utils

object LocationDisplayUtils {

    /** Collapse "Delhi, Delhi" / "Delhi / Delhi" to a single label for UI. */
    fun formatForDisplay(raw: String?): String {
        val trimmed = raw.orEmpty().trim()
        if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true)) return ""

        val parts = trimmed.split(',', '/')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val unique = mutableListOf<String>()
        for (part in parts) {
            if (unique.none { it.equals(part, ignoreCase = true) }) {
                unique.add(part)
            }
        }
        return unique.joinToString(", ")
    }
}
