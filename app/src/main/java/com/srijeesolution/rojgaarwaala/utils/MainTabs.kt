package com.srijeesolution.rojgaarwaala.utils

/**
 * Bottom bar: Profile, Categories, Home, Free Job, Stories.
 * Add Job lives in the overflow menu, not the bar.
 */
object MainTabs {
    const val PROFILE = 0
    const val CATEGORIES = 1
    const val HOME = 2
    const val FREE_JOB = 3
    const val STORIES = 4
    const val ADD_JOB = 5

    val bottomOrder = listOf(PROFILE, CATEGORIES, HOME, FREE_JOB, STORIES)
}
