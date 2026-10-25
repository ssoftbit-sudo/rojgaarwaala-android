package com.srijeesolution.rojgaarwaala.data.remote.model

import com.google.gson.annotations.SerializedName

/** Every 1 Day Job response: {status, message, data}. */
data class DayJobEnvelope<T>(
    @SerializedName("status") val status: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
)

data class DayJobTrade(
    @SerializedName("key") val key: String = "",
    @SerializedName("label") val label: String = "",
)

data class DayJobCompany(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("owner_name") val ownerName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("district") val district: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("admin_note") val adminNote: String? = null,
)

data class DayJobWorker(
    @SerializedName("trades") val trades: List<String> = emptyList(),
    @SerializedName("is_available") val isAvailable: Boolean = false,
    @SerializedName("registered") val registered: Boolean = false,
)

data class DayJobMeta(
    @SerializedName("trades") val trades: List<DayJobTrade> = emptyList(),
    @SerializedName("districts") val districts: List<String> = emptyList(),
    @SerializedName("company") val company: DayJobCompany? = null,
    @SerializedName("worker") val worker: DayJobWorker = DayJobWorker(),
    @SerializedName("terms") val terms: List<String> = emptyList(),
)

data class DayJobSlot(
    @SerializedName("trade") val trade: String = "",
    @SerializedName("label") val label: String = "",
    @SerializedName("needed") val needed: Int = 0,
    @SerializedName("filled") val filled: Int = 0,
) {
    val left: Int get() = (needed - filled).coerceAtLeast(0)
}

data class DayJobBooking(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("status") val status: String = "",
    @SerializedName("trade") val trade: String? = null,
    @SerializedName("trade_label") val tradeLabel: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("accepted_at") val acceptedAt: String? = null,
)

data class DayJobCompanyRef(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("phone") val phone: String? = null,
)

data class DayJob(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("wage_per_day") val wagePerDay: Int = 0,
    @SerializedName("work_date") val workDate: String = "",
    @SerializedName("start_time") val startTime: String = "",
    @SerializedName("starts_at") val startsAt: String? = null,
    @SerializedName("accept_closes_at") val acceptClosesAt: String? = null,
    @SerializedName("hours") val hours: Int = 8,
    @SerializedName("location") val location: String? = null,
    @SerializedName("area_name") val areaName: String? = null,
    @SerializedName("district") val district: String? = null,
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("photos") val photos: List<String> = emptyList(),
    @SerializedName("status") val status: String = "",
    @SerializedName("is_accepting") val isAccepting: Boolean = false,
    @SerializedName("radius_km") val radiusKm: Int = 5,
    @SerializedName("notified_count") val notifiedCount: Int = 0,
    @SerializedName("distance_km") val distanceKm: Double? = null,
    @SerializedName("company") val company: DayJobCompanyRef = DayJobCompanyRef(),
    @SerializedName("slots") val slots: List<DayJobSlot> = emptyList(),
    @SerializedName("needed_total") val neededTotal: Int = 0,
    @SerializedName("filled_total") val filledTotal: Int = 0,
    @SerializedName("my_booking") val myBooking: DayJobBooking? = null,
    @SerializedName("bookings") val bookings: List<DayJobBooking>? = null,
) {
    val place: String get() = areaName?.takeIf { it.isNotBlank() } ?: location.orEmpty()
}

data class DayJobListData(
    @SerializedName("jobs") val jobs: List<DayJob> = emptyList(),
    /** For nearby: the point the search was made from (the worker). */
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
)
data class DayJobData(@SerializedName("job") val job: DayJob? = null)
data class DayJobCompanyData(@SerializedName("company") val company: DayJobCompany? = null)
data class DayJobWorkerData(@SerializedName("worker") val worker: DayJobWorker? = null)

data class DayJobCompanyRequest(
    @SerializedName("name") val name: String,
    @SerializedName("owner_name") val ownerName: String?,
    @SerializedName("phone") val phone: String,
    @SerializedName("address") val address: String?,
    @SerializedName("district") val district: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("accept_terms") val acceptTerms: Boolean,
)

data class DayJobWorkerRequest(
    @SerializedName("trades") val trades: List<String>,
    @SerializedName("is_available") val isAvailable: Boolean,
)

data class DayJobAcceptRequest(@SerializedName("trade") val trade: String)

data class DayJobRepostRequest(
    @SerializedName("work_date") val workDate: String,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("wage_per_day") val wagePerDay: Int?,
)
