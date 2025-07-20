package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.srijeesolution.rojgaarwaala.databinding.FragmentViewAllJobsBinding
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.data.remote.model.JobItem
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.R
import dagger.hilt.android.AndroidEntryPoint
import com.srijeesolution.rojgaarwaala.presentation.adaptor.JobsGridAdapter

@AndroidEntryPoint
class ViewAllJobsFragment : Fragment() {
    private var _binding: FragmentViewAllJobsBinding? = null
    private val binding get() = _binding!!
    private lateinit var homePageViewModel: HomePageViewModel
    private val liveJobsAdapter = JobsGridAdapter()
    private val inReviewJobsAdapter = JobsGridAdapter(
        onEdit = { job -> editJob(job) },
        onDelete = { job -> confirmDeleteJob(job) },
        showActions = true
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentViewAllJobsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]
        binding.inReviewJobsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.inReviewJobsRecyclerView.adapter = inReviewJobsAdapter
        binding.liveJobsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.liveJobsRecyclerView.adapter = liveJobsAdapter
        observeJobList()
        homePageViewModel.getJobList()
    }

    private fun observeJobList() {
        homePageViewModel.jobListLiveData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.bringToFront()
                    binding.inReviewJobsHeader.visibility = View.GONE
                    binding.liveJobsHeader.visibility = View.GONE
                    binding.inReviewJobsRecyclerView.visibility = View.GONE
                    binding.liveJobsRecyclerView.visibility = View.GONE
                    binding.noInReviewJobsText.visibility = View.GONE
                    binding.noLiveJobsText.visibility = View.GONE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val inReviewJobs = result.data?.data?.inReview ?: emptyList()
                    val liveJobs = result.data?.data?.live ?: emptyList()

                    // In Review Section
                    if (inReviewJobs.isNotEmpty()) {
                        binding.inReviewJobsHeader.visibility = View.VISIBLE
                        binding.inReviewJobsRecyclerView.visibility = View.VISIBLE
                        binding.noInReviewJobsText.visibility = View.GONE
                        inReviewJobsAdapter.submitList(inReviewJobs)
                    } else {
                        binding.inReviewJobsHeader.visibility = View.VISIBLE
                        binding.inReviewJobsRecyclerView.visibility = View.GONE
                        binding.noInReviewJobsText.visibility = View.VISIBLE
                    }

                    // Live Section
                    if (liveJobs.isNotEmpty()) {
                        binding.liveJobsHeader.visibility = View.VISIBLE
                        binding.liveJobsRecyclerView.visibility = View.VISIBLE
                        binding.noLiveJobsText.visibility = View.GONE
                        liveJobsAdapter.submitList(liveJobs)
                    } else {
                        binding.liveJobsHeader.visibility = View.VISIBLE
                        binding.liveJobsRecyclerView.visibility = View.GONE
                        binding.noLiveJobsText.visibility = View.VISIBLE
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.inReviewJobsHeader.visibility = View.GONE
                    binding.liveJobsHeader.visibility = View.GONE
                    binding.inReviewJobsRecyclerView.visibility = View.GONE
                    binding.liveJobsRecyclerView.visibility = View.GONE
                    binding.noInReviewJobsText.visibility = View.VISIBLE
                    binding.noLiveJobsText.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "Failed to load jobs", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun editJob(job: JobItem) {
        // Navigate to AddJobFragment with job data (using Bundle)
        val bundle = Bundle().apply {
            putInt("job_id", job.id ?: -1)
            putString("job_title", job.jobTitle)
            putString("job_description", job.jobDescription)
            putString("job_category", job.jobCategory)
            putString("job_responsibility", job.jobResponsibility)
            putString("job_pdf", job.pdf)
            putString("job_image", job.image)
            putString("job_logo", job.logo)
        }
        val addJobFragment = AddJobFragment()
        addJobFragment.arguments = bundle
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, addJobFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun confirmDeleteJob(job: JobItem) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Job")
            .setMessage("Are you sure you want to delete this job?")
            .setPositiveButton("Yes") { _, _ -> deleteJob(job) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteJob(job: JobItem) {
        // Call delete API (assume homePageViewModel.deleteJob exists)
        homePageViewModel.deleteJob(job.id ?: -1)
        homePageViewModel.deleteJobLiveData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ApiResult.Success -> {
                    Toast.makeText(requireContext(), "Job deleted successfully", Toast.LENGTH_SHORT).show()
                    homePageViewModel.getJobList() // Refresh jobs
                }
                is ApiResult.Error -> {
                    Toast.makeText(requireContext(), "Failed to delete job", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 