package com.srijeesolution.rojgaarwaala.data.remote.model

import com.google.gson.annotations.SerializedName

data class HomePagBaseApiModel(
    @SerializedName("status")
    val status: Boolean? = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val dataObj: HomePageData? = null,
)

data class HomePageData(
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("bannerList")
    val bannerList: ArrayList<BannerList>? = ArrayList(),
    @SerializedName("categoryList")
    val categoryList: ArrayList<Category>? = ArrayList(),
    @SerializedName("categories")
    val categories: ArrayList<Category>? = ArrayList(),
    @SerializedName("topVideos")
    val topVideos: ArrayList<TopVideo>? = ArrayList(),
    @SerializedName("categoryVideos")
    val categoryVideos: ArrayList<CategoryVideo>? = ArrayList(),
    @SerializedName("userDetails")
    val userDetails: UserData? = null,
)

data class BannerList(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("url")
    val imageUrl: String? = null,
)

data class TopVideo(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("video_url")
    val videoUrl: String? = null,
    @SerializedName("thumbnail")
    val thumbnail: String? = null,
    @SerializedName("is_top_video")
    val isTopVideo: Int? = null,
    @SerializedName("user")
    val user: UserData? = null,
)

data class Category(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("icon_file")
    val iconFile: String? = null,
)

data class CategoryVideo(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("icon_file")
    val iconFile: String? = null,
    @SerializedName("videos")
    val videos: ArrayList<TopVideo>? = ArrayList(),
)

data class UserData(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("mobile")
    val mobile: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("state")
    val state: String? = null,
    @SerializedName("pincode")
    val pincode: String? = null,
)

data class VideoDetailsResponse(
    @SerializedName("status")
    val status: Boolean? = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: VideoDetailsData? = null
)

data class VideoDetailsData(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("video_url")
    val videoUrl: String? = null,
    @SerializedName("thumbnail")
    val thumbnail: String? = null,
    @SerializedName("is_top_video")
    val isTopVideo: Int? = null,
    @SerializedName("likes")
    val likes: Int? = null,
    @SerializedName("unlikes")
    val unlikes: Int? = null,
    @SerializedName("views")
    val views: Int? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("user")
    val user: UserData? = null,
    @SerializedName("category")
    val category: Category? = null,
    @SerializedName("related_videos")
    val relatedVideos: List<TopVideo>? = null
)

data class JobListResponse(
    @SerializedName("status")
    val status: Boolean? = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: JobListData? = null
)

data class JobListData(
    @SerializedName("in_review")
    val inReview: List<JobItem>? = null,
    @SerializedName("live")
    val live: List<JobItem>? = null
)

data class JobItem(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("job_title")
    val jobTitle: String? = null,
    @SerializedName("job_description")
    val jobDescription: String? = null,
    @SerializedName("job_category")
    val jobCategory: String? = null,
    @SerializedName("job_responsibility")
    val jobResponsibility: String? = null,
    @SerializedName("user_id")
    val userId: Int? = null,
    @SerializedName("status")
    val status: String? = null
)


