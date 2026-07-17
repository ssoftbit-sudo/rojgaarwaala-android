package com.srijeesolution.rojgaarwaala.domain.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.EnquirySubmitResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskFaqsResponse
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import kotlinx.coroutines.flow.Flow

interface HelpDeskRepository {
    fun getFaqs(): Flow<ApiResult<HelpDeskFaqsResponse>>

    fun submitEnquiry(payload: HashMap<String, String>): Flow<ApiResult<EnquirySubmitResponse>>
}
