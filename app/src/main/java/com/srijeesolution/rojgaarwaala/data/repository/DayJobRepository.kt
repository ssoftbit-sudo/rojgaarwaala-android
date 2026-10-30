package com.srijeesolution.rojgaarwaala.data.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.DayJob
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobAcceptRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobCompany
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobCompanyRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobEnvelope
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobListData
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobMeta
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobRepostRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobWorker
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobWorkerRequest
import com.srijeesolution.rojgaarwaala.network.constant.NetworkBaseUrls.Companion.BASE_URL
import com.srijeesolution.rojgaarwaala.network.retorfit.DayJobApi
import com.srijeesolution.rojgaarwaala.network.retorfit.RetrofitApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Result of a 1 Day Job call: the data and server message, or a message to show. */
sealed class DayJobOutcome<out T> {
    data class Ok<T>(val data: T, val message: String?) : DayJobOutcome<T>()
    data class Fail(val message: String) : DayJobOutcome<Nothing>()
}

/** What a company fills in to post a job. */
data class DayJobDraft(
    val title: String,
    val slots: Map<String, Int>,
    val wagePerDay: Int,
    val workDate: String,
    val startTime: String,
    val hours: Int,
    val areaName: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val photos: List<File>,
)

@Singleton
class DayJobRepository @Inject constructor() {

    private val api: DayJobApi get() = RetrofitApiService.dayJobs(BASE_URL)

    suspend fun meta(): DayJobOutcome<DayJobMeta> = call { api.meta() }

    suspend fun saveCompany(request: DayJobCompanyRequest): DayJobOutcome<DayJobCompany?> =
        call { api.saveCompany(request) }.mapData { it.company }

    suspend fun saveWorker(
        trades: List<String>,
        available: Boolean,
        alertFrom: String? = null,
        alertTo: String? = null,
        acceptTerms: Boolean? = null,
    ): DayJobOutcome<DayJobWorker?> =
        call { api.saveWorker(DayJobWorkerRequest(trades, available, alertFrom, alertTo, acceptTerms)) }.mapData { it.worker }

    /** What the company said, as a filled job form. */
    suspend fun voice(text: String): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobVoiceDraft?> =
        call { api.voice(com.srijeesolution.rojgaarwaala.data.remote.model.DayJobVoiceRequest(text)) }.mapData { it.draft }

    /** [amount] null: the hourly rate × hours. */
    suspend fun askOvertime(jobId: Int, hours: Double, amount: Int?): DayJobOutcome<Any?> =
        call { api.askOvertime(jobId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobOvertimeRequest(hours, amount)) }

    suspend fun answerOvertime(bookingId: Int, yes: Boolean): DayJobOutcome<Any?> =
        call { api.answerOvertime(bookingId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobYesRequest(yes)) }

    /** The worker offers to stay [hours] longer. */
    suspend fun requestOvertime(bookingId: Int, hours: Double): DayJobOutcome<Any?> =
        call { api.requestOvertime(bookingId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobOtRequest(hours)) }

    suspend fun decideOvertime(bookingId: Int, yes: Boolean, amount: Int?): DayJobOutcome<Any?> =
        call { api.decideOvertime(bookingId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobOtDecision(yes, amount)) }

    /** [audience] is "worker" or "company". */
    suspend fun acceptTerms(audience: String): DayJobOutcome<Any?> =
        call { api.acceptTerms(com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTermsRequest(audience)) }

    suspend fun workerDone(bookingId: Int, paid: Boolean, rating: Int?): DayJobOutcome<Any?> =
        call { api.workerDone(bookingId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobDoneRequest(paid, rating)) }

    suspend fun startWork(bookingId: Int, code: String): DayJobOutcome<Any?> =
        call { api.startWork(bookingId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobStartRequest(code)) }

    suspend fun companyDone(bookingId: Int, rating: Int): DayJobOutcome<Any?> =
        call { api.companyDone(bookingId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobRatingRequest(rating)) }

    suspend fun companyDoneAll(jobId: Int, rating: Int): DayJobOutcome<Any?> =
        call { api.companyDoneAll(jobId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobRatingRequest(rating)) }

    suspend fun confirm(bookingId: Int, coming: Boolean): DayJobOutcome<Any?> =
        call { api.confirm(bookingId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobConfirmRequest(coming)) }

    suspend fun answerPaid(bookingId: Int, paid: Boolean): DayJobOutcome<Any?> =
        call { api.paid(bookingId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobPaidRequest(paid)) }

    suspend fun wageHint(trade: String, district: String?): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobWageHint?> =
        call { api.wageHint(trade, district) }.mapData { it.hint }

    suspend fun toggleFavourite(workerId: Int): DayJobOutcome<Any?> = call { api.toggleFavourite(workerId) }

    suspend fun markNoShow(bookingId: Int): DayJobOutcome<Any?> = call { api.noShow(bookingId) }

    suspend fun nearby(latitude: Double?, longitude: Double?): DayJobOutcome<DayJobListData> =
        call { api.nearby(latitude, longitude) }

    suspend fun mine(history: Boolean): DayJobOutcome<List<DayJob>> =
        call { api.mine(if (history) "history" else "active") }.mapData { it.jobs }

    suspend fun myBookings(): DayJobOutcome<List<DayJob>> = call { api.myBookings() }.mapData { it.jobs }

    suspend fun show(id: Int): DayJobOutcome<DayJob?> = call { api.show(id) }.mapData { it.job }

    suspend fun accept(id: Int, trade: String): DayJobOutcome<DayJob?> =
        call { api.accept(id, DayJobAcceptRequest(trade)) }.mapData { it.job }

    /** "अभी Book करें" on a held seat. */
    suspend fun book(bookingId: Int): DayJobOutcome<DayJob?> = call { api.book(bookingId) }.mapData { it.job }

