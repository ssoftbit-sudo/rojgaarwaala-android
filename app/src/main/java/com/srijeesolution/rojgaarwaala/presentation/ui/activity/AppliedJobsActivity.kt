package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.JobApplicationDto
import com.srijeesolution.rojgaarwaala.databinding.ActivityAppliedJobsBinding
import com.srijeesolution.rojgaarwaala.presentation.adaptor.AppliedJobsAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.JobApplicationsViewModel
import com.srijeesolution.rojgaarwaala.utils.ApplicationPaymentCopy
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppliedJobsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppliedJobsBinding
    private val viewModel: JobApplicationsViewModel by viewModels()
    private lateinit var adapter: AppliedJobsAdapter
    private var allApplications: List<JobApplicationDto> = emptyList()
    private var paidOnlyLaunch = false
    private var showPaidOnly = false

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppliedJobsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
            Toast.makeText(this, "Please login to view job status", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        paidOnlyLaunch = intent.getBooleanExtra(EXTRA_PAID_ONLY, false)
        showPaidOnly = paidOnlyLaunch
        binding.screenTitle.text = if (paidOnlyLaunch) "Paid applications" else "Job Status"
        binding.filterChips.visibility = if (paidOnlyLaunch) View.GONE else View.VISIBLE
        updateChipStyles()

        adapter = AppliedJobsAdapter(emptyList()) { application ->
            val intent = Intent(this, ApplicationStatusActivity::class.java)
            intent.putExtra("application_id", application.id?.toString().orEmpty())
            startActivity(intent)
        }

        binding.appliedJobsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.appliedJobsRecyclerView.adapter = adapter

        binding.backButton.setOnClickListener { finish() }
        binding.chipAll.setOnClickListener {
            showPaidOnly = false
            updateChipStyles()
            renderList()
        }
        binding.chipPaid.setOnClickListener {
            showPaidOnly = true
            updateChipStyles()
            renderList()
        }

        viewModel.applications.observe(this) { applications ->
            allApplications = applications
            renderList()
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.markJobStatusSeen()
    }

    override fun onResume() {
        super.onResume()
        if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
            viewModel.refreshApplications(isLoggedIn = true)
        }
    }

    private fun renderList() {
        val visible = if (showPaidOnly) {
            allApplications.filter {
                ApplicationPaymentCopy.isFeePaid(it.paymentStatus, it.amountPaise)
            }
        } else {
            allApplications
        }
        adapter.updateApplications(visible)
        binding.emptyStateText.text = if (showPaidOnly) {
            "No paid applications yet."
        } else {
            "No job applications yet."
        }
        binding.emptyStateText.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        binding.appliedJobsRecyclerView.visibility = if (visible.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateChipStyles() {
        if (showPaidOnly) {
            binding.chipAll.setBackgroundResource(R.drawable.bg_theme_chip_muted)
            binding.chipAll.setTextColor(getColor(R.color.white))
            binding.chipPaid.setBackgroundResource(R.drawable.bg_theme_chip)
            binding.chipPaid.setTextColor(getColor(R.color.accent))
        } else {
            binding.chipAll.setBackgroundResource(R.drawable.bg_theme_chip)
            binding.chipAll.setTextColor(getColor(R.color.accent))
            binding.chipPaid.setBackgroundResource(R.drawable.bg_theme_chip_muted)
            binding.chipPaid.setTextColor(getColor(R.color.white))
        }
    }

    companion object {
        const val EXTRA_PAID_ONLY = "paid_only"
    }
}
