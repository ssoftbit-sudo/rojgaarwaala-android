package com.srijeesolution.rojgaarwaala.domain.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.EnquirySubmitResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskConfigResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskFaqsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskTutorialsResponse
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface HelpDeskRepository {
    fun getConfig(): Flow<ApiResult<HelpDeskConfigResponse>>

    fun getFaqs(issueType: String?, search: String?): Flow<ApiResult<HelpDeskFaqsResponse>>

    fun getTutorials(issueType: String): Flow<ApiResult<HelpDeskTutorialsResponse>>

    fun submitEnquiry(
        name: RequestBody,
        subject: RequestBody,
        message: RequestBody,
        issueType: RequestBody,
        problem: RequestBody?,
        email: RequestBody?,
        mobile: RequestBody?,
        photo: MultipartBody.Part?,
    ): Flow<ApiResult<EnquirySubmitResponse>>
}
