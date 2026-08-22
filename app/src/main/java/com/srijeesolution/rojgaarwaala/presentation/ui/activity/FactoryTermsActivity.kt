package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.databinding.ActivityFactoryTermsBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.FactoryTermsAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FactoryTermsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFactoryTermsBinding
    private val viewModel: EmployeeAttendanceViewModel by viewModels()
    private val adapter = FactoryTermsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFactoryTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.termsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.termsRecyclerView.adapter = adapter

        binding.backButton.setOnClickListener { finish() }
        binding.errorRetryButton.setOnClickListener { viewModel.loadFactoryTerms() }

        observeTerms()
        viewModel.loadFactoryTerms()
    }

    private fun observeTerms() {
        viewModel.factoryTermsLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.loadingOverlay.visibility = View.VISIBLE
                    binding.errorStateLayout.visibility = View.GONE
                    binding.mainContent.visibility = View.GONE
                }
                is ApiResult.Success -> {
                    binding.loadingOverlay.visibility = View.GONE
                    binding.errorStateLayout.visibility = View.GONE
                    binding.mainContent.visibility = View.VISIBLE

                    val data = result.data?.data
                    binding.factoryNameText.text = data?.factory?.name ?: "Factory"

                    val terms = data?.termsList ?: emptyList()
                    adapter.submitList(terms)
                    binding.emptyStateText.visibility =
                        if (terms.isEmpty()) View.VISIBLE else View.GONE
                    binding.termsRecyclerView.visibility =
                        if (terms.isEmpty()) View.GONE else View.VISIBLE
                }
                is ApiResult.Error -> {
                    binding.loadingOverlay.visibility = View.GONE
                    binding.mainContent.visibility = View.GONE
                    binding.errorStateLayout.visibility = View.VISIBLE
                    binding.errorStateText.text = AttendanceErrorParser.parse(result.message).message
                }
            }
        }
    }
}
