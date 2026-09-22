package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.BuildConfig
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.UserData
import com.srijeesolution.rojgaarwaala.databinding.ActivityProfileBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.AuthNavigation
import com.srijeesolution.rojgaarwaala.utils.ProfileLocationStore
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
class ProfileFragment : Fragment() {

    private var _binding: ActivityProfileBinding? = null
    private val binding get() = _binding!!
    private var isProfileUpdateCalled = false
    private var categoryDialog: AlertDialog? = null
    private var categoryPickerRequested = false
    private var categoriesObserverRegistered = false
    private var resumeFile: File? = null
    private var existingResumeUrl: String? = null
    private var cityValue: String = ""
    private var stateValue: String = ""
    private var pincodeValue: String = ""
    private var colonyValue: String = ""
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var selectedAddress: String = ""
    private val fromOtp: Boolean
        get() = arguments?.getBoolean(AuthNavigation.EXTRA_FROM_OTP, false) == true
    private var baselineFingerprint: String? = null

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private lateinit var homePageViewModel: HomePageViewModel

    private val districtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val district = result.data?.getStringExtra(LocationPickerActivity.EXTRA_SELECTED_LOCATION).orEmpty()
            if (district.isNotBlank()) {
                binding.districtEditText.text = district
            }
        }
    }

    private val mapPinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        selectedLat = data.getDoubleExtra(MapPinActivity.EXTRA_LAT, MapPinActivity.DEFAULT_LAT)
        selectedLng = data.getDoubleExtra(MapPinActivity.EXTRA_LNG, MapPinActivity.DEFAULT_LNG)
        selectedAddress = data.getStringExtra(MapPinActivity.EXTRA_ADDRESS).orEmpty()
        showSavedAddress()
    }

    private val resumeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleResumeSelection(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ActivityProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appVersionText.text =
            "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        binding.profileHeaderBar.visibility = if (fromOtp) View.VISIBLE else View.GONE

        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        binding.preferredJobCategoryEditText.setOnClickListener { openCategoryPicker() }
        binding.districtEditText.setOnClickListener {
            districtLauncher.launch(Intent(requireContext(), LocationPickerActivity::class.java))
        }
        binding.setMapAddressButton.setOnClickListener { openMapPin() }
        binding.profileAddressText.setOnClickListener { openMapPin() }
        binding.uploadProfileResumeBtn.setOnClickListener {
            resumeLauncher.launch(arrayOf("image/*", "application/pdf"))
        }

        observeProfileData()
        observeUpdateProfileData()
        observeLogoutData()
        observeCategoriesDropdown()
        fetchProfileData()

        binding.updateProfileButton.text = saveButtonIdleLabel()
        binding.updateProfileButton.setOnClickListener {
            if (fromOtp && !isProfileDirty()) {
                requireActivity().finish()
                return@setOnClickListener
            }
            validateAndUpdateProfile()
        }
        binding.profileBackButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.logoutButton.setOnClickListener { logoutUser() }
    }

    private fun openMapPin() {
        val intent = Intent(requireContext(), MapPinActivity::class.java)
        selectedLat?.let { intent.putExtra(MapPinActivity.EXTRA_LAT, it) }
        selectedLng?.let { intent.putExtra(MapPinActivity.EXTRA_LNG, it) }
        mapPinLauncher.launch(intent)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        requireActivity().currentFocus?.let { view ->
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
        pincodeValue = userProfile.pincode.orEmpty()
        colonyValue = userProfile.colony.orEmpty()
        selectedAddress = userProfile.address.orEmpty()
        selectedLat = userProfile.latitude
        selectedLng = userProfile.longitude
        binding.preferredJobCategoryEditText.setText(userProfile.preferredJobCategory)
        binding.districtEditText.text = userProfile.district.orEmpty()
        showSavedAddress()
        ProfileLocationStore.save(sharedPrefs, userProfile)
        existingResumeUrl = userProfile.resumeUrl
        if (!existingResumeUrl.isNullOrBlank()) {
            binding.profileResumeFileName.text = getString(R.string.profile_resume_saved)
            binding.profileResumeFileName.visibility = View.VISIBLE
        }
        baselineFingerprint = profileFingerprint()
    }

    private fun saveButtonIdleLabel(): String {
        return getString(if (fromOtp) R.string.profile_confirm else R.string.profile_save)
    }

    private fun profileFingerprint(): String {
        return listOf(
            binding.firstNameEditText.text.toString().trim(),
            binding.mobileEditText.text.toString().trim(),
            binding.emailEditText.text.toString().trim(),
            binding.preferredJobCategoryEditText.text.toString().trim(),
            binding.districtEditText.text?.toString()?.trim().orEmpty(),
            cityValue.trim(),
            stateValue.trim(),
            pincodeValue.trim(),
            selectedAddress.trim(),
            selectedLat?.toString().orEmpty(),
            selectedLng?.toString().orEmpty(),
            resumeFile?.name.orEmpty(),
        ).joinToString("\u0001")
    }

    private fun isProfileDirty(): Boolean {
        val baseline = baselineFingerprint ?: return true
        return profileFingerprint() != baseline
    }

    private fun showSavedAddress() {
        binding.profileAddressText.text = selectedAddress.ifBlank {
            getString(R.string.profile_address_placeholder)
        }
    }

    private fun observeProfileData() {
        homePageViewModel.profileUpdateLiveData.observe(viewLifecycleOwner) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> showLoading(true)
                is ApiResult.Success -> {
                    showLoading(false)
                    apiResponse.data?.dataObj?.userDetails?.let { populateProfile(it) }
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.profile_load_failed) + ": ${apiResponse.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private fun observeUpdateProfileData() {
        homePageViewModel.profileUpdateLiveData.observe(viewLifecycleOwner) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> showLoading(true)
                is ApiResult.Success -> {
                    showLoading(false)
                    binding.updateProfileButton.isEnabled = true
                    binding.updateProfileButton.text = saveButtonIdleLabel()
                    val payload = apiResponse.data
                    if (payload?.status == true) {
                        if (isProfileUpdateCalled) {
                            isProfileUpdateCalled = false
                            resumeFile = null
                            Toast.makeText(
                                requireContext(),
                                payload.message ?: getString(R.string.profile_updated),
                                Toast.LENGTH_SHORT,
                            ).show()
                            if (fromOtp) {
                                requireActivity().finish()
                                return@observe
                            }
                        }
                        payload.dataObj?.userDetails?.let { populateProfile(it) }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            payload?.message ?: getString(R.string.profile_update_failed),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    binding.updateProfileButton.isEnabled = true
                    binding.updateProfileButton.text = saveButtonIdleLabel()
                    val serverMsg = parseApiErrorMessage(apiResponse.message)
                    Toast.makeText(
                        requireContext(),
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
        val preferredCategory = binding.preferredJobCategoryEditText.text.toString().trim()
        val district = binding.districtEditText.text?.toString()?.trim().orEmpty()

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
            Toast.makeText(requireContext(), getString(R.string.profile_district_required), Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedAddress.isBlank() || selectedLat == null || selectedLng == null) {
            Toast.makeText(requireContext(), getString(R.string.profile_map_address_required), Toast.LENGTH_SHORT).show()
            return
        }
        if (resumeFile == null && existingResumeUrl.isNullOrBlank()) {
            Toast.makeText(requireContext(), getString(R.string.profile_resume_required), Toast.LENGTH_SHORT).show()
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

        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.PREFERRED_JOB_CATEGORY, preferredCategory))
        homePageViewModel.updateProfileMultipart(
            name = firstname,
            mobile = mobile,
            email = email,
            city = cityValue,
            state = stateValue,
            pincode = pincodeValue,
            district = district,
            colony = colonyValue,
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
        homePageViewModel.categoriesLiveData.observe(viewLifecycleOwner) { apiResponse ->
            if (!categoryPickerRequested) return@observe
            when (apiResponse) {
                is ApiResult.Success -> {
                    categoryPickerRequested = false
                    val titles = apiResponse.data?.dataObj?.categories
                        .orEmpty()
                        .mapNotNull { it.title?.trim() }
                        .filter { it.isNotEmpty() }
                    if (titles.isEmpty()) {
                        Toast.makeText(requireContext(), getString(R.string.profile_no_categories), Toast.LENGTH_SHORT).show()
                        return@observe
                    }
                    if (categoryDialog?.isShowing == true) return@observe
                    categoryDialog = AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.profile_category_picker))
                        .setItems(titles.toTypedArray()) { _, which ->
                            binding.preferredJobCategoryEditText.setText(titles[which])
                            binding.preferredJobCategoryEditText.error = null
                            sharedPrefs.setPrefsData(
                                Pair(SharedPrefsConstant.PREFERRED_JOB_CATEGORY, titles[which])
                            )
                        }
                        .also { it.setOnDismissListener { categoryDialog = null } }
                        .show()
                }
                is ApiResult.Error -> {
                    categoryPickerRequested = false
                    Toast.makeText(requireContext(), getString(R.string.profile_categories_failed), Toast.LENGTH_SHORT).show()
                }
                is ApiResult.Loading -> Unit
            }
        }
    }

    private fun handleResumeSelection(uri: Uri) {
        try {
            val resolver = requireContext().contentResolver
            val mimeType = resolver.getType(uri).orEmpty()
            val rawName = getFileName(uri) ?: "candidate_resume"
            val lower = rawName.lowercase()
            val isImage = mimeType.startsWith("image/") ||
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
            val isPdf = mimeType == "application/pdf" || lower.endsWith(".pdf")
            if (!isImage && !isPdf) {
                Toast.makeText(requireContext(), getString(R.string.resume_format_hint), Toast.LENGTH_SHORT).show()
                return
            }
            val fileName = if (isImage && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png")) {
                "$rawName.jpg"
            } else if (isPdf && !lower.endsWith(".pdf")) {
                "$rawName.pdf"
            } else {
                rawName
            }
            resolver.openInputStream(uri)?.use { input ->
                val file = File(requireContext().cacheDir, fileName)
                file.outputStream().use { output -> input.copyTo(output) }
                resumeFile = file
                binding.profileResumeFileName.text = getString(R.string.profile_resume_selected, fileName)
                binding.profileResumeFileName.visibility = View.VISIBLE
            }
        } catch (_: Exception) {
            Toast.makeText(requireContext(), getString(R.string.profile_update_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
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
        Toast.makeText(requireContext(), getString(R.string.profile_logged_out), Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        requireActivity().finish()
    }

    private fun observeLogoutData() {
        homePageViewModel.loginRegisterLiveData.observe(viewLifecycleOwner) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Success -> {
                    if (apiResponse.data?.dataObj != null) {
                        showLoading(false)
                        sharedPrefs.removeSharedPrefs(USER_AUTH_TOKEN)
                        sharedPrefs.removeSharedPrefs(USER_LOGGED_IN_STATUS)
                        sharedPrefs.removeSharedPrefs(USER_SKIP_STATUS)
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                        requireActivity().finish()
                    }
                }
                is ApiResult.Error -> showLoading(false)
                is ApiResult.Loading -> Unit
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        val bound = _binding ?: return
        bound.profileProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        bound.mainLayout.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        categoryDialog?.dismiss()
        categoryDialog = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(fromOtp: Boolean = false): ProfileFragment {
            return ProfileFragment().apply {
                arguments = bundleOf(AuthNavigation.EXTRA_FROM_OTP to fromOtp)
            }
        }
    }
}
