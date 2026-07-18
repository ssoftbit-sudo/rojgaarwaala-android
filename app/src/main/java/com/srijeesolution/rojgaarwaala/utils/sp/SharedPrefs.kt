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

    fun setVideoLiked(videoId: Int, isLiked: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean("video_liked_$videoId", isLiked)
        editor.apply()
    }

    fun isVideoLiked(videoId: Int): Boolean {
        return preferences.getBoolean("video_liked_$videoId", false)
    }

    fun setVideoDisliked(videoId: Int, isDisliked: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean("video_disliked_$videoId", isDisliked)
        editor.apply()
    }

    fun isVideoDisliked(videoId: Int): Boolean {
        return preferences.getBoolean("video_disliked_$videoId", false)
    }

    fun setImageLiked(imageKey: Int, isLiked: Boolean) {
        preferences.edit().putBoolean("image_liked_$imageKey", isLiked).apply()
    }

    fun isImageLiked(imageKey: Int): Boolean {
        return preferences.getBoolean("image_liked_$imageKey", false)
    }

    fun setImageDisliked(imageKey: Int, isDisliked: Boolean) {
        preferences.edit().putBoolean("image_disliked_$imageKey", isDisliked).apply()
    }

    fun isImageDisliked(imageKey: Int): Boolean {
        return preferences.getBoolean("image_disliked_$imageKey", false)
    }

    fun getImageLikeCount(imageKey: Int): Int {
        return preferences.getInt("image_like_count_$imageKey", 0)
    }

    fun setImageLikeCount(imageKey: Int, count: Int) {
        preferences.edit().putInt("image_like_count_$imageKey", count.coerceAtLeast(0)).apply()
    }

    fun getImageDislikeCount(imageKey: Int): Int {
        return preferences.getInt("image_dislike_count_$imageKey", 0)
    }

    fun setImageDislikeCount(imageKey: Int, count: Int) {
        preferences.edit().putInt("image_dislike_count_$imageKey", count.coerceAtLeast(0)).apply()
    }

    fun setStoryLiked(storyId: Int, isLiked: Boolean) {
        preferences.edit().putBoolean("story_liked_$storyId", isLiked).apply()
    }

    fun isStoryLiked(storyId: Int): Boolean {
        return preferences.getBoolean("story_liked_$storyId", false)
    }

    companion object {
        private const val APP_NAME = "com.kaarigar"
    }
}