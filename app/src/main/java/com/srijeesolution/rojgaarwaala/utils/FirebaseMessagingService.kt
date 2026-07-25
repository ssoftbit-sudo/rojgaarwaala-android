package com.srijeesolution.rojgaarwaala.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.MainActivity
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant

class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FirebaseMsgService"
        private const val CHANNEL_ID = NotificationUtils.CHANNEL_ID
        private const val CHANNEL_NAME = "Rojgaarwaala Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications from Rojgaarwaala app"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        
        // Save FCM token to SharedPreferences
        val sharedPrefs = SharedPrefs(this)
        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.FCM_TOKEN, token))
        Log.d(TAG, "FCM Token saved to SharedPreferences: $token")
        
        // Here you can send the token to your server
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "=== FIREBASE MESSAGE RECEIVED ===")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Full remote message: $remoteMessage")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Message Type: ${remoteMessage.messageType}")
        Log.d(TAG, "Collapse Key: ${remoteMessage.collapseKey}")
        Log.d(TAG, "Priority: ${remoteMessage.priority}")
        Log.d(TAG, "Original Priority: ${remoteMessage.originalPriority}")
        Log.d(TAG, "Sent Time: ${remoteMessage.sentTime}")
        Log.d(TAG, "TTL: ${remoteMessage.ttl}")
        
        // Log all data keys
        Log.d(TAG, "Data keys: ${remoteMessage.data.keys}")
        
        // Log the notification data
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val type = remoteMessage.data["type"]
            val id = remoteMessage.data["id"]
            val title = remoteMessage.notification?.title
            val body = remoteMessage.notification?.body
            val image = remoteMessage.notification?.imageUrl
            Log.d(TAG, "Extracted - Type: $type, ID: $id, Title: $title, Body: $body")
        } else {
            Log.d(TAG, "No data payload found")
        }

        // Log the notification body
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            Log.d(TAG, "Message Notification Title: ${it.title}")
            Log.d(TAG, "Message Notification Image: ${it.imageUrl}")
            Log.d(TAG, "Message Notification Icon: ${it.icon}")
            Log.d(TAG, "Message Notification Color: ${it.color}")
            Log.d(TAG, "Message Notification Tag: ${it.tag}")
            Log.d(TAG, "Message Notification Click Action: ${it.clickAction}")
        }

        // Always create and show notification (even for background)
        val type = remoteMessage.data["type"]
        val id = remoteMessage.data["id"]
        val applicationId = remoteMessage.data["application_id"]
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Rojgaarwaala"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "New notification"
        
        Log.d(TAG, "Final Notification Type: $type, ID: $id, Application ID: $applicationId")
        Log.d(TAG, "Creating notification with title: $title, body: $body")
        sendNotification(title, body, type, id, applicationId)
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement this method to send token to your app server.
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    private fun sendNotification(
        title: String?,
        messageBody: String?,
        type: String?,
        id: String?,
        applicationId: String? = null,
    ) {
        Log.d(TAG, "=== SENDING NOTIFICATION ===")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Body: $messageBody")
        Log.d(TAG, "Type: $type")
        Log.d(TAG, "ID: $id")
        Log.d(TAG, "Application ID: $applicationId")

        val resolvedApplicationId = applicationId ?: id
        val deepLinkUrl = if (type == "job_application_status") {
            "rojgaarwaala://notification?type=$type&application_id=$resolvedApplicationId"
        } else {
            "rojgaarwaala://notification?type=$type&id=$id"
        }
        Log.d(TAG, "Deep link URL: $deepLinkUrl")
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(deepLinkUrl)
            setPackage(packageName)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            
            putExtra("notification_type", type)
            putExtra("notification_id", id)
            putExtra("type", type)
            putExtra("id", id)
            putExtra("application_id", resolvedApplicationId)
            
            Log.d(TAG, "Intent created with data: $data")
            Log.d(TAG, "Intent package: $packageName")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        Log.d(TAG, "PendingIntent created: $pendingIntent")

        val channelId = CHANNEL_ID
        val defaultSoundUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title ?: "Rojgaarwaala")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $channelId")
        }

        // Show in-app bell on MainActivity when user opens the app
        val sharedPrefs = SharedPrefs(this)
        if (type == "job_application_status") {
            sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.JOB_STATUS_UPDATE_PENDING, true))
        } else {
            sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.NOTIFICATION_BADGE_PENDING, true))
        }

        // Use a unique notification ID to ensure proper handling
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        Log.d(TAG, "Notification sent with ID: $notificationId")
        Log.d(TAG, "=== NOTIFICATION SENT SUCCESSFULLY ===")
    }
} 