package com.srijeesolution.rojgaarwaala.utils

import android.view.View
import android.widget.TextView
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant

object VideoNewTagUtils {

    fun shouldShowNewTag(video: TopVideo, sharedPrefs: SharedPrefs): Boolean {
        val videoId = video.id ?: return false
        if (isLocallyViewed(sharedPrefs, videoId)) {
            return false
        }
        return video.showNew == true
    }

    fun bindNewTagBadge(badge: TextView?, video: TopVideo, sharedPrefs: SharedPrefs) {
        if (badge == null) {
            return
        }
        badge.visibility = if (shouldShowNewTag(video, sharedPrefs)) View.VISIBLE else View.GONE
    }

    fun markViewed(sharedPrefs: SharedPrefs, videoId: Int) {
        if (videoId <= 0) {
            return
        }
        val viewed = getViewedIds(sharedPrefs).toMutableSet()
        viewed.add(videoId.toString())
        sharedPrefs.setPrefsData(SharedPrefsConstant.VIEWED_VIDEO_IDS to viewed.joinToString(","))
    }

    private fun isLocallyViewed(sharedPrefs: SharedPrefs, videoId: Int): Boolean {
        return getViewedIds(sharedPrefs).contains(videoId.toString())
    }

    private fun getViewedIds(sharedPrefs: SharedPrefs): Set<String> {
        return sharedPrefs
            .getPrefs(SharedPrefsConstant.VIEWED_VIDEO_IDS, "")
            .orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
