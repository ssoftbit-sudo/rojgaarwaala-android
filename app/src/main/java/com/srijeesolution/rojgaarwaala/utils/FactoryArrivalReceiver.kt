package com.srijeesolution.rojgaarwaala.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.AttendanceDashboardActivity

class FactoryArrivalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val title = "Rojgaarwaala"
        val body = "क्या आप फैक्ट्री पहुंच गए हैं? अटेंडेंस लगाएं।"
        val punchIntent = Intent(context, AttendanceDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AttendanceDashboardActivity.EXTRA_PUNCH, AttendanceDashboardActivity.PUNCH_IN)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            7102,
            punchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    NotificationUtils.CHANNEL_ID,
                    "Rojgaarwaala Notifications",
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }

        val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(7103, notification)
    }
}
