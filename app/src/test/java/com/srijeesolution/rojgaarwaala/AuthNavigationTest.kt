package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.AuthNavigation
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthNavigationTest {

    @Test
    fun `otp success always opens profile, even if it was filled before`() {
        assertTrue(AuthNavigation.opensProfileAfterOtp())
    }
}
