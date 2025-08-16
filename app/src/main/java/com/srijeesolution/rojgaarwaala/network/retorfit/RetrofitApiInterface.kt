package com.srijeesolution.rojgaarwaala.network.retorfit

import com.srijeesolution.rojgaarwaala.data.remote.model.HomePagBaseApiModel
import com.srijeesolution.rojgaarwaala.data.remote.model.VideoDetailsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.JobListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.CategoryVideosResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.ImagesApiResponse
import com.srijeesolution.rojgaarwaala.network.constant.NetworkConstants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface RetrofitApiInterface {

    @POST(NetworkConstants.ON_LOGIN)
    suspend fun onLoginUser(@Body email: HashMap<String, String>): Response<HomePagBaseApiModel>

    @POST(NetworkConstants.ON_REGISTER)
    suspend fun onRegisterData(@Body email: HashMap<String, String>): Response<HomePagBaseApiModel>

    @POST(NetworkConstants.SEND_OTP)
    suspend fun sendOtp(@Body request: HashMap<String, String>): Response<HomePagBaseApiModel>

    @POST(NetworkConstants.VERIFY_OTP)
    suspend fun verifyOtp(@Body request: HashMap<String, String>): Response<HomePagBaseApiModel>

    @GET(NetworkConstants.ON_LOGOUT)
    suspend fun onLogoutData(): Response<HomePagBaseApiModel>

    @GET(NetworkConstants.HOMEPAGE_DATA)
    suspend fun getHomePageData(@Query("query") searchTerm:String): Response<HomePagBaseApiModel>

    @GET(NetworkConstants.HOMEPAGE_DATA)
    suspend fun getProfileData(@Query("query") searchTerm:String): Response<HomePagBaseApiModel>

    @GET(NetworkConstants.GET_PROFILE)
    suspend fun getProfileData(): Response<HomePagBaseApiModel>

    @POST(NetworkConstants.UPDATE_PROFILE)
    suspend fun updateProfileLiveData(@Body email: HashMap<String, String>): Response<HomePagBaseApiModel>

    @POST(NetworkConstants.JOB_SUBMIT)
    suspend fun onSubmitJob(@Body email: HashMap<String, String>): Response<HomePagBaseApiModel>

    @Multipart
    @POST(NetworkConstants.JOB_SUBMIT)
    suspend fun onSubmitJobWithFiles(
        @Part("job_title") jobTitle: RequestBody,
        @Part("job_description") jobDescription: RequestBody,
        @Part("job_category") jobCategory: RequestBody,
        @Part("job_responsibility") jobResponsibility: RequestBody,
        @Part pdf: MultipartBody.Part? = null,
        @Part image: MultipartBody.Part? = null,
        @Part logo: MultipartBody.Part? = null
    ): Response<HomePagBaseApiModel>

    @GET(NetworkConstants.CATEGORIES_LIST)
    suspend fun getCategoriesData(): Response<HomePagBaseApiModel>

    @GET(NetworkConstants.VIDEO_DETAILS)
    suspend fun getVideoDetails(@Path("id") id: Int): Response<VideoDetailsResponse>

    @GET(NetworkConstants.JOB_LIST)
    suspend fun getJobList(): Response<JobListResponse>

    @GET(NetworkConstants.CATEGORY_VIDEOS)
    suspend fun getCategoryVideos(@Path("id") id: Int): Response<CategoryVideosResponse>

    @POST(NetworkConstants.VIDEO_LIKE)
    suspend fun likeVideo(@Body request: HashMap<String, Any>): Response<HomePagBaseApiModel>

    @POST(NetworkConstants.VIDEO_UNLIKE)
    suspend fun unlikeVideo(@Body request: HashMap<String, Any>): Response<HomePagBaseApiModel>

    @POST(NetworkConstants.VIDEO_INCREMENT_VIEW)
    suspend fun incrementVideoView(@Body request: HashMap<String, Any>): Response<HomePagBaseApiModel>

    @DELETE(NetworkConstants.JOB_DELETE)
    suspend fun deleteJob(@Path("id") id: Int): Response<HomePagBaseApiModel>

    @POST(NetworkConstants.JOB_UPDATE)
    suspend fun updateJob(@Path("id") id: Int, @Body data: HashMap<String, String>): Response<HomePagBaseApiModel>

    @Multipart
    @POST(NetworkConstants.JOB_UPDATE)
    suspend fun updateJobWithFiles(
        @Path("id") id: Int,
        @Part("job_title") jobTitle: RequestBody,
        @Part("job_description") jobDescription: RequestBody,
        @Part("job_category") jobCategory: RequestBody,
        @Part("job_responsibility") jobResponsibility: RequestBody,
        @Part pdf: MultipartBody.Part? = null,
        @Part image: MultipartBody.Part? = null,
        @Part logo: MultipartBody.Part? = null
    ): Response<HomePagBaseApiModel>

    @GET(NetworkConstants.SCHEDULED_IMAGES_GROUPED)
    suspend fun getScheduledImagesGrouped(): Response<ImagesApiResponse>
    @GET(NetworkConstants.SCHEDULED_IMAGES)
    suspend fun getScheduledImages(): Response<ImageListResponse>

}