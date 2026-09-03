package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.JobApplicationDto

/**
 * Shared copy and matching for job-application payment state.
 *
 * Pure Kotlin so JVM unit tests can cover the labels used on Apply, Job Status,
 * and Application Status.
 */
object ApplicationPaymentCopy {

  const val PAYMENT_PAID = "paid"
  const val PAYMENT_PENDING = "pending"
  const val PAYMENT_FAILED = "failed"
  const val STATUS_PENDING_PAYMENT = "pending_payment"

  fun isPaid(paymentStatus: String?): Boolean =
    paymentStatus.equals(PAYMENT_PAID, ignoreCase = true)

  fun isFeePaid(paymentStatus: String?, amountPaise: Int?): Boolean {
    return isPaid(paymentStatus) && (amountPaise ?: 0) > 0
  }

  fun needsPayment(paymentStatus: String?, amountPaise: Int?): Boolean {
    val amount = amountPaise ?: 0
    return amount > 0 && !isPaid(paymentStatus)
  }

  fun matchesListing(
    application: JobApplicationDto,
    videoId: Int?,
    scheduledImageId: Int?,
  ): Boolean {
    val video = videoId ?: 0
    val image = scheduledImageId ?: 0
    return (video > 0 && application.videoId == video) ||
      (image > 0 && application.scheduledImageId == image)
  }

  fun rupeesLabel(amountPaise: Int?): String {
    val rupees = (amountPaise ?: 0) / 100
    return "₹$rupees"
  }

  fun listBadge(paymentStatus: String?, amountPaise: Int?): String? {
    return when {
      isPaid(paymentStatus) && (amountPaise ?: 0) > 0 -> "Paid ${rupeesLabel(amountPaise)}"
      isPaid(paymentStatus) -> "Paid"
      paymentStatus.equals(PAYMENT_PENDING, ignoreCase = true) -> "Payment pending"
      paymentStatus.equals(PAYMENT_FAILED, ignoreCase = true) -> "Payment failed"
      else -> null
    }
  }

  fun alreadyAppliedTitle(): String = "You have already applied for this job."

  fun applyPaymentDetail(paymentStatus: String?, amountPaise: Int?): String {
    val amount = rupeesLabel(amountPaise)
    return when {
      isPaid(paymentStatus) -> "Payment: Paid $amount"
      paymentStatus.equals(PAYMENT_FAILED, ignoreCase = true) ->
        "Payment failed. Pay $amount to complete your application."
      else -> "Payment pending. Pay $amount to complete your application."
    }
  }

  fun applicationStatusCopy(status: String?): String {
    return when (status.orEmpty().lowercase()) {
      "applied" -> "Congratulations! Your application has been submitted successfully."
      "under_review" -> "Your application is under review."
      "interview_scheduled" -> "Interview scheduled. HR will contact you."
      "selected" -> "Congratulations! You are selected."
      "rejected" -> "Application not selected this time."
      STATUS_PENDING_PAYMENT -> "Payment is pending. Complete payment to submit your application."
      "failed" -> "There was an issue with your application. Please contact support."
      else -> "Status: ${status.orEmpty().replace('_', ' ')}"
    }
  }

  fun statusPaymentLine(paymentStatus: String?, amountPaise: Int?): String {
    val amount = if ((amountPaise ?: 0) > 0) " (${rupeesLabel(amountPaise)})" else ""
    return when {
      isPaid(paymentStatus) -> "Payment: Paid$amount"
      paymentStatus.equals(PAYMENT_FAILED, ignoreCase = true) -> "Payment: Failed$amount"
      paymentStatus.equals(PAYMENT_PENDING, ignoreCase = true) -> "Payment: Pending$amount"
      paymentStatus.isNullOrBlank() -> ""
      else -> "Payment: ${paymentStatus.replaceFirstChar { it.uppercase() }}$amount"
    }
  }
}
