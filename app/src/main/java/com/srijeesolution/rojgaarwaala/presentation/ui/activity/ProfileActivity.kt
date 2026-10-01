package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.utils.AuthNavigation
import com.srijeesolution.rojgaarwaala.utils.ProfileGate
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_host)
        val required = intent.getBooleanExtra(AuthNavigation.EXTRA_FROM_OTP, false)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.profileHost,
                    ProfileFragment.newInstance(fromOtp = required),
                )
                .commit()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(required) {
                override fun handleOnBackPressed() {
                    if (!ProfileGate.isComplete(sharedPrefs)) {
                        Toast.makeText(
                            this@ProfileActivity,
                            R.string.profile_required_to_continue,
                            Toast.LENGTH_SHORT,
                        ).show()
                        return
                    }
                    finish()
                }
            },
        )
    }
}
