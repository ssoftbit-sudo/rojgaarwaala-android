package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.utils.AuthNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_host)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.profileHost,
                    ProfileFragment.newInstance(
                        fromOtp = intent.getBooleanExtra(AuthNavigation.EXTRA_FROM_OTP, false),
                    ),
                )
                .commit()
        }
    }
}
