package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo

object VideoListUtils {
    const val PREVIEW_LIMIT = 20
    /** Legacy home API returned at most 10 preview videos without pagination metadata. */
    private const val LEGACY_PREVIEW_CAP = 10
    const val VIEW_MORE_ITEM_ID = -999

    fun orderVideos(videos: List<TopVideo>): List<TopVideo> {
        return videos.sortedWith(
            compareBy<TopVideo> { it.sortOrder ?: Int.MAX_VALUE }
                .thenByDescending { it.createdAt.orEmpty() }
        )
    }

    fun inferHasMore(
        previewCount: Int,
        hasMore: Boolean? = null,
        total: Int? = null,
    ): Boolean {
        if (hasMore == true) return true
        if (total != null && total > previewCount) return true
        if (previewCount >= PREVIEW_LIMIT) return true
        if (hasMore == null && total == null && previewCount >= LEGACY_PREVIEW_CAP) return true
        return false
    }

    fun withViewMoreTile(videos: List<TopVideo>, hasMore: Boolean): List<TopVideo> {
        if (!hasMore || videos.isEmpty()) {
            return videos
        }
        return videos + TopVideo(id = VIEW_MORE_ITEM_ID, title = "View More")
    }

    fun isViewMoreItem(video: TopVideo?): Boolean = video?.id == VIEW_MORE_ITEM_ID
}
