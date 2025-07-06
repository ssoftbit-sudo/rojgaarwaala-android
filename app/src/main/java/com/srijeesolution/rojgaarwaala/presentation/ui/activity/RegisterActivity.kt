package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.ActivityLoginBinding
import com.srijeesolution.rojgaarwaala.databinding.ActivityRegisterBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    @Inject
    lateinit var sharedPrefs: SharedPrefs
    private lateinit var homePageViewModel: HomePageViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        observeLoginData()
        binding.loginButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
        binding.signUpButton.setOnClickListener {
            validateLogin()
        }
    }


    private fun observeLoginData() {
        homePageViewModel.loginRegisterLiveData.observe(this) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                }
                is ApiResult.Success -> {
                    if (apiResponse.data?.dataObj != null) {
                        binding.progressBar.visibility= View.GONE

                        sharedPrefs.setPrefsData(
                            Pair(
                                SharedPrefsConstant.USER_AUTH_TOKEN,
                                apiResponse.data?.dataObj?.token ?: ""
                            )
                        )
                        Toast.makeText(this,"Successfully logged in!", Toast.LENGTH_SHORT).show()
                        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.USER_LOGGED_IN_STATUS, true))
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }else{
                        Toast.makeText(this,""+apiResponse.data?.message, Toast.LENGTH_SHORT).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility= View.GONE
                    Toast.makeText(this,"Invalid Login Details", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun validateLogin() {
        val name = binding.nameEditText.text.toString().trim()
        val mobile = binding.mobileEditText.text.toString().trim()
        val city = binding.cityEditText.text.toString().trim()
        val state = binding.stateEditText.text.toString().trim()
        val pincode = binding.pincodeEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        if (name.isEmpty()) {
            binding.nameEditText.error = "Name is required"
            return
        }

        if (mobile.isEmpty()) {
            binding.mobileEditText.error = "Mobile number is required"
            return
        }

        if (!mobile.matches(Regex("^[6-9]\\d{9}$"))) {
            binding.mobileEditText.error = "Enter a valid 10-digit mobile number"
            return
        }

        if (city.isEmpty()) {
            binding.cityEditText.error = "City is required"
            return
        }

        if (state.isEmpty()) {
            binding.stateEditText.error = "State is required"
            return
        }

        if (pincode.isEmpty()) {
            binding.pincodeEditText.error = "Pincode is required"
            return
        }

        if (!pincode.matches(Regex("^\\d{6}$"))) {
            binding.pincodeEditText.error = "Enter a valid 6-digit pincode"
            return
        }

        if (email.isEmpty()) {
            binding.emailEditText.error = "Email is required"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Enter a valid email"
            return
        }

        if (password.isEmpty()) {
            binding.passwordEditText.error = "Password is required"
            return
        }

        if (password.length < 6) {
            binding.passwordEditText.error = "Password must be at least 6 characters"
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordEditText.error = "Confirm Password is required"
            return
        }

        if (password != confirmPassword) {
            binding.confirmPasswordEditText.error = "Passwords do not match"
            return
        }


        // ✅ If all fields are valid
        onSuccess(name, mobile, city, state, pincode, email, password)

    }

    private fun onSuccess(
        name: String,
        mobile: String,
        city: String,
        state: String,
        pincode: String,
        email: String,
        password: String
    ) {
        binding.progressBar.visibility = View.VISIBLE

        val requestBody = HashMap<String, String>().apply {
            this["name"] = name
            this["mobile"] = mobile
            this["city"] = city
            this["state"] = state
            this["pincode"] = pincode
            this["email"] = email
            this["password"] = password
            this["password_confirmation"] = password
        }

        homePageViewModel.onRegisterData(requestBody)
    }
}