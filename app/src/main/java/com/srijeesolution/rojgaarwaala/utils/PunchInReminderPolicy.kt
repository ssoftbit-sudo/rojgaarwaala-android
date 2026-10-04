package com.srijeesolution.rojgaarwaala.utils

import kotlin.math.roundToInt

/**
 * Rules for the punch-in reminder flow, kept pure so JVM tests cover them:
 * reminder -> employee opens the app -> GPS checked there -> Punch In -> stop.
 */
object PunchInReminderPolicy {

    const val NOTIFICATION_TITLE = "Punch In याद रखें"
    const val NOTIFICATION_BODY =
        "आपके ऑफिस जाने का समय होने वाला है। ऑफिस/फैक्ट्री पहुंचकर ऐप खोलें और Punch In करें।"

    /**
     * Reminders stop only once the day's attendance exists on the server: a recorded
     * punch-in, or a day the server already marked (leave, holiday) where punching
     * in is no longer offered. Being at the factory without punching keeps them going.
     */
    fun attendanceRecorded(punchInAt: String?, attendanceMarked: Boolean?, canPunchIn: Boolean?): Boolean =
        !punchInAt.isNullOrBlank() || (attendanceMarked == true && canPunchIn != true)

    /** Headline and next step shown after the employee opens the app from a reminder. */
    fun arrivalMessage(status: GeofenceEvaluator.Status, distanceMetres: Double?): String? = when (status) {
        GeofenceEvaluator.Status.INSIDE -> "✅ आप फैक्ट्री पहुंच गए हैं।\nPunch In करें"
        GeofenceEvaluator.Status.OUTSIDE -> distanceMetres?.let {
            "📍 आप फैक्ट्री से ${hindiDistance(it)} दूर हैं।\nफैक्ट्री पहुंचने के बाद Punch In करें।"
        }
        else -> null
    }

    fun hindiDistance(metres: Double): String {
        if (metres >= 1000) {
            val km = Math.round(metres / 100.0) / 10.0
            val rendered = if (km == Math.floor(km)) km.toLong().toString() else km.toString()
            return "$rendered किलोमीटर"
        }
        return "${metres.roundToInt()} मीटर"
    }
}
