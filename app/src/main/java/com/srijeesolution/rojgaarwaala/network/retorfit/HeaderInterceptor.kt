package com.srijeesolution.rojgaarwaala.network.retorfit

import android.util.Log
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.SocketTimeoutException
import javax.inject.Inject

class HeaderInterceptor @Inject constructor(private val sharedPrefs: SharedPrefs) : Interceptor {
    var appVersion: String = ""
    var isRetryOn = false
    var retryCount = 0
    var maxRetryCount = 3
    private val TAG = HeaderInterceptor::class.java.simpleName
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val response: Response
            val original = chain.request()
            var requestBuilder: Request.Builder? = null
            requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .header(
                    "Authorization",
                    "Bearer " + sharedPrefs.getPrefs(SharedPrefsConstant.USER_AUTH_TOKEN, "")
                )
                .method(original.method, original.body)
            response = chain.proceed(requestBuilder.build())
            return response
        } catch (e: Exception) {
            Log.d(TAG, "Exception-" + e.message)
            Log.d(TAG, "Exception-tryCount-" + retryCount)
            Log.d(TAG, "Exception-isRetryOn-" + isRetryOn)
            if (isRetryOn) {
                if (e is SocketTimeoutException && retryCount < maxRetryCount) {
                    retryCount++
                    // retry the request
                    return chain.call().clone().execute()
                } else {
                    chain.call().cancel()
                }
            }
        }
        return chain.proceed(chain.request())
    }
}