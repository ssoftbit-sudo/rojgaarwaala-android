package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.srijeesolution.rojgaarwaala.utils.ProfileGate
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity(),
    com.srijeesolution.rojgaarwaala.utils.ManualEdgeToEdge {
    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private var keepSplash = true

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            val splashScreen = installSplashScreen()
            super.onCreate(savedInstanceState)
            splashScreen.setKeepOnScreenCondition { keepSplash }
        } catch (t: Throwable) {
            Log.w("SplashActivity", "SplashScreen API failed; using fallback theme", t)
            super.onCreate(savedInstanceState)
        }

        navigateNext()
    }

    private fun navigateNext() {
        keepSplash = false
        val loggedIn = sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)
        val intent = when {
            loggedIn && !ProfileGate.isComplete(sharedPrefs) -> {
                ProfileGate.requiredProfileIntent(this)
            }
            loggedIn -> {
                Intent(this, MainActivity::class.java)
            }
            else -> {
                Intent(this, LoginActivity::class.java)
            }
        }
        this.intent.extras?.let { extras ->
            if (!extras.isEmpty) {
                intent.putExtras(extras)
            }
        }
        this.intent.data?.let { intent.data = it }
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
