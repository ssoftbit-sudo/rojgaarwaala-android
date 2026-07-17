package com.srijeesolution.rojgaarwaala.data.remote.model

import com.google.gson.annotations.SerializedName

data class HelpDeskFaqsResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: HelpDeskFaqsData? = null,
)

data class HelpDeskFaqsData(
    @SerializedName("categories") val categories: List<HelpDeskFaqCategory>? = emptyList(),
    @SerializedName("total") val total: Int? = 0,
)

data class HelpDeskFaqCategory(
    @SerializedName("category") val category: String? = null,
    @SerializedName("faqs") val faqs: List<HelpDeskFaqItem>? = emptyList(),
)

data class HelpDeskFaqItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("question") val question: String? = null,
    @SerializedName("answer") val answer: String? = null,
)

data class EnquirySubmitResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
)
