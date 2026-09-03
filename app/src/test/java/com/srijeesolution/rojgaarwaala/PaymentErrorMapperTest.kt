package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import com.srijeesolution.rojgaarwaala.utils.PaymentErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentErrorMapperTest {

    @Test
    fun `prefers the JSON message from the server`() {
        assertEquals(
            "Could not start the payment. Please try again.",
            PaymentErrorMapper.message(
                ApiError(
                    statusCode = 422,
                    errorMsg = "Unprocessable Entity",
                    errorBody = """{"status":false,"message":"Could not start the payment. Please try again."}""",
                ),
            ),
        )
    }

    @Test
    fun `a timeout is not reported as unreachable`() {
        assertEquals(
            PaymentErrorMapper.TIMED_OUT,
            PaymentErrorMapper.message(
                ApiError(statusCode = 0, errorMsg = "timeout", errorBody = ""),
            ),
        )
        assertEquals(
            PaymentErrorMapper.TIMED_OUT,
            PaymentErrorMapper.message(
                ApiError(statusCode = 0, errorMsg = "SocketTimeoutException: timeout", errorBody = ""),
            ),
        )
    }

    @Test
    fun `HTML 502 is treated as a gateway failure not a blank network error`() {
        assertEquals(
            PaymentErrorMapper.GATEWAY_UNAVAILABLE,
            PaymentErrorMapper.message(
                ApiError(
                    statusCode = 502,
                    errorMsg = "Bad Gateway",
                    errorBody = "<html>502 Bad Gateway</html>",
                ),
            ),
        )
    }

    @Test
    fun `other HTTP errors include the status code`() {
        assertEquals(
            "Could not reach the payment service (HTTP 404).",
            PaymentErrorMapper.message(
                ApiError(statusCode = 404, errorMsg = "Not Found", errorBody = "<html></html>"),
            ),
        )
    }

    @Test
    fun `empty failure stays on the unreachable copy`() {
        assertEquals(
            PaymentErrorMapper.UNREACHABLE,
            PaymentErrorMapper.message(null),
        )
        assertEquals(
            PaymentErrorMapper.UNREACHABLE,
            PaymentErrorMapper.message(ApiError(0, "Unable to resolve host", "")),
        )
    }
}
