package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.ActivityApplyFormBinding
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.ApplyViewModel
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ApplyFormActivity : AppCompatActivity() {

  private lateinit var binding: ActivityApplyFormBinding
  private lateinit var viewModel: ApplyViewModel
  private var selectedResumeUri: Uri? = null
  private var videoId: Int = 0
  private var scheduledImageId: Int = 0
  private var jobTitle: String = ""

  @Inject
  lateinit var sharedPrefs: SharedPrefs

  private val resumePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    uri?.let {
      selectedResumeUri = it
      binding.resumeFileName.text = getFileNameFromUri(it) ?: "Selected file"
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityApplyFormBinding.inflate(layoutInflater)
    setContentView(binding.root)

    viewModel = ViewModelProvider(this)[ApplyViewModel::class.java]

    videoId = intent.getIntExtra("video_id", 0)
    scheduledImageId = intent.getIntExtra("scheduled_image_id", 0)
    jobTitle = intent.getStringExtra("video_title") ?: intent.getStringExtra("job_title") ?: ""

    binding.jobTitle.text = jobTitle.ifBlank { "Job Application" }

    setupClickListeners()
    observeViewModel()
  }

  private fun setupClickListeners() {
    binding.backButton.setOnClickListener { finish() }

    binding.selectResumeButton.setOnClickListener {
      resumePicker.launch("*/*")
    }

    binding.submitButton.setOnClickListener {
      submitApplication()
    }
  }

  private fun submitApplication() {
    val name = binding.nameInput.text.toString().trim()
    val phone = binding.phoneInput.text.toString().trim()
    val email = binding.emailInput.text.toString().trim()

    if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
      Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
      return
    }

    if (selectedResumeUri == null) {
      Toast.makeText(this, "Please select your resume", Toast.LENGTH_SHORT).show()
      return
    }

    if (videoId <= 0 && scheduledImageId <= 0) {
      Toast.makeText(this, "Job details are missing", Toast.LENGTH_SHORT).show()
      return
    }

    binding.submitButton.isEnabled = false
    binding.submitButton.text = "Submitting..."

    viewModel.submitApplication(
      videoId = videoId.takeIf { it > 0 },
      scheduledImageId = scheduledImageId.takeIf { it > 0 },
      jobTitle = jobTitle.takeIf { it.isNotBlank() },
      name = name,
      phone = phone,
      email = email,
      resumeUri = selectedResumeUri,
    )
  }

  private fun observeViewModel() {
    viewModel.submitResult.observe(this) { success ->
      binding.submitButton.isEnabled = true
      binding.submitButton.text = "Submit Application"

      if (success) {
        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.JOB_STATUS_UPDATE_PENDING, true))

        // Only paid listings route through checkout; everything else is already
        // submitted by the time the server replies.
        val intent = if (viewModel.requiresPayment.value == true) {
          Intent(this, PaymentActivity::class.java).apply {
            putExtra(PaymentActivity.EXTRA_APPLICATION_ID, viewModel.applicationId.value)
            putExtra(PaymentActivity.EXTRA_AMOUNT_PAISE, viewModel.amountPaise.value ?: 0)
          }
        } else {
          Intent(this, ApplicationStatusActivity::class.java).apply {
            putExtra("application_id", viewModel.applicationId.value)
          }
        }

        startActivity(intent)
        finish()
      } else {
        Toast.makeText(
          this,
          viewModel.errorMessage.value ?: "Failed to submit application",
          Toast.LENGTH_LONG,
        ).show()
      }
    }

    viewModel.isLoading.observe(this) { loading ->
      binding.submitButton.isEnabled = !loading
      binding.submitButton.text = if (loading) "Submitting..." else "Submit Application"
    }
  }

  private fun getFileNameFromUri(uri: Uri): String? {
    val cursor = contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
      val nameIndex = it.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
      it.moveToFirst()
      it.getString(nameIndex)
    }
  }
}
