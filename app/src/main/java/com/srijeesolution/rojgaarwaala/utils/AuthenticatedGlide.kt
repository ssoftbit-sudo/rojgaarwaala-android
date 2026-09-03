package com.srijeesolution.rojgaarwaala.utils

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders

/**
 * Payment proof files sit behind the API auth guard, so Glide has to send the bearer
 * token with the image request. A plain URL load returns 401.
 */
object AuthenticatedGlide {

    fun url(rawUrl: String, authToken: String): GlideUrl {
        if (authToken.isBlank()) return GlideUrl(rawUrl)
        val headers = LazyHeaders.Builder()
            .addHeader("Authorization", "Bearer $authToken")
            .addHeader("Accept", "application/json")
            .build()
        return GlideUrl(rawUrl, headers)
    }
}
