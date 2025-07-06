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
    @SerializedName("userDetails")
    val userDetails: UserData? = null,
)
data class BannerList(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("url")
    val imageUrl: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("size")
    val size: String? = null,
    @SerializedName("price")
    val price: Double? = null,
    @SerializedName("videos")
    val videos: ArrayList<BannerList>? = ArrayList(),
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
data class Video(
    val id: Int,
    val title: String,
    val description: String,
    val video_url: String
)

data class Category(
    val id: Int,
    val title: String,
    val videos: List<Video>
)


