package com.srijeesolution.rojgaarwaala.utils

object PreferredJobCategory {
    fun matches(preferred: String?, categoryName: String?): Boolean {
        val a = preferred?.trim()?.lowercase().orEmpty()
        val b = categoryName?.trim()?.lowercase().orEmpty()
        return a.isNotEmpty() && a == b
    }
}
