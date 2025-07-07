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
    private val inReviewJobsAdapter = JobsGridAdapter()
    private val liveJobsAdapter = JobsGridAdapter()

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
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val inReviewJobs = result.data?.data?.inReview ?: emptyList()
                    val liveJobs = result.data?.data?.live ?: emptyList()
                    inReviewJobsAdapter.submitList(inReviewJobs)
                    liveJobsAdapter.submitList(liveJobs)
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load jobs", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 