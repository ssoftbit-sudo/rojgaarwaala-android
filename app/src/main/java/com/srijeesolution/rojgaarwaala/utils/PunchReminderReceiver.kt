package com.srijeesolution.rojgaarwaala.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.AttendanceDashboardActivity
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs

class PunchReminderReceiver : BroadcastReceiver() {
    companion object {
        /** Fixed id: each 2-minute reminder replaces the last one instead of stacking. */
        const val PUNCH_IN_NOTIFICATION_ID = 8113
        const val PUNCH_OUT_NOTIFICATION_ID = 8114
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = SharedPrefs(context.applicationContext)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (PunchReminderStore.dutyStart(prefs).isNotBlank() ||
                PunchReminderStore.dutyEnd(prefs).isNotBlank()
            ) {
                PunchReminderScheduler.schedule(context, prefs)
            }
            return
        }
        if (intent.action != PunchReminderScheduler.ACTION_PUNCH_REMINDER) return

        val punchIn = intent.getBooleanExtra(PunchReminderScheduler.EXTRA_PUNCH_IN, true)
        val duty = if (punchIn) PunchReminderStore.dutyStart(prefs) else PunchReminderStore.dutyEnd(prefs)
        if (duty.isBlank()) return
        // Keeps firing every 2 minutes until the punch is recorded on the server.
        // Showing a reminder never counts as done, and nothing here reads GPS.
        val pending = if (punchIn) {
            !PunchReminderStore.punchedInToday(prefs)
        } else {
            PunchReminderStore.punchedInToday(prefs) && !PunchReminderStore.punchedOutToday(prefs)
        }
        if (pending && PunchReminderTimes.inReminderWindow(duty, System.currentTimeMillis())) {
            showNotification(context, punchIn)
        }
        PunchReminderScheduler.schedule(context, prefs)
    }

    private fun showNotification(context: Context, punchIn: Boolean) {
        NotificationUtils.ensureNotificationChannels(context)
        val punch = if (punchIn) {
            AttendanceDashboardActivity.PUNCH_IN
        } else {
            AttendanceDashboardActivity.PUNCH_OUT
        }
        val title = if (punchIn) PunchInReminderPolicy.NOTIFICATION_TITLE else "Punch Out याद रखें"
        val body = if (punchIn) {
            PunchInReminderPolicy.NOTIFICATION_BODY
        } else {
            "क्या आप घर जा रहे हैं? अपना Punch Out करना न भूलें!"
        }
        val open = Intent(context, AttendanceDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AttendanceDashboardActivity.EXTRA_PUNCH, punch)
            putExtra("notification_type", if (punchIn) {
                "attendance_punch_in_reminder"
            } else {
                "attendance_punch_out_reminder"
            })
        }
        val pending = PendingIntent.getActivity(
            context,
            if (punchIn) 8111 else 8112,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(if (punchIn) PUNCH_IN_NOTIFICATION_ID else PUNCH_OUT_NOTIFICATION_ID, notification)
    }
}
