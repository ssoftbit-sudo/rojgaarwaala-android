package com.srijeesolution.rojgaarwaala.utils

/**
 * Decides what the payment screen should do after a verify call.
 *
 * The redirect / GPay receipt is never trusted. Only the server Inquiry result
 * is. A missing [pending] flag is treated as still confirming so Close on a
 * UPI receipt cannot be shown as "you were not charged."
 */
object PaymentVerifyDecision {

  enum class Action {
    PAID,
    RETRY,
    CONFIRMING,
    NOT_PAID,
  }

  fun next(
    paid: Boolean?,
    pending: Boolean?,
    reachedServer: Boolean,
    attempt: Int,
    maxAttempts: Int,
  ): Action {
    if (paid == true) {
      return Action.PAID
    }

    val hardDecline = reachedServer && pending == false
    if (!hardDecline && attempt < maxAttempts) {
      return Action.RETRY
    }

    return if (!reachedServer || pending != false) {
      Action.CONFIRMING
    } else {
      Action.NOT_PAID
    }
  }
}
