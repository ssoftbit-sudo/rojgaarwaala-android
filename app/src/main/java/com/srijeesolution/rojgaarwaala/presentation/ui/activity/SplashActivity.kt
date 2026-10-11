package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.srijeesolution.rojgaarwaala.R
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

        // The system splash can only show an icon. This screen carries the name and
        // tagline, held just long enough to be read.
        setContentView(R.layout.activity_splash)
        // Some phones still draw a title bar here whatever the theme says.
        supportActionBar?.hide()
        actionBar?.hide()
        listOf(
            androidx.appcompat.R.id.action_bar_container,
            resources.getIdentifier("action_bar_container", "id", "android"),
        ).forEach { id -> if (id != 0) findViewById<View>(id)?.visibility = View.GONE }
        // The logo is already on screen from the system splash; only the name and
        // tagline arrive, so it reads as one splash rather than two.
        findViewById<View>(R.id.splashCaption)?.animate()?.alpha(1f)?.setDuration(250)?.start()
        keepSplash = false
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !isDestroyed) navigateNext()
        }, BRAND_HOLD_MS)
    }

    private fun navigateNext() {
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

    companion object {
        private const val BRAND_HOLD_MS = 900L
    }
}
