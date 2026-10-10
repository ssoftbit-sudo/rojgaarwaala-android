package com.srijeesolution.rojgaarwaala.network.retorfit

import com.srijeesolution.rojgaarwaala.BuildConfig
import com.srijeesolution.rojgaarwaala.utils.DeviceKeyUtils
import com.srijeesolution.rojgaarwaala.utils.RojgaarwalaApplication
import com.srijeesolution.rojgaarwaala.utils.SessionExpiry
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Adds the session and device headers to every API call, and ends the session when
 * the server rejects the token.
 *
 * A failed call is never sent again from here: the old retry re-sent the request
 * without its headers, so a timeout came back to the screen as a 401.
 */
class HeaderInterceptor internal constructor(
    private val sessionHeaders: () -> Map<String, String>,
    private val onSessionExpired: () -> Unit,
) : Interceptor {

    @Inject
    constructor(sharedPrefs: SharedPrefs) : this(
        sessionHeaders = { headersFrom(sharedPrefs) },
        onSessionExpired = { SessionExpiry.handle(RojgaarwalaApplication.getAppContext(), sharedPrefs) },
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val headers = sessionHeaders()
        val builder = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("X-App-Version-Code", BuildConfig.VERSION_CODE.toString())
            .header("X-App-Version-Name", BuildConfig.VERSION_NAME)
        headers.forEach { (name, value) -> builder.header(name, value) }
        val request = builder.build()

        val response = chain.proceed(request)

        if (SessionExpiry.shouldLogout(
                code = response.code,
                sentAuthHeader = headers.containsKey(AUTHORIZATION),
                path = request.url.encodedPath,
            )
        ) {
            onSessionExpired()
        }

        return response
    }

    companion object {
        const val AUTHORIZATION = "Authorization"

        private fun headersFrom(sharedPrefs: SharedPrefs): Map<String, String> {
            val headers = LinkedHashMap<String, String>()
            val authToken = sharedPrefs.getPrefs(SharedPrefsConstant.USER_AUTH_TOKEN, "").orEmpty().trim()
            if (authToken.isNotEmpty()) {
                headers[AUTHORIZATION] = "Bearer $authToken"
            }
            val fcmToken = sharedPrefs.getPrefs(SharedPrefsConstant.FCM_TOKEN, "").orEmpty().trim()
            if (fcmToken.isNotEmpty()) {
                headers["fcm_token"] = fcmToken
            }
            headers["X-Device-Key"] = DeviceKeyUtils.getOrCreateDeviceKey(sharedPrefs)
            return headers
        }
    }
}
