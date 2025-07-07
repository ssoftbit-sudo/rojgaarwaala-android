package com.srijeesolution.rojgaarwaala.data.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.HomePagBaseApiModel
import com.srijeesolution.rojgaarwaala.data.remote.model.CategoryVideosResponse
import com.srijeesolution.rojgaarwaala.domain.repository.HomePageRepository
import com.srijeesolution.rojgaarwaala.network.constant.NetworkBaseUrls.Companion.BASE_URL
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.network.handler.BaseApiResponse
import com.srijeesolution.rojgaarwaala.network.retorfit.RetrofitApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class HomePageRepositoryImpl @Inject constructor() : HomePageRepository, BaseApiResponse() {

    override fun onLoginUser(email: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).onLoginUser(email)}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun onRegisterData(email: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).onRegisterData(email)}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun sendOtp(request: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).sendOtp(request)}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun verifyOtp(request: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).verifyOtp(request)}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun onLogoutData(): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).onLogoutData()}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun getHomePageData(searchTerm:String): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).getHomePageData(searchTerm)}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun getProfileData(): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).getProfileData()}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun updateProfileLiveData(data: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).updateProfileLiveData(data)}
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun onSubmitJob(data: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).onSubmitJob(data)}
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getCategoriesData(): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall {
                RetrofitApiService.create(BASE_URL).getCategoriesData()
            })
        }.flowOn(Dispatchers.IO)
    }

    override fun getVideoDetails(id: Int) = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getVideoDetails(id)
        })
    }.flowOn(Dispatchers.IO)

    override fun getJobList() = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getJobList()
        })
    }.flowOn(Dispatchers.IO)

    override fun getCategoryVideos(id: Int) = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getCategoryVideos(id)
        })
    }.flowOn(Dispatchers.IO)

    override fun likeVideo(videoId: Int) = flow {
        val request = HashMap<String, Any>()
        request["video_id"] = videoId
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).likeVideo(request)
        })
    }.flowOn(Dispatchers.IO)

    override fun unlikeVideo(videoId: Int) = flow {
        val request = HashMap<String, Any>()
        request["video_id"] = videoId
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).unlikeVideo(request)
        })
    }.flowOn(Dispatchers.IO)

    override fun incrementVideoView(videoId: Int) = flow {
        val request = HashMap<String, Any>()
        request["video_id"] = videoId
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).incrementVideoView(request)
        })
    }.flowOn(Dispatchers.IO)
}