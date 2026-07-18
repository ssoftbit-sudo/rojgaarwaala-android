package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.databinding.ActivityAppliedJobsBinding
import com.srijeesolution.rojgaarwaala.presentation.adaptor.AppliedJobsAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.JobApplicationsViewModel
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppliedJobsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppliedJobsBinding
    private val viewModel: JobApplicationsViewModel by viewModels()
    private lateinit var adapter: AppliedJobsAdapter

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

        adapter = AppliedJobsAdapter(emptyList()) { application ->
            val intent = Intent(this, ApplicationStatusActivity::class.java)
            intent.putExtra("application_id", application.id?.toString().orEmpty())
            startActivity(intent)
        }

        binding.appliedJobsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.appliedJobsRecyclerView.adapter = adapter

        binding.backButton.setOnClickListener { finish() }

        viewModel.applications.observe(this) { applications ->
            adapter.updateApplications(applications)
            binding.emptyStateText.visibility =
                if (applications.isEmpty()) View.VISIBLE else View.GONE
            binding.appliedJobsRecyclerView.visibility =
                if (applications.isEmpty()) View.GONE else View.VISIBLE
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
        viewModel.refreshApplications(isLoggedIn = true)
    }
}
