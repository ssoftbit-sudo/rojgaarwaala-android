package com.srijeesolution.rojgaarwaala.domain.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.HomePagBaseApiModel
import com.srijeesolution.rojgaarwaala.data.remote.model.VideoDetailsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.JobListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.CategoryVideosResponse
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import kotlinx.coroutines.flow.Flow

interface HomePageRepository {
    fun onLoginUser(email: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>>
    fun onRegisterData(email: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>>
    fun sendOtp(request: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>>
    fun verifyOtp(request: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>>
    fun onLogoutData(): Flow<ApiResult<HomePagBaseApiModel>>
    fun getHomePageData(searchTerm:String): Flow<ApiResult<HomePagBaseApiModel>>
    fun getProfileData(): Flow<ApiResult<HomePagBaseApiModel>>
    fun updateProfileLiveData(data: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>>
    fun onSubmitJob(data: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>>
    fun getCategoriesData(): Flow<ApiResult<HomePagBaseApiModel>>
    fun getVideoDetails(id: Int): Flow<ApiResult<VideoDetailsResponse>>
    fun getJobList(): Flow<ApiResult<JobListResponse>>
    fun getCategoryVideos(id: Int): Flow<ApiResult<CategoryVideosResponse>>
    fun likeVideo(videoId: Int): Flow<ApiResult<HomePagBaseApiModel>>
    fun unlikeVideo(videoId: Int): Flow<ApiResult<HomePagBaseApiModel>>
    fun incrementVideoView(videoId: Int): Flow<ApiResult<HomePagBaseApiModel>>
    fun deleteJob(id: Int): Flow<ApiResult<HomePagBaseApiModel>>
    fun updateJob(id: Int, data: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>>
}