package com.srijeesolution.rojgaarwaala.utils

import android.content.Context
import android.content.Intent
import com.srijeesolution.rojgaarwaala.data.remote.model.UserData
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.ProfileActivity
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant

/**
 * An active user must finish candidate profile (name, mobile, category, district,
 * map pin, resume) before Home / Free Job / Apply are available.
 */
object ProfileGate {
    fun isComplete(sharedPrefs: SharedPrefs): Boolean {
        return sharedPrefs.getPrefs(SharedPrefsConstant.CANDIDATE_PROFILE_COMPLETE, false)
    }

    fun remember(sharedPrefs: SharedPrefs, complete: Boolean) {
        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.CANDIDATE_PROFILE_COMPLETE, complete))
    }

    fun remember(sharedPrefs: SharedPrefs, user: UserData?) {
        remember(sharedPrefs, !needsCompletion(user))
    }

    fun needsCompletion(user: UserData?): Boolean {
        if (user?.candidateProfileComplete == true) return false
        if (user?.needsProfileCompletion == false) return false
        return true
    }

    fun clear(sharedPrefs: SharedPrefs) {
        sharedPrefs.removeSharedPrefs(SharedPrefsConstant.CANDIDATE_PROFILE_COMPLETE)
    }

    fun requiredProfileIntent(context: Context): Intent {
        return Intent(context, ProfileActivity::class.java)
            .putExtra(AuthNavigation.EXTRA_FROM_OTP, true)
    }
}
