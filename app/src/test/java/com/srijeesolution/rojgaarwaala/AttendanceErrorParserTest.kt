package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorMapper
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The punch endpoints report why a punch failed inside `data.error_code`, and the 401 case
 * arrives with no envelope at all. These tests cover the raw-body parsing that turns those
 * responses into the message shown on the dashboard.
 */
class AttendanceErrorParserTest {

    private fun error(statusCode: Int, body: String) =
        ApiError(statusCode = statusCode, errorMsg = "error", errorBody = body)

    @Test
    fun `a 401 is reported as an expired session without reading the body`() {
        val result = AttendanceErrorParser.parse(error(401, "anything at all"))

        assertTrue(result.isUnauthenticated)
        assertNull(result.errorCode)
        assertEquals(AttendanceErrorMapper.UNAUTHENTICATED_MESSAGE, result.message)
    }

    @Test
    fun `a null error degrades to the generic message`() {
        val result = AttendanceErrorParser.parse(null)

        assertEquals(AttendanceErrorMapper.GENERIC_MESSAGE, result.message)
        assertNull(result.errorCode)
        assertFalse(result.isUnauthenticated)
    }

    @Test
    fun `an empty body degrades to the generic message`() {
        val result = AttendanceErrorParser.parse(error(500, ""))

        assertEquals(AttendanceErrorMapper.GENERIC_MESSAGE, result.message)
        assertNull(result.errorCode)
    }

    @Test
    fun `a blank body degrades to the generic message`() {
        val result = AttendanceErrorParser.parse(error(500, "    "))

        assertEquals(AttendanceErrorMapper.GENERIC_MESSAGE, result.message)
    }

    @Test
    fun `malformed json degrades to the generic message`() {
        val result = AttendanceErrorParser.parse(error(422, "<html>gateway error</html>"))

        assertEquals(AttendanceErrorMapper.GENERIC_MESSAGE, result.message)
        assertNull(result.errorCode)
        assertFalse(result.isUnauthenticated)
    }

    @Test
    fun `the geofence error code and its server copy both survive parsing`() {
        val result = AttendanceErrorParser.parse(
            error(
                422,
                """{"status": false, "message": "You are 480 m away from ABC Steel.",
                   "data": {"error_code": "outside_geofence", "distance": 480}}"""
            )
        )

        assertEquals(AttendanceErrorMapper.OUTSIDE_GEOFENCE, result.errorCode)
        assertEquals("You are 480 m away from ABC Steel.", result.message)
        assertFalse(result.isUnauthenticated)
    }

    @Test
    fun `poor accuracy uses the fixed guidance copy over the server message`() {
        val result = AttendanceErrorParser.parse(
            error(
                422,
                """{"message": "GPS accuracy 90m exceeds 50m", "data": {"error_code": "poor_accuracy"}}"""
            )
        )

        assertEquals(AttendanceErrorMapper.POOR_ACCURACY, result.errorCode)
        assertTrue(result.message.contains("open area"))
    }

    @Test
    fun `an already punched in response is recognised`() {
        val result = AttendanceErrorParser.parse(
            error(409, """{"message": "Already marked", "data": {"error_code": "already_punched_in"}}""")
        )

        assertEquals(AttendanceErrorMapper.ALREADY_PUNCHED_IN, result.errorCode)
        assertTrue(AttendanceErrorMapper.shouldRefreshDashboard(result.errorCode))
    }

    @Test
    fun `a not an employee response disables the punch ui`() {
        val result = AttendanceErrorParser.parse(
            error(403, """{"message": "Not an employee", "data": {"error_code": "not_an_employee"}}""")
        )

        assertEquals(AttendanceErrorMapper.NOT_AN_EMPLOYEE, result.errorCode)
        assertTrue(AttendanceErrorMapper.disablesPunchUi(result.errorCode))
    }

    @Test
    fun `a body with no error code falls back to the server message`() {
        val result = AttendanceErrorParser.parse(
            error(422, """{"status": false, "message": "The latitude field is required."}""")
        )

        assertNull(result.errorCode)
        assertEquals("The latitude field is required.", result.message)
    }

    @Test
    fun `an empty error code string is treated as absent`() {
        val result = AttendanceErrorParser.parse(
            error(422, """{"message": "Something failed", "data": {"error_code": ""}}""")
        )

        assertNull(result.errorCode)
        assertEquals("Something failed", result.message)
    }

    @Test
    fun `a 200-shaped body with unauthenticated message is flagged`() {
        val result = AttendanceErrorParser.parse(
            error(500, """{"message": "Unauthenticated."}""")
        )

        assertTrue(result.isUnauthenticated)
    }

    @Test
    fun `a null data object does not crash the parser`() {
        val result = AttendanceErrorParser.parse(
            error(404, """{"status": false, "message": "Not found", "data": null}""")
        )

        assertNull(result.errorCode)
        assertEquals("Not found", result.message)
    }
}
