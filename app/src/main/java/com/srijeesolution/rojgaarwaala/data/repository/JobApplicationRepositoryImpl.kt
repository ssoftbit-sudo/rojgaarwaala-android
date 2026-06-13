package com.srijeesolution.rojgaarwaala.data.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.ConfirmPaymentRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.JobApplicationApiResponse
import com.srijeesolution.rojgaarwaala.domain.repository.JobApplicationRepository
import com.srijeesolution.rojgaarwaala.network.constant.NetworkBaseUrls.Companion.BASE_URL
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.network.handler.BaseApiResponse
import com.srijeesolution.rojgaarwaala.network.retorfit.RetrofitApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class JobApplicationRepositoryImpl @Inject constructor() : JobApplicationRepository, BaseApiResponse() {

    override fun submitApplication(
        fullName: RequestBody,
        phone: RequestBody,
        email: RequestBody,
        videoId: RequestBody?,
        scheduledImageId: RequestBody?,
        jobTitle: RequestBody?,
        resume: MultipartBody.Part,
    ): Flow<ApiResult<JobApplicationApiResponse>> = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).submitJobApplication(
                fullName = fullName,
                phone = phone,
                email = email,
                videoId = videoId,
                scheduledImageId = scheduledImageId,
                jobTitle = jobTitle,
                resume = resume,
            )
        })
    }.flowOn(Dispatchers.IO)

    override fun createRazorpayOrder(applicationId: Int): Flow<ApiResult<JobApplicationApiResponse>> = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).createJobApplicationOrder(applicationId)
        })
    }.flowOn(Dispatchers.IO)

    override fun confirmPayment(
        applicationId: Int,
        request: ConfirmPaymentRequest,
    ): Flow<ApiResult<JobApplicationApiResponse>> = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).confirmJobApplicationPayment(applicationId, request)
        })
    }.flowOn(Dispatchers.IO)

    override fun markPaymentFailed(applicationId: Int): Flow<ApiResult<JobApplicationApiResponse>> = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).markJobApplicationPaymentFailed(applicationId)
        })
    }.flowOn(Dispatchers.IO)

    override fun getApplication(applicationId: Int): Flow<ApiResult<JobApplicationApiResponse>> = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getJobApplication(applicationId)
        })
    }.flowOn(Dispatchers.IO)
}
