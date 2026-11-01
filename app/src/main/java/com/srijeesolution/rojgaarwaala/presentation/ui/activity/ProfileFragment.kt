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
import androidx.lifecycle.lifecycleScope
import com.srijeesolution.rojgaarwaala.BuildConfig
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.UserData
import com.srijeesolution.rojgaarwaala.databinding.ActivityProfileBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.AuthNavigation
import com.srijeesolution.rojgaarwaala.utils.ProfileGate
import com.srijeesolution.rojgaarwaala.utils.SessionExpiry
import com.srijeesolution.rojgaarwaala.utils.ProfileLocationStore
import com.srijeesolution.rojgaarwaala.utils.PunchReminderSync
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant.USER_AUTH_TOKEN
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant.USER_LOGGED_IN_STATUS
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant.USER_SKIP_STATUS
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: ActivityProfileBinding? = null
    private val binding get() = _binding!!
    private var isProfileUpdateCalled = false
    private var categoryDialog: AlertDialog? = null
    private var categoryPickerRequested = false
    private var categoriesObserverRegistered = false
    private var selectedCategories: MutableList<String> = mutableListOf()
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

    @Inject
    lateinit var dayJobRepository: com.srijeesolution.rojgaarwaala.data.repository.DayJobRepository

    private var workPhoto: File? = null
    private var workPhotoLabel: android.widget.TextView? = null
    private val workPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val file = File(requireContext().cacheDir, "worker_photo.jpg")
        runCatching {
            requireContext().contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            workPhoto = file
            workPhotoLabel?.text = "✓ फ़ोटो चुनी गई"
        }
    }

    /** 1 Day Job details, for people who work through it. The address is the one on this page. */
    private fun loadWorkDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            val worker = (dayJobRepository.meta() as? com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome.Ok)?.data?.worker ?: return@launch
            if (_binding == null || !worker.registered) return@launch
            showWorkDetails(worker)
        }
    }

    private fun showWorkDetails(worker: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobWorker) {
        val context = requireContext()
        val only = activity?.intent?.getBooleanExtra(EXTRA_SHOW_WORK_DETAILS, false) == true
        val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }
        val white = androidx.core.content.ContextCompat.getColor(context, R.color.white)
        val yellow = androidx.core.content.ContextCompat.getColor(context, R.color.brand_color_yellow)
        val green = androidx.core.content.ContextCompat.getColor(context, R.color.day_job_green)
        val section = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(if (only) 20 else 0), dp(16), dp(if (only) 20 else 0), dp(16))
        }
        fun heading(label: String, filled: Boolean) = android.widget.TextView(context).apply {
            text = (if (filled) "✅ " else "⬜ ") + label
            textSize = 16f
            setTextColor(if (filled) green else white)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(16), 0, dp(2))
        }
        fun field(label: String, example: String, value: String?, number: Boolean, max: Int): android.widget.EditText {
            section.addView(heading(label, !value.isNullOrBlank()))
            return android.widget.EditText(context).apply {
                hint = example
                setText(value.orEmpty())
                setTextColor(white)
                setHintTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.search_hint))
                inputType = if (number) android.text.InputType.TYPE_CLASS_NUMBER else android.text.InputType.TYPE_CLASS_TEXT
                filters = arrayOf(android.text.InputFilter.LengthFilter(max))
                section.addView(this)
            }
        }
        if (only) section.addView(android.widget.TextView(context).apply {
            text = "‹  वापस"
            textSize = 16f
            setTextColor(yellow)
            setPadding(0, dp(24), 0, dp(12))
            setOnClickListener { requireActivity().finish() }
        })
        section.addView(android.widget.TextView(context).apply {
            text = "1 दिन के काम की प्रोफ़ाइल"
            textSize = 20f
            setTextColor(white)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        section.addView(android.widget.TextView(context).apply {
            text = "${worker.profilePercent}% पूरी है। ✅ मतलब भरा हुआ है, ⬜ मतलब बाक़ी है। काम लेने के लिए उम्र, अनुभव, पता और घर वालों का नंबर ज़रूरी है। फ़ोटो ज़रूरी नहीं।"
            textSize = 14f
            setTextColor(white)
            setPadding(0, dp(6), 0, 0)
        })
        val age = field("आपकी उम्र", "जैसे: 30", worker.age?.toString(), true, 2)
        val experience = field("कितने साल का अनुभव है", "जैसे: 5", worker.experienceYears?.toString(), true, 2)
        val address = field("आपका पता", "जैसे: पंडरी, रायपुर", worker.address, false, 255)
        val emergency = field("घर वालों का मोबाइल नंबर", "10 अंक का नंबर", worker.emergencyMobile, true, 10)
        section.addView(heading("आपकी फ़ोटो (ज़रूरी नहीं)", worker.hasPhoto))
        workPhotoLabel = android.widget.TextView(context).apply {
            text = if (worker.hasPhoto) "📷 फ़ोटो बदलें" else "📷 फ़ोटो चुनने के लिए यहाँ दबाएँ"
            textSize = 16f
            setTextColor(yellow)
            setPadding(0, dp(8), 0, dp(20))
            setOnClickListener { workPhotoLauncher.launch("image/*") }
        }
        section.addView(workPhotoLabel)
        section.addView(android.widget.TextView(context).apply {
            text = "सेव करें"
            textSize = 17f
            gravity = android.view.Gravity.CENTER
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.day_job_on_green))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(14), 0, dp(14))
            setBackgroundResource(R.drawable.bg_day_job_green_button)
            setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = dayJobRepository.saveWorkerProfile(age.text.toString().trim(), experience.text.toString().trim(),
                        address.text.toString().trim(), emergency.text.toString().trim(), workPhoto)
                    val failed = result as? com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome.Fail
                    Toast.makeText(context, failed?.message ?: "जानकारी सेव हो गई", Toast.LENGTH_SHORT).show()
                    if (failed == null && only) requireActivity().finish()
                }
            }
        })
        val save = binding.updateProfileButton
        val parent = save.parent as ViewGroup
        parent.addView(section, parent.indexOfChild(save))
        if (only) {
            // Came for these details alone: nothing else on the page, so it is clear what to fill.
            var child: View = section
            while (child !== binding.root) {
                val up = child.parent as ViewGroup
                for (i in 0 until up.childCount) up.getChildAt(i).let { if (it !== child) it.visibility = View.GONE }
                up.background = null
                up.setPadding(0, 0, 0, 0)
                child = up
            }
            binding.root.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.app_background))
        }
    }

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

    private val resumeBuilderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val path = result.data?.getStringExtra(ResumeBuilderActivity.EXTRA_PDF_PATH) ?: return@registerForActivityResult
        val file = File(path).takeIf { it.exists() } ?: return@registerForActivityResult
        resumeFile = file
        binding.profileResumeFileName.text = getString(R.string.profile_resume_selected, file.name)
        binding.profileResumeFileName.visibility = View.VISIBLE
        Toast.makeText(requireContext(), "प्रोफ़ाइल सेव करें, तब रिज्यूमे जुड़ जाएगा", Toast.LENGTH_LONG).show()
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
        binding.profileBackButton.visibility = if (fromOtp) View.GONE else View.VISIBLE

        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]
        loadWorkDetails()

        binding.preferredJobCategoryEditText.setOnClickListener { openCategoryPicker() }
        binding.districtEditText.setOnClickListener {
            districtLauncher.launch(Intent(requireContext(), LocationPickerActivity::class.java))
        }
        binding.setMapAddressButton.setOnClickListener { openMapPin() }
        binding.profileAddressText.setOnClickListener { openMapPin() }
        binding.buildProfileResumeBtn.setOnClickListener {
            resumeBuilderLauncher.launch(ResumeBuilderActivity.intent(requireContext()))
        }
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
            if (fromOtp && !isProfileDirty() && ProfileGate.isComplete(sharedPrefs)) {
                openAppHome()
                return@setOnClickListener
            }
            if (fromOtp && !isProfileDirty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.profile_required_to_continue),
                    Toast.LENGTH_SHORT,
                ).show()
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
        lockMobileIfVerified(userProfile.mobile)
        binding.emailEditText.setText(userProfile.email)
        cityValue = userProfile.city.orEmpty()
        stateValue = userProfile.state.orEmpty()
        pincodeValue = userProfile.pincode.orEmpty()
        colonyValue = userProfile.colony.orEmpty()
        selectedAddress = userProfile.address.orEmpty()
        selectedLat = userProfile.latitude
        selectedLng = userProfile.longitude
        selectedCategories = preferredCategoriesFromProfile(userProfile).toMutableList()
        binding.preferredJobCategoryEditText.setText(selectedCategories.joinToString(", "))
        binding.districtEditText.text = userProfile.district.orEmpty()
        showSavedAddress()
        ProfileLocationStore.save(sharedPrefs, userProfile)
        ProfileGate.remember(sharedPrefs, userProfile)
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
                        payload.dataObj?.userDetails?.let { populateProfile(it) }
                        if (isProfileUpdateCalled) {
                            isProfileUpdateCalled = false
                            resumeFile = null
                            Toast.makeText(
                                requireContext(),
                                payload.message ?: getString(R.string.profile_updated),
                                Toast.LENGTH_SHORT,
                            ).show()
                            if (fromOtp) {
                                if (ProfileGate.isComplete(sharedPrefs)) {
                                    openAppHome()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.profile_required_to_continue),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            }
                        }
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
        val preferredCategory = selectedCategoriesFromField().joinToString(", ")
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
        // No resume check: it is optional on the profile and asked for with each application.

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
            preferredJobCategory = selectedCategoriesFromField().firstOrNull().orEmpty(),
            resumePart = resumePart,
            address = selectedAddress.takeIf { it.isNotBlank() },
            latitude = selectedLat,
            longitude = selectedLng,
            preferredJobCategories = JSONArray(selectedCategoriesFromField()).toString(),
        )
    }

    private fun openCategoryPicker() {
        categoryPickerRequested = true
        homePageViewModel.getCategoriesData()
    }

    /**
     * The number is the login identity, proven by OTP, so it is shown but not edited.
     * Left editable only for an account that has no number yet.
     */
    private fun lockMobileIfVerified(mobile: String?) {
        val hasNumber = mobile.orEmpty().count { it.isDigit() } >= 10
        binding.mobileEditText.apply {
            isFocusable = !hasNumber
            isFocusableInTouchMode = !hasNumber
            isCursorVisible = !hasNumber
            isLongClickable = !hasNumber
            alpha = if (hasNumber) 0.7f else 1f
            setOnClickListener(
                if (hasNumber) {
                    View.OnClickListener {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.profile_mobile_locked),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                } else {
                    null
                },
            )
        }
    }

    private fun preferredCategoriesFromProfile(userProfile: UserData): List<String> {
        val fromList = userProfile.preferredJobCategories
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(3)
        if (fromList.isNotEmpty()) {
            return fromList
        }
        return userProfile.preferredJobCategory
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(3)
    }

    private fun selectedCategoriesFromField(): List<String> {
        if (selectedCategories.isNotEmpty()) {
            return selectedCategories.take(3)
        }
        return binding.preferredJobCategoryEditText.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(3)
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
                    // Saved names can differ in case from the list ("Helper" vs
                    // "helper"). Matching them exactly left the saved one unticked
                    // while it still used up one of the three slots.
                    val picks = selectedCategoriesFromField()
                        .mapNotNull { saved -> titles.firstOrNull { it.equals(saved, ignoreCase = true) } }
                        .distinct()
                        .take(3)
                        .toMutableList()
                    val checked = BooleanArray(titles.size) { titles[it] in picks }
                    categoryDialog = AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.profile_category_picker))
                        .setMultiChoiceItems(titles.toTypedArray(), checked) { dialog, which, isChecked ->
                            val name = titles[which]
                            if (isChecked) {
                                if (picks.size >= 3) {
                                    // The dialog redraws rows from `checked`, so
                                    // clearing only the row left a 4th tick showing.
                                    checked[which] = false
                                    (dialog as AlertDialog).listView.setItemChecked(which, false)
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.profile_category_max),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    return@setMultiChoiceItems
                                }
                                if (name !in picks) picks.add(name)
                            } else {
                                picks.remove(name)
                            }
                        }
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            if (picks.isEmpty()) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.profile_category_required),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@setPositiveButton
                            }
                            selectedCategories = picks.toMutableList()
                            binding.preferredJobCategoryEditText.setText(selectedCategories.joinToString(", "))
                            binding.preferredJobCategoryEditText.error = null
                            sharedPrefs.setPrefsData(
                                Pair(
                                    SharedPrefsConstant.PREFERRED_JOB_CATEGORY,
                                    selectedCategories.joinToString(", "),
                                )
                            )
                        }
                        .setNegativeButton(android.R.string.cancel, null)
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

    private fun openAppHome() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
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
        SessionExpiry.clearSession(requireContext(), sharedPrefs)
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
                        PunchReminderSync.clear(requireContext(), sharedPrefs)
                        sharedPrefs.removeSharedPrefs(USER_AUTH_TOKEN)
                        sharedPrefs.removeSharedPrefs(USER_LOGGED_IN_STATUS)
                        sharedPrefs.removeSharedPrefs(USER_SKIP_STATUS)
                        ProfileGate.clear(sharedPrefs)
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
        const val EXTRA_SHOW_WORK_DETAILS = "show_work_details"

        fun newInstance(fromOtp: Boolean = false): ProfileFragment {
            return ProfileFragment().apply {
                arguments = bundleOf(AuthNavigation.EXTRA_FROM_OTP to fromOtp)
            }
        }
    }
}
