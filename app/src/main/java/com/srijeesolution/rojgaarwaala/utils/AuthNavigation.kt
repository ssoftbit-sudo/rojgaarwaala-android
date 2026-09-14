package com.srijeesolution.rojgaarwaala.utils

/**
 * After OTP the employee always lands on profile first, whether the form was
 * filled on a previous login or not.
 */
object AuthNavigation {
    const val EXTRA_FROM_OTP = "extra_from_otp"

    fun opensProfileAfterOtp(): Boolean = true
}
