package com.srijeesolution.rojgaarwaala.network.retorfit

import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobAcceptRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobConfirmRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobPaidRequest
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

    @GET("day-jobs/wage-hint")
    suspend fun wageHint(
        @Query("trade") trade: String,
        @Query("district") district: String?,
    ): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobWageHintData>>

    @POST("day-jobs/favourites/{workerId}")
    suspend fun toggleFavourite(@Path("workerId") workerId: Int): Response<DayJobEnvelope<Any>>

    @POST("day-jobs/bookings/{id}/confirm")
    suspend fun confirm(@Path("id") id: Int, @Body body: DayJobConfirmRequest): Response<DayJobEnvelope<Any>>

    @POST("day-jobs/bookings/{id}/paid")
    suspend fun paid(@Path("id") id: Int, @Body body: DayJobPaidRequest): Response<DayJobEnvelope<Any>>

    @POST("day-jobs/bookings/{id}/no-show")
    suspend fun noShow(@Path("id") id: Int): Response<DayJobEnvelope<Any>>

    @POST("day-jobs/bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") id: Int): Response<DayJobEnvelope<Any>>

    // Gang leader
    @GET("day-jobs/team")
    suspend fun team(): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData>>

    @POST("day-jobs/team")
    suspend fun createTeam(@Body body: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamNameRequest): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData>>

    @POST("day-jobs/team/rename")
    suspend fun renameTeam(@Body body: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamNameRequest): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData>>

    @POST("day-jobs/team/invite")
    suspend fun invite(@Body body: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamInviteRequest): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData>>

    @POST("day-jobs/team/invites/{id}/respond")
    suspend fun respondInvite(@Path("id") id: Int, @Body body: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobYesRequest): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData>>

    @POST("day-jobs/team/members/{id}/remove")
    suspend fun removeMember(@Path("id") id: Int): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData>>

    @POST("day-jobs/team/leave")
    suspend fun leaveTeam(): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData>>

    @POST("day-jobs/team/disband")
    suspend fun disbandTeam(): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData>>

    @GET("day-jobs/{id}/team-options")
    suspend fun teamOptions(@Path("id") id: Int): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamOptionsData>>

    @POST("day-jobs/{id}/accept-team")
    suspend fun acceptForTeam(@Path("id") id: Int, @Body body: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamAcceptRequest): Response<DayJobEnvelope<DayJobData>>

    // On the way
    @POST("day-jobs/bookings/{id}/location")
    suspend fun sendLocation(@Path("id") id: Int, @Body body: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobLocationRequest): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobLocationReply>>

    @POST("day-jobs/bookings/{id}/location/stop")
    suspend fun stopLocation(@Path("id") id: Int): Response<DayJobEnvelope<Any>>

    @GET("day-jobs/{id}/tracking")
    suspend fun tracking(@Path("id") id: Int): Response<DayJobEnvelope<com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTrackData>>
}
