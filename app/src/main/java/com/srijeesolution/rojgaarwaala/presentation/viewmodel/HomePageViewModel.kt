package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srijeesolution.rojgaarwaala.data.remote.model.HomePagBaseApiModel
import com.srijeesolution.rojgaarwaala.data.remote.model.VideoDetailsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.JobListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.CategoryVideosResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideosListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.ImagesApiResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.StoriesResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.ActiveStoriesResponse
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
    private var _cityListLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val cityListLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _cityListLiveData
    private var _videoDetailsLiveData : MutableLiveData<ApiResult<VideoDetailsResponse>> = MutableLiveData()
    val videoDetailsLiveData : LiveData<ApiResult<VideoDetailsResponse>> = _videoDetailsLiveData
    private var _jobListLiveData : MutableLiveData<ApiResult<JobListResponse>> = MutableLiveData()
    val jobListLiveData : LiveData<ApiResult<JobListResponse>> = _jobListLiveData
    private var _topVideosLiveData : MutableLiveData<ApiResult<TopVideosListResponse>> = MutableLiveData()
    val topVideosLiveData : LiveData<ApiResult<TopVideosListResponse>> = _topVideosLiveData
    private var _categoryVideosLiveData : MutableLiveData<ApiResult<CategoryVideosResponse>> = MutableLiveData()
    val categoryVideosLiveData : LiveData<ApiResult<CategoryVideosResponse>> = _categoryVideosLiveData
    private var _likeVideoLiveData : MutableLiveData<ApiResult<com.srijeesolution.rojgaarwaala.data.remote.model.VideoLikeApiModel>> = MutableLiveData()
    val likeVideoLiveData : LiveData<ApiResult<com.srijeesolution.rojgaarwaala.data.remote.model.VideoLikeApiModel>> = _likeVideoLiveData
    private var _unlikeVideoLiveData : MutableLiveData<ApiResult<com.srijeesolution.rojgaarwaala.data.remote.model.VideoLikeApiModel>> = MutableLiveData()
    val unlikeVideoLiveData : LiveData<ApiResult<com.srijeesolution.rojgaarwaala.data.remote.model.VideoLikeApiModel>> = _unlikeVideoLiveData
    private var _removeVideoReactionLiveData : MutableLiveData<ApiResult<com.srijeesolution.rojgaarwaala.data.remote.model.VideoLikeApiModel>> = MutableLiveData()
    val removeVideoReactionLiveData : LiveData<ApiResult<com.srijeesolution.rojgaarwaala.data.remote.model.VideoLikeApiModel>> = _removeVideoReactionLiveData
    private var _incrementViewLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val incrementViewLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _incrementViewLiveData
    private var _deleteJobLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val deleteJobLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _deleteJobLiveData
    private var _updateJobLiveData : MutableLiveData<ApiResult<HomePagBaseApiModel>> = MutableLiveData()
    val updateJobLiveData : LiveData<ApiResult<HomePagBaseApiModel>> = _updateJobLiveData
    private var _scheduledImagesLiveData : MutableLiveData<ApiResult<ImagesApiResponse>> = MutableLiveData()
    val scheduledImagesLiveData : LiveData<ApiResult<ImagesApiResponse>> = _scheduledImagesLiveData
    private var _imageListLiveData : MutableLiveData<ApiResult<ImageListResponse>> = MutableLiveData()
    val imageListLiveData : LiveData<ApiResult<ImageListResponse>> = _imageListLiveData
    private var _storiesLiveData : MutableLiveData<ApiResult<StoriesResponse>> = MutableLiveData()
    val storiesLiveData : LiveData<ApiResult<StoriesResponse>> = _storiesLiveData
    private var _activeStoriesLiveData : MutableLiveData<ApiResult<ActiveStoriesResponse>> = MutableLiveData()
    val activeStoriesLiveData : LiveData<ApiResult<ActiveStoriesResponse>> = _activeStoriesLiveData
    private val _hasUnseenStoriesLiveData = MutableLiveData(false)
    val hasUnseenStoriesLiveData: LiveData<Boolean> = _hasUnseenStoriesLiveData

    private var lastKnownStoryIds: Set<Int> = emptySet()
    private var storiesNavBadgeDismissed = false
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

    fun updateProfileMultipart(
        name: String,
        mobile: String,
        email: String,
        city: String,
        state: String,
        pincode: String,
        district: String,
        colony: String,
        preferredJobCategory: String,
        resumePart: okhttp3.MultipartBody.Part?,
    ) {
        viewModelScope.launch {
            homePageRepository.updateProfileMultipart(
                name, mobile, email, city, state, pincode,
                district, colony, preferredJobCategory, resumePart
            ).collectLatest {
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
        logoFile: okhttp3.MultipartBody.Part?,
        locations: List<String> = emptyList(),
    ) {
        viewModelScope.launch {
            homePageRepository.onSubmitJobWithFiles(
                jobTitle, jobDescription, jobCategory, jobResponsibility,
                pdfFile, imageFile, logoFile, locations
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

    fun getCityList() {
        viewModelScope.launch {
            homePageRepository.getCityList().collectLatest {
                _cityListLiveData.postValue(it)
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

    fun getCategoryVideos(id: Int, page: Int = 1, perPage: Int = 20) {
        viewModelScope.launch {
            homePageRepository.getCategoryVideos(id, page, perPage).collectLatest{
                _categoryVideosLiveData.postValue(it)
            }
        }
    }

    fun getTopVideos(page: Int = 1, perPage: Int = 20) {
        viewModelScope.launch {
            homePageRepository.getTopVideos(page, perPage).collectLatest {
                _topVideosLiveData.postValue(it)
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

    fun removeVideoReaction(videoId: Int) {
        viewModelScope.launch {
            homePageRepository.removeVideoReaction(videoId).collectLatest {
                _removeVideoReactionLiveData.postValue(it)
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

    fun getScheduledImagesGrouped() {
        viewModelScope.launch {
            homePageRepository.getScheduledImagesGrouped().collectLatest{
                _scheduledImagesLiveData.postValue(it)
            }
        }
    }

    fun getScheduledImages() {
        viewModelScope.launch {
            homePageRepository.getScheduledImages().collectLatest{
                _imageListLiveData.postValue(it)
            }
        }
    }

    private var sectionStoriesFetchedAt = 0L
    private var activeStoriesFetchedAt = 0L

    fun getSectionStoriesGrouped(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        val cached = _storiesLiveData.value
        val hasCachedData = cached is ApiResult.Success &&
            cached.data?.data?.timeGroups?.any { !it.stories.isNullOrEmpty() } == true
        if (!forceRefresh && hasCachedData && now - sectionStoriesFetchedAt < SECTION_STORIES_CACHE_MS) {
            return
        }
        viewModelScope.launch {
            homePageRepository.getSectionStoriesGrouped().collectLatest {
                if (it is ApiResult.Success) {
                    sectionStoriesFetchedAt = System.currentTimeMillis()
                }
                _storiesLiveData.postValue(it)
            }
        }
    }

    fun getActiveStories(deviceKey: String, forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && now - activeStoriesFetchedAt < ACTIVE_STORIES_CACHE_MS) {
            return
        }
        viewModelScope.launch {
            homePageRepository.getActiveStories(deviceKey).collectLatest {
                if (it is ApiResult.Success) {
                    activeStoriesFetchedAt = System.currentTimeMillis()
                    processActiveStoriesResponse(it.data)
                }
                _activeStoriesLiveData.postValue(it)
            }
        }
    }

    fun dismissStoriesNavBadge() {
        storiesNavBadgeDismissed = true
        _hasUnseenStoriesLiveData.postValue(false)
    }

    private fun processActiveStoriesResponse(response: ActiveStoriesResponse?) {
        val stories = response?.data?.stories.orEmpty()
        val currentIds = stories.mapNotNull { it.id }.toSet()
        val hasNewStories = currentIds.any { it !in lastKnownStoryIds }
        lastKnownStoryIds = currentIds

        if (hasNewStories && response?.data?.hasUnseen == true) {
            storiesNavBadgeDismissed = false
        }

        val hasUnseen = response?.data?.hasUnseen == true && !storiesNavBadgeDismissed
        _hasUnseenStoriesLiveData.postValue(hasUnseen)
    }

    private fun updateLocalStorySeen(storyId: Int) {
        val current = _activeStoriesLiveData.value as? ApiResult.Success ?: return
        val response = current.data ?: return
        val data = response.data ?: return
        val updatedStories = data.stories?.map { story ->
            if (story.id == storyId) story.copy(seen = true) else story
        } ?: return
        val hasUnseen = updatedStories.any { it.seen != true }
        val updatedResponse = response.copy(
            data = data.copy(stories = updatedStories, hasUnseen = hasUnseen)
        )
        _activeStoriesLiveData.postValue(ApiResult.Success(updatedResponse))
        if (!storiesNavBadgeDismissed) {
            _hasUnseenStoriesLiveData.postValue(hasUnseen)
        }
    }

    fun preloadStories(deviceKey: String) {
        getSectionStoriesGrouped()
        getActiveStories(deviceKey)
    }

    companion object {
        private const val SECTION_STORIES_CACHE_MS = 10 * 60 * 1000L
        private const val ACTIVE_STORIES_CACHE_MS = 2 * 60 * 1000L
    }

    fun markStoryViewed(storyId: Int, deviceKey: String) {
        updateLocalStorySeen(storyId)
        viewModelScope.launch {
            homePageRepository.markStoryViewed(storyId, deviceKey).collectLatest { }
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
        logoFile: okhttp3.MultipartBody.Part?,
        locations: List<String> = emptyList(),
    ) {
        viewModelScope.launch {
            homePageRepository.updateJobWithFiles(
                id, jobTitle, jobDescription, jobCategory, jobResponsibility,
                pdfFile, imageFile, logoFile, locations
            ).collectLatest{
                _updateJobLiveData.postValue(it)
            }
        }
    }
}