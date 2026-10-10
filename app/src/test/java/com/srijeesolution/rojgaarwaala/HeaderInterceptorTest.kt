package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.network.retorfit.HeaderInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class HeaderInterceptorTest {

    private lateinit var server: MockWebServer
    private var expiredCalls = 0

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        expiredCalls = 0
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(headers: Map<String, String>): OkHttpClient =
        OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor(HeaderInterceptor({ headers }, { expiredCalls++ }))
            .build()

    private fun post(path: String): Request =
        Request.Builder()
            .url(server.url(path))
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()

    private val signedIn = mapOf("Authorization" to "Bearer token-1", "X-Device-Key" to "device-1")

    @Test
    fun `adds the session and version headers`() {
        server.enqueue(MockResponse().setBody("{}"))

        client(signedIn).newCall(post("/api/employee/dashboard")).execute().close()

        val sent = server.takeRequest()
        assertEquals("Bearer token-1", sent.getHeader("Authorization"))
        assertEquals("device-1", sent.getHeader("X-Device-Key"))
        assertEquals("application/json", sent.getHeader("Accept"))
        assertEquals(BuildConfig.VERSION_CODE.toString(), sent.getHeader("X-App-Version-Code"))
    }

    @Test
    fun `a timed out punch is sent once and surfaces as a network error`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        // Would be consumed only if the interceptor sent the request a second time.
        server.enqueue(MockResponse().setResponseCode(401))

        var failure: IOException? = null
        try {
            client(signedIn).newCall(post("/api/employee/attendance/punch-in")).execute().close()
        } catch (e: IOException) {
            failure = e
        }

        assertTrue("timeout must reach the caller", failure != null)
        assertEquals(1, server.requestCount)
        assertEquals(0, expiredCalls)
    }

    @Test
    fun `a rejected token ends the session once per response`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{\"message\":\"Unauthenticated.\"}"))

        val response = client(signedIn).newCall(post("/api/employee/dashboard")).execute()

        assertEquals(401, response.code)
        response.close()
        assertEquals(1, expiredCalls)
    }

    @Test
    fun `a 401 without a token does not end a session`() {
        server.enqueue(MockResponse().setResponseCode(401))

        client(mapOf("X-Device-Key" to "device-1")).newCall(post("/api/home")).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(0, expiredCalls)
    }

    @Test
    fun `a wrong OTP does not end a session`() {
        server.enqueue(MockResponse().setResponseCode(401))

        client(signedIn).newCall(post("/api/verify-otp")).execute().close()

        assertEquals(0, expiredCalls)
    }
}
