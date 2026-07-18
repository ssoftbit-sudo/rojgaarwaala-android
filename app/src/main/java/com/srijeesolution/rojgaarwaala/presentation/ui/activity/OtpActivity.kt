package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.ActivityOtpBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class OtpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpBinding
    private lateinit var homePageViewModel: HomePageViewModel
    private var mobile: String? = null
    private var resendTimer: CountDownTimer? = null

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        mobile = intent.getStringExtra("mobile")
        binding.otpSubtitleText.text = "Enter the OTP sent to +91 ${mobile.orEmpty()}"

        intent.getStringExtra("fallback_otp")?.takeIf { it.isNotBlank() }?.let { otp ->
            binding.fallbackOtpText.visibility = View.VISIBLE
            binding.fallbackOtpText.text = "SMS not received? Use OTP: $otp"
            binding.otpEditText.setText(otp)
        }

        observeVerifyOtp()
        observeResendOtp()
        binding.submitOtpButton.setOnClickListener { verifyOtp() }
        binding.resendOtpButton.setOnClickListener { resendOtp() }
        startResendCooldown(60)
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        super.onDestroy()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun verifyOtp() {
        val otp = binding.otpEditText.text.toString().trim()
        if (TextUtils.isEmpty(otp)) {
            binding.otpEditText.error = "OTP is required"
            return
        }
        if (otp.length < 6) {
            binding.otpEditText.error = "Enter the 6-digit OTP"
            return
        }

        hideKeyboard()
        binding.submitOtpButton.isEnabled = false
        binding.submitOtpButton.text = "Verifying..."
        binding.progressBar.visibility = View.VISIBLE

        val requestBody = HashMap<String, String>()
        requestBody["mobile"] = mobile ?: ""
        requestBody["otp"] = otp
        homePageViewModel.verifyOtp(requestBody)
    }

    private fun resendOtp() {
        val phone = mobile?.takeIf { it.isNotBlank() } ?: return
        binding.resendOtpButton.isEnabled = false
        binding.resendOtpButton.text = "Sending..."
        val requestBody = hashMapOf("mobile" to phone)
        homePageViewModel.sendOtp(requestBody)
    }

    private fun startResendCooldown(seconds: Long) {
        resendTimer?.cancel()
        binding.resendOtpButton.isEnabled = false
        resendTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.resendOtpButton.text = "Resend OTP in ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                binding.resendOtpButton.isEnabled = true
                binding.resendOtpButton.text = "Resend OTP"
            }
        }.start()
    }

    private fun observeVerifyOtp() {
        homePageViewModel.verifyOtpLiveData.observe(this) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.submitOtpButton.isEnabled = true
                    binding.submitOtpButton.text = "Verify OTP"

                    if (apiResponse.data?.status == true) {
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
                        Toast.makeText(
                            this,
                            apiResponse.data?.message ?: "Invalid OTP",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.submitOtpButton.isEnabled = true
                    binding.submitOtpButton.text = "Verify OTP"
                    Toast.makeText(
                        this,
                        parseApiErrorMessage(apiResponse.message),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun observeResendOtp() {
        homePageViewModel.sendOtpLiveData.observe(this) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> Unit
                is ApiResult.Success -> {
                    if (apiResponse.data?.status == true) {
                        apiResponse.data.dataObj?.otp?.takeIf { it.isNotBlank() }?.let { otp ->
                            binding.fallbackOtpText.visibility = View.VISIBLE
                            binding.fallbackOtpText.text = "SMS not received? Use OTP: $otp"
                            binding.otpEditText.setText(otp)
                        }
                        Toast.makeText(this, "OTP sent again", Toast.LENGTH_SHORT).show()
                        startResendCooldown(60)
                    } else {
                        binding.resendOtpButton.isEnabled = true
                        binding.resendOtpButton.text = "Resend OTP"
                        Toast.makeText(
                            this,
                            apiResponse.data?.message ?: "Failed to resend OTP",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.resendOtpButton.isEnabled = true
                    binding.resendOtpButton.text = "Resend OTP"
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
        return error?.errorMsg?.takeIf { it.isNotBlank() } ?: "Invalid OTP"
    }
}
