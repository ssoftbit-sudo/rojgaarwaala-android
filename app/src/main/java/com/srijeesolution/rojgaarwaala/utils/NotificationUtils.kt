package com.srijeesolution.rojgaarwaala.utils

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

object NotificationUtils {
    private const val TAG = "NotificationUtils"
    private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    const val CHANNEL_ID = "rojgaarwaala_channel"
    private const val LEGACY_CHANNEL_ID = "default_channel"

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            CHANNEL_ID to "Rojgaarwaala Notifications",
            LEGACY_CHANNEL_ID to "Rojgaarwaala Notifications (legacy)",
        ).forEach { (id, name) ->
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications from Rojgaarwaala app"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun requestNotificationPermission(activity: Activity) {
        ensureNotificationChannels(activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    fun getFirebaseToken(onTokenReceived: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")
            onTokenReceived(token ?: "")
        }
    }

    fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) {
                    "Subscribed to $topic"
                } else {
                    "Failed to subscribe to $topic"
                }
                Log.d(TAG, msg)
            }
    }

    fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) {
                    "Unsubscribed from $topic"
                } else {
                    "Failed to unsubscribe from $topic"
                }
                Log.d(TAG, msg)
            }
    }
} 