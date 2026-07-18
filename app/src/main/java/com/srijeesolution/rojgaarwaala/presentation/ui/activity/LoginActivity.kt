package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.ActivityLoginBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
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

        observeSendOtp()
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

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun normalizeMobile(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        val normalized = when {
            digits.length == 10 -> digits
            digits.length > 10 && digits.startsWith("91") -> digits.takeLast(10)
            else -> null
        }
        return normalized?.takeIf { it.length == 10 && it.first() in '6'..'9' }
    }

    private fun requestOtp() {
        val mobile = normalizeMobile(binding.mobileEditText.text.toString().trim())
        if (mobile.isNullOrEmpty()) {
            binding.mobileEditText.error = "Enter a valid 10-digit mobile number"
            return
        }

        hideKeyboard()
        binding.submitButton.isEnabled = false
        binding.submitButton.text = "Sending..."
        binding.progressBar.visibility = View.VISIBLE

        val requestBody = HashMap<String, String>()
        requestBody["mobile"] = mobile
        homePageViewModel.sendOtp(requestBody)
    }

    private fun observeSendOtp() {
        homePageViewModel.sendOtpLiveData.observe(this) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.submitButton.isEnabled = true
                    binding.submitButton.text = "Send OTP"

                    if (apiResponse.data?.status == true) {
                        val mobile = normalizeMobile(binding.mobileEditText.text.toString().trim()).orEmpty()
                        val intent = Intent(this, OtpActivity::class.java)
                        intent.putExtra("mobile", mobile)
                        apiResponse.data.dataObj?.otp?.takeIf { it.isNotBlank() }?.let { otp ->
                            intent.putExtra("fallback_otp", otp)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            apiResponse.data?.message ?: "Failed to send OTP",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.submitButton.isEnabled = true
                    binding.submitButton.text = "Send OTP"
                    Toast.makeText(
                        this,
                        parseApiErrorMessage(apiResponse.message),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun parseApiErrorMessage(error: ApiError?): String {
        val body = error?.errorBody.orEmpty()
        if (body.isNotBlank()) {
            try {
                val message = JSONObject(body).optString("message")
                if (message.isNotBlank()) return message
            } catch (_: Exception) {
                // fall through
            }
        }
        return error?.errorMsg?.takeIf { it.isNotBlank() } ?: "Failed to send OTP"
    }
}
