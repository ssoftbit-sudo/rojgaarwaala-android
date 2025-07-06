package com.srijeesolution.rojgaarwaala.domain.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.HomePagBaseApiModel
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
}