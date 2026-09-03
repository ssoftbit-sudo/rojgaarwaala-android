package com.srijeesolution.rojgaarwaala.data.remote.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class ImagesApiResponse(
    @SerializedName("success")
    val success: Boolean? = false,
    @SerializedName("data")
    val data: List<ImageCategory>? = emptyList(),
    @SerializedName("total_images")
    val totalImages: Int? = 0,
    @SerializedName("categories_count")
    val categoriesCount: Int? = 0
)

@Parcelize
data class ImageCategory(
    @SerializedName("category")
    val category: String? = null,
    @SerializedName("count")
    val count: Int? = 0,
    @SerializedName("images")
    val images: List<ScheduledImage>? = emptyList()
) : Parcelable

@Parcelize
data class ScheduledImage(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName(value = "image_path", alternate = ["image_url"])
    val imagePath: String? = null,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("publish_date")
    val publishDate: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null
) : Parcelable 