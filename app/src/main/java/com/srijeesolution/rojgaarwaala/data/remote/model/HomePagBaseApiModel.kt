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
    @SerializedName("cityList")
    val cityList: ArrayList<CityItem>? = ArrayList(),
)

data class CityItem(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("name")
    val name: String? = null,
)

data class BannerList(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("url")
    val imageUrl: String? = null,
    @SerializedName("position")
    val position: Int? = null,
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
    val isTopVideo: Boolean? = null,
    @SerializedName("user")
    val user: UserData? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("sort_order")
    val sortOrder: Int? = null,
    @SerializedName("views")
    val views: Int? = null,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("location_hint")
    val locationHint: String? = null,
    @SerializedName("show_new")
    val showNew: Boolean? = null,
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
    @SerializedName("stream_url")
    val stream_url: String? = null,
    @SerializedName("thumbnail")
    val thumbnail: String? = null,
    @SerializedName("is_top_video")
    val isTopVideo: Boolean? = null,
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
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("location_hint")
    val locationHint: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("related_videos")
    val relatedVideos: List<TopVideo>? = null
)

data class VideoLikeApiModel(
    @SerializedName("status")
    val status: Boolean? = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: VideoReactionData? = null
)

data class VideoReactionData(
    @SerializedName("video_id")
    val videoId: Int? = null,
    @SerializedName("like_count")
    val likeCount: Int? = null,
    @SerializedName("unlike_count")
    val unlikeCount: Int? = null,
    @SerializedName("is_liked")
    val isLiked: Boolean? = null,
    @SerializedName("is_unliked")
    val isUnliked: Boolean? = null
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
    @SerializedName("pdf")
    val pdf: String? = null,
    @SerializedName("image")
    val image: String? = null,
    @SerializedName("logo")
    val logo: String? = null,
    @SerializedName("user_id")
    val userId: Int? = null,
    @SerializedName("status")
    val status: String? = null
)

data class CategoryVideosResponse(
    @SerializedName("status")
    val status: Boolean? = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: CategoryVideosData? = null
)

data class CategoryVideosData(
    @SerializedName("category")
    val category: Category? = null,
    @SerializedName("videos")
    val videos: List<TopVideo>? = null
)

data class ImageListResponse(
    @SerializedName("status")
    val status: Boolean? = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: ImageListData? = null
)

data class ImageListData(
    @SerializedName("categoryImages")
    val categoryImages: List<ImageSubItem>? = null
)

data class ImageSubItem(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("icon_file")
    val iconFile: String? = null,
    @SerializedName("images")
    val images: List<ImageData>? = null
)

data class ImageData(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("publish_date")
    val publishDate: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("user")
    val user: UserData? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("sort_order")
    val sortOrder: Int? = null,
)

data class StoriesResponse(
    @SerializedName("status")
    val status: Boolean? = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: StoriesData? = null
)

data class StoriesData(
    @SerializedName("timeGroups")
    val timeGroups: List<TimeGroup>? = null
)

data class TimeGroup(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("icon_file")
    val iconFile: String? = null,
    @SerializedName("stories")
    val stories: List<Story>? = null
)

data class Story(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("media_type")
    val mediaType: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("video_url")
    val videoUrl: String? = null,
    @SerializedName("link_url")
    val linkUrl: String? = null,
    @SerializedName("publish_date")
    val publishDate: String? = null,
    @SerializedName("position")
    val position: Int? = null,
    @SerializedName("created_by")
    val createdBy: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)