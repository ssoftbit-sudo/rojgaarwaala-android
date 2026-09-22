package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem

object JobAlertFeed {
    fun imagesForPreferredCategory(
        categories: List<ImageSubItem>,
        preferred: String?,
    ): List<ImageData> {
        val matched = categories.flatMap { category ->
            if (PreferredJobCategory.matches(preferred, category.title)) {
                category.images.orEmpty()
            } else {
                emptyList()
            }
        }
        if (matched.isNotEmpty()) return matched
        return categories.flatMap { it.images.orEmpty() }
    }
}
