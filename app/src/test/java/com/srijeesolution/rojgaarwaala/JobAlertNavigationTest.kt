package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.JobAlertNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JobAlertNavigationTest {

    @Test
    fun `paper cut push opens the free job tab`() {
        assertTrue(JobAlertNavigation.isJobAlert("paper_cut_job"))
        assertEquals(3, JobAlertNavigation.tabForType("paper_cut_job"))
    }

    @Test
    fun `other notification types stay off the free job tab`() {
        assertFalse(JobAlertNavigation.isJobAlert("job_application_status"))
        assertFalse(JobAlertNavigation.isJobAlert("home"))
        assertNull(JobAlertNavigation.tabForType("vlp"))
        assertNull(JobAlertNavigation.tabForType(null))
    }

    @Test
    fun `scheduled image id is used when data id is missing`() {
        assertEquals("42", JobAlertNavigation.resolveImageId(null, "42"))
        assertEquals("42", JobAlertNavigation.resolveImageId("", "42"))
        assertEquals("9", JobAlertNavigation.resolveImageId("9", null))
    }
}
