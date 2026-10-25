package com.srijeesolution.rojgaarwaala.network.constant

class NetworkBaseUrls {
    companion object {
        /** The live server, or a local one in a debug build made with -PAPI_BASE_URL. */
        const val BASE_URL = com.srijeesolution.rojgaarwaala.BuildConfig.API_BASE_URL
    }
}