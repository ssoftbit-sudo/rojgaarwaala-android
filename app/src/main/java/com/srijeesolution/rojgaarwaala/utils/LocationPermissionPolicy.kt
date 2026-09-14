package com.srijeesolution.rojgaarwaala.utils

/**
 * Punch GPS only needs "while using the app". "Allow all the time" is optional for
 * factory-arrival alerts and must never block the attendance screen.
 */
object LocationPermissionPolicy {

    fun hasForegroundLocation(fineGranted: Boolean, coarseGranted: Boolean): Boolean =
        fineGranted || coarseGranted

    /**
     * Auto-prompting for background location races the foreground dialog on Samsung
     * (Allow all the time vs Allow only while using the app). Punch then never receives
     * its permission callback, so the screen does not advance.
     */
    fun shouldPromptBackgroundLocation(
        sdkInt: Int,
        foregroundGranted: Boolean,
        backgroundGranted: Boolean,
        punchInProgress: Boolean,
    ): Boolean {
        if (punchInProgress) return false
        if (sdkInt < 29) return false
        if (!foregroundGranted || backgroundGranted) return false
        return false
    }

    fun canRegisterArrivalGeofence(
        sdkInt: Int,
        foregroundGranted: Boolean,
        backgroundGranted: Boolean,
    ): Boolean {
        if (!foregroundGranted) return false
        return if (sdkInt >= 29) backgroundGranted else true
    }
}
