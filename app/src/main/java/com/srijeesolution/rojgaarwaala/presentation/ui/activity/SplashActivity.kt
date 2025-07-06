package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.ActivitySplashBinding
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    @Inject
    lateinit var sharedPrefs: SharedPrefs
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.brand_color_yellow)

        val scaleUp = ScaleAnimation(
            1f, 1.1f,
            1f, 1.1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleUp.duration = 750
        scaleUp.repeatMode = Animation.REVERSE
        scaleUp.repeatCount = Animation.INFINITE

        binding.logoSplash.startAnimation(scaleUp)
        Handler(Looper.getMainLooper()).postDelayed({
            if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }else if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_SKIP_STATUS, false)) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }else {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 3500) // 3000 is the delayed time in milliseconds.
    }
}