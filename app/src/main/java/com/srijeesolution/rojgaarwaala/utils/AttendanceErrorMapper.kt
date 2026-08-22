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

    const val UNAUTHENTICATED_MESSAGE = "Your session has expired. Please login again."
    const val GENERIC_MESSAGE = "Something went wrong. Please try again."

    /**
     * @param serverMessage the `message` field of the envelope, used verbatim where the
     * backend already builds richer copy (distance / radius for [OUTSIDE_GEOFENCE]).
     */
    fun message(errorCode: String?, serverMessage: String? = null): String {
        val fallback = serverMessage?.takeIf { it.isNotBlank() }
        return when (errorCode) {
            NOT_AN_EMPLOYEE -> fallback ?: "You are not registered as an employee."
            EMPLOYEE_INACTIVE -> fallback ?: "Your employee account is inactive."
            NO_ACTIVE_ASSIGNMENT -> "You are not assigned to any factory today"
            TERMS_NOT_ACCEPTED ->
                "Please read and accept your factory terms and conditions to mark attendance."
            ALREADY_PUNCHED_IN -> "Attendance already marked today"
            NOT_PUNCHED_IN -> fallback ?: "You have not punched in yet today."
            ALREADY_PUNCHED_OUT -> fallback ?: "You have already punched out today."
            OUTSIDE_GEOFENCE -> fallback ?: "You are outside the allowed factory area"
            POOR_ACCURACY ->
                "Unable to get accurate location. Move to an open area away from buildings and try again."
            FACTORY_LOCATION_MISSING ->
                fallback ?: "Factory location is not set. Please contact your supervisor."
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
