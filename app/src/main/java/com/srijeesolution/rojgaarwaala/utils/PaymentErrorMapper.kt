package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import org.json.JSONObject

/**
 * Turns an order/verify HTTP failure into the copy shown on the payment screen.
 *
 * The previous fallback — "Could not reach the payment service." — was used for
 * timeouts, HTML 502 pages, and empty bodies alike, so a gateway problem looked
 * identical to a missing table.
 */
object PaymentErrorMapper {

    const val UNREACHABLE = "Could not reach the payment service."
    const val TIMED_OUT = "Payment service timed out. Please try again."
    const val GATEWAY_UNAVAILABLE = "Could not start the payment. Please try again."

    fun message(error: ApiError?): String {
        val body = error?.errorBody
        if (!body.isNullOrBlank()) {
            try {
                val parsed = JSONObject(body)
                val serverMessage = parsed.optString("message")
                if (serverMessage.isNotBlank()) return serverMessage
            } catch (_: Exception) {
            }
        }

        val statusCode = error?.statusCode ?: 0
        val errorMsg = error?.errorMsg.orEmpty().lowercase()

        if (statusCode == 0 && (errorMsg.contains("timeout") || errorMsg.contains("timed out"))) {
            return TIMED_OUT
        }

        if (statusCode in 500..599) {
            return GATEWAY_UNAVAILABLE
        }

        if (statusCode > 0) {
            return "Could not reach the payment service (HTTP $statusCode)."
        }

        return UNREACHABLE
    }
}
