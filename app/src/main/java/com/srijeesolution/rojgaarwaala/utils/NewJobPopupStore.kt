package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant

object NewJobPopupStore {
    private const val MAX_SEEN = 200

    fun unseen(sharedPrefs: SharedPrefs, jobs: List<ImageData>): List<ImageData> {
        val seen = seenIds(sharedPrefs)
        return jobs.filter { job ->
            val id = job.id ?: return@filter false
            id.toString() !in seen
        }
    }

    fun markSeen(sharedPrefs: SharedPrefs, jobs: List<ImageData>) {
        val seen = seenIds(sharedPrefs).toMutableList()
        jobs.mapNotNull { it.id?.toString() }.forEach { id ->
            seen.remove(id)
            seen.add(id)
        }
        val clipped = if (seen.size > MAX_SEEN) seen.takeLast(MAX_SEEN) else seen
        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.SEEN_NEW_JOB_IDS, clipped.joinToString(",")))
    }

    private fun seenIds(sharedPrefs: SharedPrefs): List<String> {
        return sharedPrefs.getPrefs(SharedPrefsConstant.SEEN_NEW_JOB_IDS, "")
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
