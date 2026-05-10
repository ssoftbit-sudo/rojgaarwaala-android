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

    fun updateApplicationStatus(applicationId: String, status: String, paymentId: String?) {
        viewModelScope.launch {
            try {
                val updateData = hashMapOf<String, Any>(
                    "status" to status,
                    "paymentId" to (paymentId ?: ""),
                    "paidAt" to System.currentTimeMillis()
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
}