package com.srijeesolution.rojgaarwaala.data.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.AcceptTermsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.AttendanceListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.BulkPunchRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.BulkPunchResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeDashboardResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeePaymentsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.FactoryTermsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.MissedPunchBody
import com.srijeesolution.rojgaarwaala.data.remote.model.MissedPunchResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.MonthlySummaryResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.OtRequestBody
import com.srijeesolution.rojgaarwaala.data.remote.model.OtRequestResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.TeamResponse
import com.srijeesolution.rojgaarwaala.domain.repository.EmployeeAttendanceRepository
import com.srijeesolution.rojgaarwaala.network.constant.NetworkBaseUrls.Companion.BASE_URL
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.network.handler.BaseApiResponse
import com.srijeesolution.rojgaarwaala.network.retorfit.RetrofitApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class EmployeeAttendanceRepositoryImpl @Inject constructor() :
    EmployeeAttendanceRepository, BaseApiResponse() {

    override fun getDashboard(): Flow<ApiResult<EmployeeDashboardResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).getEmployeeDashboard() })
    }.flowOn(Dispatchers.IO)

    override fun punchIn(request: PunchRequest): Flow<ApiResult<PunchResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).employeePunchIn(request) })
    }.flowOn(Dispatchers.IO)

    override fun punchOut(request: PunchRequest): Flow<ApiResult<PunchResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).employeePunchOut(request) })
    }.flowOn(Dispatchers.IO)

    override fun getAttendance(month: String): Flow<ApiResult<AttendanceListResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).getEmployeeAttendance(month) })
    }.flowOn(Dispatchers.IO)

    override fun getMonthlySummary(month: String): Flow<ApiResult<MonthlySummaryResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).getEmployeeMonthlySummary(month) })
    }.flowOn(Dispatchers.IO)

    override fun getPayments(month: String?): Flow<ApiResult<EmployeePaymentsResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).getEmployeePayments(month) })
    }.flowOn(Dispatchers.IO)

    override fun getFactoryTerms(): Flow<ApiResult<FactoryTermsResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).getEmployeeFactoryTerms() })
    }.flowOn(Dispatchers.IO)

    override fun acceptFactoryTerms(): Flow<ApiResult<AcceptTermsResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).acceptEmployeeFactoryTerms() })
    }.flowOn(Dispatchers.IO)

    override fun getTeam(): Flow<ApiResult<TeamResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).getEmployeeTeam() })
    }.flowOn(Dispatchers.IO)

    override fun bulkPunchIn(request: BulkPunchRequest): Flow<ApiResult<BulkPunchResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).employeeBulkPunchIn(request) })
    }.flowOn(Dispatchers.IO)

    override fun submitOtRequest(body: OtRequestBody): Flow<ApiResult<OtRequestResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).submitOtRequest(body) })
    }.flowOn(Dispatchers.IO)

    override fun submitMissedPunch(body: MissedPunchBody): Flow<ApiResult<MissedPunchResponse>> = flow {
        emit(safeApiCall { RetrofitApiService.create(BASE_URL).submitMissedPunch(body) })
    }.flowOn(Dispatchers.IO)
}
