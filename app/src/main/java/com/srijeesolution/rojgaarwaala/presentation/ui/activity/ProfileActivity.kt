package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.ActivityProfileBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant.USER_AUTH_TOKEN
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant.USER_LOGGED_IN_STATUS
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant.USER_SKIP_STATUS
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    var isProfileUpdateCalled=false

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private lateinit var homePageViewModel: HomePageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        // Observe profile data
        observeProfileData()

        // Observe update profile response
        observeUpdateProfileData()
        observeLogoutData()
        // Fetch existing profile data
        fetchProfileData()

        // Handle Update Profile button click
        binding.updateProfileButton.setOnClickListener {
            validateAndUpdateProfile()
        }

        // Handle Logout button click
        binding.logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * Fetches the user's existing profile data from the server or local storage.
     */
    private fun fetchProfileData() {
        // Show ProgressBar while fetching data
        showLoading(true)

        // Assuming there's a method in ViewModel to fetch profile data
        homePageViewModel.getProfileData()
    }

    /**
     * Observes the LiveData for fetching profile data.
     */
    private fun observeProfileData() {
        homePageViewModel.profileUpdateLiveData.observe(this) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> {
                    showLoading(true)
                }
                is ApiResult.Success -> {
                    showLoading(false)
                    apiResponse.data?.dataObj?.let { userProfile ->
                        // Pre-fill the input fields with existing data
                        binding.firstNameEditText.setText(userProfile.userDetails?.name)
//                        binding.mobileEditText.setText(userProfile.mobile)
                        binding.emailEditText.setText(userProfile.userDetails?.email)
                    }
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "Failed to load profile: ${apiResponse.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Observes the LiveData for updating profile data.
     */
    private fun observeUpdateProfileData() {
        homePageViewModel.profileUpdateLiveData.observe(this) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> {
                    showLoading(true)
                }
                is ApiResult.Success -> {
                    showLoading(false)
                    binding.updateProfileButton.isEnabled = true
                    binding.updateProfileButton.text = "Update Profile"
                    
                    if (apiResponse.data?.dataObj != null) {
                        if (isProfileUpdateCalled){
                            isProfileUpdateCalled = false
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                        // Optionally, update shared preferences or local storage if needed
                        apiResponse.data.dataObj.userDetails?.let { userProfile ->
                            // Pre-fill the input fields with existing data
                            binding.firstNameEditText.setText(userProfile.name)
                            binding.mobileEditText.setText(userProfile.mobile)
                            binding.emailEditText.setText(userProfile.email)
                            binding.cityEditText.setText(userProfile.city)
                            binding.stateEditText.setText(userProfile.state)
                            binding.pincodeEditText.setText(userProfile.pincode)
                        }
                    } else {
                        Toast.makeText(this, apiResponse.data?.message ?: "Update failed", Toast.LENGTH_SHORT).show()
                    }
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    binding.updateProfileButton.isEnabled = true
                    binding.updateProfileButton.text = "Update Profile"
                    Toast.makeText(this, "Update Failed: ${apiResponse.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Validates the input fields and initiates the profile update process.
     */
    private fun validateAndUpdateProfile() {
        val firstname = binding.firstNameEditText.text.toString().trim()
        val mobile = binding.mobileEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val city = binding.cityEditText.text.toString().trim()
        val state = binding.stateEditText.text.toString().trim()
        val pincode = binding.pincodeEditText.text.toString().trim()

        // Validation
        if (firstname.isEmpty()) {
            binding.firstNameEditText.error = "First name is required"
            return
        }

        if (mobile.isEmpty() || mobile.length != 10) {
            binding.mobileEditText.error = "Enter a valid 10-digit mobile number"
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

        // If validation passes, proceed to update profile
        updateUserProfile(firstname, mobile, email,city,state,pincode)
    }

    /**
     * Initiates the profile update API call.
     */
    private fun updateUserProfile(firstname: String,mobile: String, email: String, city: String, state: String, pincode: String) {
        // Hide keyboard and show button loader
        hideKeyboard()
        isProfileUpdateCalled = true
        binding.updateProfileButton.isEnabled = false
        binding.updateProfileButton.text = "Updating..."
        showLoading(true)
        
        val requestBody = HashMap<String, String>()
        requestBody["name"] = firstname
        requestBody["mobile"] = mobile
        requestBody["email"] = email
        requestBody["city"] = city
        requestBody["state"] = state
        requestBody["pincode"] = pincode
        homePageViewModel.updateProfileLiveData(requestBody)
    }

    /**
     * Logs out the user by clearing shared preferences and navigating to the Login screen.
     */
    private fun logoutUser() {
        // Clear shared preferences
       /* showLoading(true)
        homePageViewModel.onLogoutData()*/
        sharedPrefs.removeSharedPrefs(SharedPrefsConstant.USER_AUTH_TOKEN)
        sharedPrefs.removeSharedPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS)
        Toast.makeText(this, "Successfully logged out!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun observeLogoutData() {
        homePageViewModel.loginRegisterLiveData.observe(this) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                }
                is ApiResult.Success -> {
                    if (apiResponse.data?.dataObj != null) {
                        showLoading(false)
                        sharedPrefs.removeSharedPrefs(USER_AUTH_TOKEN)
                        sharedPrefs.removeSharedPrefs(USER_LOGGED_IN_STATUS)
                        sharedPrefs.removeSharedPrefs(USER_SKIP_STATUS)
                        // Navigate to LoginActivity
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }else{
                        Toast.makeText(this,""+apiResponse.data?.message, Toast.LENGTH_SHORT).show()
                    }
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this,"Invalid Login Details", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.profileProgressBar.visibility = View.VISIBLE
            setViewsEnabled(false)
        } else {
            binding.profileProgressBar.visibility = View.GONE
            setViewsEnabled(true)
        }
    }

    private fun setViewsEnabled(isEnabled: Boolean) {
        // Disable or enable all interaction views during loading
        binding.mainLayout.isEnabled = isEnabled
        for (i in 0 until binding.mainLayout.childCount) {
            val child: View = binding.mainLayout.getChildAt(i)
            child.isEnabled = isEnabled
        }
    }
}
