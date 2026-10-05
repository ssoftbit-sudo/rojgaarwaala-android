package com.srijeesolution.rojgaarwaala.utils

/**
 * Decides which update prompt the home screen should show for the values
 * returned by /api/app-config.
 *
 * Re-derived from the installed version code instead of trusting the server
 * flags alone, so a build that already meets the target is never prompted.
 */
object AppUpdatePolicy {

  enum class Prompt {
    NONE,
    OPTIONAL,
    REQUIRED,
  }

  fun decide(
    installedVersionCode: Int,
    minVersionCode: Int?,
    latestVersionCode: Int?,
    forceUpdate: Boolean?,
  ): Prompt {
    val min = minVersionCode ?: 0
    val latest = maxOf(latestVersionCode ?: 0, min)

    if (forceUpdate == true && min > 0 && installedVersionCode < min) {
      return Prompt.REQUIRED
    }

    return if (latest > 0 && installedVersionCode < latest) {
      Prompt.OPTIONAL
    } else {
      Prompt.NONE
    }
  }
}
