package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.OtpSms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpSmsTest {
    @Test fun readsTheSixDigitCode() {
        assertEquals("482913", OtpSms.extract("Thanks for registration Your OTP is 482913"))
        // The DLT-registered text, misspelling and all, exactly as it arrives today.
        assertEquals("123456", OtpSms.extract("Thanks for registeration Your OTP is 123456"))
        assertEquals("004211", OtpSms.extract("OTP: 004211. Valid 5 min"))
    }

    @Test fun skipsPhoneNumbersAndShortNumbers() {
        assertEquals("771204", OtpSms.extract("Call 9340738151 or use 771204"))
        assertNull(OtpSms.extract("Your code is 1234"))
        assertNull(OtpSms.extract(null))
    }
}
