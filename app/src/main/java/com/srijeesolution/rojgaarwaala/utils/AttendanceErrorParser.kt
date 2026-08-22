package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import org.json.JSONObject

/**
 * Pulls the punch failure details out of the raw error body.
 *
 * The 401 response has no `status`/`data` envelope at all, so every field is read
 * defensively and a malformed body degrades to a generic message.
 */
data class AttendancePunchError(
    val errorCode: String?,
    val message: String,
    val isUnauthenticated: Boolean,
)

object AttendanceErrorParser {

    fun parse(error: ApiError?): AttendancePunchError {
        if (error?.statusCode == 401) {
            return AttendancePunchError(
                errorCode = null,
                message = AttendanceErrorMapper.UNAUTHENTICATED_MESSAGE,
                isUnauthenticated = true,
            )
        }

        val body = error?.errorBody.orEmpty()
        if (body.isBlank()) {
            return AttendancePunchError(
                errorCode = null,
                message = AttendanceErrorMapper.GENERIC_MESSAGE,
                isUnauthenticated = false,
            )
        }

        return try {
            val json = JSONObject(body)
            val serverMessage = json.optString("message").takeIf { it.isNotBlank() }
            val errorCode = json.optJSONObject("data")
                ?.optString("error_code")
                ?.takeIf { it.isNotBlank() }
            AttendancePunchError(
                errorCode = errorCode,
                message = AttendanceErrorMapper.message(errorCode, serverMessage),
                isUnauthenticated = serverMessage.equals("Unauthenticated.", ignoreCase = true),
            )
        } catch (e: Exception) {
            AttendancePunchError(
                errorCode = null,
                message = AttendanceErrorMapper.GENERIC_MESSAGE,
                isUnauthenticated = false,
            )
        }
    }
}
