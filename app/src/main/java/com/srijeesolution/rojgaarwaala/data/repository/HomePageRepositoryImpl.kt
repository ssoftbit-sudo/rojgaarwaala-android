package com.srijeesolution.rojgaarwaala.data.repository

import com.srijeesolution.rojgaarwaala.data.remote.model.HomePagBaseApiModel
import com.srijeesolution.rojgaarwaala.data.remote.model.ImagesApiResponse
import com.srijeesolution.rojgaarwaala.domain.repository.HomePageRepository
import com.srijeesolution.rojgaarwaala.network.constant.NetworkBaseUrls.Companion.BASE_URL
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.network.handler.BaseApiResponse
import com.srijeesolution.rojgaarwaala.network.retorfit.RetrofitApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import javax.inject.Inject
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class HomePageRepositoryImpl @Inject constructor() : HomePageRepository, BaseApiResponse() {

    override fun onLoginUser(email: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).onLoginUser(email)}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun onRegisterData(email: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).onRegisterData(email)}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun sendOtp(request: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).sendOtp(request)}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun verifyOtp(request: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).verifyOtp(request)}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun onLogoutData(): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).onLogoutData()}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun getHomePageData(searchTerm:String): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).getHomePageData(searchTerm)}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun getProfileData(): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).getProfileData()}
            )
        }.flowOn(Dispatchers.IO)
    }
    override fun updateProfileLiveData(data: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).updateProfileLiveData(data)}
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun updateProfileMultipart(
        name: String,
        mobile: String,
        email: String,
        city: String,
        state: String,
        pincode: String,
        district: String,
        colony: String,
        preferredJobCategory: String,
        resumePart: MultipartBody.Part?,
    ): Flow<ApiResult<HomePagBaseApiModel>> {
        val text = "text/plain".toMediaTypeOrNull()
        return flow {
            emit(safeApiCall {
                RetrofitApiService.create(BASE_URL).updateProfileMultipart(
                    name = name.toRequestBody(text),
                    mobile = mobile.toRequestBody(text),
                    email = email.toRequestBody(text),
                    city = city.toRequestBody(text),
                    state = state.toRequestBody(text),
                    pincode = pincode.toRequestBody(text),
                    district = district.toRequestBody(text),
                    colony = colony.toRequestBody(text),
                    preferredJobCategory = preferredJobCategory.toRequestBody(text),
                    resume = resumePart,
                )
            })
        }.flowOn(Dispatchers.IO)
    }

    override fun onSubmitJob(data: HashMap<String, String>): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall{
                RetrofitApiService.create(BASE_URL).onSubmitJob(data)}
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun onSubmitJobWithFiles(
        jobTitle: String,
        jobDescription: String,
        jobCategory: String,
        jobResponsibility: String,
        pdfFile: MultipartBody.Part?,
        imageFile: MultipartBody.Part?,
        logoFile: MultipartBody.Part?,
        locations: List<String>,
    ): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall {
                RetrofitApiService.create(BASE_URL).onSubmitJobWithFiles(
                    jobTitle = jobTitle.toRequestBody("text/plain".toMediaTypeOrNull()),
                    jobDescription = jobDescription.toRequestBody("text/plain".toMediaTypeOrNull()),
                    jobCategory = jobCategory.toRequestBody("text/plain".toMediaTypeOrNull()),
                    jobResponsibility = jobResponsibility.toRequestBody("text/plain".toMediaTypeOrNull()),
                    pdf = pdfFile,
                    image = imageFile,
                    logo = logoFile,
                    locations = locations.map { location ->
                        MultipartBody.Part.createFormData("locations[]", location)
                    },
                )
            })
        }.flowOn(Dispatchers.IO)
    }

    override fun getCategoriesData(): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall {
                RetrofitApiService.create(BASE_URL).getCategoriesData()
            })
        }.flowOn(Dispatchers.IO)
    }

    override fun getCityList(): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall {
                RetrofitApiService.create(BASE_URL).getCityList()
            })
        }.flowOn(Dispatchers.IO)
    }

    override fun getVideoDetails(id: Int) = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getVideoDetails(id)
        })
    }.flowOn(Dispatchers.IO)

    override fun getJobList() = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getJobList()
        })
    }.flowOn(Dispatchers.IO)

    override fun getCategoryVideos(id: Int, page: Int, perPage: Int) = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getCategoryVideos(id, page, perPage)
        })
    }.flowOn(Dispatchers.IO)

    override fun getTopVideos(page: Int, perPage: Int) = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getTopVideos(page, perPage)
        })
    }.flowOn(Dispatchers.IO)

    override fun likeVideo(videoId: Int) = flow {
        val request = HashMap<String, Any>()
        request["video_id"] = videoId
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).likeVideo(request)
        })
    }.flowOn(Dispatchers.IO)

    override fun unlikeVideo(videoId: Int) = flow {
        val request = HashMap<String, Any>()
        request["video_id"] = videoId
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).unlikeVideo(request)
        })
    }.flowOn(Dispatchers.IO)

    override fun removeVideoReaction(videoId: Int) = flow {
        val request = HashMap<String, Any>()
        request["video_id"] = videoId
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).removeVideoReaction(request)
        })
    }.flowOn(Dispatchers.IO)

    override fun incrementVideoView(videoId: Int) = flow {
        val request = HashMap<String, Any>()
        request["video_id"] = videoId
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).incrementVideoView(request)
        })
    }.flowOn(Dispatchers.IO)

    override fun deleteJob(id: Int) = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).deleteJob(id)
        })
    }.flowOn(Dispatchers.IO)

    override fun updateJob(id: Int, data: HashMap<String, String>) = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).updateJob(id, data)
        })
    }.flowOn(Dispatchers.IO)

    override fun updateJobWithFiles(
        id: Int,
        jobTitle: String,
        jobDescription: String,
        jobCategory: String,
        jobResponsibility: String,
        pdfFile: MultipartBody.Part?,
        imageFile: MultipartBody.Part?,
        logoFile: MultipartBody.Part?,
        locations: List<String>,
    ): Flow<ApiResult<HomePagBaseApiModel>> {
        return flow {
            emit(safeApiCall {
                RetrofitApiService.create(BASE_URL).updateJobWithFiles(
                    id = id,
                    jobTitle = jobTitle.toRequestBody("text/plain".toMediaTypeOrNull()),
                    jobDescription = jobDescription.toRequestBody("text/plain".toMediaTypeOrNull()),
                    jobCategory = jobCategory.toRequestBody("text/plain".toMediaTypeOrNull()),
                    jobResponsibility = jobResponsibility.toRequestBody("text/plain".toMediaTypeOrNull()),
                    pdf = pdfFile,
                    image = imageFile,
                    logo = logoFile,
                    locations = locations.map { location ->
                        MultipartBody.Part.createFormData("locations[]", location)
                    },
                )
            })
        }.flowOn(Dispatchers.IO)
    }

    override fun getScheduledImagesGrouped() = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getScheduledImagesGrouped()
        })
    }.flowOn(Dispatchers.IO)

    override fun getScheduledImages() = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getScheduledImages()
        })
    }.flowOn(Dispatchers.IO)

    override fun getSectionStoriesGrouped() = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getSectionStoriesGrouped()
        })
    }.flowOn(Dispatchers.IO)

    override fun getActiveStories(deviceKey: String) = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getActiveStories(deviceKey)
        })
    }.flowOn(Dispatchers.IO)

    override fun markStoryViewed(storyId: Int, deviceKey: String) = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).markStoryViewed(storyId, deviceKey)
        })
    }.flowOn(Dispatchers.IO)

    override fun likeStory(storyId: Int) = flow {
        val request = HashMap<String, Any>()
        request["story_id"] = storyId
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).likeStory(request)
        })
    }.flowOn(Dispatchers.IO)

    override fun unlikeStory(storyId: Int) = flow {
        val request = HashMap<String, Any>()
        request["story_id"] = storyId
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).unlikeStory(request)
        })
    }.flowOn(Dispatchers.IO)

    override fun getStoryLikeStatus(storyId: Int) = flow {
        emit(safeApiCall {
            RetrofitApiService.create(BASE_URL).getStoryLikeStatus(storyId)
        })
    }.flowOn(Dispatchers.IO)
}