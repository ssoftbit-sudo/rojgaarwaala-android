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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplyFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApplyFormBinding
    private lateinit var viewModel: ApplyViewModel
    private var selectedResumeUri: Uri? = null
    private var videoId: Int = 0
    private var videoTitle: String = ""

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

        // Get intent extras
        videoId = intent.getIntExtra("video_id", 0)
        videoTitle = intent.getStringExtra("video_title") ?: ""

        binding.jobTitle.text = videoTitle

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

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

        binding.submitButton.isEnabled = false
        binding.submitButton.text = "Submitting..."

        viewModel.submitApplication(videoId, name, phone, email, selectedResumeUri)
    }

    private fun observeViewModel() {
        viewModel.submitResult.observe(this) { success ->
            binding.submitButton.isEnabled = true
            binding.submitButton.text = "Submit Application"

            if (success) {
                // Navigate to payment screen
                val intent = Intent(this, PaymentActivity::class.java)
                intent.putExtra("video_id", videoId)
                intent.putExtra("application_id", viewModel.applicationId.value)
                intent.putExtra("candidate_name", binding.nameInput.text?.toString()?.trim().orEmpty())
                intent.putExtra("candidate_phone", binding.phoneInput.text?.toString()?.trim().orEmpty())
                intent.putExtra("candidate_email", binding.emailInput.text?.toString()?.trim().orEmpty())
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(
                    this,
                    viewModel.errorMessage.value ?: "Failed to submit application",
                    Toast.LENGTH_LONG
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