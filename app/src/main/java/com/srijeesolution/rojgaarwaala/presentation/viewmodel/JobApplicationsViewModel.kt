package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srijeesolution.rojgaarwaala.data.remote.model.JobApplicationDto
import com.srijeesolution.rojgaarwaala.domain.repository.JobApplicationRepository
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobApplicationsViewModel @Inject constructor(
    private val repository: JobApplicationRepository,
    private val sharedPrefs: SharedPrefs,
) : ViewModel() {

    private val _applications = MutableLiveData<List<JobApplicationDto>>(emptyList())
    val applications: LiveData<List<JobApplicationDto>> = _applications

    private val _badgeCount = MutableLiveData(0)
    val badgeCount: LiveData<Int> = _badgeCount

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun refreshApplications(isLoggedIn: Boolean) {
        if (!isLoggedIn) {
            _applications.value = emptyList()
            _badgeCount.value = 0
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            repository.getMyApplications().collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val apps = result.data?.data?.applications.orEmpty()
                        _applications.value = apps
                        _errorMessage.value = null
                        updateBadgeCount(apps.size)
                    }
                    is ApiResult.Error -> {
                        _errorMessage.value = result.message?.errorMsg ?: "Could not load applications"
                    }
                    is ApiResult.Loading -> Unit
                }
                _isLoading.value = false
            }
        }
    }

    fun markJobStatusSeen() {
        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.JOB_STATUS_UPDATE_PENDING, false))
        _badgeCount.value = 0
    }

    fun notifyJobStatusUpdated() {
        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.JOB_STATUS_UPDATE_PENDING, true))
        val currentCount = _badgeCount.value ?: 0
        if (currentCount <= 0) {
            _badgeCount.value = (_applications.value?.size ?: 1).coerceAtLeast(1)
        }
    }

    private fun updateBadgeCount(applicationCount: Int) {
        val pending = sharedPrefs.getPrefs(SharedPrefsConstant.JOB_STATUS_UPDATE_PENDING, false)
        _badgeCount.value = if (pending && applicationCount > 0) applicationCount else 0
    }
}
