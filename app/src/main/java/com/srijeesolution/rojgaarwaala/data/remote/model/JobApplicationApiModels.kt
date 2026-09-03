package com.srijeesolution.rojgaarwaala.data.remote.model

import com.google.gson.annotations.SerializedName

data class JobApplicationApiResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: JobApplicationApiData? = null,
)

data class JobApplicationApiData(
    @SerializedName("application") val application: JobApplicationDto? = null,
    @SerializedName("applications") val applications: List<JobApplicationDto>? = null,
    // Defaults to free: an app that has not heard otherwise should never send a
    // user to a payment screen.
    @SerializedName("requires_payment") val requiresPayment: Boolean? = false,
    @SerializedName("amount_paise") val amountPaise: Int? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("application_id") val applicationId: Int? = null,
    // Hosted payment page details returned by the order endpoint.
    @SerializedName("order_id") val orderId: String? = null,
    @SerializedName("transaction_id") val transactionId: String? = null,
    @SerializedName("payment_link") val paymentLink: String? = null,
    @SerializedName("already_applied") val alreadyApplied: Boolean? = false,
    @SerializedName("already_paid") val alreadyPaid: Boolean? = false,
    // Verify endpoint result.
    @SerializedName("paid") val paid: Boolean? = false,
    @SerializedName("status") val paymentState: String? = null,
    @SerializedName("reason") val reason: String? = null,
)

data class JobApplicationDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("video_id") val videoId: Int? = null,
    @SerializedName("scheduled_image_id") val scheduledImageId: Int? = null,
    @SerializedName("job_title") val jobTitle: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("amount_paise") val amountPaise: Int? = null,
    @SerializedName("applied_at") val appliedAt: String? = null,
    @SerializedName("paid_at") val paidAt: String? = null,
    @SerializedName("resume_url") val resumeUrl: String? = null,
    @SerializedName("timeline") val timeline: List<JobApplicationTimelineEntry>? = null,
)

data class JobApplicationTimelineEntry(
    @SerializedName("status") val status: String? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("at") val at: String? = null,
)

data class VerifyPaymentRequest(
    @SerializedName("order_id") val orderId: String? = null,
)
