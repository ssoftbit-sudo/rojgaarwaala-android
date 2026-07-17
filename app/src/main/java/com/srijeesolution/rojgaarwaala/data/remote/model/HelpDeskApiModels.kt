package com.srijeesolution.rojgaarwaala.data.remote.model

import com.google.gson.annotations.SerializedName

data class HelpDeskConfigResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: HelpDeskConfigData? = null,
)

data class HelpDeskConfigData(
    @SerializedName("support") val support: HelpDeskSupport? = null,
    @SerializedName("issue_categories") val issueCategories: List<HelpDeskIssueCategory>? = emptyList(),
)

data class HelpDeskSupport(
    @SerializedName("call_phone") val callPhone: String? = null,
    @SerializedName("whatsapp_phone") val whatsappPhone: String? = null,
)

data class HelpDeskIssueCategory(
    @SerializedName("key") val key: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("issue_type") val issueType: String? = null,
    @SerializedName("content_type") val contentType: String? = null,
)

data class HelpDeskFaqsResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: HelpDeskFaqsData? = null,
)

data class HelpDeskFaqsData(
    @SerializedName("categories") val categories: List<HelpDeskFaqCategory>? = emptyList(),
    @SerializedName("suggestions") val suggestions: List<HelpDeskFaqSuggestion>? = emptyList(),
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
    @SerializedName("audio_url") val audioUrl: String? = null,
)

data class HelpDeskFaqSuggestion(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("question") val question: String? = null,
    @SerializedName("answer") val answer: String? = null,
)

data class HelpDeskTutorialsResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: HelpDeskTutorialsData? = null,
)

data class HelpDeskTutorialsData(
    @SerializedName("tutorials") val tutorials: List<HelpDeskTutorialItem>? = emptyList(),
)

data class HelpDeskTutorialItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("video_url") val videoUrl: String? = null,
    @SerializedName("audio_url") val audioUrl: String? = null,
    @SerializedName("text_content") val textContent: String? = null,
)

data class EnquirySubmitResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
)
