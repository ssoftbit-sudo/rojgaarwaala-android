package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceErrorMapperTest {

    @Test
    fun `not an employee falls back to server message when present`() {
        assertEquals(
            "You are not linked to any employer.",
            AttendanceErrorMapper.message(
                AttendanceErrorMapper.NOT_AN_EMPLOYEE,
                "You are not linked to any employer.",
            ),
        )
    }

    @Test
    fun `not an employee has its own copy when server sends nothing`() {
        assertEquals(
            "You are not registered as an employee.",
            AttendanceErrorMapper.message(AttendanceErrorMapper.NOT_AN_EMPLOYEE, null),
        )
    }

    @Test
    fun `employee inactive uses server message`() {
        assertEquals(
            "Your account was deactivated on 01 Aug.",
            AttendanceErrorMapper.message(
                AttendanceErrorMapper.EMPLOYEE_INACTIVE,
                "Your account was deactivated on 01 Aug.",
            ),
        )
    }

    @Test
    fun `no active assignment always uses the fixed copy`() {
        assertEquals(
            "You are not assigned to any factory today",
            AttendanceErrorMapper.message(AttendanceErrorMapper.NO_ACTIVE_ASSIGNMENT, null),
        )
        assertEquals(
            "You are not assigned to any factory today",
            AttendanceErrorMapper.message(
                AttendanceErrorMapper.NO_ACTIVE_ASSIGNMENT,
                "No assignment found for 2025-08-14",
            ),
        )
    }

    @Test
    fun `already punched in always uses the fixed copy`() {
        assertEquals(
            "Attendance already marked today",
            AttendanceErrorMapper.message(
                AttendanceErrorMapper.ALREADY_PUNCHED_IN,
                "Punch in already recorded at 09:12 AM",
            ),
        )
    }

    @Test
    fun `not punched in prefers the server message`() {
        assertEquals(
            "Punch in first.",
            AttendanceErrorMapper.message(AttendanceErrorMapper.NOT_PUNCHED_IN, "Punch in first."),
        )
        assertEquals(
            "You have not punched in yet today.",
            AttendanceErrorMapper.message(AttendanceErrorMapper.NOT_PUNCHED_IN, null),
        )
    }

    @Test
    fun `already punched out prefers the server message`() {
        assertEquals(
            "Punched out at 06:30 PM",
            AttendanceErrorMapper.message(
                AttendanceErrorMapper.ALREADY_PUNCHED_OUT,
                "Punched out at 06:30 PM",
            ),
        )
        assertEquals(
            "You have already punched out today.",
            AttendanceErrorMapper.message(AttendanceErrorMapper.ALREADY_PUNCHED_OUT, null),
        )
    }

    @Test
    fun `outside geofence shows the server message verbatim`() {
        val serverMessage = "You are 420 m away from Unit 2. Allowed radius is 150 m."
        assertEquals(
            serverMessage,
            AttendanceErrorMapper.message(AttendanceErrorMapper.OUTSIDE_GEOFENCE, serverMessage),
        )
    }

    @Test
    fun `outside geofence has a fallback when the server sends no message`() {
        assertEquals(
            "You are outside the allowed factory area",
            AttendanceErrorMapper.message(AttendanceErrorMapper.OUTSIDE_GEOFENCE, null),
        )
    }

    @Test
    fun `poor accuracy ignores the server message and gives guidance`() {
        assertEquals(
            "Unable to get accurate location. Move to an open area away from buildings and try again.",
            AttendanceErrorMapper.message(
                AttendanceErrorMapper.POOR_ACCURACY,
                "Accuracy 84 m exceeds threshold 50 m",
            ),
        )
    }

    @Test
    fun `factory location missing prefers the server message`() {
        assertEquals(
            "Unit 2 has no coordinates.",
            AttendanceErrorMapper.message(
                AttendanceErrorMapper.FACTORY_LOCATION_MISSING,
                "Unit 2 has no coordinates.",
            ),
        )
        assertEquals(
            "Factory location is not set. Please contact your supervisor.",
            AttendanceErrorMapper.message(AttendanceErrorMapper.FACTORY_LOCATION_MISSING, null),
        )
    }

    @Test
    fun `unknown code falls back to the server message`() {
        assertEquals(
            "Server exploded",
            AttendanceErrorMapper.message("something_new_from_backend", "Server exploded"),
        )
    }

    @Test
    fun `unknown code with no server message uses the generic copy`() {
        assertEquals(
            AttendanceErrorMapper.GENERIC_MESSAGE,
            AttendanceErrorMapper.message("something_new_from_backend", null),
        )
    }

    @Test
    fun `null code uses the generic copy`() {
        assertEquals(AttendanceErrorMapper.GENERIC_MESSAGE, AttendanceErrorMapper.message(null, null))
    }

    @Test
    fun `blank server message is treated as absent`() {
        assertEquals(AttendanceErrorMapper.GENERIC_MESSAGE, AttendanceErrorMapper.message(null, "   "))
    }

    @Test
    fun `only employee level codes disable the punch buttons`() {
        assertTrue(AttendanceErrorMapper.disablesPunchUi(AttendanceErrorMapper.NOT_AN_EMPLOYEE))
        assertTrue(AttendanceErrorMapper.disablesPunchUi(AttendanceErrorMapper.EMPLOYEE_INACTIVE))
        assertFalse(AttendanceErrorMapper.disablesPunchUi(AttendanceErrorMapper.OUTSIDE_GEOFENCE))
        assertFalse(AttendanceErrorMapper.disablesPunchUi(AttendanceErrorMapper.POOR_ACCURACY))
        assertFalse(AttendanceErrorMapper.disablesPunchUi(null))
    }

    @Test
    fun `state conflicts trigger a dashboard refresh`() {
        assertTrue(AttendanceErrorMapper.shouldRefreshDashboard(AttendanceErrorMapper.ALREADY_PUNCHED_IN))
        assertTrue(AttendanceErrorMapper.shouldRefreshDashboard(AttendanceErrorMapper.NOT_PUNCHED_IN))
        assertTrue(AttendanceErrorMapper.shouldRefreshDashboard(AttendanceErrorMapper.ALREADY_PUNCHED_OUT))
        assertFalse(AttendanceErrorMapper.shouldRefreshDashboard(AttendanceErrorMapper.OUTSIDE_GEOFENCE))
        assertFalse(AttendanceErrorMapper.shouldRefreshDashboard(null))
    }
}
