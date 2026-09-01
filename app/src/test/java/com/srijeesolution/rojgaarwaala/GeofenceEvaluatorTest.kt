package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeFactory
import com.srijeesolution.rojgaarwaala.utils.GeofenceEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The client geofence only earns its place if it agrees with the server, otherwise the app
 * promises a punch the backend then refuses (or hides a punch that would have worked).
 *
 * The expected distances and formatted strings below were produced by running the deployed
 * `App\Services\GeofenceService` over the same coordinates, so these assertions pin the two
 * implementations together rather than restating the Kotlin arithmetic.
 */
class GeofenceEvaluatorTest {

    private fun factory(
        latitude: Double? = MUMBAI_LAT,
        longitude: Double? = MUMBAI_LNG,
        radius: Int? = 200,
        accuracyThreshold: Int? = 50,
        name: String? = "ABC Factory",
    ) = EmployeeFactory(
        id = 1,
        name = name,
        code = "ABC001",
        address = "Andheri",
        latitude = latitude,
        longitude = longitude,
        geofenceRadius = radius,
        gpsAccuracyThreshold = accuracyThreshold,
    )

    // --- Haversine parity with GeofenceService::distanceInMetres ---

    @Test
    fun `distance matches the backend for identical points`() {
        assertEquals(
            0.0,
            GeofenceEvaluator.distanceInMetres(MUMBAI_LAT, MUMBAI_LNG, MUMBAI_LAT, MUMBAI_LNG),
            0.0,
        )
    }

    @Test
    fun `distance matches the backend for a short northward hop`() {
        assertEquals(
            99.96,
            GeofenceEvaluator.distanceInMetres(MUMBAI_LAT, MUMBAI_LNG, 19.076899, MUMBAI_LNG),
            0.0,
        )
    }

    @Test
    fun `distance matches the backend for a short eastward hop`() {
        assertEquals(
            249.9,
            GeofenceEvaluator.distanceInMetres(MUMBAI_LAT, MUMBAI_LNG, MUMBAI_LAT, 72.880078),
            0.0,
        )
    }

    @Test
    fun `distance matches the backend beyond a kilometre`() {
        assertEquals(
            1501.13,
            GeofenceEvaluator.distanceInMetres(MUMBAI_LAT, MUMBAI_LNG, 19.0895, MUMBAI_LNG),
            0.0,
        )
    }

    @Test
    fun `distance matches the backend across the country`() {
        assertEquals(
            1148094.87,
            GeofenceEvaluator.distanceInMetres(28.6139, 77.2090, MUMBAI_LAT, MUMBAI_LNG),
            0.0,
        )
    }

    @Test
    fun `distance matches the backend in the southern hemisphere`() {
        assertEquals(
            148.26,
            GeofenceEvaluator.distanceInMetres(-33.8688, 151.2093, -33.8700, 151.2100),
            0.0,
        )
    }

    // --- formatDistance parity with GeofenceService::formatDistance ---

    @Test
    fun `formats distances exactly as the backend does`() {
        assertEquals("0 meters", GeofenceEvaluator.formatDistance(0.0))
        assertEquals("45 meters", GeofenceEvaluator.formatDistance(45.4))
        assertEquals("46 meters", GeofenceEvaluator.formatDistance(45.5))
        assertEquals("999 meters", GeofenceEvaluator.formatDistance(999.4))
        // PHP prints a whole float without its decimal, so "1 KM" and not "1.0 KM".
        assertEquals("1 KM", GeofenceEvaluator.formatDistance(1000.0))
        assertEquals("1.23 KM", GeofenceEvaluator.formatDistance(1234.56))
        assertEquals("12.5 KM", GeofenceEvaluator.formatDistance(12500.0))
    }

    // --- Decision order, mirroring GeofenceService::verify ---

    @Test
    fun `allows a punch well inside the radius`() {
        val result = GeofenceEvaluator.evaluate(factory(), 19.076899, MUMBAI_LNG, 10.0)

        assertEquals(GeofenceEvaluator.Status.INSIDE, result.status)
        assertTrue(result.allowed)
        assertEquals(99.96, result.distanceMetres!!, 0.0)
        assertTrue(result.message.contains("100 meters"))
        assertTrue(result.message.contains("ABC Factory"))
    }

    @Test
    fun `treats the radius boundary as inside just like the backend`() {
        // The backend rejects only when distance > radius, so exactly on the line is
        // allowed. A perfect fix isolates the distance check from the uncertainty one.
        val onLine = GeofenceEvaluator.evaluate(
            factory(radius = 100),
            19.076899,
            MUMBAI_LNG,
            0.0,
        )
        assertEquals(GeofenceEvaluator.Status.INSIDE, onLine.status)

        val justOutside = GeofenceEvaluator.evaluate(
            factory(radius = 99),
            19.076899,
            MUMBAI_LNG,
            0.0,
        )
        assertEquals(GeofenceEvaluator.Status.OUTSIDE, justOutside.status)
    }

    @Test
    fun `shrinks the usable area by the accuracy, which is the point of the rule`() {
        // Standing on the boundary with a 10m fix can no longer be confirmed as inside:
        // the true position may be up to 109.96m out of a 100m geofence. This is the
        // deliberate cost of only crediting attendance the fix can actually prove.
        val result = GeofenceEvaluator.evaluate(
            factory(radius = 100),
            19.076899,
            MUMBAI_LNG,
            10.0,
        )

        assertEquals(GeofenceEvaluator.Status.POOR_ACCURACY, result.status)
        assertFalse(result.allowed)
    }

