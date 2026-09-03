package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.data.remote.model.JobApplicationDto
import com.srijeesolution.rojgaarwaala.utils.ApplicationPaymentCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicationPaymentCopyTest {

  @Test
  fun `paid application with fee shows Paid rupees badge`() {
    assertEquals("Paid ₹100", ApplicationPaymentCopy.listBadge("paid", 10000))
    assertTrue(ApplicationPaymentCopy.isPaid("paid"))
    assertFalse(ApplicationPaymentCopy.needsPayment("paid", 10000))
    assertTrue(ApplicationPaymentCopy.isFeePaid("paid", 10000))
    assertFalse(ApplicationPaymentCopy.isFeePaid("paid", 0))
  }

  @Test
  fun `pending payment shows pending badge and needs pay`() {
    assertEquals("Payment pending", ApplicationPaymentCopy.listBadge("pending", 10000))
    assertTrue(ApplicationPaymentCopy.needsPayment("pending", 10000))
  }

  @Test
  fun `free apply has no payment badge`() {
    assertNull(ApplicationPaymentCopy.listBadge(null, 0))
    assertFalse(ApplicationPaymentCopy.needsPayment("paid", 0))
  }

  @Test
  fun `matches listing by video or image id`() {
    val videoApp = JobApplicationDto(id = 1, videoId = 181)
    val imageApp = JobApplicationDto(id = 2, scheduledImageId = 9)

    assertTrue(ApplicationPaymentCopy.matchesListing(videoApp, videoId = 181, scheduledImageId = 0))
    assertFalse(ApplicationPaymentCopy.matchesListing(videoApp, videoId = 182, scheduledImageId = 0))
    assertTrue(ApplicationPaymentCopy.matchesListing(imageApp, videoId = 0, scheduledImageId = 9))
  }

  @Test
  fun `pending payment status copy is not a success message`() {
    assertEquals(
      "Payment is pending. Complete payment to submit your application.",
      ApplicationPaymentCopy.applicationStatusCopy("pending_payment"),
    )
    assertEquals("Payment: Paid (₹100)", ApplicationPaymentCopy.statusPaymentLine("paid", 10000))
    assertEquals("Payment: Pending (₹100)", ApplicationPaymentCopy.statusPaymentLine("pending", 10000))
  }

  @Test
  fun `apply form paid detail`() {
    assertEquals(
      "Payment: Paid ₹100",
      ApplicationPaymentCopy.applyPaymentDetail("paid", 10000),
    )
  }
}
