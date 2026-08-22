package com.srijeesolution.rojgaarwaala.domain.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.AttendanceListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeDashboardResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeePaymentsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.FactoryTermsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.MonthlySummaryResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchResponse
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import kotlinx.coroutines.flow.Flow

interface EmployeeAttendanceRepository {
    fun getDashboard(): Flow<ApiResult<EmployeeDashboardResponse>>

    fun punchIn(request: PunchRequest): Flow<ApiResult<PunchResponse>>

    fun punchOut(request: PunchRequest): Flow<ApiResult<PunchResponse>>

    fun getAttendance(month: String): Flow<ApiResult<AttendanceListResponse>>

    fun getMonthlySummary(month: String): Flow<ApiResult<MonthlySummaryResponse>>

    fun getPayments(month: String?): Flow<ApiResult<EmployeePaymentsResponse>>

    fun getFactoryTerms(): Flow<ApiResult<FactoryTermsResponse>>
}
