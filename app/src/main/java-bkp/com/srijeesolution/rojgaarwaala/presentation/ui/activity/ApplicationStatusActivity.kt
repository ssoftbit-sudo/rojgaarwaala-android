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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplicationStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[StatusViewModel::class.java]

        applicationId = intent.getStringExtra("application_id") ?: ""

        setupClickListeners()
        observeViewModel()
        loadStatus()
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
            "pending" -> "Resume HR ko forward kar diya gaya hai" to android.R.color.holo_blue_dark
            "selected" -> "Selected" to android.R.color.holo_green_dark
            "rejected" -> "Rejected" to android.R.color.holo_red_dark
            "paid" -> "Payment completed. Application under review." to android.R.color.holo_orange_dark
            else -> "Status: $status" to android.R.color.darker_gray
        }

        binding.statusText.text = statusText
        binding.statusText.setTextColor(resources.getColor(statusColor))
    }
}