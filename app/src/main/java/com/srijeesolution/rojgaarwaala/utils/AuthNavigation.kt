package com.srijeesolution.rojgaarwaala.utils

/**
 * After OTP, users with an unfinished candidate profile must stay on Profile
 * until it is saved. Completed profiles go straight to Home.
 */
object AuthNavigation {
    const val EXTRA_FROM_OTP = "extra_from_otp"
}
