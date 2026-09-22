package com.srijeesolution.rojgaarwaala.utils

/**
 * Nearby paper-cut job pushes open the Free Job tab, not Home or Applied Jobs.
 */
object JobAlertNavigation {
    const val TYPE_PAPER_CUT = "paper_cut_job"
    const val TAB_FREE_JOB = MainTabs.FREE_JOB

    fun isJobAlert(type: String?): Boolean =
        type == TYPE_PAPER_CUT || type == "scheduled_image" || type == "free_job"

    fun tabForType(type: String?): Int? =
        if (isJobAlert(type)) TAB_FREE_JOB else null

    fun resolveImageId(id: String?, scheduledImageId: String?): String? =
        scheduledImageId?.takeIf { it.isNotBlank() } ?: id?.takeIf { it.isNotBlank() }
}
