package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ApplyViewModel @Inject constructor() : ViewModel() {

    private val _submitResult = MutableLiveData<Boolean>()
    val submitResult: LiveData<Boolean> = _submitResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _applicationId = MutableLiveData<String>()
    val applicationId: LiveData<String> = _applicationId

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun submitApplication(videoId: Int, name: String, phone: String, email: String, resumeUri: Uri) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Upload resume to Firebase Storage
                val resumeUrl = uploadResume(resumeUri, videoId, name)

                // Create application document
                val application = hashMapOf(
                    "videoId" to videoId,
                    "name" to name,
                    "phone" to phone,
                    "email" to email,
                    "resumeUrl" to resumeUrl,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )

                val docRef = firestore.collection("applications").add(application).await()
                _applicationId.value = docRef.id
                _submitResult.value = true

            } catch (e: Exception) {
                e.printStackTrace()
                _submitResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadResume(uri: Uri, videoId: Int, name: String): String {
        val fileName = "resume_${videoId}_${name}_${System.currentTimeMillis()}.pdf"
        val storageRef = storage.reference.child("resumes/$fileName")

        storageRef.putFile(uri).await()
        return storageRef.downloadUrl.await().toString()
    }
}