    /** The leader answers their team's turn. */
    suspend fun answerTeamTurn(jobId: Int, yes: Boolean): DayJobOutcome<DayJob?> =
        call { api.teamTurn(jobId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobYesRequest(yes)) }.mapData { it.job }

    suspend fun inviteMember(jobId: Int, userId: Int): DayJobOutcome<Any?> = call { api.inviteMember(jobId, userId) }

    suspend fun reject(jobId: Int): DayJobOutcome<Any?> = call { api.reject(jobId) }

    suspend fun zones(latitude: Double?, longitude: Double?): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobZonesData> =
        call { api.zones(latitude, longitude) }

    suspend fun saveZones(ids: List<Int>): DayJobOutcome<Any?> =
        call { api.saveZones(com.srijeesolution.rojgaarwaala.data.remote.model.DayJobZonesRequest(ids)) }

    suspend fun askZone(body: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobZoneAskRequest): DayJobOutcome<Any?> =
        call { api.askZone(body) }

    /** The company calls the same worker back for [workDate]. */
    suspend fun rebook(bookingId: Int, workDate: String): DayJobOutcome<DayJob?> =
        call { api.rebook(bookingId, DayJobRepostRequest(workDate, null, null)) }.mapData { it.job }

    suspend fun cancelJob(id: Int): DayJobOutcome<DayJob?> = call { api.cancel(id) }.mapData { it.job }

    suspend fun cancelBooking(bookingId: Int, reason: String? = null): DayJobOutcome<Any?> =
        call { api.cancelBooking(bookingId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobCancelRequest(reason)) }

    suspend fun repost(id: Int, workDate: String, startTime: String?, wage: Int?): DayJobOutcome<DayJob?> =
        call { api.repost(id, DayJobRepostRequest(workDate, startTime, wage)) }.mapData { it.job }

    // On the way
    suspend fun sendLocation(bookingId: Int, latitude: Double, longitude: Double): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobLocationReply?> =
        call { api.sendLocation(bookingId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobLocationRequest(latitude, longitude)) }

    suspend fun stopLocation(bookingId: Int): DayJobOutcome<Any?> = call { api.stopLocation(bookingId) }

    suspend fun tracking(jobId: Int): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTrackData> = call { api.tracking(jobId) }

    // Gang leader
    suspend fun team(): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData> = call { api.team() }

    suspend fun createTeam(name: String): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData> =
        call { api.createTeam(com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamNameRequest(name)) }

    suspend fun renameTeam(name: String): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData> =
        call { api.renameTeam(com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamNameRequest(name)) }

    suspend fun invite(mobile: String): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData> =
        call { api.invite(com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamInviteRequest(mobile)) }

    suspend fun respondInvite(id: Int, yes: Boolean): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData> =
        call { api.respondInvite(id, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobYesRequest(yes)) }

    suspend fun removeMember(id: Int): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData> = call { api.removeMember(id) }

    suspend fun leaveTeam(): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData> = call { api.leaveTeam() }

    suspend fun disbandTeam(): DayJobOutcome<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData> = call { api.disbandTeam() }

    suspend fun teamOptions(jobId: Int): DayJobOutcome<List<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamOption>> =
        call { api.teamOptions(jobId) }.mapData { it.people }

    suspend fun acceptForTeam(jobId: Int, picks: List<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamPick>): DayJobOutcome<DayJob?> =
        call { api.acceptForTeam(jobId, com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamAcceptRequest(picks)) }.mapData { it.job }

    suspend fun create(draft: DayJobDraft): DayJobOutcome<DayJob?> {
        val text = "text/plain".toMediaType()
        val fields = linkedMapOf<String, RequestBody>()
        fun put(key: String, value: Any) { fields[key] = value.toString().toRequestBody(text) }
        put("title", draft.title)
        put("wage_per_day", draft.wagePerDay)
        put("work_date", draft.workDate)
        put("start_time", draft.startTime)
        put("hours", draft.hours)
        put("area_name", draft.areaName)
        put("location", draft.areaName)
        put("latitude", draft.latitude)
        put("longitude", draft.longitude)
        if (draft.description.isNotBlank()) put("description", draft.description)
        draft.slots.entries.forEachIndexed { i, (trade, count) ->
            put("slots[$i][trade]", trade)
            put("slots[$i][count]", count)
        }
        val photos = draft.photos.mapIndexed { i, file ->
            MultipartBody.Part.createFormData("photos[$i]", file.name, file.asRequestBody("image/jpeg".toMediaType()))
        }
        return call { api.create(fields, photos) }.mapData { it.job }
    }

    private suspend fun <T> call(block: suspend () -> Response<DayJobEnvelope<T>>): DayJobOutcome<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = block()
                val body = response.body()
                if (response.isSuccessful && body != null && body.status) {
                    @Suppress("UNCHECKED_CAST")
                    DayJobOutcome.Ok(body.data as T, body.message)
                } else {
                    DayJobOutcome.Fail(errorMessage(response.errorBody()?.string(), body?.message))
                }
            } catch (e: Exception) {
                DayJobOutcome.Fail("इंटरनेट नहीं है। फिर कोशिश करें।")
            }
        }

    private fun errorMessage(errorBody: String?, fallback: String?): String {
        val fromBody = runCatching { JSONObject(errorBody.orEmpty()).optString("message") }.getOrNull()
        return fromBody?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() } ?: "कुछ गड़बड़ हुई। फिर कोशिश करें।"
    }

    private inline fun <T, R> DayJobOutcome<T>.mapData(transform: (T) -> R): DayJobOutcome<R> = when (this) {
        is DayJobOutcome.Ok -> DayJobOutcome.Ok(transform(data), message)
        is DayJobOutcome.Fail -> this
    }
}
