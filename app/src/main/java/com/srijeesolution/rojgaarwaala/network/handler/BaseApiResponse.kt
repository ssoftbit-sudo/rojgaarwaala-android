package com.srijeesolution.rojgaarwaala.network.handler

import retrofit2.Response

abstract class BaseApiResponse {
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): ApiResult<T> {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                body?.let {
                    return ApiResult.Success(body)
                }
            }
            val errorBody=response.errorBody()?.string()
            return error(response.code(), response.message(), errorBody?:"")
        } catch (e: Exception) {
            return error(0, e.message ?: e.toString(), "")
        }
    }

    private fun <T> error(statusCode:Int, errorMessage: String, errorBody:String): ApiResult<T> =
        ApiResult.Error(
            message = ApiError(
                statusCode = statusCode,
                errorMsg = errorMessage,
                errorBody = errorBody
            ), data = null
        )
}