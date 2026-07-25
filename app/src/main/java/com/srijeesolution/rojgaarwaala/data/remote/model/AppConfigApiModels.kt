package com.srijeesolution.rojgaarwaala.data.remote.model

import com.google.gson.annotations.SerializedName

data class AppConfigResponse(
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: AppConfigData? = null,
)

data class AppConfigData(
    @SerializedName("android") val android: AndroidUpdateConfig? = null,
)

data class AndroidUpdateConfig(
    @SerializedName("min_version_code") val minVersionCode: Int? = null,
    @SerializedName("latest_version_code") val latestVersionCode: Int? = null,
    @SerializedName("force_update") val forceUpdate: Boolean? = null,
    @SerializedName("update_available") val updateAvailable: Boolean? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("store_url") val storeUrl: String? = null,
)
