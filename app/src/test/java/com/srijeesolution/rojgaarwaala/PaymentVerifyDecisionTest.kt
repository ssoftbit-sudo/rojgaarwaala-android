package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.PaymentVerifyDecision
import com.srijeesolution.rojgaarwaala.utils.PaymentVerifyDecision.Action
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentVerifyDecisionTest {

  @Test
  fun `paid inquiry finishes immediately`() {
    assertEquals(
      Action.PAID,
      PaymentVerifyDecision.next(
        paid = true,
        pending = false,
        reachedServer = true,
        attempt = 1,
        maxAttempts = 10,
      ),
    )
  }

  @Test
  fun `close after gpay paid retries while inquiry is catching up`() {
    assertEquals(
      Action.RETRY,
      PaymentVerifyDecision.next(
        paid = false,
        pending = true,
        reachedServer = true,
        attempt = 1,
        maxAttempts = 10,
      ),
    )
  }

  @Test
  fun `missing pending is treated as confirming not as a failed charge`() {
    assertEquals(
      Action.RETRY,
      PaymentVerifyDecision.next(
        paid = false,
        pending = null,
        reachedServer = true,
        attempt = 1,
        maxAttempts = 10,
      ),
    )
    assertEquals(
      Action.CONFIRMING,
      PaymentVerifyDecision.next(
        paid = false,
        pending = null,
        reachedServer = true,
        attempt = 10,
        maxAttempts = 10,
      ),
    )
  }

  @Test
  fun `hard decline fails fast so the customer can try another method`() {
    assertEquals(
      Action.NOT_PAID,
      PaymentVerifyDecision.next(
        paid = false,
        pending = false,
        reachedServer = true,
        attempt = 1,
        maxAttempts = 10,
      ),
    )
  }

  @Test
  fun `unreachable inquiry never tells the customer they were not charged`() {
    assertEquals(
      Action.RETRY,
      PaymentVerifyDecision.next(
        paid = null,
        pending = null,
        reachedServer = false,
        attempt = 3,
        maxAttempts = 10,
      ),
    )
    assertEquals(
      Action.CONFIRMING,
      PaymentVerifyDecision.next(
        paid = null,
        pending = null,
        reachedServer = false,
        attempt = 10,
        maxAttempts = 10,
      ),
    )
  }
}
