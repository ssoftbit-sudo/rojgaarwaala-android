package com.srijeesolution.rojgaarwaala

import android.os.Build
import com.srijeesolution.rojgaarwaala.utils.LocationPermissionPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionPolicyTest {

    @Test
    fun `while using the app is enough foreground permission`() {
        assertTrue(LocationPermissionPolicy.hasForegroundLocation(fineGranted = true, coarseGranted = false))
        assertTrue(LocationPermissionPolicy.hasForegroundLocation(fineGranted = false, coarseGranted = true))
        assertFalse(LocationPermissionPolicy.hasForegroundLocation(fineGranted = false, coarseGranted = false))
    }

    @Test
    fun `never auto-prompts for allow all the time during punch`() {
        assertFalse(
            LocationPermissionPolicy.shouldPromptBackgroundLocation(
                sdkInt = Build.VERSION_CODES.Q,
                foregroundGranted = true,
                backgroundGranted = false,
                punchInProgress = true,
            ),
        )
    }

    @Test
    fun `never auto-prompts for allow all the time on the punch screen`() {
        assertFalse(
            LocationPermissionPolicy.shouldPromptBackgroundLocation(
                sdkInt = Build.VERSION_CODES.R,
                foregroundGranted = true,
                backgroundGranted = false,
                punchInProgress = false,
            ),
        )
        assertFalse(
            LocationPermissionPolicy.shouldPromptBackgroundLocation(
                sdkInt = Build.VERSION_CODES.Q,
                foregroundGranted = false,
                backgroundGranted = false,
                punchInProgress = false,
            ),
        )
    }

    @Test
    fun `arrival geofence stays optional when the user picked while using the app`() {
        assertFalse(
            LocationPermissionPolicy.canRegisterArrivalGeofence(
                sdkInt = Build.VERSION_CODES.Q,
                foregroundGranted = true,
                backgroundGranted = false,
            ),
        )
        assertTrue(
            LocationPermissionPolicy.canRegisterArrivalGeofence(
                sdkInt = Build.VERSION_CODES.Q,
                foregroundGranted = true,
                backgroundGranted = true,
            ),
        )
        assertTrue(
            LocationPermissionPolicy.canRegisterArrivalGeofence(
                sdkInt = Build.VERSION_CODES.P,
                foregroundGranted = true,
                backgroundGranted = false,
            ),
        )
    }
}
