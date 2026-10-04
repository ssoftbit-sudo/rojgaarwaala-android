package com.srijeesolution.rojgaarwaala.utils

/**
 * Punch GPS only needs "while using the app". The app never asks for "Allow all the
 * time": punch-in reminders are time-based and location is read only while the
 * attendance screen is open.
 */
object LocationPermissionPolicy {

    fun hasForegroundLocation(fineGranted: Boolean, coarseGranted: Boolean): Boolean =
        fineGranted || coarseGranted
}
