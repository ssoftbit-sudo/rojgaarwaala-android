package com.srijeesolution.rojgaarwaala.domain.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.ConfirmPaymentRequest
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

    fun createRazorpayOrder(applicationId: Int): Flow<ApiResult<JobApplicationApiResponse>>

    fun confirmPayment(applicationId: Int, request: ConfirmPaymentRequest): Flow<ApiResult<JobApplicationApiResponse>>

    fun markPaymentFailed(applicationId: Int): Flow<ApiResult<JobApplicationApiResponse>>

    fun getApplication(applicationId: Int): Flow<ApiResult<JobApplicationApiResponse>>
}
