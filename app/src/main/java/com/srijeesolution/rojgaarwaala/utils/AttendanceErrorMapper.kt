package com.srijeesolution.rojgaarwaala.utils

/**
 * Maps the `error_code` returned by the employee punch endpoints to the copy shown
 * to the user, plus the follow-up action the screen must take.
 *
 * Pure Kotlin on purpose so it can be covered by JVM unit tests.
 */
object AttendanceErrorMapper {

    const val NOT_AN_EMPLOYEE = "not_an_employee"
    const val EMPLOYEE_INACTIVE = "employee_inactive"
    const val NO_ACTIVE_ASSIGNMENT = "no_active_assignment"
    const val TERMS_NOT_ACCEPTED = "terms_not_accepted"
    const val ALREADY_PUNCHED_IN = "already_punched_in"
    const val NOT_PUNCHED_IN = "not_punched_in"
    const val ALREADY_PUNCHED_OUT = "already_punched_out"
    const val OUTSIDE_GEOFENCE = "outside_geofence"
    const val POOR_ACCURACY = "poor_accuracy"
    const val FACTORY_LOCATION_MISSING = "factory_location_missing"

    const val UNAUTHENTICATED_MESSAGE = "आपका सेशन खत्म हो गया है। फिर से लॉगिन करें।"
    const val GENERIC_MESSAGE = "कुछ गड़बड़ हो गई। फिर कोशिश करें।"

    /**
     * @param serverMessage the `message` field of the envelope, used verbatim where the
     * backend already builds richer copy (distance / radius for [OUTSIDE_GEOFENCE]).
     */
    fun message(errorCode: String?, serverMessage: String? = null): String {
        val fallback = serverMessage?.takeIf { it.isNotBlank() }
        return when (errorCode) {
            NOT_AN_EMPLOYEE -> fallback ?: "आप कर्मचारी नहीं हैं।"
            EMPLOYEE_INACTIVE -> fallback ?: "आपका अकाउंट बंद है।"
            NO_ACTIVE_ASSIGNMENT -> "आज आप किसी फैक्ट्री में नहीं लगे हैं"
            TERMS_NOT_ACCEPTED ->
                "हाजिरी लगाने से पहले फैक्ट्री के नियम पढ़कर मान लें।"
            ALREADY_PUNCHED_IN -> "आज हाजिरी पहले ही लग चुकी है"
            NOT_PUNCHED_IN -> fallback ?: "आज अभी पंच इन नहीं लगा है।"
            ALREADY_PUNCHED_OUT -> fallback ?: "आज पंच आउट पहले ही लग चुका है।"
            OUTSIDE_GEOFENCE -> fallback ?: "आप फैक्ट्री की सीमा से बाहर हैं"
            POOR_ACCURACY ->
                "सही लोकेशन नहीं मिल रही। खुली जगह पर जाकर फिर कोशिश करें।"
            FACTORY_LOCATION_MISSING ->
                fallback ?: "फैक्ट्री की लोकेशन सेट नहीं है। सुपरवाइज़र से बात करें।"
            else -> fallback ?: GENERIC_MESSAGE
        }
    }

    /** Punch buttons stay hidden for these — retrying can never succeed for this user. */
    fun disablesPunchUi(errorCode: String?): Boolean =
        errorCode == NOT_AN_EMPLOYEE || errorCode == EMPLOYEE_INACTIVE

    /** The employee has to agree to the factory's terms before this punch can be retried. */
    fun requiresTermsAcceptance(errorCode: String?): Boolean = errorCode == TERMS_NOT_ACCEPTED

    /** The backend already moved on, so the screen has to re-sync with it. */
    fun shouldRefreshDashboard(errorCode: String?): Boolean =
        errorCode == ALREADY_PUNCHED_IN ||
            errorCode == NOT_PUNCHED_IN ||
            errorCode == ALREADY_PUNCHED_OUT
}
