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
    @SerializedName("paid_rate") val paidRate: Int? = null,
    @SerializedName("rating_avg") val ratingAvg: Double? = null,
    @SerializedName("rating_count") val ratingCount: Int = 0,
)

data class DayJobWorker(
    @SerializedName("trades") val trades: List<String> = emptyList(),
    @SerializedName("is_available") val isAvailable: Boolean = false,
    @SerializedName("registered") val registered: Boolean = false,
    @SerializedName("alert_from") val alertFrom: String? = null,
    @SerializedName("alert_to") val alertTo: String? = null,
    @SerializedName("held_back_until") val heldBackUntil: String? = null,
    // No new bookings until then (no-show or too many cancels), with the reason in Hindi.
    @SerializedName("booking_blocked_until") val bookingBlockedUntil: String? = null,
    @SerializedName("booking_block_message") val bookingBlockMessage: String? = null,
    @SerializedName("rating_avg") val ratingAvg: Double? = null,
    @SerializedName("rating_count") val ratingCount: Int = 0,
)

data class DayJobMeta(
    /** Posting by speaking is switched on at the server. */
    @SerializedName("voice_available") val voiceAvailable: Boolean = false,
    @SerializedName("trades") val trades: List<DayJobTrade> = emptyList(),
    @SerializedName("districts") val districts: List<String> = emptyList(),
    @SerializedName("company") val company: DayJobCompany? = null,
    @SerializedName("worker") val worker: DayJobWorker = DayJobWorker(),
    @SerializedName("terms") val terms: List<String> = emptyList(),
    @SerializedName("worker_terms") val workerTerms: List<String> = emptyList(),
    @SerializedName("needs_worker_terms") val needsWorkerTerms: Boolean = false,
    @SerializedName("needs_company_terms") val needsCompanyTerms: Boolean = false,
    @SerializedName("cancel_reasons") val cancelReasons: List<String> = emptyList(),
    /** Jobs where this user still has to press Job Done, as worker and as company. */
    @SerializedName("pending_done") val pendingDone: List<Int> = emptyList(),
    @SerializedName("pending_done_company") val pendingDoneCompany: List<Int> = emptyList(),
    /** Labour zones (chowks) the worker picked; work near them reaches the worker too. */
    @SerializedName("my_zones") val myZones: List<DayJobZone> = emptyList(),
    @SerializedName("max_zones") val maxZones: Int = 2,
)

data class DayJobZone(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("district") val district: String? = null,
    @SerializedName("radius_km") val radiusKm: Int = 3,
    @SerializedName("distance_km") val distanceKm: Double? = null,
)

data class DayJobZoneAsk(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("status") val status: String = "pending",
    @SerializedName("admin_note") val adminNote: String? = null,
)

data class DayJobZonesData(
    @SerializedName("zones") val zones: List<DayJobZone> = emptyList(),
    @SerializedName("requests") val requests: List<DayJobZoneAsk> = emptyList(),
    @SerializedName("max_zones") val maxZones: Int = 2,
)

data class DayJobZonesRequest(@SerializedName("zone_ids") val zoneIds: List<Int>)

data class DayJobZoneAskRequest(
    @SerializedName("name") val name: String,
    @SerializedName("landmark") val landmark: String?,
    @SerializedName("district") val district: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("note") val note: String?,
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
    @SerializedName("worker_id") val workerId: Int = 0,
    @SerializedName("favourite") val favourite: Boolean = false,
    @SerializedName("status") val status: String = "",
    @SerializedName("trade") val trade: String? = null,
    @SerializedName("trade_label") val tradeLabel: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("accepted_at") val acceptedAt: String? = null,
    @SerializedName("confirmed") val confirmed: Boolean = false,
    @SerializedName("confirm_asked") val confirmAsked: Boolean = false,
    @SerializedName("paid_answer") val paidAnswer: String? = null,
    @SerializedName("can_answer_payment") val canAnswerPayment: Boolean = false,
    @SerializedName("can_mark_no_show") val canMarkNoShow: Boolean = false,
    // Team seats: the company calls the leader; the member sees who booked them.
    @SerializedName("team_name") val teamName: String? = null,
    @SerializedName("leader_name") val leaderName: String? = null,
    @SerializedName("call_mobile") val callMobile: String? = null,
    @SerializedName("booked_by_leader") val bookedByLeader: Boolean = false,
    // On the way: the worker shares only after tapping "घर से निकल पड़ा".
    @SerializedName("can_share_location") val canShareLocation: Boolean = false,
    @SerializedName("sharing_location") val sharingLocation: Boolean = false,
    @SerializedName("arrived") val arrived: Boolean = false,
    /** The supervisor typed this worker's start code: attendance is in. */
    @SerializedName("started") val started: Boolean = false,
    /** Company side: the worker's code can be typed now. */
    @SerializedName("can_start") val canStart: Boolean = false,
    /** Worker side, on the day: the 4 digits to tell the supervisor. */
    @SerializedName("start_code") val startCode: String? = null,
    // Job Done: the worker's own (my_booking) and the company's, per worker (owner list).
    @SerializedName("can_mark_done") val canMarkDone: Boolean = false,
    @SerializedName("done_by_me") val doneByMe: Boolean = false,
    @SerializedName("done_by_company") val doneByCompany: Boolean = false,
    @SerializedName("my_rating") val myRating: Int? = null,
    @SerializedName("company_done") val companyDone: Boolean = false,
    @SerializedName("worker_done") val workerDone: Boolean = false,
    @SerializedName("company_rating") val companyRating: Int? = null,
    @SerializedName("worker_rating") val workerRating: Int? = null,
    @SerializedName("not_paid") val notPaid: Boolean = false,
    @SerializedName("worker_rating_avg") val workerRatingAvg: Double? = null,
    @SerializedName("worker_rating_count") val workerRatingCount: Int = 0,
    // Easy booking: "हाँ" keeps the seat a few minutes until "अभी Book करें".
    @SerializedName("on_hold") val onHold: Boolean = false,
    /** The leader picked this member; booked once they press "जाऊँगा". */
    @SerializedName("team_ask") val teamAsk: Boolean = false,
    /** Overtime asked on the day: asked, accepted or declined. */
    @SerializedName("ot_status") val otStatus: String? = null,
    @SerializedName("ot_hours") val otHours: Double? = null,
    @SerializedName("ot_amount") val otAmount: Int? = null,
    /** The worker may offer to stay longer today; the money is worked out from this rate. */
    @SerializedName("can_request_overtime") val canRequestOvertime: Boolean = false,
    @SerializedName("hourly_rate") val hourlyRate: Int = 0,
    @SerializedName("held_until") val heldUntil: String? = null,
    @SerializedName("can_rebook") val canRebook: Boolean = false,
)

