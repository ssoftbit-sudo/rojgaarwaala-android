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
class StatusViewModel @Inject constructor() : ViewModel() {

    private val _applicationStatus = MutableLiveData<String>()
    val applicationStatus: LiveData<String> = _applicationStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val firestore = FirebaseFirestore.getInstance()

    fun getApplicationStatus(applicationId: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val doc = firestore.collection("applications")
                    .document(applicationId)
                    .get()
                    .await()

                val status = doc.getString("status") ?: "unknown"
                _applicationStatus.value = status

            } catch (e: Exception) {
                e.printStackTrace()
                _applicationStatus.value = "error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}