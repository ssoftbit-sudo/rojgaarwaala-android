package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srijeesolution.rojgaarwaala.data.remote.model.EnquirySubmitResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskConfigResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskFaqsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskTutorialsResponse
import com.srijeesolution.rojgaarwaala.domain.repository.HelpDeskRepository
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
class HelpDeskViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val helpDeskRepository: HelpDeskRepository,
) : ViewModel() {

    private val _configLiveData = MutableLiveData<ApiResult<HelpDeskConfigResponse>>()
    val configLiveData: LiveData<ApiResult<HelpDeskConfigResponse>> = _configLiveData

    private val _faqsLiveData = MutableLiveData<ApiResult<HelpDeskFaqsResponse>>()
    val faqsLiveData: LiveData<ApiResult<HelpDeskFaqsResponse>> = _faqsLiveData

    private val _tutorialsLiveData = MutableLiveData<ApiResult<HelpDeskTutorialsResponse>>()
    val tutorialsLiveData: LiveData<ApiResult<HelpDeskTutorialsResponse>> = _tutorialsLiveData

    private val _submitLiveData = MutableLiveData<ApiResult<EnquirySubmitResponse>>()
    val submitLiveData: LiveData<ApiResult<EnquirySubmitResponse>> = _submitLiveData

    fun loadConfig() {
        viewModelScope.launch {
            helpDeskRepository.getConfig().collectLatest { _configLiveData.postValue(it) }
        }
    }

    fun loadFaqs(issueType: String, search: String? = null) {
        viewModelScope.launch {
            helpDeskRepository.getFaqs(issueType, search).collectLatest { _faqsLiveData.postValue(it) }
        }
    }

    fun loadTutorials(issueType: String = "app") {
        viewModelScope.launch {
            helpDeskRepository.getTutorials(issueType).collectLatest { _tutorialsLiveData.postValue(it) }
        }
    }

    fun submitComplaint(name: String, problem: String, message: String, photoUri: Uri?) {
        viewModelScope.launch {
            val photoPart = photoUri?.let { uriToPhotoPart(it) }
            helpDeskRepository.submitEnquiry(
                name = name.toRequestBody("text/plain".toMediaTypeOrNull()),
                subject = "Help Desk: $problem".toRequestBody("text/plain".toMediaTypeOrNull()),
                message = message.toRequestBody("text/plain".toMediaTypeOrNull()),
                issueType = "other".toRequestBody("text/plain".toMediaTypeOrNull()),
                problem = problem.toRequestBody("text/plain".toMediaTypeOrNull()),
                email = null,
                mobile = null,
                photo = photoPart,
            ).collectLatest { _submitLiveData.postValue(it) }
        }
    }

    private fun uriToPhotoPart(uri: Uri): MultipartBody.Part {
        val resolver = appContext.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val bytes = resolver.openInputStream(uri)!!.use { it.readBytes() }
        val fileName = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        } ?: "screenshot.jpg"
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("photo", fileName, body)
    }
}
