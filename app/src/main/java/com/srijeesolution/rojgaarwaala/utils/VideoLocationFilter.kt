package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo

object VideoLocationFilter {

    fun matches(video: TopVideo, locationQuery: String): Boolean {
        if (HomeLocationDefaults.skipsDistrictFilter(locationQuery)) return true

        val needle = locationQuery.trim().lowercase()
        if (needle.isEmpty()) return true

        val haystacks = listOfNotNull(
            video.location,
            video.locationHint,
            extractLocationFromDescription(video.description),
        ).map { it.trim().lowercase() }.filter { it.isNotEmpty() }

        return haystacks.any { hay ->
            hay.contains(needle) || needle.contains(hay)
        }
    }

    fun filterVideos(videos: List<TopVideo>, locationQuery: String): List<TopVideo> {
        if (HomeLocationDefaults.skipsDistrictFilter(locationQuery)) return videos
        val trimmed = locationQuery.trim()
        if (trimmed.isEmpty()) return videos
        return videos.filter { matches(it, trimmed) }
    }

    private fun extractLocationFromDescription(description: String?): String? {
        if (description.isNullOrBlank()) return null
        val regex = Regex("(?:Location|Loc\\.?)\\s*:\\s*([^\\n\\r]+)", RegexOption.IGNORE_CASE)
        return regex.find(description)?.groupValues?.getOrNull(1)?.trim()
    }
}
