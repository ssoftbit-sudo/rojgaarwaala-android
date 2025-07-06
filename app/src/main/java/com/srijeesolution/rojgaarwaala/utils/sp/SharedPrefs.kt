package com.srijeesolution.rojgaarwaala.utils.sp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import javax.inject.Inject

class SharedPrefs @Inject constructor(context: Context) {

    private val preferences: SharedPreferences = context.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE)

    fun setPrefsData(pair: Pair<String, Any>){
        val key = pair.first
        val value = pair.second
        Log.d("SharedPref", "setPrefsData key ${pair.first} value= ${pair.second}")
        val edit = preferences.edit()
        when (value) {
            is String -> edit.putString(key, value)
            is Int -> edit.putInt(key, value)
            is Boolean -> edit.putBoolean(key, value)
            is Long -> edit.putLong(key, value)
            is Float -> edit.putFloat(key, value)
            else -> error("Only primitive types can be stored in SharedPreferences")
        }.apply()
    }

    fun getPrefs(key : String, default: String = "") = preferences.getString(key, default)
    fun getPrefs(key : String, default: Int = -1) = preferences.getInt(key, default)
    fun getPrefs(key : String, default: Boolean = false) = preferences.getBoolean(key, default)
    fun getPrefs(key : String, default: Long = 0L) = preferences.getLong(key, default)
    fun getPrefs(key : String, default: Float = 0F) = preferences.getFloat(key, default)

    fun removeSharedPrefs( key: String) {
        if(checkSharedPrefs(key)) {
            val editor = preferences.edit()
            editor.remove(key)
            editor.apply()
        }
    }

    fun checkSharedPrefs(key: String): Boolean {
       return preferences.contains(key)
    }

    companion object {
        private const val APP_NAME = "com.kaarigar"
    }
}