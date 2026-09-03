package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.data.remote.model.JobApplicationDto
import com.srijeesolution.rojgaarwaala.databinding.ActivityApplyFormBinding
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.ApplyViewModel
import com.srijeesolution.rojgaarwaala.utils.ApplicationPaymentCopy
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
  private var existingApplication: JobApplicationDto? = null

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

  override fun onResume() {
    super.onResume()
    viewModel.loadExistingForListing(
      videoId = videoId.takeIf { it > 0 },
      scheduledImageId = scheduledImageId.takeIf { it > 0 },
    )
  }

  private fun setupClickListeners() {
    binding.backButton.setOnClickListener { finish() }

    binding.selectResumeButton.setOnClickListener {
      resumePicker.launch("*/*")
    }

    binding.submitButton.setOnClickListener {
      submitApplication()
    }

    binding.viewStatusButton.setOnClickListener {
      existingApplication?.id?.let { openStatus(it) }
    }

    binding.payNowButton.setOnClickListener {
      val application = existingApplication ?: return@setOnClickListener
      openPayment(application.id, application.amountPaise)
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
    viewModel.existingApplication.observe(this) { application ->
      existingApplication = application
      showExistingApplication(application)
    }

    viewModel.checkingExisting.observe(this) { checking ->
      binding.existingCheckProgress.visibility =
        if (checking && existingApplication == null) View.VISIBLE else View.GONE
    }

    viewModel.submitResult.observe(this) { success ->
      binding.submitButton.isEnabled = true
      binding.submitButton.text = "Submit Application"

      if (success) {
        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.JOB_STATUS_UPDATE_PENDING, true))

        val applicationId = viewModel.applicationId.value?.toIntOrNull()
        if (viewModel.requiresPayment.value == true) {
          openPayment(applicationId, viewModel.amountPaise.value)
        } else {
          openStatus(applicationId)
        }
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

  private fun showExistingApplication(application: JobApplicationDto?) {
    if (application == null) {
      binding.alreadyAppliedPanel.visibility = View.GONE
      binding.applyFormFields.visibility = View.VISIBLE
      return
    }

    binding.applyFormFields.visibility = View.GONE
    binding.alreadyAppliedPanel.visibility = View.VISIBLE
    binding.alreadyAppliedTitle.text = ApplicationPaymentCopy.alreadyAppliedTitle()
    binding.alreadyAppliedPayment.text = ApplicationPaymentCopy.applyPaymentDetail(
      application.paymentStatus,
      application.amountPaise,
    )
    binding.payNowButton.visibility =
      if (ApplicationPaymentCopy.needsPayment(application.paymentStatus, application.amountPaise)) {
        View.VISIBLE
      } else {
        View.GONE
      }
  }

  private fun openStatus(applicationId: Int?) {
    if (applicationId == null || applicationId <= 0) return
    startActivity(
      Intent(this, ApplicationStatusActivity::class.java).apply {
        putExtra("application_id", applicationId.toString())
      },
    )
  }

  private fun openPayment(applicationId: Int?, amountPaise: Int?) {
    if (applicationId == null || applicationId <= 0) return
    startActivity(
      Intent(this, PaymentActivity::class.java).apply {
        putExtra(PaymentActivity.EXTRA_APPLICATION_ID, applicationId.toString())
        putExtra(PaymentActivity.EXTRA_AMOUNT_PAISE, amountPaise ?: 0)
      },
    )
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
