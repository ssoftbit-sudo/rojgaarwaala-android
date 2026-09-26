package com.srijeesolution.rojgaarwaala.utils

import android.content.Context
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeDashboardData
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs

object PunchReminderSync {
    fun apply(context: Context, prefs: SharedPrefs, data: EmployeeDashboardData?) {
        val today = data?.today
        val factory = today?.factory
        val dutyStart = factory?.dutyStart.orEmpty()
        val dutyEnd = factory?.dutyEnd.orEmpty()
        if (dutyStart.isBlank() && dutyEnd.isBlank()) {
            clear(context, prefs)
            return
        }
        PunchReminderStore.save(
            prefs = prefs,
            dutyStart = dutyStart,
            dutyEnd = dutyEnd,
            punchedInToday = !today?.punchInAt.isNullOrBlank(),
            punchedOutToday = !today?.punchOutAt.isNullOrBlank(),
            today = today?.date?.takeIf { it.isNotBlank() } ?: PunchReminderStore.todayDate(),
        )
        PunchReminderScheduler.schedule(context, prefs)
    }

    fun clear(context: Context, prefs: SharedPrefs) {
        PunchReminderScheduler.cancel(context)
        PunchReminderStore.clear(prefs)
    }
}
