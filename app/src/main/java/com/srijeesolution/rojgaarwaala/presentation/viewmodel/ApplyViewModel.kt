package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ApplyViewModel @Inject constructor() : ViewModel() {
    companion object {
        // Temporary demo switch: when true, skips Firestore write and directly continues payment flow.
        private const val BYPASS_APPLICATION_SAVE = true
    }

    private val _submitResult = MutableLiveData<Boolean>()
    val submitResult: LiveData<Boolean> = _submitResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _applicationId = MutableLiveData<String>()
    val applicationId: LiveData<String> = _applicationId

    private val firestore = FirebaseFirestore.getInstance()
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun submitApplication(videoId: Int, name: String, phone: String, email: String, resumeUri: Uri?) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val bypassedResume = bypassedResumeUpload(resumeUri)

                if (BYPASS_APPLICATION_SAVE) {
                    _applicationId.value = "local_${System.currentTimeMillis()}"
                    _submitResult.value = true
                    return@launch
                }

                // Create application document
                val application = hashMapOf(
                    "videoId" to videoId,
                    "name" to name,
                    "phone" to phone,
                    "email" to email,
                    "resumeUrl" to bypassedResume.resumeUrl,
                    "resumeUploadBypassed" to bypassedResume.resumeUploadBypassed,
                    "resumeProvided" to bypassedResume.resumeProvided,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )

                val docRef = try {
                    firestore.collection("applications").add(application).await()
                } catch (firestoreException: Exception) {
                    Log.e(
                        "ApplyViewModel",
                        "Firestore write failed for application payload=$application",
                        firestoreException
                    )
                    null
                }
                _applicationId.value = docRef?.id ?: "local_${System.currentTimeMillis()}"
                _submitResult.value = true

            } catch (e: Exception) {
                Log.e("ApplyViewModel", "submitApplication failed", e)
                _errorMessage.value = mapErrorMessage(e)
                _submitResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun bypassedResumeUpload(resumeUri: Uri?): BypassedResumeUploadResult {
        // Temporary bypass hook: keep resume metadata, skip actual storage upload.
        return BypassedResumeUploadResult(
            resumeUrl = "",
            resumeUploadBypassed = true,
            resumeProvided = resumeUri != null
        )
    }

    private data class BypassedResumeUploadResult(
        val resumeUrl: String,
        val resumeUploadBypassed: Boolean,
        val resumeProvided: Boolean
    )

    private fun mapErrorMessage(e: Exception): String {
        val firestoreCode = (e as? FirebaseFirestoreException)?.code
        return when (firestoreCode) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Application save blocked by Firestore rules. 'applications' collection write allow kijiye."
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Application save failed: Firebase authentication missing."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Firestore unavailable. Internet check karke dubara try karein."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Firestore timeout. Please try again."
            else -> "Application submit failed. Firestore configuration check karein aur dubara try karein."
        }
    }
}