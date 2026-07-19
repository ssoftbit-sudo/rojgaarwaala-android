package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.ActivityProfileBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.ColonySuggestions
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant.USER_AUTH_TOKEN
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant.USER_LOGGED_IN_STATUS
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant.USER_SKIP_STATUS
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import org.json.JSONObject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var isProfileUpdateCalled = false
    private var categoryDialog: AlertDialog? = null
    private var categoryPickerRequested = false
    private var categoriesObserverRegistered = false
    private var resumeFile: File? = null
    private var existingResumeUrl: String? = null

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private lateinit var homePageViewModel: HomePageViewModel

    private val districtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val district = result.data?.getStringExtra(LocationPickerActivity.EXTRA_SELECTED_LOCATION).orEmpty()
            if (district.isNotBlank()) {
                binding.districtEditText.text = district
                updateColonySuggestions(district)
            }
        }
    }

    private val resumeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleResumeSelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        binding.preferredJobCategoryEditText.setOnClickListener { openCategoryPicker() }
        binding.districtEditText.setOnClickListener {
            districtLauncher.launch(Intent(this, LocationPickerActivity::class.java))
        }
        binding.uploadProfileResumeBtn.setOnClickListener {
            resumeLauncher.launch(arrayOf("image/*", "application/pdf"))
        }
        binding.colonyEditText.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, emptyList<String>())
        )

        observeProfileData()
        observeUpdateProfileData()
        observeLogoutData()
        observeCategoriesDropdown()
        fetchProfileData()

        binding.updateProfileButton.setOnClickListener { validateAndUpdateProfile() }
        binding.profileBackButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.logoutButton.setOnClickListener { logoutUser() }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun fetchProfileData() {
        showLoading(true)
        homePageViewModel.getProfileData()
    }

    private fun bindProfileFields() {
        // populated from observer
    }

    private fun populateProfile(userProfile: com.srijeesolution.rojgaarwaala.data.remote.model.UserData) {
        binding.firstNameEditText.setText(userProfile.name)
        binding.mobileEditText.setText(userProfile.mobile)
        binding.emailEditText.setText(userProfile.email)
        binding.cityEditText.setText(userProfile.city)
        binding.stateEditText.setText(userProfile.state)
        binding.pincodeEditText.setText(userProfile.pincode)
        binding.preferredJobCategoryEditText.setText(userProfile.preferredJobCategory)
        binding.districtEditText.text = userProfile.district.orEmpty()
        binding.colonyEditText.setText(userProfile.colony)
        updateColonySuggestions(userProfile.district)
        existingResumeUrl = userProfile.resumeUrl
        if (!existingResumeUrl.isNullOrBlank()) {
            binding.profileResumeFileName.text = "Saved resume on profile"
            binding.profileResumeFileName.visibility = View.VISIBLE
        }
    }

    private fun observeProfileData() {
        homePageViewModel.profileUpdateLiveData.observe(this) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> showLoading(true)
                is ApiResult.Success -> {
                    showLoading(false)
                    apiResponse.data?.dataObj?.userDetails?.let { populateProfile(it) }
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "Failed to load profile: ${apiResponse.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeUpdateProfileData() {
        homePageViewModel.profileUpdateLiveData.observe(this) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> showLoading(true)
                is ApiResult.Success -> {
                    showLoading(false)
                    binding.updateProfileButton.isEnabled = true
                    binding.updateProfileButton.text = "Update Profile"
                    val payload = apiResponse.data
                    if (payload?.status == true) {
                        if (isProfileUpdateCalled) {
                            isProfileUpdateCalled = false
                            resumeFile = null
                            Toast.makeText(
                                this,
                                payload.message ?: "Profile updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        payload.dataObj?.userDetails?.let { populateProfile(it) }
                    } else {
                        Toast.makeText(
                            this,
                            payload?.message ?: "Update failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    binding.updateProfileButton.isEnabled = true
                    binding.updateProfileButton.text = "Update Profile"
                    val serverMsg = parseApiErrorMessage(apiResponse.message)
                    Toast.makeText(
                        this,
                        serverMsg?.takeIf { it.isNotBlank() } ?: "Update failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun validateAndUpdateProfile() {
        val firstname = binding.firstNameEditText.text.toString().trim()
        val mobile = binding.mobileEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val city = binding.cityEditText.text.toString().trim()
        val state = binding.stateEditText.text.toString().trim()
        val pincode = binding.pincodeEditText.text.toString().trim()
        val preferredCategory = binding.preferredJobCategoryEditText.text.toString().trim()
        val district = binding.districtEditText.text?.toString()?.trim().orEmpty()
        val colony = binding.colonyEditText.text.toString().trim()

        if (firstname.isEmpty()) {
            binding.firstNameEditText.error = "First name is required"
            return
        }
        if (mobile.length != 10) {
            binding.mobileEditText.error = "Enter a valid 10-digit mobile number"
            return
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Enter a valid email"
            return
        }
        if (preferredCategory.isEmpty()) {
            binding.preferredJobCategoryEditText.error = "Job category is required"
            return
        }
        if (district.isEmpty()) {
            Toast.makeText(this, "District is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (resumeFile == null && existingResumeUrl.isNullOrBlank()) {
            Toast.makeText(this, "Please upload resume (photo or PDF)", Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()
        isProfileUpdateCalled = true
        binding.updateProfileButton.isEnabled = false
        binding.updateProfileButton.text = "Updating..."
        showLoading(true)

        val resumePart = resumeFile?.let { file ->
            MultipartBody.Part.createFormData(
                "resume",
                file.name,
                file.asRequestBody(mimeTypeForUpload(file).toMediaTypeOrNull())
            )
        }

        homePageViewModel.updateProfileMultipart(
            firstname, mobile, email, city, state, pincode,
            district, colony, preferredCategory, resumePart
        )
    }

    private fun openCategoryPicker() {
        categoryPickerRequested = true
        homePageViewModel.getCategoriesData()
    }

    private fun observeCategoriesDropdown() {
        if (categoriesObserverRegistered) return
        categoriesObserverRegistered = true
        homePageViewModel.categoriesLiveData.observe(this) { apiResponse ->
            if (!categoryPickerRequested) return@observe
            when (apiResponse) {
                is ApiResult.Success -> {
                    categoryPickerRequested = false
                    val titles = apiResponse.data?.dataObj?.categories
                        .orEmpty()
                        .mapNotNull { it.title?.trim() }
                        .filter { it.isNotEmpty() }
                    if (titles.isEmpty()) {
                        Toast.makeText(this, "No categories found", Toast.LENGTH_SHORT).show()
                        return@observe
                    }
                    if (categoryDialog?.isShowing == true) return@observe
                    categoryDialog = AlertDialog.Builder(this)
                        .setTitle("Select Category")
                        .setItems(titles.toTypedArray()) { _, which ->
                            binding.preferredJobCategoryEditText.setText(titles[which])
                            binding.preferredJobCategoryEditText.error = null
                        }
                        .also { it.setOnDismissListener { categoryDialog = null } }
                        .show()
                }
                is ApiResult.Error -> {
                    categoryPickerRequested = false
                    Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
                is ApiResult.Loading -> Unit
            }
        }
    }

    private fun updateColonySuggestions(district: String?) {
        val colonies = ColonySuggestions.forDistrict(district)
        binding.colonyEditText.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, colonies)
        )
    }

    private fun handleResumeSelection(uri: Uri) {
        try {
            val mimeType = contentResolver.getType(uri).orEmpty()
            val rawName = getFileName(uri) ?: "candidate_resume"
            val lower = rawName.lowercase()
            val isImage = mimeType.startsWith("image/") ||
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
            val isPdf = mimeType == "application/pdf" || lower.endsWith(".pdf")
            if (!isImage && !isPdf) {
                Toast.makeText(this, "Please choose JPG/PNG photo or PDF only", Toast.LENGTH_SHORT).show()
                return
            }
            val fileName = if (isImage && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png")) {
                "$rawName.jpg"
            } else if (isPdf && !lower.endsWith(".pdf")) {
                "$rawName.pdf"
            } else {
                rawName
            }
            contentResolver.openInputStream(uri)?.use { input ->
                val file = File(cacheDir, fileName)
                file.outputStream().use { output -> input.copyTo(output) }
                resumeFile = file
                binding.profileResumeFileName.text = "Selected: $fileName"
                binding.profileResumeFileName.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to read resume file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) name = cursor.getString(index)
        }
        return name ?: uri.lastPathSegment
    }

    private fun mimeTypeForUpload(file: File): String {
        val lower = file.name.lowercase()
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            else -> "image/jpeg"
        }
    }

    private fun parseApiErrorMessage(error: ApiError?): String? {
        val body = error?.errorBody.orEmpty()
        if (body.isNotBlank()) {
            try {
                val message = JSONObject(body).optString("message")
                if (message.isNotBlank()) return message
            } catch (_: Exception) {
                // fall through
            }
        }
        return error?.errorMsg?.takeIf { it.isNotBlank() }
    }

    private fun logoutUser() {
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
            when (apiResponse) {
                is ApiResult.Success -> {
                    if (apiResponse.data?.dataObj != null) {
                        showLoading(false)
                        sharedPrefs.removeSharedPrefs(USER_AUTH_TOKEN)
                        sharedPrefs.removeSharedPrefs(USER_LOGGED_IN_STATUS)
                        sharedPrefs.removeSharedPrefs(USER_SKIP_STATUS)
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
                is ApiResult.Error -> showLoading(false)
                is ApiResult.Loading -> Unit
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.profileProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.mainLayout.isEnabled = !isLoading
    }
}
