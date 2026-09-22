package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem
import kotlin.math.roundToInt

/** Jobs that can be shown as map pins, with distance from the seeker's current location. */
object JobMapPins {

    fun withCoordinates(categories: List<ImageSubItem>): List<ImageData> {
        return categories.asSequence()
            .flatMap { it.images.orEmpty().asSequence() }
            .filter { it.latitude != null && it.longitude != null }
            .distinctBy { it.id ?: "${it.latitude},${it.longitude},${it.title}" }
            .toList()
    }

    fun withDistanceFrom(
        jobs: List<ImageData>,
        userLat: Double?,
        userLng: Double?,
    ): List<ImageData> {
        if (userLat == null || userLng == null) return jobs
        return jobs.map { job ->
            val lat = job.latitude ?: return@map job
            val lng = job.longitude ?: return@map job
            job.copy(distanceKm = kmBetween(userLat, userLng, lat, lng))
        }
    }

    fun kmBetween(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): Double {
        val metres = GeofenceEvaluator.distanceInMetres(fromLat, fromLng, toLat, toLng)
        return Math.round(metres / 10.0) / 100.0
    }

    fun distanceLabel(km: Double?): String {
        if (km == null) return ""
        return if (km < 1.0) {
            "आपसे ${(km * 1000).roundToInt()} मी दूर"
        } else {
            "आपसे $km किमी दूर"
        }
    }
}
