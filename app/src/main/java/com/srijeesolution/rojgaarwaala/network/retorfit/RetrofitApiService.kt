package com.srijeesolution.rojgaarwaala.network.retorfit

import com.srijeesolution.rojgaarwaala.BuildConfig
import com.srijeesolution.rojgaarwaala.utils.RojgaarwalaApplication
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
object RetrofitApiService {
    @JvmStatic
    fun create(baseUrl: String): RetrofitApiInterface {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(HeaderInterceptor(SharedPrefs(RojgaarwalaApplication.getAppContext())))
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(RetrofitApiInterface::class.java)
    }

    /**
     * build interceptor with the header information and define expiry time for API request/response
     */

}