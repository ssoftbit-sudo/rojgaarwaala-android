package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.ActivityLoginBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    @Inject
    lateinit var sharedPrefs: SharedPrefs
    private lateinit var homePageViewModel: HomePageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        observeOtpRequest()
        binding.submitButton.setOnClickListener {
            requestOtp()
        }
        binding.signUpButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding.skipButton.setOnClickListener {
            sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.USER_SKIP_STATUS, true))
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finishAffinity()
        }
    }

    private fun requestOtp() {
        val email = binding.mobileEditText.text.toString().trim()
        if (email.isEmpty()) {
            binding.mobileEditText.error = "Email is required"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.mobileEditText.error = "Enter a valid email address"
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        val requestBody = HashMap<String, String>()
        requestBody["email"] = email
        requestBody["password"] = "password"
        homePageViewModel.onLoginData(requestBody)
    }

    private fun observeOtpRequest() {
        homePageViewModel.loginRegisterLiveData.observe(this) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (apiResponse.data?.dataObj != null) {
                        // Navigate to OTP screen, pass mobile number
                        val intent = Intent(this, OtpActivity::class.java)
                        intent.putExtra("mobile", binding.mobileEditText.text.toString().trim())
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, apiResponse.data?.message ?: "Failed to send OTP", Toast.LENGTH_SHORT).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to send OTP", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}