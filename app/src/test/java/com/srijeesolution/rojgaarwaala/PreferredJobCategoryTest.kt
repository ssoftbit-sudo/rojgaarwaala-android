package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.PreferredJobCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferredJobCategoryTest {

    @Test
    fun `same category matches ignoring case and spaces`() {
        assertTrue(PreferredJobCategory.matches("Hospital", "hospital"))
        assertTrue(PreferredJobCategory.matches("  Driver ", "Driver"))
    }

    @Test
    fun `other category or blank preferred does not match`() {
        assertFalse(PreferredJobCategory.matches("Hospital", "Driver"))
        assertFalse(PreferredJobCategory.matches("", "Hospital"))
        assertFalse(PreferredJobCategory.matches(null, "Hospital"))
    }
}
