package com.srijeesolution.rojgaarwaala.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    fun getRelativeTimeSpanString(context: Context, timeString: String?): String {
        if (timeString.isNullOrEmpty()) return ""

        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(timeString) ?: return ""

            val now = Date()
            val diffInMillis = now.time - date.time

            val seconds = diffInMillis / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val months = days / 30
            val years = days / 365

            return when {
                years > 0 -> "$years year${if (years > 1) "s" else ""} ago"
                months > 0 -> "$months month${if (months > 1) "s" else ""} ago"
                days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
                hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
                minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
                else -> "Just now"
            }
        } catch (e: Exception) {
            ""
        }
    }
}