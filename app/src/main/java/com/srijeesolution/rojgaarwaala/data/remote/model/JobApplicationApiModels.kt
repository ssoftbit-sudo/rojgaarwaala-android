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
    @SerializedName("requires_payment") val requiresPayment: Boolean? = true,
    @SerializedName("razorpay_key_id") val razorpayKeyId: String? = null,
    @SerializedName("amount_paise") val amountPaise: Int? = null,
    @SerializedName("order_id") val orderId: String? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("application_id") val applicationId: Int? = null,
)

data class JobApplicationDto(
    @SerializedName("id") val id: Int? = null,
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

data class ConfirmPaymentRequest(
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerializedName("razorpay_order_id") val razorpayOrderId: String? = null,
)
