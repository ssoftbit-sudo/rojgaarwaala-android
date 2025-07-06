package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    @Inject
    lateinit var sharedPrefs: SharedPrefs
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate()
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Keep the splash screen visible for this Activity
        splashScreen.setKeepOnScreenCondition { true }
        
        // Navigate to the appropriate screen after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = when {
                sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false) -> {
                    Intent(this, MainActivity::class.java)
                }
                sharedPrefs.getPrefs(SharedPrefsConstant.USER_SKIP_STATUS, false) -> {
                    Intent(this, MainActivity::class.java)
                }
                else -> {
                    Intent(this, LoginActivity::class.java)
                }
            }
            
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 1000) // Reduced delay since we're using the system splash screen
    }
}