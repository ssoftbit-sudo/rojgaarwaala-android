package com.srijeesolution.rojgaarwaala.utils

/**
 * Hindi copy for the labour attendance screens. Server payloads stay English so the
 * contract tests do not move; the app translates only what the employee sees.
 */
object AttendanceHindi {

    fun greeting(value: String?): String = when (value?.trim()?.lowercase()) {
        "good morning" -> "सुप्रभात"
        "good afternoon" -> "नमस्कार"
        "good evening" -> "शुभ संध्या"
        "welcome" -> "स्वागत है"
        else -> value?.takeIf { it.isNotBlank() } ?: "स्वागत है"
    }

    fun status(value: String?): String {
        val key = value?.trim()?.lowercase()?.replace('_', ' ') ?: return "हाजिरी नहीं लगी"
        return when (key) {
            "present" -> "हाजिर"
            "absent" -> "गैरहाजिर"
            "half day" -> "आधा दिन"
            "not marked", "unmarked" -> "हाजिरी नहीं लगी"
            "0 hours", "0 hour" -> "0 घंटे"
            "marked" -> "लग गई"
            "paid leave" -> "पेड छुट्टी"
            "unpaid leave" -> "बिना पे छुट्टी"
            else -> value
        }
    }

    fun paymentType(value: String?): String {
        val key = value?.trim()?.lowercase()?.replace('_', ' ') ?: return "-"
        return when (key) {
            "advance" -> "एडवांस"
            "salary", "salary payment", "salary paid" -> "सैलरी"
            "bonus" -> "बोनस"
            "deduction" -> "कटौती"
            "other" -> "अन्य"
            else -> value
        }
    }
}
