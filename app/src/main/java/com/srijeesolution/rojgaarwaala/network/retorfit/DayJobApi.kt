package com.srijeesolution.rojgaarwaala.network.retorfit

import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobAcceptRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobCompanyData
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobCompanyRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobData
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobEnvelope
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobListData
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobMeta
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobRepostRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobWorkerData
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobWorkerRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

/** 1 Day Job endpoints (backend: routes/api.php, prefix day-jobs). */
interface DayJobApi {
    @GET("day-jobs/meta")
    suspend fun meta(): Response<DayJobEnvelope<DayJobMeta>>

    @POST("day-jobs/company")
    suspend fun saveCompany(@Body body: DayJobCompanyRequest): Response<DayJobEnvelope<DayJobCompanyData>>

    @POST("day-jobs/worker")
    suspend fun saveWorker(@Body body: DayJobWorkerRequest): Response<DayJobEnvelope<DayJobWorkerData>>

    @Multipart
    @POST("day-jobs")
    suspend fun create(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part photos: List<MultipartBody.Part>,
    ): Response<DayJobEnvelope<DayJobData>>

    @GET("day-jobs/mine")
    suspend fun mine(@Query("tab") tab: String): Response<DayJobEnvelope<DayJobListData>>

    @GET("day-jobs/nearby")
    suspend fun nearby(
        @Query("latitude") latitude: Double?,
        @Query("longitude") longitude: Double?,
    ): Response<DayJobEnvelope<DayJobListData>>

    @GET("day-jobs/bookings")
    suspend fun myBookings(): Response<DayJobEnvelope<DayJobListData>>

    @GET("day-jobs/{id}")
    suspend fun show(@Path("id") id: Int): Response<DayJobEnvelope<DayJobData>>

    @POST("day-jobs/{id}/accept")
    suspend fun accept(@Path("id") id: Int, @Body body: DayJobAcceptRequest): Response<DayJobEnvelope<DayJobData>>

    @POST("day-jobs/{id}/cancel")
    suspend fun cancel(@Path("id") id: Int): Response<DayJobEnvelope<DayJobData>>

    @POST("day-jobs/{id}/repost")
    suspend fun repost(@Path("id") id: Int, @Body body: DayJobRepostRequest): Response<DayJobEnvelope<DayJobData>>

    @POST("day-jobs/bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") id: Int): Response<DayJobEnvelope<Any>>
}
