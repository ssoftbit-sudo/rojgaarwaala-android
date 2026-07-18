package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srijeesolution.rojgaarwaala.data.remote.model.JobApplicationDto
import com.srijeesolution.rojgaarwaala.domain.repository.JobApplicationRepository
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
  private val repository: JobApplicationRepository,
) : ViewModel() {

  private val _applicationDetails = MutableLiveData<JobApplicationDto?>()
  val applicationDetails: LiveData<JobApplicationDto?> = _applicationDetails

  private val _isLoading = MutableLiveData<Boolean>()
  val isLoading: LiveData<Boolean> = _isLoading

  fun getApplicationDetails(applicationId: String) {
    val id = applicationId.toIntOrNull()
    if (id == null || id <= 0) {
      _applicationDetails.value = null
      return
    }

    _isLoading.value = true
    viewModelScope.launch {
      repository.getApplication(id).collectLatest { result ->
        when (result) {
          is ApiResult.Success -> {
            _applicationDetails.value = result.data?.data?.application
          }
          is ApiResult.Loading -> Unit
          is ApiResult.Error -> {
            _applicationDetails.value = null
          }
        }
        _isLoading.value = false
      }
    }
  }
}
