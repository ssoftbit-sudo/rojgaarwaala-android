package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor() : ViewModel() {

    private val _updateResult = MutableLiveData<Boolean>()
    val updateResult: LiveData<Boolean> = _updateResult

    private val firestore = FirebaseFirestore.getInstance()

    fun updateApplicationAfterPayment(applicationId: String, paymentId: String?) {
        viewModelScope.launch {
            try {
                val updateData = hashMapOf<String, Any>(
                    // After successful payment, resume can be treated as forwarded to HR queue.
                    "status" to "pending",
                    "paymentStatus" to "paid",
                    "paymentId" to (paymentId ?: ""),
                    "paidAt" to System.currentTimeMillis(),
                    "hrForwardedAt" to System.currentTimeMillis()
                )

                firestore.collection("applications")
                    .document(applicationId)
                    .update(updateData)
                    .await()

                _updateResult.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _updateResult.value = false
            }
        }
    }

    fun updatePaymentFailure(applicationId: String) {
        if (applicationId.isBlank()) return
        viewModelScope.launch {
            try {
                firestore.collection("applications")
                    .document(applicationId)
                    .update(
                        mapOf(
                            "paymentStatus" to "failed",
                            "status" to "payment_failed",
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
            } catch (_: Exception) {
                // Best-effort update; do not block UI flow on failure.
            }
        }
    }
}