data class DayJobTeamTurn(
    /** offered (say yes/no) or yes (pick who goes). */
    @SerializedName("status") val status: String = "",
    @SerializedName("respond_by") val respondBy: String? = null,
    @SerializedName("pick_by") val pickBy: String? = null,
)

data class DayJobFreeMember(
    @SerializedName("team_name") val teamName: String = "",
    @SerializedName("user_id") val userId: Int = 0,
    @SerializedName("name") val name: String? = null,
)

data class DayJobCompanyRef(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("paid_rate") val paidRate: Int? = null,
    @SerializedName("rating_avg") val ratingAvg: Double? = null,
    @SerializedName("rating_count") val ratingCount: Int = 0,
)

data class DayJob(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    /** English words of the job written in Devanagari, so the Hindi voice says them right. */
    @SerializedName("speech_hi") val speechHi: Map<String, String>? = null,
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
    /** Null from an older server; then fall back to the status. */
    @SerializedName("can_cancel") val canCancel: Boolean? = null,
    @SerializedName("can_ask_overtime") val canAskOvertime: Boolean = false,
    @SerializedName("hourly_rate") val hourlyRate: Int = 0,
    /** A re-booking kept for this worker for a while. */
    @SerializedName("reserved_for_me") val reservedForMe: Boolean = false,
    @SerializedName("reserved_until") val reservedUntil: String? = null,
    @SerializedName("rejected_by_me") val rejectedByMe: Boolean = false,
    /** Not yet takeable by this worker (a day ahead; 3 days with 20+ ratings): from when. */
    @SerializedName("can_take_from") val canTakeFrom: String? = null,
    /** This leader's team's turn at the job, if it is theirs now. */
    @SerializedName("team_turn") val teamTurn: DayJobTeamTurn? = null,
    @SerializedName("teams_turn_now") val teamsTurnNow: Boolean = false,
    /** Owner: free people in the teams coming who could fill an empty seat. */
    @SerializedName("team_free_members") val teamFreeMembers: List<DayJobFreeMember>? = null,
    @SerializedName("radius_km") val radiusKm: Int = 5,
    @SerializedName("notified_count") val notifiedCount: Int = 0,
    @SerializedName("can_track") val canTrack: Boolean = false,
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
    @SerializedName("alert_from") val alertFrom: String? = null,
    @SerializedName("alert_to") val alertTo: String? = null,
    @SerializedName("accept_terms") val acceptTerms: Boolean? = null,
)

data class DayJobVoiceRequest(@SerializedName("text") val text: String)

/** The job form as Claude filled it from what the company said; nulls are left for them. */
data class DayJobVoiceDraft(
    @SerializedName("title") val title: String? = null,
    @SerializedName("slots") val slots: List<DayJobVoiceSlot> = emptyList(),
    @SerializedName("wage_per_day") val wagePerDay: Int? = null,
    @SerializedName("work_date") val workDate: String? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("hours") val hours: Int? = null,
    @SerializedName("area_name") val areaName: String? = null,
    @SerializedName("description") val description: String? = null,
)

data class DayJobVoiceSlot(@SerializedName("trade") val trade: String = "", @SerializedName("count") val count: Int = 0)
data class DayJobVoiceData(@SerializedName("draft") val draft: DayJobVoiceDraft? = null)

