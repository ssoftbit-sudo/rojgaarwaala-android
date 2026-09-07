package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmployeeAttendanceViewModel @Inject constructor(
    private val employeeAttendanceRepository: EmployeeAttendanceRepository,
) : ViewModel() {

    private val _dashboardLiveData = MutableLiveData<ApiResult<EmployeeDashboardResponse>>()
    val dashboardLiveData: LiveData<ApiResult<EmployeeDashboardResponse>> = _dashboardLiveData

    private val _punchInLiveData = MutableLiveData<ApiResult<PunchResponse>>()
    val punchInLiveData: LiveData<ApiResult<PunchResponse>> = _punchInLiveData

    private val _punchOutLiveData = MutableLiveData<ApiResult<PunchResponse>>()
    val punchOutLiveData: LiveData<ApiResult<PunchResponse>> = _punchOutLiveData

    private val _attendanceLiveData = MutableLiveData<ApiResult<AttendanceListResponse>>()
    val attendanceLiveData: LiveData<ApiResult<AttendanceListResponse>> = _attendanceLiveData

    private val _monthlySummaryLiveData = MutableLiveData<ApiResult<MonthlySummaryResponse>>()
    val monthlySummaryLiveData: LiveData<ApiResult<MonthlySummaryResponse>> = _monthlySummaryLiveData

    private val _paymentsLiveData = MutableLiveData<ApiResult<EmployeePaymentsResponse>>()
    val paymentsLiveData: LiveData<ApiResult<EmployeePaymentsResponse>> = _paymentsLiveData

    private val _factoryTermsLiveData = MutableLiveData<ApiResult<FactoryTermsResponse>>()
    val factoryTermsLiveData: LiveData<ApiResult<FactoryTermsResponse>> = _factoryTermsLiveData

    private val _acceptTermsLiveData = MutableLiveData<ApiResult<AcceptTermsResponse>>()
    val acceptTermsLiveData: LiveData<ApiResult<AcceptTermsResponse>> = _acceptTermsLiveData

    private val _teamLiveData = MutableLiveData<ApiResult<TeamResponse>>()
    val teamLiveData: LiveData<ApiResult<TeamResponse>> = _teamLiveData

    private val _bulkPunchLiveData = MutableLiveData<ApiResult<BulkPunchResponse>>()
    val bulkPunchLiveData: LiveData<ApiResult<BulkPunchResponse>> = _bulkPunchLiveData

    private val _otRequestLiveData = MutableLiveData<ApiResult<OtRequestResponse>>()
    val otRequestLiveData: LiveData<ApiResult<OtRequestResponse>> = _otRequestLiveData

    private val _missedPunchLiveData = MutableLiveData<ApiResult<MissedPunchResponse>>()
    val missedPunchLiveData: LiveData<ApiResult<MissedPunchResponse>> = _missedPunchLiveData

    fun loadDashboard() {
        _dashboardLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.getDashboard().collectLatest {
                _dashboardLiveData.postValue(it)
            }
        }
    }

    fun punchIn(latitude: Double, longitude: Double, accuracy: Double) {
        _punchInLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.punchIn(PunchRequest(latitude, longitude, accuracy))
                .collectLatest { _punchInLiveData.postValue(it) }
        }
    }

    fun punchOut(latitude: Double, longitude: Double, accuracy: Double) {
        _punchOutLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.punchOut(PunchRequest(latitude, longitude, accuracy))
                .collectLatest { _punchOutLiveData.postValue(it) }
        }
    }

    fun loadAttendance(month: String) {
        _attendanceLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.getAttendance(month).collectLatest {
                _attendanceLiveData.postValue(it)
            }
        }
    }

    fun loadMonthlySummary(month: String) {
        _monthlySummaryLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.getMonthlySummary(month).collectLatest {
                _monthlySummaryLiveData.postValue(it)
            }
        }
    }

    fun loadPayments(month: String? = null) {
        _paymentsLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.getPayments(month).collectLatest {
                _paymentsLiveData.postValue(it)
            }
        }
    }

    fun loadFactoryTerms() {
        _factoryTermsLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.getFactoryTerms().collectLatest {
                _factoryTermsLiveData.postValue(it)
            }
        }
    }

    fun acceptFactoryTerms() {
        _acceptTermsLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.acceptFactoryTerms().collectLatest {
                _acceptTermsLiveData.postValue(it)
            }
        }
    }

    fun loadTeam() {
        _teamLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.getTeam().collectLatest { _teamLiveData.postValue(it) }
        }
    }

    fun bulkPunchIn(employeeIds: List<Int>, latitude: Double, longitude: Double, accuracy: Double) {
        _bulkPunchLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.bulkPunchIn(
                BulkPunchRequest(employeeIds, latitude, longitude, accuracy),
            ).collectLatest { _bulkPunchLiveData.postValue(it) }
        }
    }

    fun submitOtRequest(hours: Int, reason: String) {
        _otRequestLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.submitOtRequest(OtRequestBody(hours, reason))
                .collectLatest { _otRequestLiveData.postValue(it) }
        }
    }

    fun submitMissedPunch(punchType: String, reason: String, workDate: String? = null) {
        _missedPunchLiveData.value = ApiResult.Loading()
        viewModelScope.launch {
            employeeAttendanceRepository.submitMissedPunch(
                MissedPunchBody(punchType = punchType, reason = reason, workDate = workDate),
            )
                .collectLatest { _missedPunchLiveData.postValue(it) }
        }
    }
}
