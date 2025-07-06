package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.ActivityOtpBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OtpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpBinding
    private lateinit var homePageViewModel: HomePageViewModel
    private var mobile: String? = null

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        mobile = intent.getStringExtra("mobile")

        observeVerifyOtp()
        binding.submitOtpButton.setOnClickListener {
            verifyOtp()
        }
    }

    private fun verifyOtp() {
        val otp = binding.otpEditText.text.toString().trim()
        if (TextUtils.isEmpty(otp)) {
            binding.otpEditText.error = "OTP is required"
            return
        }
        if (otp.length < 4) {
            binding.otpEditText.error = "Enter a valid OTP"
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        val requestBody = HashMap<String, String>()
        requestBody["mobile"] = mobile ?: ""
        requestBody["otp"] = otp
        homePageViewModel.verifyOtp(requestBody)
    }

    private fun observeVerifyOtp() {
        homePageViewModel.verifyOtpLiveData.observe(this) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (apiResponse.data?.status == true) {
                        // Save user data and token if available
                        apiResponse.data.dataObj?.let { dataObj ->
                            dataObj.token?.let { token ->
                                sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.USER_AUTH_TOKEN, token))
                            }
                        }
                        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.USER_LOGGED_IN_STATUS, true))
                        Toast.makeText(this, "Successfully logged in!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finishAffinity()
                    } else {
                        Toast.makeText(this, apiResponse.data?.message ?: "Invalid OTP", Toast.LENGTH_SHORT).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 