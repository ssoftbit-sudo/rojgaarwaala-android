package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeFactory
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Client-side mirror of the backend `GeofenceService`, used only to tell the employee where
 * they stand before they tap punch. The server re-runs the same checks on every punch and
 * remains the authority; this exists so a labourer is not sent to a factory gate only to be
 * rejected after a network round trip.
 *
 * The formula, the earth radius, the order of the checks and the fallback defaults are
 * deliberately identical to `App\Services\GeofenceService` so the two cannot disagree about
 * a borderline position.
 *
 * Pure Kotlin on purpose so it can be covered by JVM unit tests.
 */
object GeofenceEvaluator {

    /** Mean earth radius in metres, matching GeofenceService::EARTH_RADIUS_METRES. */
    const val EARTH_RADIUS_METRES = 6371000.0

    /** Mirrors config('attendance.default_geofence_radius'). */
    const val DEFAULT_RADIUS_METRES = 200

    /** Mirrors config('attendance.default_accuracy_threshold'). */
    const val DEFAULT_ACCURACY_THRESHOLD_METRES = 50

    enum class Status {
        /** Inside the allowed radius with a good enough fix: punching should succeed. */
        INSIDE,

        /** A trustworthy fix that falls outside the radius. */
        OUTSIDE,

        /** The fix is too imprecise to judge, so the server would reject it. */
        POOR_ACCURACY,

        /** The factory has no coordinates, so no punch can ever be verified. */
        FACTORY_LOCATION_MISSING,

        /** No assignment for today, so there is no geofence to measure against. */
        NO_FACTORY,

        /** Waiting on the first GPS fix. */
        LOCATING,
    }

    data class Evaluation(
        val status: Status,
        /** Null until both a factory location and a device fix are known. */
        val distanceMetres: Double?,
        val radiusMetres: Int,
        val accuracyThresholdMetres: Int,
        /** Short label for the status chip. */
        val label: String,
        /** Sentence shown under the punch button, explaining what to do. */
        val message: String,
    ) {
        /** True only when a punch is expected to be accepted by the server. */
        val allowed: Boolean get() = status == Status.INSIDE
    }

    /**
     * Great-circle distance in metres, rounded to 2 decimals like the backend.
     */
    fun distanceInMetres(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double,
    ): Double {
        val latFrom = Math.toRadians(fromLatitude)
        val latTo = Math.toRadians(toLatitude)
        val deltaLat = latTo - latFrom
        val deltaLon = Math.toRadians(toLongitude - fromLongitude)

        val a = sin(deltaLat / 2).pow(2) +
            cos(latFrom) * cos(latTo) * sin(deltaLon / 2).pow(2)

        val metres = 2 * EARTH_RADIUS_METRES * asin(min(1.0, sqrt(a)))
        return Math.round(metres * 100.0) / 100.0
    }

    /**
     * @param accuracy the device's reported horizontal accuracy in metres, or null if unknown.
     * A null accuracy is treated as acceptable, matching the backend, which only rejects an
     * accuracy it was actually given.
     */
    fun evaluate(
        factory: EmployeeFactory?,
        latitude: Double?,
        longitude: Double?,
        accuracy: Double?,
    ): Evaluation {
        // PHP's ?: treats 0 as absent, so a 0 radius must fall back the same way here.
        val radius = factory?.geofenceRadius?.takeIf { it > 0 } ?: DEFAULT_RADIUS_METRES
        val threshold = factory?.gpsAccuracyThreshold?.takeIf { it > 0 }
            ?: DEFAULT_ACCURACY_THRESHOLD_METRES

        if (factory == null) {
            return Evaluation(
                status = Status.NO_FACTORY,
                distanceMetres = null,
                radiusMetres = radius,
                accuracyThresholdMetres = threshold,
                label = "No factory",
                message = "You are not assigned to any factory today. Please contact your supervisor.",
            )
        }

        val factoryLat = factory.latitude
        val factoryLng = factory.longitude
        if (factoryLat == null || factoryLng == null) {
            return Evaluation(
                status = Status.FACTORY_LOCATION_MISSING,
                distanceMetres = null,
                radiusMetres = radius,
                accuracyThresholdMetres = threshold,
                label = "Location not set",
                message = "Factory location is not set. Please contact your supervisor.",
            )
        }

        if (latitude == null || longitude == null) {
            return Evaluation(
                status = Status.LOCATING,
                distanceMetres = null,
                radiusMetres = radius,
                accuracyThresholdMetres = threshold,
                label = "Locating...",
                message = "Finding your location...",
            )
        }

        val distance = distanceInMetres(latitude, longitude, factoryLat, factoryLng)

        if (accuracy != null && accuracy > threshold) {
            return Evaluation(
                status = Status.POOR_ACCURACY,
                distanceMetres = distance,
                radiusMetres = radius,
                accuracyThresholdMetres = threshold,
                label = "Weak GPS",
                message = "GPS signal is weak. Move to an open area, away from buildings, and wait a moment.",
            )
        }

        val factoryName = factory.name?.takeIf { it.isNotBlank() } ?: "the factory"

        return if (distance > radius) {
            Evaluation(
                status = Status.OUTSIDE,
                distanceMetres = distance,
                radiusMetres = radius,
                accuracyThresholdMetres = threshold,
                label = "Outside area",
                message = "You are ${formatDistance(distance)} away from $factoryName. " +
                    "Move within $radius meters of the factory to mark attendance.",
            )
        } else {
            Evaluation(
                status = Status.INSIDE,
                distanceMetres = distance,
                radiusMetres = radius,
                accuracyThresholdMetres = threshold,
                label = "Inside area",
                message = "You are ${formatDistance(distance)} from $factoryName, " +
                    "inside the allowed $radius meter area. You can mark attendance.",
            )
        }
    }

    /** Mirrors GeofenceService::formatDistance. */
    fun formatDistance(metres: Double): String {
        if (metres >= 1000) {
            val km = Math.round(metres / 1000 * 100.0) / 100.0
            // PHP renders a whole float as "1", not "1.0", so drop the empty decimal to keep
            // the app and the server messages identical.
            val rendered = if (km == Math.floor(km)) km.toLong().toString() else km.toString()
            return "$rendered KM"
        }
        return "${metres.roundToInt()} meters"
    }
}
