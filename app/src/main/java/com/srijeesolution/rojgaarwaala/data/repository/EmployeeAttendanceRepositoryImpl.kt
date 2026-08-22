package com.srijeesolution.rojgaarwaala.data.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.AttendanceListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeDashboardResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeePaymentsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.FactoryTermsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.MonthlySummaryResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchResponse
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
}
