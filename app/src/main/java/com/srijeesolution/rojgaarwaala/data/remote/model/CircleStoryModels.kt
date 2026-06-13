package com.srijeesolution.rojgaarwaala.data.remote.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class ActiveStoriesResponse(
    @SerializedName("status")
    val status: Boolean? = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: ActiveStoriesData? = null
)

data class ActiveStoriesData(
    @SerializedName("stories")
    val stories: List<CircleStory>? = null,
    @SerializedName("has_unseen")
    val hasUnseen: Boolean? = null
)

@Parcelize
data class CircleStory(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("media_type")
    val mediaType: String? = "image",
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("video_url")
    val videoUrl: String? = null,
    @SerializedName("link_url")
    val linkUrl: String? = null,
    @SerializedName("expires_at")
    val expiresAt: String? = null,
    @SerializedName("seen")
    val seen: Boolean? = false,
    @SerializedName("created_at")
    val createdAt: String? = null
) : Parcelable

fun Story.toCircleStory(): CircleStory {
    return CircleStory(
        id = id,
        title = title,
        description = description,
        mediaType = mediaType ?: "image",
        thumbnailUrl = imageUrl,
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        linkUrl = linkUrl,
        seen = true
    )
}
