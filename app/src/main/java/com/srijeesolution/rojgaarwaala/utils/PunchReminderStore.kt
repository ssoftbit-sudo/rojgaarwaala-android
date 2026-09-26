package com.srijeesolution.rojgaarwaala.utils

import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PunchReminderStore {
    fun save(
        prefs: SharedPrefs,
        dutyStart: String?,
        dutyEnd: String?,
        punchedInToday: Boolean,
        punchedOutToday: Boolean,
        today: String,
    ) {
        prefs.setPrefsData(Pair(SharedPrefsConstant.PUNCH_DUTY_START, dutyStart.orEmpty()))
        prefs.setPrefsData(Pair(SharedPrefsConstant.PUNCH_DUTY_END, dutyEnd.orEmpty()))
        prefs.setPrefsData(Pair(SharedPrefsConstant.PUNCH_IN_DONE_TODAY, punchedInToday))
        prefs.setPrefsData(Pair(SharedPrefsConstant.PUNCH_OUT_DONE_TODAY, punchedOutToday))
        prefs.setPrefsData(Pair(SharedPrefsConstant.PUNCH_REMINDER_DATE, today))
    }

    fun dutyStart(prefs: SharedPrefs): String =
        prefs.getPrefs(SharedPrefsConstant.PUNCH_DUTY_START, "").orEmpty()

    fun dutyEnd(prefs: SharedPrefs): String =
        prefs.getPrefs(SharedPrefsConstant.PUNCH_DUTY_END, "").orEmpty()

    fun punchedInToday(prefs: SharedPrefs, today: String = todayDate()): Boolean {
        return prefs.getPrefs(SharedPrefsConstant.PUNCH_REMINDER_DATE, "") == today &&
            prefs.getPrefs(SharedPrefsConstant.PUNCH_IN_DONE_TODAY, false)
    }

    fun punchedOutToday(prefs: SharedPrefs, today: String = todayDate()): Boolean {
        return prefs.getPrefs(SharedPrefsConstant.PUNCH_REMINDER_DATE, "") == today &&
            prefs.getPrefs(SharedPrefsConstant.PUNCH_OUT_DONE_TODAY, false)
    }

    fun markDone(prefs: SharedPrefs, punchIn: Boolean, today: String = todayDate()) {
        prefs.setPrefsData(Pair(SharedPrefsConstant.PUNCH_REMINDER_DATE, today))
        if (punchIn) {
            prefs.setPrefsData(Pair(SharedPrefsConstant.PUNCH_IN_DONE_TODAY, true))
        } else {
            prefs.setPrefsData(Pair(SharedPrefsConstant.PUNCH_OUT_DONE_TODAY, true))
        }
    }

    fun clear(prefs: SharedPrefs) {
        prefs.removeSharedPrefs(SharedPrefsConstant.PUNCH_DUTY_START)
        prefs.removeSharedPrefs(SharedPrefsConstant.PUNCH_DUTY_END)
        prefs.removeSharedPrefs(SharedPrefsConstant.PUNCH_IN_DONE_TODAY)
        prefs.removeSharedPrefs(SharedPrefsConstant.PUNCH_OUT_DONE_TODAY)
        prefs.removeSharedPrefs(SharedPrefsConstant.PUNCH_REMINDER_DATE)
    }

    fun todayDate(nowMillis: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
    }
}
