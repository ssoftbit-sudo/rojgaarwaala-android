package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srijeesolution.rojgaarwaala.data.remote.model.EnquirySubmitResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskFaqsResponse
import com.srijeesolution.rojgaarwaala.domain.repository.HelpDeskRepository
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HelpDeskViewModel @Inject constructor(
    private val helpDeskRepository: HelpDeskRepository,
) : ViewModel() {

    private val _faqsLiveData = MutableLiveData<ApiResult<HelpDeskFaqsResponse>>()
    val faqsLiveData: LiveData<ApiResult<HelpDeskFaqsResponse>> = _faqsLiveData

    private val _submitLiveData = MutableLiveData<ApiResult<EnquirySubmitResponse>>()
    val submitLiveData: LiveData<ApiResult<EnquirySubmitResponse>> = _submitLiveData

    fun loadFaqs() {
        viewModelScope.launch {
            helpDeskRepository.getFaqs().collectLatest { _faqsLiveData.postValue(it) }
        }
    }

    fun submitEnquiry(payload: HashMap<String, String>) {
        viewModelScope.launch {
            helpDeskRepository.submitEnquiry(payload).collectLatest { _submitLiveData.postValue(it) }
        }
    }
}
