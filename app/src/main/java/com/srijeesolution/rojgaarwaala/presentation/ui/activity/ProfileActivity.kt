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
import com.srijeesolution.rojgaarwaala.BuildConfig
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.UserData
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
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var isProfileUpdateCalled = false
    private var categoryDialog: AlertDialog? = null
    private var categoryPickerRequested = false
    private var categoriesObserverRegistered = false
    private var resumeFile: File? = null
    private var existingResumeUrl: String? = null
    private var cityValue: String = ""
    private var stateValue: String = ""
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var selectedAddress: String = ""

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

    private val mapPinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        selectedLat = data.getDoubleExtra(MapPinActivity.EXTRA_LAT, MapPinActivity.DEFAULT_LAT)
        selectedLng = data.getDoubleExtra(MapPinActivity.EXTRA_LNG, MapPinActivity.DEFAULT_LNG)
        selectedAddress = data.getStringExtra(MapPinActivity.EXTRA_ADDRESS).orEmpty()
        val city = data.getStringExtra(MapPinActivity.EXTRA_CITY).orEmpty()
        val state = data.getStringExtra(MapPinActivity.EXTRA_STATE).orEmpty()
        if (city.isNotBlank()) cityValue = city
        if (state.isNotBlank()) stateValue = state
        val colony = data.getStringExtra(MapPinActivity.EXTRA_COLONY).orEmpty()
        if (colony.isNotBlank()) binding.colonyEditText.setText(colony)
        val pincode = data.getStringExtra(MapPinActivity.EXTRA_PINCODE).orEmpty()
        if (binding.pincodeEditText.text.toString().trim().isEmpty() && pincode.isNotBlank()) {
            binding.pincodeEditText.setText(pincode)
        }
        showSavedAddress()
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

        binding.appVersionText.text =
            "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        binding.preferredJobCategoryEditText.setOnClickListener { openCategoryPicker() }
        binding.districtEditText.setOnClickListener {
            districtLauncher.launch(Intent(this, LocationPickerActivity::class.java))
        }
        binding.setMapAddressButton.setOnClickListener { openMapPin() }
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

    private fun openMapPin() {
        val intent = Intent(this, MapPinActivity::class.java)
        selectedLat?.let { intent.putExtra(MapPinActivity.EXTRA_LAT, it) }
        selectedLng?.let { intent.putExtra(MapPinActivity.EXTRA_LNG, it) }
        mapPinLauncher.launch(intent)
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

    private fun populateProfile(userProfile: UserData) {
        binding.firstNameEditText.setText(userProfile.name)
        binding.mobileEditText.setText(userProfile.mobile)
        binding.emailEditText.setText(userProfile.email)
        cityValue = userProfile.city.orEmpty()
        stateValue = userProfile.state.orEmpty()
        selectedAddress = userProfile.address.orEmpty()
        selectedLat = userProfile.latitude
        selectedLng = userProfile.longitude
        binding.pincodeEditText.setText(userProfile.pincode)
        binding.preferredJobCategoryEditText.setText(userProfile.preferredJobCategory)
        binding.districtEditText.text = userProfile.district.orEmpty()
        binding.colonyEditText.setText(userProfile.colony)
        showSavedAddress()
        updateColonySuggestions(userProfile.district)
        existingResumeUrl = userProfile.resumeUrl
        if (!existingResumeUrl.isNullOrBlank()) {
            binding.profileResumeFileName.text = getString(R.string.profile_resume_saved)
            binding.profileResumeFileName.visibility = View.VISIBLE
        }
    }

    private fun showSavedAddress() {
        binding.profileAddressText.text = selectedAddress.ifBlank {
            getString(R.string.profile_address_placeholder)
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
                    Toast.makeText(
                        this,
                        getString(R.string.profile_load_failed) + ": ${apiResponse.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
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
                    binding.updateProfileButton.text = getString(R.string.profile_save)
                    val payload = apiResponse.data
                    if (payload?.status == true) {
                        if (isProfileUpdateCalled) {
                            isProfileUpdateCalled = false
                            resumeFile = null
                            Toast.makeText(
                                this,
                                payload.message ?: getString(R.string.profile_updated),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        payload.dataObj?.userDetails?.let { populateProfile(it) }
                    } else {
                        Toast.makeText(
                            this,
                            payload?.message ?: getString(R.string.profile_update_failed),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    binding.updateProfileButton.isEnabled = true
                    binding.updateProfileButton.text = getString(R.string.profile_save)
                    val serverMsg = parseApiErrorMessage(apiResponse.message)
                    Toast.makeText(
                        this,
                        serverMsg?.takeIf { it.isNotBlank() } ?: getString(R.string.profile_update_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun validateAndUpdateProfile() {
        val firstname = binding.firstNameEditText.text.toString().trim()
        val mobile = binding.mobileEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val pincode = binding.pincodeEditText.text.toString().trim()
        val preferredCategory = binding.preferredJobCategoryEditText.text.toString().trim()
        val district = binding.districtEditText.text?.toString()?.trim().orEmpty()
        val colony = binding.colonyEditText.text.toString().trim()

        if (firstname.isEmpty()) {
            binding.firstNameEditText.error = getString(R.string.profile_name_required)
            return
        }
        if (mobile.length != 10) {
            binding.mobileEditText.error = getString(R.string.profile_mobile_invalid)
            return
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = getString(R.string.profile_email_invalid)
            return
        }
        if (preferredCategory.isEmpty()) {
            binding.preferredJobCategoryEditText.error = getString(R.string.profile_category_required)
            return
        }
        if (district.isEmpty()) {
            Toast.makeText(this, getString(R.string.profile_district_required), Toast.LENGTH_SHORT).show()
            return
        }
        if (resumeFile == null && existingResumeUrl.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.profile_resume_required), Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()
        isProfileUpdateCalled = true
        binding.updateProfileButton.isEnabled = false
        binding.updateProfileButton.text = getString(R.string.profile_saving)
        showLoading(true)

        val resumePart = resumeFile?.let { file ->
            MultipartBody.Part.createFormData(
                "resume",
                file.name,
                file.asRequestBody(mimeTypeForUpload(file).toMediaTypeOrNull()),
            )
        }

        homePageViewModel.updateProfileMultipart(
            name = firstname,
            mobile = mobile,
            email = email,
            city = cityValue,
            state = stateValue,
            pincode = pincode,
            district = district,
            colony = colony,
            preferredJobCategory = preferredCategory,
            resumePart = resumePart,
            address = selectedAddress.takeIf { it.isNotBlank() },
            latitude = selectedLat,
            longitude = selectedLng,
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
                        Toast.makeText(this, getString(R.string.profile_no_categories), Toast.LENGTH_SHORT).show()
                        return@observe
                    }
                    if (categoryDialog?.isShowing == true) return@observe
                    categoryDialog = AlertDialog.Builder(this)
                        .setTitle(getString(R.string.profile_category_picker))
                        .setItems(titles.toTypedArray()) { _, which ->
                            binding.preferredJobCategoryEditText.setText(titles[which])
                            binding.preferredJobCategoryEditText.error = null
                        }
                        .also { it.setOnDismissListener { categoryDialog = null } }
                        .show()
                }
                is ApiResult.Error -> {
                    categoryPickerRequested = false
                    Toast.makeText(this, getString(R.string.profile_categories_failed), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, getString(R.string.resume_format_hint), Toast.LENGTH_SHORT).show()
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
                binding.profileResumeFileName.text = getString(R.string.profile_resume_selected, fileName)
                binding.profileResumeFileName.visibility = View.VISIBLE
            }
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.profile_update_failed), Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, getString(R.string.profile_logged_out), Toast.LENGTH_SHORT).show()
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
