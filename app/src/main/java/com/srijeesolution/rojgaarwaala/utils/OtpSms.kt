package com.srijeesolution.rojgaarwaala.utils

/** Reads the login code out of an OTP SMS, e.g. "Thanks for registration Your OTP is 482913". */
object OtpSms {

    const val LENGTH = 6

    /**
     * The first run of exactly [LENGTH] digits, or null. Longer numbers (a phone number
     * in the text) are skipped rather than cut down to six digits.
     */
    fun extract(message: String?): String? {
        if (message.isNullOrBlank()) return null
        return Regex("(?<!\\d)\\d{$LENGTH}(?!\\d)").find(message)?.value
    }
}
