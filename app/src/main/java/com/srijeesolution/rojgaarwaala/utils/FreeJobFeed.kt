package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class FreeJobItem(
    val job: ImageData,
    val categoryTitle: String?,
)

object FreeJobFeed {
    enum class Filter { ALL, WITH_LOCATION, WITHOUT_LOCATION }
    enum class ViewMode { LIST, TILE }
    enum class Sort { NEWEST, OLDEST }

    fun hasCoordinates(job: ImageData): Boolean =
        job.latitude != null && job.longitude != null

    fun hasPlaceText(job: ImageData): Boolean =
        !job.areaName.isNullOrBlank() || !job.location.isNullOrBlank()

    fun hasLocation(job: ImageData): Boolean = hasCoordinates(job) || hasPlaceText(job)

    fun flatten(categories: List<ImageSubItem>): List<FreeJobItem> {
        return categories.flatMap { category ->
            category.images.orEmpty().map { job ->
                FreeJobItem(job = job, categoryTitle = category.title)
            }
        }
    }

    fun withLocation(items: List<FreeJobItem>): List<FreeJobItem> =
        items.filter { hasLocation(it.job) }

    fun withoutLocation(items: List<FreeJobItem>): List<FreeJobItem> =
        items.filter { !hasLocation(it.job) }

    fun categoriesWithoutLocation(categories: List<ImageSubItem>): List<ImageSubItem> {
        return categories.mapNotNull { category ->
            val images = category.images.orEmpty().filter { !hasLocation(it) }
            if (images.isEmpty()) null else category.copy(images = images)
        }
    }

    fun companyName(item: FreeJobItem): String {
        return item.job.user?.name?.trim().orEmpty().ifBlank {
            item.categoryTitle?.trim().orEmpty().ifBlank { "Rojgaarwaala" }
        }
    }

    fun salaryLine(job: ImageData): String {
        return job.salaryText?.trim().orEmpty().ifBlank {
            job.offer?.trim().orEmpty()
        }
    }

    fun placeLine(job: ImageData): String {
        return job.areaName?.trim()?.takeIf { it.isNotEmpty() }
            ?: job.location?.trim()?.takeIf { it.isNotEmpty() }
            ?: ""
    }

    fun mapGeoUri(job: ImageData): String? {
        val place = placeLine(job)
        val query = if (place.isNotBlank()) {
            URLEncoder.encode(place, StandardCharsets.UTF_8.name()).replace("+", "%20")
        } else {
            null
        }
        val lat = job.latitude
        val lng = job.longitude
        if (lat != null && lng != null) {
            return if (query != null) "geo:$lat,$lng?q=$query" else "geo:$lat,$lng?q=$lat,$lng"
        }
        if (query == null) return null
        return "geo:0,0?q=$query"
    }

    fun postedKey(job: ImageData): String =
        job.createdAt?.trim().orEmpty().ifBlank { job.publishDate?.trim().orEmpty() }

    fun sortedItems(items: List<FreeJobItem>, sort: Sort): List<FreeJobItem> {
        return when (sort) {
            Sort.NEWEST -> items.sortedByDescending { postedKey(it.job) }
            Sort.OLDEST -> items.sortedBy { postedKey(it.job) }
        }
    }

    fun sortedCategories(categories: List<ImageSubItem>, sort: Sort): List<ImageSubItem> {
        val mapped = categories.map { category ->
            val images = when (sort) {
                Sort.NEWEST -> category.images.orEmpty().sortedByDescending { postedKey(it) }
                Sort.OLDEST -> category.images.orEmpty().sortedBy { postedKey(it) }
            }
            category.copy(images = images)
        }
        return when (sort) {
            Sort.NEWEST -> mapped.sortedByDescending { postedKey(it.images?.firstOrNull() ?: ImageData()) }
            Sort.OLDEST -> mapped.sortedBy { postedKey(it.images?.firstOrNull() ?: ImageData()) }
        }
    }

    fun appendItems(existing: List<FreeJobItem>, incoming: List<FreeJobItem>): List<FreeJobItem> {
        if (existing.isEmpty()) return incoming
        val seen = existing.mapNotNull { it.job.id }.toSet()
        return existing + incoming.filter { it.job.id == null || it.job.id !in seen }
    }

    fun withDistances(items: List<FreeJobItem>, userLat: Double?, userLng: Double?): List<FreeJobItem> {
        if (userLat == null || userLng == null) return items
        return items.map { item ->
            val job = item.job
            if (job.distanceKm != null || job.latitude == null || job.longitude == null) {
                item
            } else {
                item.copy(job = job.copy(distanceKm = JobMapPins.kmBetween(userLat, userLng, job.latitude, job.longitude)))
            }
        }
    }

    fun categoriesWithDistances(
        categories: List<ImageSubItem>,
        userLat: Double?,
        userLng: Double?,
    ): List<ImageSubItem> {
        if (userLat == null || userLng == null) return categories
        return categories.map { category ->
            category.copy(
                images = category.images?.map { job ->
                    if (job.distanceKm != null || job.latitude == null || job.longitude == null) {
                        job
                    } else {
                        job.copy(distanceKm = JobMapPins.kmBetween(userLat, userLng, job.latitude, job.longitude))
                    }
                },
            )
        }
    }

    fun mergeCategories(existing: List<ImageSubItem>, incoming: List<ImageSubItem>): List<ImageSubItem> {
        if (existing.isEmpty()) return incoming
        val merged = existing.associateBy { it.id }.toMutableMap()
        val order = existing.map { it.id }.toMutableList()
        incoming.forEach { category ->
            val key = category.id
            val previous = if (key != null) merged[key] else null
            if (key != null && previous != null) {
                val seen = previous.images.orEmpty().mapNotNull { it.id }.toSet()
                val extra = category.images.orEmpty().filter { it.id == null || it.id !in seen }
                merged[key] = previous.copy(images = previous.images.orEmpty() + extra)
            } else {
                merged[key] = category
                order.add(key)
            }
        }
        return order.mapNotNull { merged[it] }
    }
}
