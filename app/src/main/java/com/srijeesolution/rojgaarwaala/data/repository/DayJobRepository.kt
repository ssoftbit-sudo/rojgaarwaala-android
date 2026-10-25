package com.srijeesolution.rojgaarwaala.data.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.DayJob
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobAcceptRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobCompany
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobCompanyRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobEnvelope
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

    suspend fun saveWorker(trades: List<String>, available: Boolean): DayJobOutcome<DayJobWorker?> =
        call { api.saveWorker(DayJobWorkerRequest(trades, available)) }.mapData { it.worker }

    suspend fun nearby(latitude: Double?, longitude: Double?): DayJobOutcome<List<DayJob>> =
        call { api.nearby(latitude, longitude) }.mapData { it.jobs }

    suspend fun mine(history: Boolean): DayJobOutcome<List<DayJob>> =
        call { api.mine(if (history) "history" else "active") }.mapData { it.jobs }

    suspend fun myBookings(): DayJobOutcome<List<DayJob>> = call { api.myBookings() }.mapData { it.jobs }

    suspend fun show(id: Int): DayJobOutcome<DayJob?> = call { api.show(id) }.mapData { it.job }

    suspend fun accept(id: Int, trade: String): DayJobOutcome<DayJob?> =
        call { api.accept(id, DayJobAcceptRequest(trade)) }.mapData { it.job }

    suspend fun cancelJob(id: Int): DayJobOutcome<DayJob?> = call { api.cancel(id) }.mapData { it.job }

    suspend fun cancelBooking(bookingId: Int): DayJobOutcome<Any?> = call { api.cancelBooking(bookingId) }

    suspend fun repost(id: Int, workDate: String, startTime: String?, wage: Int?): DayJobOutcome<DayJob?> =
        call { api.repost(id, DayJobRepostRequest(workDate, startTime, wage)) }.mapData { it.job }

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
                DayJobOutcome.Fail("No internet. Please try again.")
            }
        }

    private fun errorMessage(errorBody: String?, fallback: String?): String {
        val fromBody = runCatching { JSONObject(errorBody.orEmpty()).optString("message") }.getOrNull()
        return fromBody?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
    }

    private inline fun <T, R> DayJobOutcome<T>.mapData(transform: (T) -> R): DayJobOutcome<R> = when (this) {
        is DayJobOutcome.Ok -> DayJobOutcome.Ok(transform(data), message)
        is DayJobOutcome.Fail -> this
    }
}
