package com.srijeesolution.rojgaarwaala.network.retorfit

import com.srijeesolution.rojgaarwaala.BuildConfig
import com.srijeesolution.rojgaarwaala.utils.RojgaarwalaApplication
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object RetrofitApiService {

    /**
     * One client for the whole app. Building a new one per call, as this used to,
     * threw away the connection pool, so every request paid for a fresh TLS handshake.
     */
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor(SharedPrefs(RojgaarwalaApplication.getAppContext())))
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                        redactHeader(HeaderInterceptor.AUTHORIZATION)
                    })
                }
            }
            .build()
    }

    private val apis = ConcurrentHashMap<String, RetrofitApiInterface>()

    @JvmStatic
    fun create(baseUrl: String): RetrofitApiInterface =
        apis.getOrPut(baseUrl) { build(baseUrl, httpClient) }

    internal fun build(baseUrl: String, client: OkHttpClient): RetrofitApiInterface =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RetrofitApiInterface::class.java)
}
