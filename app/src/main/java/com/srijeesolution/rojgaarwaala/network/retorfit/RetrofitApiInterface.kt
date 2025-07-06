package com.srijeesolution.rojgaarwaala.network.retorfit

import com.srijeesolution.rojgaarwaala.data.remote.model.HomePagBaseApiModel
import com.srijeesolution.rojgaarwaala.network.constant.NetworkConstants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RetrofitApiInterface {

    @POST(NetworkConstants.ON_LOGIN)
    suspend fun onLoginUser(@Body email: HashMap<String, String>): Response<HomePagBaseApiModel>

    @POST(NetworkConstants.ON_REGISTER)
    suspend fun onRegisterData(@Body email: HashMap<String, String>): Response<HomePagBaseApiModel>

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

}