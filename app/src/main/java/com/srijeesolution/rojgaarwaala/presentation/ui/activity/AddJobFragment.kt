package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.FragmentAddJobBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.ColonySuggestions
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.app.AlertDialog
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import org.json.JSONObject

import com.srijeesolution.rojgaarwaala.R
import com.google.android.material.chip.Chip
import com.bumptech.glide.Glide
import android.app.Dialog
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import android.graphics.drawable.Drawable
import android.widget.ProgressBar
import android.view.MotionEvent
import kotlin.math.sqrt
import android.webkit.WebView
import android.webkit.WebViewClient
import android.graphics.Bitmap

@AndroidEntryPoint
class AddJobFragment : Fragment() {
    private var _binding: FragmentAddJobBinding? = null
    private val binding get() = _binding!!
    @Inject
    lateinit var sharedPrefs: SharedPrefs
    private lateinit var homePageViewModel: HomePageViewModel
    private var updateJobId: Int? = null
    private var categoryDialog: AlertDialog? = null
    private var categoryPickerTarget: CategoryPickerTarget = CategoryPickerTarget.COMPANY
    private var categoriesObserverRegistered = false
    private var categoryPickerRequested = false

    private enum class CategoryPickerTarget {
        COMPANY, CANDIDATE
    }
    
    // File upload variables
    private var pdfFile: File? = null
    private var imageFile: File? = null
    private var logoFile: File? = null
    private var candidateResumeFile: File? = null
    
    // Existing file URLs (for edit mode)
    private var existingPdfUrl: String? = null
    private var existingImageUrl: String? = null
    private var existingLogoUrl: String? = null
    
    private var candidateResumeUri: Uri? = null
    private val companyJobLocations = mutableListOf<String>()
    
