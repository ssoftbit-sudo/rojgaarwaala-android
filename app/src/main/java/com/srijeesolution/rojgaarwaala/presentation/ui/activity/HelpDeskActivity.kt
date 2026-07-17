package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.databinding.ActivityHelpDeskBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.ui.adapter.HelpFaqAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HelpDeskViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HelpDeskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpDeskBinding
    private val viewModel: HelpDeskViewModel by viewModels()
    private val faqAdapter = HelpFaqAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpDeskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.helpDeskBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.helpDeskFaqList.layoutManager = LinearLayoutManager(this)
        binding.helpDeskFaqList.adapter = faqAdapter

        binding.enquirySubmitButton.setOnClickListener {
            submitEnquiry()
        }

        observeFaqs()
        observeSubmit()
        viewModel.loadFaqs()
    }

    private fun observeFaqs() {
        viewModel.faqsLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.helpDeskLoading.visibility = View.VISIBLE
                    binding.helpDeskEmptyText.visibility = View.GONE
                }
                is ApiResult.Success -> {
                    binding.helpDeskLoading.visibility = View.GONE
                    val categories = result.data?.data?.categories.orEmpty()
                    val mapped = categories.mapNotNull { category ->
                        val title = category.category?.trim().orEmpty()
                        if (title.isBlank()) return@mapNotNull null
                        val faqs = category.faqs.orEmpty().mapNotNull { faq ->
                            val id = faq.id ?: return@mapNotNull null
                            val question = faq.question?.trim().orEmpty()
                            val answer = faq.answer?.trim().orEmpty()
                            if (question.isBlank() || answer.isBlank()) return@mapNotNull null
                            Triple(id, question, answer)
                        }
                        if (faqs.isEmpty()) return@mapNotNull null
                        title to faqs
                    }

                    if (mapped.isEmpty()) {
                        binding.helpDeskEmptyText.visibility = View.VISIBLE
                    } else {
                        binding.helpDeskEmptyText.visibility = View.GONE
                        faqAdapter.submitCategories(mapped)
                    }
                }
                is ApiResult.Error -> {
                    binding.helpDeskLoading.visibility = View.GONE
                    binding.helpDeskEmptyText.visibility = View.VISIBLE
                    Toast.makeText(this, result.message?.errorMsg ?: "Failed to load FAQs", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeSubmit() {
        viewModel.submitLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.enquirySubmitButton.isEnabled = false
                }
                is ApiResult.Success -> {
                    binding.enquirySubmitButton.isEnabled = true
                    Toast.makeText(
                        this,
                        result.data?.message ?: getString(com.srijeesolution.rojgaarwaala.R.string.enquiry_submitted),
                        Toast.LENGTH_LONG,
                    ).show()
                    clearEnquiryForm()
                    hideKeyboard()
                }
                is ApiResult.Error -> {
                    binding.enquirySubmitButton.isEnabled = true
                    Toast.makeText(this, result.message?.errorMsg ?: "Submission failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun submitEnquiry() {
        val name = binding.enquiryNameInput.text?.toString()?.trim().orEmpty()
        val email = binding.enquiryEmailInput.text?.toString()?.trim().orEmpty()
        val mobile = binding.enquiryMobileInput.text?.toString()?.trim().orEmpty()
        val subject = binding.enquirySubjectInput.text?.toString()?.trim().orEmpty()
        val message = binding.enquiryMessageInput.text?.toString()?.trim().orEmpty()

        when {
            name.isBlank() -> binding.enquiryNameInput.error = getString(com.srijeesolution.rojgaarwaala.R.string.required_field)
            email.isBlank() -> binding.enquiryEmailInput.error = getString(com.srijeesolution.rojgaarwaala.R.string.required_field)
            mobile.length < 10 -> binding.enquiryMobileInput.error = getString(com.srijeesolution.rojgaarwaala.R.string.invalid_mobile)
            subject.isBlank() -> binding.enquirySubjectInput.error = getString(com.srijeesolution.rojgaarwaala.R.string.required_field)
            message.isBlank() -> binding.enquiryMessageInput.error = getString(com.srijeesolution.rojgaarwaala.R.string.required_field)
            else -> {
                viewModel.submitEnquiry(
                    hashMapOf(
                        "name" to name,
                        "email" to email,
                        "mobile" to mobile,
                        "subject" to subject,
                        "message" to message,
                    ),
                )
            }
        }
    }

    private fun clearEnquiryForm() {
        binding.enquirySubjectInput.text?.clear()
        binding.enquiryMessageInput.text?.clear()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}
