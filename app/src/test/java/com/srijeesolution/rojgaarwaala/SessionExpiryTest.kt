package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.SessionExpiry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExpiryTest {

    @Test
    fun `a rejected token ends the session`() {
        assertTrue(SessionExpiry.shouldLogout(401, sentAuthHeader = true, path = "/api/employee/dashboard"))
        assertTrue(SessionExpiry.shouldLogout(401, sentAuthHeader = true, path = "/api/profile/update/"))
    }

    @Test
    fun `a 401 on a request that carried no token proves nothing`() {
        assertFalse(SessionExpiry.shouldLogout(401, sentAuthHeader = false, path = "/api/home"))
    }

    @Test
    fun `a wrong OTP is not an expired session`() {
        assertFalse(SessionExpiry.shouldLogout(401, sentAuthHeader = true, path = "/api/verify-otp"))
        assertFalse(SessionExpiry.shouldLogout(401, sentAuthHeader = true, path = "/api/login"))
        assertFalse(SessionExpiry.shouldLogout(401, sentAuthHeader = true, path = "/api/logout"))
    }

    @Test
    fun `other failures leave the session alone`() {
        assertFalse(SessionExpiry.shouldLogout(403, sentAuthHeader = true, path = "/api/employee/dashboard"))
        assertFalse(SessionExpiry.shouldLogout(422, sentAuthHeader = true, path = "/api/employee/attendance/punch-in"))
        assertFalse(SessionExpiry.shouldLogout(500, sentAuthHeader = true, path = "/api/home"))
        assertFalse(SessionExpiry.shouldLogout(200, sentAuthHeader = true, path = "/api/home"))
    }
}
