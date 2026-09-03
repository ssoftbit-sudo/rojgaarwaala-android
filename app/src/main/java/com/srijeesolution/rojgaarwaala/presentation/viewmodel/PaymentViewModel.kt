package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srijeesolution.rojgaarwaala.data.remote.model.VerifyPaymentRequest
import com.srijeesolution.rojgaarwaala.domain.repository.JobApplicationRepository
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.utils.PaymentErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the hosted payment page flow.
 *
 * The app only ever asks the server what happened; it never decides that a
 * payment succeeded on its own. The redirect back from the gateway is a hint to
 * go and check, nothing more.
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
  private val repository: JobApplicationRepository,
) : ViewModel() {

  private val _state = MutableLiveData<PaymentState>(PaymentState.Idle)
  val state: LiveData<PaymentState> = _state

  /** Set once the order call succeeds, so a resumed screen knows what to verify. */
  var currentOrderId: String? = null
    private set

  /** True between opening the payment page and settling the outcome. */
  var awaitingGatewayResult: Boolean = false
    private set

  fun startPayment(applicationId: Int) {
    if (_state.value is PaymentState.Preparing) return

    _state.value = PaymentState.Preparing
    viewModelScope.launch {
      repository.createPaymentOrder(applicationId).collectLatest { result ->
        when (result) {
          is ApiResult.Loading -> Unit

          is ApiResult.Success -> {
            val body = result.data
            val data = body?.data

            when {
              body?.status != true -> {
                _state.value = PaymentState.Error(body?.message ?: "Could not start the payment.")
              }

              data?.alreadyPaid == true -> {
                _state.value = PaymentState.Paid
              }

              !data?.paymentLink.isNullOrBlank() -> {
                currentOrderId = data?.orderId
                awaitingGatewayResult = true
                _state.value = PaymentState.OpenPaymentPage(data?.paymentLink.orEmpty())
              }

              else -> {
                _state.value = PaymentState.Error("Payment page is unavailable right now.")
              }
            }
          }

          is ApiResult.Error -> {
            _state.value = PaymentState.Error(PaymentErrorMapper.message(result.message))
          }
        }
      }
    }
  }

  /**
   * Asks the server for the real outcome. Retried a few times because the
   * customer can return before the gateway has finished settling.
   */
  fun verifyPayment(applicationId: Int, attempt: Int = 1) {
    if (_state.value is PaymentState.Verifying && attempt == 1) return

    _state.value = PaymentState.Verifying
    viewModelScope.launch {
      repository.verifyPayment(applicationId, VerifyPaymentRequest(currentOrderId))
        .collectLatest { result ->
          val data = (result as? ApiResult.Success)?.data?.data
          val reachedServer = result is ApiResult.Success

          if (data?.paid == true) {
            awaitingGatewayResult = false
            currentOrderId = null
            _state.value = PaymentState.Paid
            return@collectLatest
          }

          if (attempt < MAX_VERIFY_ATTEMPTS) {
            delay(VERIFY_RETRY_DELAY_MS)
            verifyPayment(applicationId, attempt + 1)
            return@collectLatest
          }

          awaitingGatewayResult = false
          _state.value = if (reachedServer) {
            PaymentState.NotPaid(data?.reason)
          } else {
            PaymentState.Error("Could not confirm the payment. Check My Applications in a minute.")
          }
        }
    }
  }

  /** The customer dismissed the payment page without a redirect. */
  fun onPaymentPageDismissed() {
    if (_state.value is PaymentState.OpenPaymentPage) {
      _state.value = PaymentState.Idle
    }
  }

  fun onPaymentPageLaunched() {
    if (_state.value is PaymentState.OpenPaymentPage) {
      _state.value = PaymentState.AwaitingResult
    }
  }

  sealed interface PaymentState {
    data object Idle : PaymentState

    data object Preparing : PaymentState

    /** One-shot instruction to open the hosted page in a Custom Tab. */
    data class OpenPaymentPage(val url: String) : PaymentState

    data object AwaitingResult : PaymentState

    data object Verifying : PaymentState

    data object Paid : PaymentState

    data class NotPaid(val reason: String?) : PaymentState

    data class Error(val message: String) : PaymentState
  }

  private companion object {
    const val MAX_VERIFY_ATTEMPTS = 3
    const val VERIFY_RETRY_DELAY_MS = 2500L
  }
}
