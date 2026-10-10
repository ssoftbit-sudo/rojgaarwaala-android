package com.srijeesolution.rojgaarwaala.utils

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.LoginActivity
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant

/**
 * One place that ends a session, whether the user logged out or the server stopped
 * accepting the token. Without it a rejected token left every screen failing quietly.
 */
object SessionExpiry {

    /** A 401 from these means "wrong OTP" or similar, not "your session ended". */
    private val AUTH_PATHS = listOf("login", "register", "send-otp", "verify-otp", "logout")

    /**
     * Only a request that carried a token can prove the token is dead. Kept pure so
     * JVM tests cover it.
     */
    fun shouldLogout(code: Int, sentAuthHeader: Boolean, path: String): Boolean {
        if (code != 401 || !sentAuthHeader) return false
        val last = path.trimEnd('/').substringAfterLast('/')
        return last !in AUTH_PATHS
    }

    /** Forgets the signed-in user. Shared by manual logout and an expired session. */
    fun clearSession(context: Context, prefs: SharedPrefs) {
        PunchReminderSync.clear(context, prefs)
        prefs.removeSharedPrefs(SharedPrefsConstant.USER_AUTH_TOKEN)
        prefs.removeSharedPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS)
        ProfileGate.clear(prefs)
    }

    /**
     * Called from the network thread. Several requests can be rejected together when
     * a screen loads; the token check makes every one after the first a no-op.
     */
    @Synchronized
    fun handle(context: Context, prefs: SharedPrefs) {
        if (prefs.getPrefs(SharedPrefsConstant.USER_AUTH_TOKEN, "").isNullOrBlank()) return

        val appContext = context.applicationContext
        clearSession(appContext, prefs)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(appContext, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            appContext.startActivity(
                Intent(appContext, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
        }
    }
}
