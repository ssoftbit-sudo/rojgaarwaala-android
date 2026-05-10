package com.srijeesolution.rojgaarwaala.network.handler

sealed class ApiResult<T>(
    val data: T? = null,
    val message: ApiError? = null
) {
    class Success<T>(data: T?): ApiResult<T>(data)
    class Error<T>(message: ApiError?, data: T?= null) : ApiResult<T>(data, message)
    class Loading<T>: ApiResult<T>()
}

fun <T, R> ApiResult<T>.map(transform: (T?) -> R): ApiResult<R> {
    return when (this) {
        is ApiResult.Success -> ApiResult.Success(transform(data))
        is ApiResult.Error -> ApiResult.Error(message)
        is ApiResult.Loading -> ApiResult.Loading()
    }
}
