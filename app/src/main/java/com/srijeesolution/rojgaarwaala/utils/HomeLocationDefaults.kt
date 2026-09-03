package com.srijeesolution.rojgaarwaala.utils

/**
 * Default toolbar location: show full home feed (no district filter), same as before location feature.
 */
object HomeLocationDefaults {

    const val ALL_CHHATTISGARH = "All Chhattisgarh"

    fun normalize(raw: String?): String {
        val trimmed = raw.orEmpty().trim()
        return if (trimmed.isBlank()) ALL_CHHATTISGARH else trimmed
    }

    /** True when home should show all categories/videos (no district filter). */
    fun skipsDistrictFilter(location: String?): Boolean {
        val key = location.orEmpty().trim().lowercase()
        return key.isEmpty() || key == "all chhattisgarh" || key == "all chhattishgarh"
    }
}
