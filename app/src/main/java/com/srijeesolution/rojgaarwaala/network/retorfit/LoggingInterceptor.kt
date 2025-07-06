package com.srijeesolution.rojgaarwaala.network.retorfit

import kotlin.Throws

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import java.io.IOException

class LoggingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        var requestLog = "RequestMethod= ${request.method}\nRequestUrl= ${request.url}\nRequestHeader= \n${request.headers}"
        if (request.method.compareTo("post", ignoreCase = true) == 0) {
            requestLog = """
                $requestLog
                RequestBody = ${bodyToString(request)}
                """.trimIndent()
        }
        val response: Response = chain.proceed(request)
        val bodyString = response.body!!.string()
        return response.newBuilder()
            .body(ResponseBody.create(response.body!!.contentType(), bodyString))
            .build()

    }

    companion object {
        fun bodyToString(request: Request): String {
            return try {
                val copy = request.newBuilder().build()
                val buffer = Buffer()
                copy.body!!.writeTo(buffer)
                buffer.readUtf8()
            } catch (e: IOException) {
                "did not work"
            }
        }
    }
}