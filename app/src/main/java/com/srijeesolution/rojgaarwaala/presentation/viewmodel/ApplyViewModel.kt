package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srijeesolution.rojgaarwaala.domain.repository.JobApplicationRepository
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class ApplyViewModel @Inject constructor(
  @ApplicationContext private val appContext: Context,
  private val repository: JobApplicationRepository,
) : ViewModel() {

  private val _submitResult = MutableLiveData<Boolean>()
  val submitResult: LiveData<Boolean> = _submitResult

  private val _isLoading = MutableLiveData<Boolean>()
  val isLoading: LiveData<Boolean> = _isLoading

  private val _applicationId = MutableLiveData<String>()
  val applicationId: LiveData<String> = _applicationId

  private val _razorpayKeyId = MutableLiveData<String>()
  val razorpayKeyId: LiveData<String> = _razorpayKeyId

  private val _amountPaise = MutableLiveData<Int>()
  val amountPaise: LiveData<Int> = _amountPaise

  private val _errorMessage = MutableLiveData<String>()
  val errorMessage: LiveData<String> = _errorMessage

  fun submitApplication(
    videoId: Int?,
    scheduledImageId: Int?,
    jobTitle: String?,
    name: String,
    phone: String,
    email: String,
    resumeUri: Uri?,
  ) {
    if (resumeUri == null) {
      _errorMessage.value = "Please select your resume."
      _submitResult.value = false
      return
    }

    _isLoading.value = true

    viewModelScope.launch {
      try {
        val resumePart = uriToResumePart(resumeUri)
        val fullName = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val phoneBody = phone.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val videoBody = videoId?.takeIf { it > 0 }?.toString()
          ?.toRequestBody("text/plain".toMediaTypeOrNull())
        val imageBody = scheduledImageId?.takeIf { it > 0 }?.toString()
          ?.toRequestBody("text/plain".toMediaTypeOrNull())
        val titleBody = jobTitle?.takeIf { it.isNotBlank() }
          ?.toRequestBody("text/plain".toMediaTypeOrNull())

        repository.submitApplication(
          fullName = fullName,
          phone = phoneBody,
          email = emailBody,
          videoId = videoBody,
          scheduledImageId = imageBody,
          jobTitle = titleBody,
          resume = resumePart,
        ).collectLatest { result ->
          when (result) {
            is ApiResult.Success -> {
              val data = result.data?.data
              val appId = data?.application?.id ?: data?.applicationId
              if (appId == null || result.data?.status != true) {
                _errorMessage.value = result.data?.message ?: "Could not save application."
                _submitResult.value = false
              } else {
                _applicationId.value = appId.toString()
                _razorpayKeyId.value = data.razorpayKeyId.orEmpty()
                _amountPaise.value = data.amountPaise ?: data.application?.amountPaise ?: 10000
                _submitResult.value = true
              }
            }
            is ApiResult.Error -> {
              _errorMessage.value = "Application submit failed. Please try again."
              _submitResult.value = false
            }
          }
          _isLoading.value = false
        }
      } catch (e: Exception) {
        Log.e("ApplyViewModel", "submitApplication failed", e)
        _errorMessage.value = "Application submit failed. Please try again."
        _submitResult.value = false
        _isLoading.value = false
      }
    }
  }

  private fun uriToResumePart(uri: Uri): MultipartBody.Part {
    val resolver = appContext.contentResolver
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    val bytes = resolver.openInputStream(uri)!!.use { it.readBytes() }
    val fileName = resolver.query(uri, null, null, null, null)?.use { cursor ->
      val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
    } ?: "resume"
    val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("resume", fileName, body)
  }
}
