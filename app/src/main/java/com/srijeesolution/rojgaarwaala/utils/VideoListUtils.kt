package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo

object VideoListUtils {
    const val PREVIEW_LIMIT = 20
    const val VIEW_MORE_ITEM_ID = -999

    fun orderVideos(videos: List<TopVideo>): List<TopVideo> {
        return videos.sortedWith(
            compareBy<TopVideo> { it.sortOrder ?: Int.MAX_VALUE }
                .thenByDescending { it.createdAt.orEmpty() }
        )
    }

    fun withViewMoreTile(videos: List<TopVideo>, hasMore: Boolean): List<TopVideo> {
        if (!hasMore || videos.isEmpty()) {
            return videos
        }
        return videos + TopVideo(id = VIEW_MORE_ITEM_ID, title = "View More")
    }

    fun isViewMoreItem(video: TopVideo?): Boolean = video?.id == VIEW_MORE_ITEM_ID
}
