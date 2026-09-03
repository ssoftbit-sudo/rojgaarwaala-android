package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import java.util.UUID

object DeviceKeyUtils {
    fun getOrCreateDeviceKey(sharedPrefs: SharedPrefs): String {
        val existing = sharedPrefs.getPrefs(SharedPrefsConstant.DEVICE_KEY, "").orEmpty()
        if (existing.isNotBlank()) {
            return existing
        }

        val generated = UUID.randomUUID().toString()
        sharedPrefs.setPrefsData(SharedPrefsConstant.DEVICE_KEY to generated)
        return generated
    }
}
