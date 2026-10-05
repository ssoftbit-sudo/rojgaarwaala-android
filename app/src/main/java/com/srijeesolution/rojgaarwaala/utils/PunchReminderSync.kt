package com.srijeesolution.rojgaarwaala.utils

import android.app.NotificationManager
import android.content.Context
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeDashboardData
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs

object PunchReminderSync {
    fun apply(context: Context, prefs: SharedPrefs, data: EmployeeDashboardData?) {
        val today = data?.today
        val factory = today?.factory
        val dutyStart = factory?.dutyStart.orEmpty()
        val dutyEnd = factory?.dutyEnd.orEmpty()
        // Staff who approve attendance never punch, and without an assignment
        // there is nothing to punch into, so neither should be reminded.
        val remindable = data?.employee?.isAuthorisedStaff() != true &&
            data?.employee?.isActive != false &&
            today?.hasActiveAssignment != false
        if (!remindable || (dutyStart.isBlank() && dutyEnd.isBlank())) {
            clear(context, prefs)
            return
        }
        PunchReminderStore.saveFactory(
            prefs = prefs,
            name = factory?.name,
            latitude = factory?.latitude,
            longitude = factory?.longitude,
            radius = factory?.geofenceRadius,
        )
        val selfAbsent = SelfAbsentPolicy.silencesReminders(today?.selfAbsent, today?.punchInAt)
        val punchedIn = selfAbsent || PunchInReminderPolicy.attendanceRecorded(
            punchInAt = today?.punchInAt,
            attendanceMarked = today?.attendanceMarked,
            canPunchIn = today?.canPunchIn,
        )
        // No punch-in is coming, so there is no punch-out to remind about either.
        val punchedOut = selfAbsent || !today?.punchOutAt.isNullOrBlank()
        PunchReminderStore.save(
            prefs = prefs,
            dutyStart = dutyStart,
            dutyEnd = dutyEnd,
            punchedInToday = punchedIn,
            punchedOutToday = punchedOut,
            today = today?.date?.takeIf { it.isNotBlank() } ?: PunchReminderStore.todayDate(),
        )
        if (punchedIn) dismiss(context, PunchReminderReceiver.PUNCH_IN_NOTIFICATION_ID)
        if (punchedOut) dismiss(context, PunchReminderReceiver.PUNCH_OUT_NOTIFICATION_ID)
        PunchReminderScheduler.schedule(context, prefs)
    }

    /**
     * The server recorded the punch-in: today's 2-minute reminders stop now, not at
     * the next dashboard load. Finding the factory on GPS alone never stops them.
     */
    fun onPunchedIn(context: Context, prefs: SharedPrefs) {
        PunchReminderStore.markDone(prefs, punchIn = true)
        dismiss(context, PunchReminderReceiver.PUNCH_IN_NOTIFICATION_ID)
        PunchReminderScheduler.schedule(context, prefs)
    }

    /** Same for punch-out: today's punch-out reminders stop once it is recorded. */
    fun onPunchedOut(context: Context, prefs: SharedPrefs) {
        PunchReminderStore.markDone(prefs, punchIn = false)
        dismiss(context, PunchReminderReceiver.PUNCH_OUT_NOTIFICATION_ID)
        PunchReminderScheduler.schedule(context, prefs)
    }

    /** The employee marked themselves absent: neither reminder has anything left to ask today. */
    fun onMarkedAbsent(context: Context, prefs: SharedPrefs) {
        PunchReminderStore.markDone(prefs, punchIn = true)
        PunchReminderStore.markDone(prefs, punchIn = false)
        dismiss(context, PunchReminderReceiver.PUNCH_IN_NOTIFICATION_ID)
        dismiss(context, PunchReminderReceiver.PUNCH_OUT_NOTIFICATION_ID)
        PunchReminderScheduler.schedule(context, prefs)
    }

    fun clear(context: Context, prefs: SharedPrefs) {
        PunchReminderScheduler.cancel(context)
        PunchReminderStore.clear(prefs)
        dismiss(context, PunchReminderReceiver.PUNCH_IN_NOTIFICATION_ID)
        dismiss(context, PunchReminderReceiver.PUNCH_OUT_NOTIFICATION_ID)
    }

    private fun dismiss(context: Context, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }
}
