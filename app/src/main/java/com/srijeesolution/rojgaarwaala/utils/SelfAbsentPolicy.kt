package com.srijeesolution.rojgaarwaala.utils

/**
 * Rules for an employee marking themselves absent for today, kept pure so JVM
 * tests cover them.
 */
object SelfAbsentPolicy {

    const val REASON_OTHER = "अन्य"

    /** Choices offered when marking absent; the last one is the catch-all. */
    val REASONS = listOf("बीमार हूँ", "घर का काम है", "गाँव / बाहर जा रहा हूँ", REASON_OTHER)

    /**
     * Offered only when the server says so. An older backend omits the flag, and
     * the button must then stay hidden rather than call an endpoint that is not there.
     */
    fun canOffer(canMarkAbsent: Boolean?, selfAbsent: Boolean?): Boolean =
        canMarkAbsent == true && selfAbsent != true

    /**
     * Having said they are not coming, the employee has answered for the day: both
     * reminders stop, even though punching in stays open in case they turn up.
     */
    fun silencesReminders(selfAbsent: Boolean?, punchInAt: String?): Boolean =
        selfAbsent == true && punchInAt.isNullOrBlank()

    /** Status shown beside a worker in the supervisor's team list. */
    fun teamStatus(statusText: String, selfAbsent: Boolean?, reason: String?): String {
        if (selfAbsent != true) return statusText
        val why = reason?.takeIf { it.isNotBlank() }
        return if (why == null) "$statusText, खुद बताया" else "$statusText, खुद बताया: $why"
    }

    fun notice(reason: String?): String = buildString {
        append("आज आपने खुद को गैरहाजिर लगाया है।")
        reason?.takeIf { it.isNotBlank() }?.let { append("\nकारण: ").append(it) }
        append("\nफिर भी फैक्ट्री आ जाएं तो पंच इन लगा दें, गैरहाजिरी हट जाएगी।")
    }
}
