package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srijeesolution.rojgaarwaala.data.remote.model.ConfirmPaymentRequest
import com.srijeesolution.rojgaarwaala.domain.repository.JobApplicationRepository
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
  private val repository: JobApplicationRepository,
) : ViewModel() {

  private val _updateResult = MutableLiveData<Boolean>()
  val updateResult: LiveData<Boolean> = _updateResult

  private val _orderReady = MutableLiveData<PaymentOrderState?>()
  val orderReady: LiveData<PaymentOrderState?> = _orderReady

  private val _isLoading = MutableLiveData<Boolean>()
  val isLoading: LiveData<Boolean> = _isLoading

  private val _errorMessage = MutableLiveData<String>()
  val errorMessage: LiveData<String> = _errorMessage

  fun createRazorpayOrder(applicationId: Int) {
    _isLoading.value = true
    viewModelScope.launch {
      repository.createRazorpayOrder(applicationId).collectLatest { result ->
        when (result) {
          is ApiResult.Success -> {
            val data = result.data?.data
            if (result.data?.status == true && !data?.orderId.isNullOrBlank()) {
              _orderReady.value = PaymentOrderState(
                orderId = data?.orderId.orEmpty(),
                razorpayKeyId = data?.razorpayKeyId.orEmpty(),
                amountPaise = data?.amountPaise ?: 10000,
              )
            } else {
              _errorMessage.value = result.data?.message ?: "Could not start payment."
              _orderReady.value = null
            }
          }
          is ApiResult.Error -> {
            _errorMessage.value = "Could not create payment order."
            _orderReady.value = null
          }
        }
        _isLoading.value = false
      }
    }
  }

  fun updateApplicationAfterPayment(
    applicationId: Int,
    paymentId: String?,
    orderId: String?,
  ) {
    if (paymentId.isNullOrBlank()) {
      _updateResult.value = false
      return
    }

    _isLoading.value = true
    viewModelScope.launch {
      repository.confirmPayment(
        applicationId,
        ConfirmPaymentRequest(
          razorpayPaymentId = paymentId,
          razorpayOrderId = orderId,
        ),
      ).collectLatest { result ->
        _updateResult.value = result is ApiResult.Success && result.data?.status == true
        _isLoading.value = false
      }
    }
  }

  fun updatePaymentFailure(applicationId: Int) {
    viewModelScope.launch {
      repository.markPaymentFailed(applicationId).collectLatest { }
    }
  }

  data class PaymentOrderState(
    val orderId: String,
    val razorpayKeyId: String,
    val amountPaise: Int,
  )
}
