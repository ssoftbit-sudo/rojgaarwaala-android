package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srijeesolution.rojgaarwaala.data.remote.model.VerifyPaymentRequest
import com.srijeesolution.rojgaarwaala.domain.repository.JobApplicationRepository
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.utils.PaymentErrorMapper
import com.srijeesolution.rojgaarwaala.utils.PaymentVerifyDecision
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
    when (_state.value) {
      is PaymentState.Preparing,
      is PaymentState.Verifying,
      is PaymentState.Confirming,
      PaymentState.Paid,
      -> return
      else -> Unit
    }

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
   * Asks the server for the real outcome. Retried while Inquiry is catching up
   * with a just-completed UPI payment. A hard decline stops immediately.
   */
  fun verifyPayment(applicationId: Int, attempt: Int = 1) {
    if (attempt == 1 && (_state.value is PaymentState.Verifying || _state.value is PaymentState.Paid)) {
      return
    }

    _state.value = PaymentState.Verifying
    viewModelScope.launch {
      repository.verifyPayment(applicationId, VerifyPaymentRequest(currentOrderId))
        .collectLatest { result ->
          val data = (result as? ApiResult.Success)?.data?.data
          val reachedServer = result is ApiResult.Success

          when (
            PaymentVerifyDecision.next(
              paid = data?.paid,
              pending = data?.pending,
              reachedServer = reachedServer,
              attempt = attempt,
              maxAttempts = MAX_VERIFY_ATTEMPTS,
            )
          ) {
            PaymentVerifyDecision.Action.PAID -> {
              awaitingGatewayResult = false
              currentOrderId = null
              _state.value = PaymentState.Paid
            }

            PaymentVerifyDecision.Action.RETRY -> {
              delay(VERIFY_RETRY_DELAY_MS)
              verifyPayment(applicationId, attempt + 1)
            }

            PaymentVerifyDecision.Action.CONFIRMING -> {
              awaitingGatewayResult = false
              _state.value = PaymentState.Confirming(
                data?.reason
                  ?: "We're confirming your payment. Check My Applications in a minute. Do not pay again.",
              )
            }

            PaymentVerifyDecision.Action.NOT_PAID -> {
              awaitingGatewayResult = false
              _state.value = PaymentState.NotPaid(data?.reason)
            }
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

    /** Inquiry has not settled yet. Must not start a second charge. */
    data class Confirming(val message: String) : PaymentState

    data class Error(val message: String) : PaymentState
  }

  private companion object {
    const val MAX_VERIFY_ATTEMPTS = 10
    const val VERIFY_RETRY_DELAY_MS = 3000L
  }
}
