package com.srijeesolution.rojgaarwaala.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs

object PunchReminderScheduler {
    const val ACTION_PUNCH_REMINDER = "com.srijeesolution.rojgaarwaala.ACTION_PUNCH_REMINDER"
    const val EXTRA_PUNCH_IN = "punch_in"
    const val REQUEST_PUNCH_IN = 8101
    const val REQUEST_PUNCH_OUT = 8102

    /**
     * One alarm per kind, keyed by a fixed request code, so calling this again on
     * every login, sync or app start replaces the pending alarm instead of adding
     * a second one.
     */
    fun schedule(context: Context, prefs: SharedPrefs) {
        val now = System.currentTimeMillis()
        val punchedIn = PunchReminderStore.punchedInToday(prefs)
        val inAt = PunchReminderTimes.nextReminderMillis(
            PunchReminderStore.dutyStart(prefs),
            now,
            doneToday = punchedIn,
        )
        // Punch-out reminders only matter once the employee has punched in today.
        // Punching in reschedules, which brings today's punch-out window back.
        val outAt = PunchReminderTimes.nextReminderMillis(
            PunchReminderStore.dutyEnd(prefs),
            now,
            doneToday = !punchedIn || PunchReminderStore.punchedOutToday(prefs),
        )
        setAlarm(context, REQUEST_PUNCH_IN, true, inAt)
        setAlarm(context, REQUEST_PUNCH_OUT, false, outAt)
        Log.d("PunchReminder", "scheduled in=$inAt out=$outAt")
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent(context, REQUEST_PUNCH_IN, true))
        manager.cancel(pendingIntent(context, REQUEST_PUNCH_OUT, false))
    }

    private fun setAlarm(context: Context, requestCode: Int, punchIn: Boolean, atMillis: Long?) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, requestCode, punchIn)
        if (atMillis == null) {
            manager.cancel(pending)
            return
        }
        // Android 12+ denies exact alarms unless the user allowed "Alarms & reminders".
        // The fallback must still fire while the phone sleeps in a pocket, which a
        // plain set() does not, or the 2-minute punch-in reminders go quiet.
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (!exactAllowed) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
            return
        }
        val show = pendingIntent(context, requestCode, punchIn)
        try {
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, show), pending)
        } catch (t: Throwable) {
            Log.w("PunchReminder", "exact alarm failed, falling back", t)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        }
    }

    private fun pendingIntent(context: Context, requestCode: Int, punchIn: Boolean): PendingIntent {
        val intent = Intent(context, PunchReminderReceiver::class.java).apply {
            action = ACTION_PUNCH_REMINDER
            putExtra(EXTRA_PUNCH_IN, punchIn)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
