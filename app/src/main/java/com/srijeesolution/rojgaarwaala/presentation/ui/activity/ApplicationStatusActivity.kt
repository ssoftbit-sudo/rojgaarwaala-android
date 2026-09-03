package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.data.remote.model.JobApplicationDto
import com.srijeesolution.rojgaarwaala.databinding.ActivityApplicationStatusBinding
import com.srijeesolution.rojgaarwaala.presentation.adaptor.ApplicationTimelineAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.StatusViewModel
import com.srijeesolution.rojgaarwaala.utils.ApplicationPaymentCopy
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApplicationStatusBinding
    private lateinit var viewModel: StatusViewModel
    private lateinit var timelineAdapter: ApplicationTimelineAdapter
    private var applicationId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplicationStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[StatusViewModel::class.java]
        applicationId = intent.getStringExtra("application_id") ?: ""

        timelineAdapter = ApplicationTimelineAdapter(emptyList())
        binding.timelineRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.timelineRecyclerView.adapter = timelineAdapter
        binding.timelineRecyclerView.isNestedScrollingEnabled = false

        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        loadStatus()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.checkStatusButton.setOnClickListener { loadStatus() }
        binding.payNowButton.setOnClickListener {
            val application = viewModel.applicationDetails.value ?: return@setOnClickListener
            startActivity(
                Intent(this, PaymentActivity::class.java).apply {
                    putExtra(PaymentActivity.EXTRA_APPLICATION_ID, application.id?.toString().orEmpty())
                    putExtra(PaymentActivity.EXTRA_AMOUNT_PAISE, application.amountPaise ?: 0)
                },
            )
        }
    }

    private fun loadStatus() {
        if (applicationId.isNotEmpty()) {
            viewModel.getApplicationDetails(applicationId)
        }
    }

    private fun observeViewModel() {
        viewModel.applicationDetails.observe(this) { application ->
            if (application == null) return@observe

            binding.jobTitleText.text = application.jobTitle ?: "Job Application"
            binding.categoryText.text = application.categoryName?.let { "Category - $it" }.orEmpty()
            binding.categoryText.visibility =
                if (application.categoryName.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.appliedDateText.text = application.appliedAt?.let {
                "Applied on $it"
            } ?: "Application submitted"

            updateStatusDisplay(application)

            val timeline = application.timeline.orEmpty()
            timelineAdapter.updateEntries(timeline)
            binding.timelineEmptyText.visibility = if (timeline.isEmpty()) View.VISIBLE else View.GONE
            binding.timelineRecyclerView.visibility = if (timeline.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.checkStatusButton.isEnabled = !loading
            binding.checkStatusButton.text = if (loading) "Loading..." else "Refresh Status"
        }
    }

    private fun updateStatusDisplay(application: JobApplicationDto) {
        binding.statusText.text = ApplicationPaymentCopy.applicationStatusCopy(application.status)

        val paymentLine = ApplicationPaymentCopy.statusPaymentLine(
            application.paymentStatus,
            application.amountPaise,
        )
        if (paymentLine.isBlank()) {
            binding.paymentStatusText.visibility = View.GONE
        } else {
            binding.paymentStatusText.visibility = View.VISIBLE
            binding.paymentStatusText.text = paymentLine
        }

        binding.payNowButton.visibility =
            if (ApplicationPaymentCopy.needsPayment(application.paymentStatus, application.amountPaise)) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }
}
