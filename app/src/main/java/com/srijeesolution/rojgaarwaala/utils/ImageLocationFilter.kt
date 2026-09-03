package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData

object ImageLocationFilter {

    fun matches(image: ImageData, locationQuery: String): Boolean {
        if (HomeLocationDefaults.skipsDistrictFilter(locationQuery)) return true

        val needle = locationQuery.trim().lowercase()
        if (needle.isEmpty()) return true

        val haystacks = listOfNotNull(
            image.location,
            extractLocationFromDescription(image.description),
        ).map { it.trim().lowercase() }.filter { it.isNotEmpty() }

        return haystacks.any { hay ->
            hay.contains(needle) || needle.contains(hay)
        }
    }

    private fun extractLocationFromDescription(description: String?): String? {
        if (description.isNullOrBlank()) return null
        val regex = Regex("(?:Location|Loc\\.?)\\s*:\\s*([^\\n\\r]+)", RegexOption.IGNORE_CASE)
        return regex.find(description)?.groupValues?.getOrNull(1)?.trim()
    }
}
