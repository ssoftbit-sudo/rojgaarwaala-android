package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srijeesolution.rojgaarwaala.data.remote.model.HomePagBaseApiModel
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
    private var _homepageLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val homepageLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _homepageLiveData
    private var _profileUpdateLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val profileUpdateLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _profileUpdateLiveData
    private var _jobSubmitLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val jobSubmitLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _jobSubmitLiveData
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
}