    // Activity result launchers for file selection
    private val pdfLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handlePdfSelection(it) }
    }
    
    private val imageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageSelection(it) }
    }
    
    private val logoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleLogoSelection(it) }
    }
    private val candidateResumeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleCandidateResumeSelection(it) }
    }

    private val candidateDistrictLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val district = result.data?.getStringExtra(LocationPickerActivity.EXTRA_SELECTED_LOCATION).orEmpty()
            if (district.isNotBlank()) {
                binding.candidateDistrict.text = district
                updateColonySuggestions(district)
            }
        }
    }

    private val companyLocationsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selected = result.data
                ?.getStringArrayListExtra(LocationPickerActivity.EXTRA_SELECTED_LOCATIONS)
                .orEmpty()
                .filter { it.isNotBlank() }
            if (selected.isNotEmpty()) {
                companyJobLocations.clear()
                companyJobLocations.addAll(selected.distinct())
                renderCompanyLocationChips()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddJobBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        // Check if user is logged in
        if (!sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
            // User is not logged in, navigate to login screen
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            Toast.makeText(requireContext(), "Please login to add jobs", Toast.LENGTH_SHORT).show()
            return
        }

        // Check for update mode
        arguments?.let { args ->
            updateJobId = args.getInt("job_id", -1).takeIf { it != -1 }
            binding.jobTitle.setText(args.getString("job_title", ""))
            binding.jobDescription.setText(args.getString("job_description", ""))
            binding.jobCategory.setText(args.getString("job_category", ""))
            binding.jobResponsibility.setText(args.getString("job_responsibility", ""))
            
            // Handle existing files
            existingPdfUrl = args.getString("job_pdf")
            existingImageUrl = args.getString("job_image")
            existingLogoUrl = args.getString("job_logo")
            
            if (updateJobId != null) {
                binding.submitBtn.text = "Update Job"
                // Update page title for edit mode
                updatePageTitle("Update Job")
                // Show existing files if available
                showExistingFiles()
            } else {
                // Set default title for new job
                updatePageTitle("Add New Job")
            }
        } ?: run {
            // Set default title for new job when no arguments
            updatePageTitle("Add New Job")
        }

        observeJobSubmitData()
        observeJobUpdateData()
        setupCategoryDropdown()
        observeCategoriesDropdown()
        setupUserTypeFlow()
        renderCompanyLocationChips()
        binding.submitBtn.setOnClickListener {
            hideKeyboard()
            validateLogin()
        }
        // Set up file upload button click listeners
        setupFileUploadListeners()
    }

    private fun setupUserTypeFlow() {
        binding.userTypeGroup.setOnCheckedChangeListener { _, _ ->
            updateUserTypeUi()
        }

        binding.candidateDistrict.setOnClickListener {
            candidateDistrictLauncher.launch(Intent(requireContext(), LocationPickerActivity::class.java))
        }
        binding.addCompanyLocationBtn.setOnClickListener {
            val intent = Intent(requireContext(), LocationPickerActivity::class.java).apply {
                putExtra(LocationPickerActivity.EXTRA_MULTI_SELECT, true)
                putStringArrayListExtra(
                    LocationPickerActivity.EXTRA_PRESELECTED_LOCATIONS,
                    ArrayList(companyJobLocations)
                )
            }
            companyLocationsLauncher.launch(intent)
        }
        binding.candidateColony.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, emptyList<String>())
        )
        updateUserTypeUi()
    }

    private fun updateUserTypeUi() {
        val isCandidate = binding.userTypeGroup.checkedRadioButtonId == binding.candidateTypeRadio.id
        binding.candidateFieldsContainer.visibility = if (isCandidate) View.VISIBLE else View.GONE
        binding.jobTitleSection.visibility = if (isCandidate) View.GONE else View.VISIBLE
        binding.companyCategorySection.visibility = if (isCandidate) View.GONE else View.VISIBLE
        binding.companyLocationsSection.visibility = if (isCandidate) View.GONE else View.VISIBLE
        binding.companyUploadSection.visibility = if (isCandidate) View.GONE else View.VISIBLE
        binding.jobResponsibility.hint = if (isCandidate) {
            "Why should recruiter consider you?"
        } else {
            "Enter job responsibility"
        }
        binding.submitBtn.text = when {
            isCandidate -> "Submit Profile"
            updateJobId != null -> "Update Job"
            else -> "Submit Job"
        }
        if (isCandidate) {
            updateColonySuggestions(binding.candidateDistrict.text?.toString())
        }
    }

    private fun updateColonySuggestions(district: String?) {
        val colonies = ColonySuggestions.forDistrict(district)
        binding.candidateColony.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, colonies)
        )
    }

    private fun resetSubmitButton() {
        val isCandidate = binding.userTypeGroup.checkedRadioButtonId == binding.candidateTypeRadio.id
        binding.submitBtn.isEnabled = true
        binding.submitBtn.text = when {
            isCandidate -> "Submit Profile"
            updateJobId != null -> "Update Job"
            else -> "Submit Job"
        }
    }

    private fun showValidationErrors(errors: List<String>) {
        binding.submitValidationErrors.text = errors.joinToString("\n") { "• $it" }
        binding.submitValidationErrors.visibility = if (errors.isEmpty()) View.GONE else View.VISIBLE
        resetSubmitButton()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        view?.let { v ->
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    private fun observeJobSubmitData() {
        homePageViewModel.jobSubmitLiveData.observe(viewLifecycleOwner) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility= View.GONE
                    resetSubmitButton()
                    if (apiResponse.data?.status == true) {
                        Toast.makeText(
                            requireContext(),
                            apiResponse.data?.message ?: "Job added successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        (activity as? MainActivity)?.selectTabFromFragment(0)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            apiResponse.data?.message ?: "Submit failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility= View.GONE
                    resetSubmitButton()
                    val serverMsg = parseApiErrorMessage(apiResponse.message)
                    Toast.makeText(
                        requireContext(),
                        serverMsg?.takeIf { it.isNotBlank() } ?: "Failed Job Submit",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun observeJobUpdateData() {
        homePageViewModel.updateJobLiveData.observe(viewLifecycleOwner) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility= View.GONE
                    resetSubmitButton()
                    Toast.makeText(requireContext(),"Job updated successfully!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.rootLayout, ViewAllJobsFragment())
                        .commit()
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility= View.GONE
                    resetSubmitButton()
                    Toast.makeText(requireContext(),"Failed to update job", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupCategoryDropdown() {
        binding.jobCategory.isFocusable = false
        binding.jobCategory.isClickable = true
        binding.jobCategory.setOnClickListener {
            openCategoryPicker(CategoryPickerTarget.COMPANY)
        }

        binding.candidateJobCategory.isFocusable = false
        binding.candidateJobCategory.isClickable = true
        binding.candidateJobCategory.keyListener = null
        binding.candidateJobCategory.setOnClickListener {
            openCategoryPicker(CategoryPickerTarget.CANDIDATE)
        }
    }

    private fun openCategoryPicker(target: CategoryPickerTarget) {
        categoryPickerTarget = target
        categoryPickerRequested = true
        binding.progressBar.visibility = View.VISIBLE
        homePageViewModel.getCategoriesData()
    }

    private fun observeCategoriesDropdown() {
        if (categoriesObserverRegistered) return
        categoriesObserverRegistered = true

        homePageViewModel.categoriesLiveData.observe(viewLifecycleOwner) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (!categoryPickerRequested) return@observe
                    categoryPickerRequested = false
                    val categories = apiResponse.data?.dataObj?.categories.orEmpty()
                    val titles = categories.mapNotNull { it.title?.trim() }.filter { it.isNotEmpty() }
                    if (titles.isEmpty()) {
                        Toast.makeText(requireContext(), "No categories found", Toast.LENGTH_SHORT).show()
                        return@observe
                    }
                    if (categoryDialog?.isShowing == true) return@observe
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Select Category")
                    builder.setItems(titles.toTypedArray()) { _, which ->
                        val selected = titles[which]
                        when (categoryPickerTarget) {
                            CategoryPickerTarget.COMPANY -> binding.jobCategory.setText(selected)
                            CategoryPickerTarget.CANDIDATE -> binding.candidateJobCategory.setText(selected)
                        }
                        binding.candidateJobCategory.error = null
                        binding.jobCategory.error = null
                    }
                    categoryDialog = builder.create()
                    categoryDialog?.setOnDismissListener { categoryDialog = null }
                    categoryDialog?.show()
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    categoryPickerRequested = false
                    Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validateLogin(): Boolean {
        val isCandidate = binding.userTypeGroup.checkedRadioButtonId == binding.candidateTypeRadio.id
        val jobTitle = binding.jobTitle.text.toString().trim()
        val jobDescription = binding.jobDescription.text.toString().trim()
        val jobCategory = binding.jobCategory.text.toString().trim()
        val jobResponsibility = binding.jobResponsibility.text.toString().trim()
        val candidateCategory = binding.candidateJobCategory.text.toString().trim()
        val candidateDistrict = binding.candidateDistrict.text.toString().trim()
        val candidateColony = binding.candidateColony.text.toString().trim()
        val candidateMobile = binding.candidateMobile.text.toString().trim()

        binding.jobTitle.error = null
        binding.jobDescription.error = null
        binding.jobCategory.error = null
        binding.jobResponsibility.error = null
        binding.candidateJobCategory.error = null
        binding.candidateColony.error = null
        binding.candidateMobile.error = null

        val errors = mutableListOf<String>()

        if (!isCandidate && jobTitle.isEmpty()) {
            binding.jobTitle.error = getString(R.string.field_is_required, "Job Title")
            errors.add(getString(R.string.field_is_required, "Job Title"))
        }

        if (jobDescription.isEmpty()) {
            binding.jobDescription.error = getString(R.string.field_is_required, "Job Description")
            errors.add(getString(R.string.field_is_required, "Job Description"))
        }

        if (!isCandidate && jobCategory.isEmpty()) {
            binding.jobCategory.error = getString(R.string.field_is_required, "Job Category")
            errors.add(getString(R.string.field_is_required, "Job Category"))
        }

        if (jobResponsibility.isEmpty()) {
            val label = if (isCandidate) "Why should recruiter consider you" else "Job Responsibility"
            binding.jobResponsibility.error = getString(R.string.field_is_required, label)
            errors.add(getString(R.string.field_is_required, label))
        }

        if (!isCandidate && companyJobLocations.isEmpty()) {
            errors.add(getString(R.string.field_is_required, "At least one Job Location"))
        }

        if (isCandidate) {
            if (candidateCategory.isEmpty()) {
                binding.candidateJobCategory.error = getString(R.string.field_is_required, "Interested Job Category")
                errors.add(getString(R.string.field_is_required, "Interested Job Category"))
            }
            if (candidateDistrict.isEmpty()) {
                errors.add(getString(R.string.field_is_required, "District"))
            }
            if (candidateColony.isEmpty()) {
                binding.candidateColony.error = getString(R.string.field_is_required, "Colony / Area")
                errors.add(getString(R.string.field_is_required, "Colony / Area"))
            }
            if (candidateMobile.length != 10) {
                binding.candidateMobile.error = getString(R.string.field_is_required, "Valid 10 digit Mobile Number")
                errors.add(getString(R.string.field_is_required, "Valid 10 digit Mobile Number"))
            }
            if (candidateResumeFile == null) {
                errors.add(getString(R.string.field_is_required, "Candidate Resume (Photo or PDF)"))
            }
        }

        if (errors.isNotEmpty()) {
            showValidationErrors(errors)
            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, binding.submitValidationErrors.top)
            }
            return false
        }

        binding.submitValidationErrors.visibility = View.GONE
        binding.submitBtn.isEnabled = false
        binding.submitBtn.text = if (updateJobId != null) "Updating..." else "Submitting..."

        val effectiveTitle = if (isCandidate) candidateCategory else jobTitle
        val effectiveCategory = if (isCandidate) candidateCategory else jobCategory
        val locationLabel = listOf(candidateDistrict, candidateColony)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        val effectiveResponsibility = if (isCandidate) {
            "$jobResponsibility | district=$candidateDistrict|colony=$candidateColony|preferred_location=$locationLabel|candidate_mobile=$candidateMobile|post_type=candidate"
        } else {
            jobResponsibility
        }
        onSuccess(effectiveTitle, jobDescription, effectiveCategory, effectiveResponsibility)
        return true
    }

    private fun onSuccess(
        jobTitle: String,
        jobDescription: String,
        jobCategory: String,
        jobResponsibility: String) {
        binding.progressBar.visibility = View.VISIBLE

        val requestBody = HashMap<String, String>().apply {
            this["job_title"] = jobTitle
            this["job_description"] = jobDescription
            this["job_category"] = jobCategory
            this["job_responsibility"] = jobResponsibility
        }

        if (updateJobId != null) {
            // For update, handle file uploads
            updateJobWithFiles(updateJobId!!, requestBody)
        } else {
            // For new job submission, include files if available
            submitJobWithFiles(requestBody)
        }
    }

    private fun updateJobWithFiles(id: Int, requestBody: HashMap<String, String>) {
        // Create MultipartBody.Part objects for files if available
        val pdfPart = pdfFile?.let { file ->
            val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("pdf", file.name, requestFile)
        }

        val imagePart = imageFile?.let { file ->
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", file.name, requestFile)
        }

        val logoPart = logoFile?.let { file ->
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("logo", file.name, requestFile)
        }

        // Extract text fields from requestBody
        val jobTitle = requestBody["job_title"] ?: ""
        val jobDescription = requestBody["job_description"] ?: ""
        val jobCategory = requestBody["job_category"] ?: ""
        val jobResponsibility = requestBody["job_responsibility"] ?: ""

        // Call the new multipart update API method
        homePageViewModel.updateJobWithFiles(
            id, jobTitle, jobDescription, jobCategory, jobResponsibility,
            pdfPart, imagePart, logoPart, companyJobLocations.toList()
        )
    }

    private fun submitJobWithFiles(requestBody: HashMap<String, String>) {
        val pdfPart = pdfFile?.let { file ->
            val requestFile = file.asRequestBody(mimeTypeForUpload(file).toMediaTypeOrNull())
            MultipartBody.Part.createFormData("pdf", file.name, requestFile)
        }

        val imagePart = imageFile?.let { file ->
            val requestFile = file.asRequestBody(mimeTypeForUpload(file).toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", file.name, requestFile)
        }

        val logoPart = logoFile?.let { file ->
            val requestFile = file.asRequestBody(mimeTypeForUpload(file).toMediaTypeOrNull())
            MultipartBody.Part.createFormData("logo", file.name, requestFile)
        }

        // Extract text fields from requestBody
        val jobTitle = requestBody["job_title"] ?: ""
        val jobDescription = requestBody["job_description"] ?: ""
        val jobCategory = requestBody["job_category"] ?: ""
        val jobResponsibility = requestBody["job_responsibility"] ?: ""

        // Call the new multipart API method
        homePageViewModel.onSubmitJobWithFiles(
            jobTitle, jobDescription, jobCategory, jobResponsibility,
            pdfPart, imagePart, logoPart, companyJobLocations.toList()
        )
    }

    private fun renderCompanyLocationChips() {
        binding.companyLocationsChipGroup.removeAllViews()
        companyJobLocations.forEach { location ->
            val chip = Chip(requireContext()).apply {
                text = location
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    companyJobLocations.remove(location)
                    renderCompanyLocationChips()
                }
            }
            binding.companyLocationsChipGroup.addView(chip)
        }
        binding.companyLocationsHint.visibility =
            if (companyJobLocations.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupFileUploadListeners() {
        binding.uploadPdfBtn.setOnClickListener {
            pdfLauncher.launch("application/pdf")
        }
        
        binding.uploadImageBtn.setOnClickListener {
            imageLauncher.launch("image/*")
        }
        
        binding.uploadLogoBtn.setOnClickListener {
            logoLauncher.launch("image/*")
        }

        binding.uploadCandidateResumeBtn.setOnClickListener {
            candidateResumeLauncher.launch(arrayOf("image/*", "application/pdf"))
        }
    }

    private fun handleCandidateResumeSelection(uri: Uri) {
        try {
            val mimeType = requireContext().contentResolver.getType(uri).orEmpty()
            val rawName = getFileName(uri) ?: "candidate_resume"
            val lower = rawName.lowercase()

            val isImage = mimeType.startsWith("image/") ||
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
            val isPdf = mimeType == "application/pdf" || lower.endsWith(".pdf")

            if (!isImage && !isPdf) {
                Toast.makeText(
                    requireContext(),
                    "Please choose JPG/PNG photo or PDF only",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val fileName = ensureFileExtension(rawName, mimeType, isPhoto = isImage)

            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, fileName)
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            candidateResumeFile = file
            candidateResumeUri = uri
            val typeLabel = if (isImage) "Photo" else "PDF"
            binding.candidateResumeFileName.text = "Selected ($typeLabel): $fileName"
            binding.candidateResumeFileName.visibility = View.VISIBLE

            if (isImage) {
                imageFile = file
                pdfFile = null
            } else {
                pdfFile = file
                imageFile = null
            }

            if (isImage) {
                binding.candidateResumePreview.visibility = View.VISIBLE
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(binding.candidateResumePreview)
            } else {
                binding.candidateResumePreview.visibility = View.GONE
                binding.candidateResumePreview.setImageDrawable(null)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error selecting resume file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ensureFileExtension(fileName: String, mimeType: String, isPhoto: Boolean): String {
        if (fileName.contains('.')) return fileName
        return when {
            mimeType.equals("image/png", true) -> "$fileName.png"
            mimeType.startsWith("image/") || isPhoto -> "$fileName.jpg"
            mimeType == "application/pdf" -> "$fileName.pdf"
            else -> fileName
        }
    }

    private fun handlePdfSelection(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = getFileName(uri) ?: "document.pdf"
            val file = File(requireContext().cacheDir, fileName)
            
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            pdfFile = file
            binding.pdfFileName.text = "📄 Selected: $fileName (will replace existing)"
            binding.pdfFileName.visibility = View.VISIBLE
            binding.pdfFileName.setOnClickListener(null) // Remove existing file click listener
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error selecting PDF file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = getFileName(uri) ?: "image.jpg"
            val file = File(requireContext().cacheDir, fileName)
            
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            imageFile = file
            binding.imageFileName.text = "🖼️ Selected: $fileName (will replace existing)"
            binding.imageFileName.visibility = View.VISIBLE
            binding.imageFileName.setOnClickListener(null) // Remove existing file click listener
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error selecting image file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLogoSelection(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = getFileName(uri) ?: "logo.jpg"
            val file = File(requireContext().cacheDir, fileName)
            
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            logoFile = file
            binding.logoFileName.text = "🏢 Selected: $fileName (will replace existing)"
            binding.logoFileName.visibility = View.VISIBLE
            binding.logoFileName.setOnClickListener(null) // Remove existing file click listener
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error selecting logo file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayNameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            return it.getString(displayNameIndex)
                        }
                    }
                }
                "file_${System.currentTimeMillis()}"
            }
            "file" -> uri.lastPathSegment
            else -> "file_${System.currentTimeMillis()}"
        }
    }

    private fun showExistingFiles() {
        // Show existing PDF
        existingPdfUrl?.let { url ->
            binding.pdfFileName.text = "📄 Existing PDF (Tap to view)"
            binding.pdfFileName.visibility = View.VISIBLE
            binding.pdfFileName.setOnClickListener {
                openFileUrl(url, "PDF")
            }
        }
        
        // Show existing Image
        existingImageUrl?.let { url ->
            binding.imageFileName.text = "🖼️ Existing Image (Tap to view)"
            binding.imageFileName.visibility = View.VISIBLE
            binding.imageFileName.setOnClickListener {
                openFileUrl(url, "Image")
            }
        }
        
        // Show existing Logo
        existingLogoUrl?.let { url ->
            binding.logoFileName.text = "🏢 Existing Logo (Tap to view)"
            binding.logoFileName.visibility = View.VISIBLE
            binding.logoFileName.setOnClickListener {
                openFileUrl(url, "Logo")
            }
        }
    }

    private fun openFileUrl(url: String, fileType: String) {
        try {
            when (fileType.lowercase()) {
                "pdf" -> {
                    // For PDF, show in popup preview
                    showPdfPreview(url, fileType)
                }
                "image", "logo" -> {
                    // For images, show in popup preview
                    showImagePreview(url, fileType)
                }
                else -> {
                    // Default fallback
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    if (intent.resolveActivity(requireContext().packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(requireContext(), "No app found to open $fileType", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening $fileType: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPdfPreview(pdfUrl: String, fileType: String) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_pdf_preview)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val webView = dialog.findViewById<WebView>(R.id.pdfWebView)
        val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)
        val titleText = dialog.findViewById<TextView>(R.id.titleText)
        val loadingIndicator = dialog.findViewById<ProgressBar>(R.id.loadingIndicator)
        val errorText = dialog.findViewById<TextView>(R.id.errorText)

        // Set title
        titleText.text = "PDF Document Preview"

        // Setup WebView for PDF viewing
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = true
            allowContentAccess = true
        }

        // WebView client to handle loading states
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                loadingIndicator.visibility = View.VISIBLE
                errorText.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadingIndicator.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                loadingIndicator.visibility = View.GONE
                errorText.visibility = View.VISIBLE
                errorText.text = "Failed to load PDF. Tap to open in browser."
                errorText.setOnClickListener {
                    // Fallback to external browser
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                        if (intent.resolveActivity(requireContext().packageManager) != null) {
                            startActivity(intent)
                            dialog.dismiss()
                        } else {
                            Toast.makeText(requireContext(), "No app found to open PDF", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error opening PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Try multiple PDF viewing methods
        try {
            // Method 1: Try Google Docs viewer first
            val googleDocsUrl = "https://docs.google.com/viewer?url=${Uri.encode(pdfUrl)}&embedded=true"
            webView.loadUrl(googleDocsUrl)
        } catch (e: Exception) {
            // Method 2: Fallback to direct PDF URL
            try {
                webView.loadUrl(pdfUrl)
            } catch (e2: Exception) {
                // Method 3: Show error with fallback option
                loadingIndicator.visibility = View.GONE
                errorText.visibility = View.VISIBLE
                errorText.text = "Failed to load PDF. Tap to open in browser."
                errorText.setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                        if (intent.resolveActivity(requireContext().packageManager) != null) {
                            startActivity(intent)
                            dialog.dismiss()
                        } else {
                            Toast.makeText(requireContext(), "No app found to open PDF", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e3: Exception) {
                        Toast.makeText(requireContext(), "Error opening PDF: ${e3.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Close button click
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Close on outside click
        dialog.setOnDismissListener {
            webView.destroy()
        }

        dialog.show()
    }

    private fun showImagePreview(imageUrl: String, fileType: String) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_image_preview)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val imageView = dialog.findViewById<ImageView>(R.id.previewImageView)
        val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)
        val titleText = dialog.findViewById<TextView>(R.id.titleText)
        val loadingIndicator = dialog.findViewById<ProgressBar>(R.id.loadingIndicator)

        // Set title
        titleText.text = when (fileType.lowercase()) {
            "image" -> "Job Image Preview"
            "logo" -> "Company Logo Preview"
            else -> "Image Preview"
        }

        // Setup zoom functionality
        setupImageZoom(imageView)

        // Load image using Glide
        try {
            Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.no_image_placeholder)
                .error(R.drawable.no_image_placeholder)
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingIndicator.visibility = View.GONE
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        loadingIndicator.visibility = View.GONE
                        return false
                    }
                })
                .into(imageView)
        } catch (e: Exception) {
            loadingIndicator.visibility = View.GONE
            imageView.setImageResource(R.drawable.no_image_placeholder)
        }

        // Close button click
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Close on outside click
        dialog.setOnDismissListener {
            // Clean up if needed
        }

        dialog.show()
    }

    private fun setupImageZoom(imageView: ImageView) {
        var scaleFactor = 1.0f
        val minScale = 0.5f
        val maxScale = 3.0f

        imageView.setOnTouchListener { _, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    // Multi-touch detected
                    val oldDist = getDistance(event)
                    if (oldDist > 10f) {
                        val oldScale = scaleFactor
                        scaleFactor = (oldDist * oldScale) / oldDist
                        scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
                        imageView.scaleX = scaleFactor
                        imageView.scaleY = scaleFactor
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 2) {
                        val newDist = getDistance(event)
                        if (newDist > 10f) {
                            val oldScale = scaleFactor
                            scaleFactor = (newDist * oldScale) / newDist
                            scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
                            imageView.scaleX = scaleFactor
                            imageView.scaleY = scaleFactor
                        }
                    }
                }
            }
            true
        }
    }

    private fun getDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    private fun updatePageTitle(title: String) {
        activity?.title = title
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
        return error?.errorMsg?.takeIf { it.isNotBlank() } ?: "Failed Job Submit"
    }

    private fun mimeTypeForUpload(file: File): String {
        return when {
            file.name.lowercase().endsWith(".pdf") -> "application/pdf"
            file.name.lowercase().endsWith(".png") -> "image/png"
            file.name.lowercase().endsWith(".webp") -> "image/webp"
            file.name.lowercase().endsWith(".gif") -> "image/gif"
            else -> "image/jpeg"
        }
    }
}