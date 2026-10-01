package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.data.remote.model.UserData
import com.srijeesolution.rojgaarwaala.utils.ProfileGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileGateTest {

    @Test
    fun `otp user with an unfinished profile must stay on profile`() {
        assertTrue(
            ProfileGate.needsCompletion(
                UserData(candidateProfileComplete = false, needsProfileCompletion = true),
            ),
        )
    }

    @Test
    fun `finished profile can open home`() {
        assertFalse(
            ProfileGate.needsCompletion(
                UserData(candidateProfileComplete = true, needsProfileCompletion = false),
            ),
        )
    }

    @Test
    fun `missing flags are treated as unfinished so skip cannot sneak in`() {
        assertTrue(ProfileGate.needsCompletion(null))
        assertTrue(ProfileGate.needsCompletion(UserData()))
    }
}
