package com.srijeesolution.rojgaarwaala.data.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.EnquirySubmitResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskFaqsResponse
import com.srijeesolution.rojgaarwaala.domain.repository.HelpDeskRepository
import com.srijeesolution.rojgaarwaala.network.constant.NetworkBaseUrls.Companion.BASE_URL
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.network.handler.BaseApiResponse
import com.srijeesolution.rojgaarwaala.network.retorfit.RetrofitApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class HelpDeskRepositoryImpl @Inject constructor() : HelpDeskRepository, BaseApiResponse() {

    override fun getFaqs(): Flow<ApiResult<HelpDeskFaqsResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).getHelpDeskFaqs() })
    }.flowOn(Dispatchers.IO)

    override fun submitEnquiry(payload: HashMap<String, String>): Flow<ApiResult<EnquirySubmitResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).submitEnquiry(payload) })
    }.flowOn(Dispatchers.IO)
}
