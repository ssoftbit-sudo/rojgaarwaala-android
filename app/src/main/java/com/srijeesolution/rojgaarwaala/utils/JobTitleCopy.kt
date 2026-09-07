package com.srijeesolution.rojgaarwaala.utils

/**
 * Shared title rules for apply, status, lists and the payload sent to the server.
 *
 * "Job Opportunity" is the old Android fallback and must never be treated as a real title.
 */
object JobTitleCopy {

  const val FALLBACK = "Job Application"

  fun isPlaceholder(title: String?): Boolean {
    val normalized = title?.trim().orEmpty()
    if (normalized.isEmpty()) {
      return true
    }
    return normalized.equals("Job Opportunity", ignoreCase = true) ||
      normalized.equals(FALLBACK, ignoreCase = true)
  }

  fun display(title: String?, fallback: String = FALLBACK): String {
    val normalized = title?.trim().orEmpty()
    return if (isPlaceholder(normalized)) fallback else normalized
  }

  fun forRequest(title: String?): String? {
    val normalized = title?.trim().orEmpty()
    return normalized.takeIf { it.isNotEmpty() && !isPlaceholder(it) }
  }
}
