package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.ActivityFactoryTermsBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.FactoryTermsAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import dagger.hilt.android.AndroidEntryPoint

/**
 * The factory's rules. Opened in two ways: as a read-only reference from the attendance
 * screen, and as a gate ([EXTRA_REQUIRE_ACCEPTANCE]) that the employee must accept before
 * attendance is usable at all.
 */
@AndroidEntryPoint
class FactoryTermsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFactoryTermsBinding
    private val viewModel: EmployeeAttendanceViewModel by viewModels()
    private val adapter = FactoryTermsAdapter()

    /** True when this screen is blocking access to attendance. */
    private var requireAcceptance = false
    private var accepted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFactoryTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requireAcceptance = intent.getBooleanExtra(EXTRA_REQUIRE_ACCEPTANCE, false)

        binding.termsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.termsRecyclerView.adapter = adapter

        binding.backButton.setOnClickListener { finish() }
        binding.errorRetryButton.setOnClickListener { viewModel.loadFactoryTerms() }

        binding.agreeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            setAcceptEnabled(isChecked)
        }
        binding.acceptButton.setOnClickListener {
            if (!binding.agreeCheckBox.isChecked) return@setOnClickListener
            binding.acceptErrorText.visibility = View.GONE
            viewModel.acceptFactoryTerms()
        }

        observeTerms()
        observeAcceptance()
        viewModel.loadFactoryTerms()
    }

    private fun setAcceptEnabled(enabled: Boolean) {
        binding.acceptButton.isEnabled = enabled
        binding.acceptButton.isClickable = enabled
        binding.acceptButton.alpha = if (enabled) 1f else 0.6f
        binding.acceptButton.setBackgroundResource(
            if (enabled) R.drawable.bg_punch_in_button else R.drawable.bg_punch_disabled_button,
        )
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

                    // Trust the server's view of whether acceptance is outstanding: the caller
                    // only knows what was true when the attendance screen last loaded.
                    val stillRequired = data?.terms?.acceptanceRequired ?: requireAcceptance
                    val showBar = requireAcceptance && stillRequired && terms.isNotEmpty()
                    binding.acceptBar.visibility = if (showBar) View.VISIBLE else View.GONE
                    if (showBar) setAcceptEnabled(binding.agreeCheckBox.isChecked)

                    // Nothing left to agree to (an admin removed the terms while this was
                    // open), so stop blocking.
                    if (requireAcceptance && !stillRequired) finishAccepted()
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

    private fun observeAcceptance() {
        viewModel.acceptTermsLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.loadingOverlay.visibility = View.VISIBLE
                    binding.acceptErrorText.visibility = View.GONE
                }
                is ApiResult.Success -> {
                    binding.loadingOverlay.visibility = View.GONE
                    if (result.data?.status == false) {
                        showAcceptError(
                            result.data?.message ?: "Could not save your acceptance. Please try again.",
                        )
                    } else {
                        finishAccepted()
                    }
                }
                is ApiResult.Error -> {
                    binding.loadingOverlay.visibility = View.GONE
                    showAcceptError(AttendanceErrorParser.parse(result.message).message)
                }
            }
        }
    }

    private fun showAcceptError(message: String) {
        binding.acceptErrorText.text = message
        binding.acceptErrorText.visibility = View.VISIBLE
    }

    private fun finishAccepted() {
        accepted = true
        setResult(RESULT_OK)
        finish()
    }

    override fun finish() {
        // Backing out of the gate without agreeing must not leave attendance open behind it.
        if (requireAcceptance && !accepted) setResult(RESULT_CANCELED)
        super.finish()
    }

    companion object {
        const val EXTRA_REQUIRE_ACCEPTANCE = "extra_require_acceptance"
    }
}
