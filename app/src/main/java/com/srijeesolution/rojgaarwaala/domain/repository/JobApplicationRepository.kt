package com.srijeesolution.rojgaarwaala.domain.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.VerifyPaymentRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.JobApplicationApiResponse
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface JobApplicationRepository {
    fun submitApplication(
        fullName: RequestBody,
        phone: RequestBody,
        email: RequestBody,
        videoId: RequestBody?,
        scheduledImageId: RequestBody?,
        jobTitle: RequestBody?,
        resume: MultipartBody.Part,
    ): Flow<ApiResult<JobApplicationApiResponse>>

    fun createPaymentOrder(applicationId: Int): Flow<ApiResult<JobApplicationApiResponse>>

    fun verifyPayment(applicationId: Int, request: VerifyPaymentRequest): Flow<ApiResult<JobApplicationApiResponse>>

    fun markPaymentFailed(applicationId: Int): Flow<ApiResult<JobApplicationApiResponse>>

    fun getApplication(applicationId: Int): Flow<ApiResult<JobApplicationApiResponse>>

    fun getMyApplications(): Flow<ApiResult<JobApplicationApiResponse>>
}
