package com.srijeesolution.rojgaarwaala.data.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.EnquirySubmitResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskConfigResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskFaqsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskTutorialsResponse
import com.srijeesolution.rojgaarwaala.domain.repository.HelpDeskRepository
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

class HelpDeskRepositoryImpl @Inject constructor() : HelpDeskRepository, BaseApiResponse() {

    override fun getConfig(): Flow<ApiResult<HelpDeskConfigResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).getHelpDeskConfig() })
    }.flowOn(Dispatchers.IO)

    override fun getFaqs(issueType: String?, search: String?): Flow<ApiResult<HelpDeskFaqsResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).getHelpDeskFaqs(issueType, search) })
    }.flowOn(Dispatchers.IO)

    override fun getTutorials(issueType: String): Flow<ApiResult<HelpDeskTutorialsResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).getHelpDeskTutorials(issueType) })
    }.flowOn(Dispatchers.IO)

    override fun submitEnquiry(
        name: RequestBody,
        subject: RequestBody,
        message: RequestBody,
        issueType: RequestBody,
        problem: RequestBody?,
        email: RequestBody?,
        mobile: RequestBody?,
        photo: MultipartBody.Part?,
    ): Flow<ApiResult<EnquirySubmitResponse>> = flow {
        emit(
            safeApiCall {
                RetrofitApiService.create(BASE_URL).submitEnquiryWithPhoto(
                    name = name,
                    subject = subject,
                    message = message,
                    issueType = issueType,
                    problem = problem,
                    email = email,
                    mobile = mobile,
                    photo = photo,
                )
            },
        )
    }.flowOn(Dispatchers.IO)
}
