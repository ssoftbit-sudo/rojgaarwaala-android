package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData

object ImageLocationFilter {

    fun matches(image: ImageData, locationQuery: String): Boolean {
        if (HomeLocationDefaults.skipsDistrictFilter(locationQuery)) return true

        val needle = locationQuery.trim().lowercase()
        if (needle.isEmpty()) return true

        // The admin files each job under districts. When present they decide it:
        // the place text is a Google address and often never names the district.
        val districts = image.districts.orEmpty().map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (districts.isNotEmpty()) {
            return districts.any { sameDistrict(it, needle) }
        }

        // Older backend, or a job with no district: fall back to the place text.
        val haystacks = listOfNotNull(
            image.location,
            extractLocationFromDescription(image.description),
        ).map { it.trim().lowercase() }.filter { it.isNotEmpty() }

        return haystacks.any { hay ->
            hay.contains(needle) || needle.contains(hay)
        }
    }

    /** "Janjgir" and "Janjgir-Champa" are the same district; "Balod" and "Baloda Bazar" are not. */
    private fun sameDistrict(a: String, b: String): Boolean =
        a == b || a.substringBefore('-') == b || b.substringBefore('-') == a

    private fun extractLocationFromDescription(description: String?): String? {
        if (description.isNullOrBlank()) return null
        val regex = Regex("(?:Location|Loc\\.?)\\s*:\\s*([^\\n\\r]+)", RegexOption.IGNORE_CASE)
        return regex.find(description)?.groupValues?.getOrNull(1)?.trim()
    }
}
