package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.MapAddressMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class MapAddressMapperTest {

    @Test
    fun `uses locality as city and admin area as state`() {
        val parts = MapAddressMapper.fromParts(
            addressLine = "Shankar Nagar, Raipur, Chhattisgarh 492001, India",
            locality = "Raipur",
            subAdminArea = "Raipur District",
            adminArea = "Chhattisgarh",
            subLocality = "Shankar Nagar",
            thoroughfare = "GE Road",
            featureName = "12",
            postalCode = "492001",
        )
        assertEquals("Shankar Nagar, Raipur, Chhattisgarh 492001, India", parts.address)
        assertEquals("Raipur", parts.city)
        assertEquals("Chhattisgarh", parts.state)
        assertEquals("Shankar Nagar", parts.colony)
        assertEquals("492001", parts.pincode)
    }

    @Test
    fun `falls back to subAdminArea when locality is missing`() {
        val parts = MapAddressMapper.fromParts(
            addressLine = null,
            locality = null,
            subAdminArea = "Durg",
            adminArea = "Chhattisgarh",
            subLocality = null,
            thoroughfare = "Patel Chowk",
            featureName = "House 4",
            postalCode = "490001",
        )
        assertEquals("Durg", parts.city)
        assertEquals("Patel Chowk", parts.colony)
        assertEquals("Patel Chowk, Durg, Chhattisgarh, 490001", parts.address)
    }

    @Test
    fun `skips numeric feature names so house numbers do not become colony`() {
        val parts = MapAddressMapper.fromParts(
            addressLine = "Raipur",
            locality = "Raipur",
            subAdminArea = null,
            adminArea = "Chhattisgarh",
            subLocality = null,
            thoroughfare = null,
            featureName = "21",
            postalCode = "492001",
        )
        assertEquals("", parts.colony)
        assertEquals("492001", parts.pincode)
    }
}
