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
        val alreadyDone = if (punchIn) {
            PunchReminderStore.punchedInToday(prefs)
        } else {
            PunchReminderStore.punchedOutToday(prefs)
        }
        if (!alreadyDone) {
            showNotification(context, punchIn)
            PunchReminderStore.markDone(prefs, punchIn)
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
        val title = if (punchIn) "Punch In याद रखें" else "Punch Out याद रखें"
        val body = if (punchIn) {
            "क्या आप आ गए हैं? अपना Punch In करना न भूलें!"
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
        manager.notify(if (punchIn) 8113 else 8114, notification)
    }
}
