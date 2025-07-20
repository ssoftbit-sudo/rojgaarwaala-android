package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srijeesolution.rojgaarwaala.data.remote.model.HomePagBaseApiModel
import com.srijeesolution.rojgaarwaala.data.remote.model.VideoDetailsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.JobListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.CategoryVideosResponse
import com.srijeesolution.rojgaarwaala.domain.repository.HomePageRepository
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class HomePageViewModel @Inject constructor(private val homePageRepository: HomePageRepository): ViewModel(){
    private var _loginRegisterLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val loginRegisterLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _loginRegisterLiveData
    private var _sendOtpLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val sendOtpLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _sendOtpLiveData
    private var _verifyOtpLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val verifyOtpLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _verifyOtpLiveData
    private var _homepageLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val homepageLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _homepageLiveData
    private var _profileUpdateLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val profileUpdateLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _profileUpdateLiveData
    private var _jobSubmitLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val jobSubmitLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _jobSubmitLiveData
    private var _categoriesLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val categoriesLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _categoriesLiveData
    private var _videoDetailsLiveData : MutableLiveData<ApiResult<VideoDetailsResponse>> = MutableLiveData()
    val videoDetailsLiveData : LiveData<ApiResult<VideoDetailsResponse>> = _videoDetailsLiveData
    private var _jobListLiveData : MutableLiveData<ApiResult<JobListResponse>> = MutableLiveData()
    val jobListLiveData : LiveData<ApiResult<JobListResponse>> = _jobListLiveData
    private var _categoryVideosLiveData : MutableLiveData<ApiResult<CategoryVideosResponse>> = MutableLiveData()
    val categoryVideosLiveData : LiveData<ApiResult<CategoryVideosResponse>> = _categoryVideosLiveData
    private var _likeVideoLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val likeVideoLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _likeVideoLiveData
    private var _unlikeVideoLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val unlikeVideoLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _unlikeVideoLiveData
    private var _incrementViewLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val incrementViewLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _incrementViewLiveData
    private var _deleteJobLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val deleteJobLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _deleteJobLiveData
    private var _updateJobLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val updateJobLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _updateJobLiveData
    
    fun onLoginData(email: HashMap<String, String>) {
        viewModelScope.launch {
            homePageRepository.onLoginUser(email).collectLatest{
                _loginRegisterLiveData.postValue(it)
            }
        }
    }
    fun onRegisterData(email: HashMap<String, String>) {
        viewModelScope.launch {
            homePageRepository.onRegisterData(email).collectLatest{
                _loginRegisterLiveData.postValue(it)
            }
        }
    }
    fun sendOtp(request: HashMap<String, String>) {
        viewModelScope.launch {
            homePageRepository.sendOtp(request).collectLatest{
                _sendOtpLiveData.postValue(it)
            }
        }
    }
    fun verifyOtp(request: HashMap<String, String>) {
        viewModelScope.launch {
            homePageRepository.verifyOtp(request).collectLatest{
                _verifyOtpLiveData.postValue(it)
            }
        }
    }
    fun onLogoutData() {
        viewModelScope.launch {
            homePageRepository.onLogoutData().collectLatest{
                _loginRegisterLiveData.postValue(it)
            }
        }
    }
    fun getHomePageData(searchTerm:String) {
        viewModelScope.launch {
            homePageRepository.getHomePageData(searchTerm).collectLatest{
                _homepageLiveData.postValue(it)
            }
        }
    }

    fun getProfileData() {
        viewModelScope.launch {
            homePageRepository.getProfileData().collectLatest{
                _profileUpdateLiveData.postValue(it)
            }
        }
    }
    fun updateProfileLiveData(data: HashMap<String, String>) {
        viewModelScope.launch {
            homePageRepository.updateProfileLiveData(data).collectLatest{
                _profileUpdateLiveData.postValue(it)
            }
        }
    }

    fun onSubmitJob(email: HashMap<String, String>) {
        viewModelScope.launch {
            homePageRepository.onSubmitJob(email).collectLatest{
                _jobSubmitLiveData.postValue(it)
            }
        }
    }

    fun onSubmitJobWithFiles(
        jobTitle: String,
        jobDescription: String,
        jobCategory: String,
        jobResponsibility: String,
        pdfFile: okhttp3.MultipartBody.Part?,
        imageFile: okhttp3.MultipartBody.Part?,
        logoFile: okhttp3.MultipartBody.Part?
    ) {
        viewModelScope.launch {
            homePageRepository.onSubmitJobWithFiles(
                jobTitle, jobDescription, jobCategory, jobResponsibility,
                pdfFile, imageFile, logoFile
            ).collectLatest{
                _jobSubmitLiveData.postValue(it)
            }
        }
    }

    fun getCategoriesData() {
        viewModelScope.launch {
            homePageRepository.getCategoriesData().collectLatest{
                _categoriesLiveData.postValue(it)
            }
        }
    }

    fun getVideoDetails(id: Int) {
        viewModelScope.launch {
            homePageRepository.getVideoDetails(id).collectLatest{
                _videoDetailsLiveData.postValue(it)
            }
        }
    }

    fun getJobList() {
        viewModelScope.launch {
            homePageRepository.getJobList().collectLatest{
                _jobListLiveData.postValue(it)
            }
        }
    }

    fun getCategoryVideos(id: Int) {
        viewModelScope.launch {
            homePageRepository.getCategoryVideos(id).collectLatest{
                _categoryVideosLiveData.postValue(it)
            }
        }
    }

    fun likeVideo(videoId: Int) {
        viewModelScope.launch {
            homePageRepository.likeVideo(videoId).collectLatest{
                _likeVideoLiveData.postValue(it)
            }
        }
    }

    fun unlikeVideo(videoId: Int) {
        viewModelScope.launch {
            homePageRepository.unlikeVideo(videoId).collectLatest{
                _unlikeVideoLiveData.postValue(it)
            }
        }
    }

    fun incrementVideoView(videoId: Int) {
        viewModelScope.launch {
            homePageRepository.incrementVideoView(videoId).collectLatest{
                _incrementViewLiveData.postValue(it)
            }
        }
    }

    fun deleteJob(id: Int) {
        viewModelScope.launch {
            homePageRepository.deleteJob(id).collectLatest{
                _deleteJobLiveData.postValue(it)
            }
        }
    }

    fun updateJob(id: Int, data: HashMap<String, String>) {
        viewModelScope.launch {
            homePageRepository.updateJob(id, data).collectLatest{
                _updateJobLiveData.postValue(it)
            }
        }
    }

    fun updateJobWithFiles(
        id: Int,
        jobTitle: String,
        jobDescription: String,
        jobCategory: String,
        jobResponsibility: String,
        pdfFile: okhttp3.MultipartBody.Part?,
        imageFile: okhttp3.MultipartBody.Part?,
        logoFile: okhttp3.MultipartBody.Part?
    ) {
        viewModelScope.launch {
            homePageRepository.updateJobWithFiles(
                id, jobTitle, jobDescription, jobCategory, jobResponsibility,
                pdfFile, imageFile, logoFile
            ).collectLatest{
                _updateJobLiveData.postValue(it)
            }
        }
    }
}