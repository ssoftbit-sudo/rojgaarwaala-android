package com.srijeesolution.rojgaarwaala.network.handler

data class ApiError(
    val statusCode : Int = 0,
    val errorMsg : String,
    val errorBody: String
)
