package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.JobTitleCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JobTitleCopyTest {

  @Test
  fun `placeholder titles are not sent to the server`() {
    assertTrue(JobTitleCopy.isPlaceholder("Job Opportunity"))
    assertTrue(JobTitleCopy.isPlaceholder("  "))
    assertNull(JobTitleCopy.forRequest("Job Opportunity"))
    assertEquals("Warehouse Helper", JobTitleCopy.forRequest("Warehouse Helper"))
  }

  @Test
  fun `status and list screens hide the placeholder`() {
    assertEquals("Job Application", JobTitleCopy.display("Job Opportunity"))
    assertEquals("Warehouse Helper", JobTitleCopy.display("Warehouse Helper"))
    assertFalse(JobTitleCopy.isPlaceholder("Warehouse Helper"))
  }
}
