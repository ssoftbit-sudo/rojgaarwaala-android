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

        observeLoginData()
        binding.loginButton.setOnClickListener {
            validateLogin()
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
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (TextUtils.isEmpty(email)) {
            binding.emailEditText.error = "Email is required"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Enter a valid email"
            return
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordEditText.error = "Password is required"
            return
        }

        if (password.length < 6) {
            binding.passwordEditText.error = "Password must be at least 6 characters"
            return
        }

        // If validation passes
        onSuccess(email,password)
    }
    private fun onSuccess(email: String, password: String) {
        binding.progressBar.visibility= View.VISIBLE
        val requestBody = HashMap<String, String>()
        requestBody["email"] = email
        requestBody["password"] = password
        homePageViewModel.onLoginData(requestBody)
    }
}