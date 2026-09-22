package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.data.remote.model.UserData
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant

object ProfileLocationStore {
    fun save(sharedPrefs: SharedPrefs, profile: UserData) {
        save(
            sharedPrefs,
            profile.latitude,
            profile.longitude,
            profile.address ?: listOfNotNull(profile.city, profile.colony, profile.pincode)
                .filter { it.isNotBlank() }
                .joinToString(", "),
        )
        profile.preferredJobCategory?.takeIf { it.isNotBlank() }?.let {
            sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.PREFERRED_JOB_CATEGORY, it))
        }
    }

    fun save(sharedPrefs: SharedPrefs, latitude: Double?, longitude: Double?, address: String?) {
        if (latitude != null) {
            sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.PROFILE_LATITUDE, latitude.toFloat()))
        }
        if (longitude != null) {
            sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.PROFILE_LONGITUDE, longitude.toFloat()))
        }
        if (!address.isNullOrBlank()) {
            sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.PROFILE_MAP_ADDRESS, address))
        }
    }

    fun latitude(sharedPrefs: SharedPrefs): Double? {
        if (!sharedPrefs.checkSharedPrefs(SharedPrefsConstant.PROFILE_LATITUDE)) return null
        val value = sharedPrefs.getPrefs(SharedPrefsConstant.PROFILE_LATITUDE, 0F)
        return if (value == 0F) null else value.toDouble()
    }

    fun longitude(sharedPrefs: SharedPrefs): Double? {
        if (!sharedPrefs.checkSharedPrefs(SharedPrefsConstant.PROFILE_LONGITUDE)) return null
        val value = sharedPrefs.getPrefs(SharedPrefsConstant.PROFILE_LONGITUDE, 0F)
        return if (value == 0F) null else value.toDouble()
    }

    fun address(sharedPrefs: SharedPrefs): String =
        sharedPrefs.getPrefs(SharedPrefsConstant.PROFILE_MAP_ADDRESS, "") ?: ""

    fun radiusKm(sharedPrefs: SharedPrefs): Int {
        val stored = sharedPrefs.getPrefs(SharedPrefsConstant.FREE_JOB_RADIUS_KM, 15)
        return stored.coerceIn(1, 30)
    }

    fun preferredCategory(sharedPrefs: SharedPrefs): String =
        sharedPrefs.getPrefs(SharedPrefsConstant.PREFERRED_JOB_CATEGORY, "") ?: ""
}
