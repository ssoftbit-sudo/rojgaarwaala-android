package com.srijeesolution.rojgaarwaala.network.retorfit

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
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create()).build()
        return retrofit.create(RetrofitApiInterface::class.java)
    }

    /**
     * build interceptor with the header information and define expiry time for API request/response
     */
    private val httpClient: OkHttpClient
        get() {
            // initializing logging interceptor
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            // initializing http client
            val httpClient = OkHttpClient.Builder()
            // adding header interceptor
            httpClient.addInterceptor(HeaderInterceptor(SharedPrefs(RojgaarwalaApplication.getAppContext())))
            httpClient.addInterceptor(LoggingInterceptor())
            httpClient.addInterceptor(interceptor).build() //commented for removing logs on production
            return httpClient.build()
        }
    private val interceptor1 = run {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.apply {
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        }
    }
}