data class DayJobOvertimeRequest(
    @SerializedName("hours") val hours: Double,
    @SerializedName("amount") val amount: Int?,
)

data class DayJobOtRequest(@SerializedName("hours") val hours: Double)
data class DayJobOtDecision(@SerializedName("yes") val yes: Boolean, @SerializedName("amount") val amount: Int?)

data class DayJobTermsRequest(@SerializedName("audience") val audience: String)
data class DayJobCancelRequest(@SerializedName("reason") val reason: String?)
data class DayJobDoneRequest(
    @SerializedName("paid") val paid: Boolean,
    @SerializedName("rating") val rating: Int?,
    @SerializedName("note") val note: String? = null,
)
data class DayJobRatingRequest(@SerializedName("rating") val rating: Int, @SerializedName("note") val note: String? = null)

data class DayJobConfirmRequest(@SerializedName("coming") val coming: Boolean)
data class DayJobPaidRequest(@SerializedName("paid") val paid: Boolean)

data class DayJobAcceptRequest(@SerializedName("trade") val trade: String, @SerializedName("hold") val hold: Boolean = true)

data class DayJobRepostRequest(
    @SerializedName("work_date") val workDate: String,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("wage_per_day") val wagePerDay: Int?,
)

data class DayJobWageHint(
    @SerializedName("label") val label: String = "",
    @SerializedName("district") val district: String? = null,
    @SerializedName("low") val low: Int = 0,
    @SerializedName("typical") val typical: Int = 0,
    @SerializedName("high") val high: Int = 0,
    @SerializedName("jobs") val jobs: Int = 0,
)

data class DayJobWageHintData(@SerializedName("hint") val hint: DayJobWageHint? = null)

// ------------------------------------------------------------------ gang leader

data class DayJobTeamPerson(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
)

data class DayJobTeamMember(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("user_id") val userId: Int = 0,
    @SerializedName("name") val name: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("status") val status: String = "",
    @SerializedName("trades") val trades: List<String> = emptyList(),
    @SerializedName("free") val free: Boolean = false,
    @SerializedName("booked_today") val bookedToday: Boolean = false,
)

data class DayJobTeam(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("leader") val leader: DayJobTeamPerson = DayJobTeamPerson(),
    @SerializedName("people") val people: Int = 1,
    @SerializedName("members") val members: List<DayJobTeamMember> = emptyList(),
)

data class DayJobTeamInvite(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("team_name") val teamName: String? = null,
    @SerializedName("leader_name") val leaderName: String? = null,
)

data class DayJobTeamData(
    @SerializedName("role") val role: String = "none",
    @SerializedName("team") val team: DayJobTeam? = null,
    @SerializedName("invites") val invites: List<DayJobTeamInvite> = emptyList(),
    @SerializedName("max_people") val maxPeople: Int = 20,
)

data class DayJobTeamOption(
    @SerializedName("user_id") val userId: Int = 0,
    @SerializedName("name") val name: String? = null,
    @SerializedName("trades") val trades: List<DayJobTrade> = emptyList(),
    @SerializedName("available") val available: Boolean = false,
    @SerializedName("reason") val reason: String? = null,
)

data class DayJobTeamOptionsData(@SerializedName("people") val people: List<DayJobTeamOption> = emptyList())

data class DayJobTeamNameRequest(@SerializedName("name") val name: String)
data class DayJobTeamInviteRequest(@SerializedName("mobile") val mobile: String)
data class DayJobYesRequest(@SerializedName("yes") val yes: Boolean)
data class DayJobTeamPick(@SerializedName("user_id") val userId: Int, @SerializedName("trade") val trade: String)
data class DayJobTeamAcceptRequest(@SerializedName("picks") val picks: List<DayJobTeamPick>)

// ------------------------------------------------------------------ on the way

data class DayJobLocationRequest(@SerializedName("latitude") val latitude: Double, @SerializedName("longitude") val longitude: Double)

data class DayJobLocationReply(
    @SerializedName("stop") val stop: Boolean = false,
    @SerializedName("arrived") val arrived: Boolean = false,
    @SerializedName("distance_km") val distanceKm: Double? = null,
)

data class DayJobTrackSite(
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("name") val name: String? = null,
)

data class DayJobTrackWorker(
    @SerializedName("booking_id") val bookingId: Int = 0,
    @SerializedName("name") val name: String? = null,
    @SerializedName("trade_label") val tradeLabel: String? = null,
    /** not_started, on_way, stopped or arrived. */
    @SerializedName("state") val state: String = "not_started",
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("distance_km") val distanceKm: Double? = null,
    @SerializedName("eta_minutes") val etaMinutes: Int? = null,
    @SerializedName("updated_seconds_ago") val updatedSecondsAgo: Int? = null,
    @SerializedName("call_mobile") val callMobile: String? = null,
)

data class DayJobTrackData(
    @SerializedName("site") val site: DayJobTrackSite = DayJobTrackSite(),
    @SerializedName("workers") val workers: List<DayJobTrackWorker> = emptyList(),
)

data class DayJobStartRequest(@SerializedName("code") val code: String)