    @Test
    fun `blocks a punch outside the radius and says how far away it is`() {
        val result = GeofenceEvaluator.evaluate(factory(), 19.0895, MUMBAI_LNG, 10.0)

        assertEquals(GeofenceEvaluator.Status.OUTSIDE, result.status)
        assertFalse(result.allowed)
        assertTrue(result.message.contains("1.5 KM"))
        assertTrue(result.message.contains("200 meters"))
    }

    @Test
    fun `rejects a fix whose uncertainty reaches past the radius`() {
        // Standing right on the factory, but the fix could be anywhere within 250m,
        // which the 200m geofence cannot contain.
        val result = GeofenceEvaluator.evaluate(factory(radius = 200), MUMBAI_LAT, MUMBAI_LNG, 250.0)

        assertEquals(GeofenceEvaluator.Status.POOR_ACCURACY, result.status)
        assertFalse(result.allowed)
        // The distance is still reported, matching the backend's poor_accuracy payload.
        assertEquals(0.0, result.distanceMetres!!, 0.0)
    }

    @Test
    fun `accepts a vague fix that the radius can still contain`() {
        // The case that used to fail: near the gate on network positioning only, where the
        // handset reports 100m. 99.96 + 100 fits inside 200, and the old fixed 50m cutoff
        // refused it even though the geofence proves the position.
        val result = GeofenceEvaluator.evaluate(factory(radius = 200), 19.076899, MUMBAI_LNG, 100.0)

        assertEquals(GeofenceEvaluator.Status.INSIDE, result.status)
        assertTrue(result.allowed)
    }

    @Test
    fun `treats an uncertainty that lands exactly on the radius as inside`() {
        // Standing on the factory itself, so distance is exactly 0 and the sum is exactly
        // the radius: the backend rejects only when the sum exceeds it.
        val result = GeofenceEvaluator.evaluate(factory(radius = 200), MUMBAI_LAT, MUMBAI_LNG, 200.0)

        assertEquals(GeofenceEvaluator.Status.INSIDE, result.status)
    }

    @Test
    fun `reports outside rather than weak GPS when the position itself is too far`() {
        // Distance alone breaks the geofence, so the employee needs to move, not wait
        // for a better fix. The backend orders the checks the same way.
        val result = GeofenceEvaluator.evaluate(factory(radius = 200), 19.0895, MUMBAI_LNG, 500.0)

        assertEquals(GeofenceEvaluator.Status.OUTSIDE, result.status)
    }

    @Test
    fun `ignores the accuracy threshold column now that the radius bounds the fix`() {
        // A tight threshold no longer blocks a punch the geofence can prove.
        val result = GeofenceEvaluator.evaluate(
            factory(radius = 200, accuracyThreshold = 5),
            MUMBAI_LAT,
            MUMBAI_LNG,
            120.0,
        )

        assertEquals(GeofenceEvaluator.Status.INSIDE, result.status)
    }

    @Test
    fun `treats an unknown accuracy as acceptable`() {
        val result = GeofenceEvaluator.evaluate(factory(), MUMBAI_LAT, MUMBAI_LNG, null)

        assertEquals(GeofenceEvaluator.Status.INSIDE, result.status)
    }

    @Test
    fun `reports a factory with no coordinates as unconfigured`() {
        val result = GeofenceEvaluator.evaluate(
            factory(latitude = null, longitude = null),
            MUMBAI_LAT,
            MUMBAI_LNG,
            5.0,
        )

        assertEquals(GeofenceEvaluator.Status.FACTORY_LOCATION_MISSING, result.status)
        assertFalse(result.allowed)
        assertEquals(null, result.distanceMetres)
    }

    @Test
    fun `reports no factory when there is no assignment`() {
        val result = GeofenceEvaluator.evaluate(null, MUMBAI_LAT, MUMBAI_LNG, 5.0)

        assertEquals(GeofenceEvaluator.Status.NO_FACTORY, result.status)
        assertFalse(result.allowed)
    }

    @Test
    fun `waits for a fix before deciding`() {
        val result = GeofenceEvaluator.evaluate(factory(), null, null, null)

        assertEquals(GeofenceEvaluator.Status.LOCATING, result.status)
        assertFalse(result.allowed)
        assertEquals(null, result.distanceMetres)
    }

    @Test
    fun `falls back to the backend defaults when the factory omits them`() {
        val result = GeofenceEvaluator.evaluate(
            factory(radius = null, accuracyThreshold = null),
            MUMBAI_LAT,
            MUMBAI_LNG,
            5.0,
        )

        assertEquals(GeofenceEvaluator.DEFAULT_RADIUS_METRES, result.radiusMetres)
        assertEquals(GeofenceEvaluator.DEFAULT_ACCURACY_THRESHOLD_METRES, result.accuracyThresholdMetres)
    }

    @Test
    fun `treats a zero radius as absent because PHP does`() {
        // PHP's `?:` falls through on 0, so a misconfigured 0 must not lock every punch out.
        val result = GeofenceEvaluator.evaluate(
            factory(radius = 0, accuracyThreshold = 0),
            MUMBAI_LAT,
            MUMBAI_LNG,
            5.0,
        )

        assertEquals(GeofenceEvaluator.DEFAULT_RADIUS_METRES, result.radiusMetres)
        assertEquals(GeofenceEvaluator.DEFAULT_ACCURACY_THRESHOLD_METRES, result.accuracyThresholdMetres)
        assertEquals(GeofenceEvaluator.Status.INSIDE, result.status)
    }

    @Test
    fun `falls back to a generic factory name when it is missing`() {
        val result = GeofenceEvaluator.evaluate(
            factory(name = null),
            19.0895,
            MUMBAI_LNG,
            10.0,
        )

        assertTrue(result.message.contains("the factory"))
    }

    private companion object {
        const val MUMBAI_LAT = 19.0760
        const val MUMBAI_LNG = 72.8777
    }
}
