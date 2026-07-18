package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.ActivityApplicationStatusBinding
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.StatusViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApplicationStatusBinding
    private lateinit var viewModel: StatusViewModel
    private var applicationId: String = ""
    private var statusOverride: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplicationStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[StatusViewModel::class.java]

        applicationId = intent.getStringExtra("application_id") ?: ""
        statusOverride = intent.getStringExtra("status_override") ?: ""

        setupClickListeners()
        observeViewModel()
        if (statusOverride.isNotEmpty()) {
            updateStatusDisplay(statusOverride)
            binding.checkStatusButton.isEnabled = false
            binding.checkStatusButton.text = "Demo Status"
        } else {
            loadStatus()
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        binding.checkStatusButton.setOnClickListener {
            loadStatus()
        }
    }

    private fun loadStatus() {
        if (statusOverride.isNotEmpty()) {
            updateStatusDisplay(statusOverride)
            return
        }
        if (applicationId.isNotEmpty()) {
            viewModel.getApplicationStatus(applicationId)
        }
    }

    private fun observeViewModel() {
        viewModel.applicationStatus.observe(this) { status ->
            updateStatusDisplay(status)
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.checkStatusButton.isEnabled = !loading
            binding.checkStatusButton.text = if (loading) "Loading..." else "Refresh Status"
        }
    }

    private fun updateStatusDisplay(status: String) {
        val (statusText, statusColor) = when (status.lowercase()) {
            "applied" -> "Congratulations! Your application has been submitted successfully." to android.R.color.holo_green_dark
            "under_review" -> "Your application is under review." to android.R.color.holo_blue_dark
            "interview_scheduled" -> "Interview scheduled. HR will contact you." to android.R.color.holo_orange_dark
            "selected" -> "Congratulations! You are selected." to android.R.color.holo_green_dark
            "rejected" -> "Application not selected this time." to android.R.color.holo_red_dark
            "pending_payment" -> "Payment pending. Please complete payment." to android.R.color.holo_orange_dark
            "failed" -> "Payment failed. Please retry from apply flow." to android.R.color.holo_red_dark
            else -> "Status: ${status.replace('_', ' ')}" to android.R.color.darker_gray
        }

        binding.statusText.text = statusText
        binding.statusText.setTextColor(resources.getColor(statusColor))
    }
}