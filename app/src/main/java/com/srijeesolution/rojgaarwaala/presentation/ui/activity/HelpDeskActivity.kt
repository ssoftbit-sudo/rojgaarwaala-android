package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.HelpDeskIssueCategory
import com.srijeesolution.rojgaarwaala.databinding.ActivityHelpDeskBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.ui.adapter.HelpFaqAdapter
import com.srijeesolution.rojgaarwaala.presentation.ui.adapter.HelpSuggestionAdapter
import com.srijeesolution.rojgaarwaala.presentation.ui.adapter.HelpTutorialAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HelpDeskViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HelpDeskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpDeskBinding
    private val viewModel: HelpDeskViewModel by viewModels()
    private val faqAdapter = HelpFaqAdapter()
    private val suggestionAdapter = HelpSuggestionAdapter()
    private val tutorialAdapter = HelpTutorialAdapter(
        onOpenVideo = { url -> openExternalUrl(url) },
        onOpenAudio = { url -> openExternalUrl(url) },
    )

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private var issueCategories = emptyList<HelpDeskIssueCategory>()
    private var callPhone = "919201949203"
    private var whatsappPhone = "919201949203"
    private var selectedIssueKey = "apply"
    private var selectedContentType = "faq"
    private var selectedPhotoUri: Uri? = null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            binding.enquiryPhotoName.visibility = View.VISIBLE
            binding.enquiryPhotoName.text = uri.lastPathSegment ?: getString(R.string.photo_selected)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpDeskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.helpDeskBackButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.helpDeskCallButton.setOnClickListener { dialPhone(callPhone) }
        binding.helpDeskWhatsappButton.setOnClickListener { openWhatsApp(whatsappPhone) }
        binding.enquiryPhotoButton.setOnClickListener { photoPicker.launch("image/*") }
        binding.enquirySubmitButton.setOnClickListener { submitComplaint() }

        binding.helpDeskFaqList.layoutManager = LinearLayoutManager(this)
        binding.helpDeskFaqList.adapter = faqAdapter
        binding.helpDeskSuggestionsList.layoutManager = LinearLayoutManager(this)
        binding.helpDeskSuggestionsList.adapter = suggestionAdapter
        binding.helpDeskTutorialList.layoutManager = LinearLayoutManager(this)
        binding.helpDeskTutorialList.adapter = tutorialAdapter

        setupProblemSpinner()
        setupSearchWatcher()
        observeViewModel()

        binding.helpDeskIssueSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val category = issueCategories.getOrNull(position) ?: return
                selectedIssueKey = category.key.orEmpty().ifBlank { "apply" }
                selectedContentType = category.contentType.orEmpty().ifBlank { "faq" }
                renderSelectedCategory()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        viewModel.loadConfig()
    }

    private fun setupProblemSpinner() {
        val problems = resources.getStringArray(R.array.help_desk_problems)
        binding.enquiryProblemSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, problems)
    }

    private fun setupSearchWatcher() {
        binding.helpDeskSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (selectedContentType != "faq") return
                searchRunnable?.let(searchHandler::removeCallbacks)
                searchRunnable = Runnable {
                    viewModel.loadFaqs(selectedIssueKey, s?.toString()?.trim()?.takeIf { it.isNotEmpty() })
                }
                searchHandler.postDelayed(searchRunnable!!, 400)
            }
        })
    }

    private fun observeViewModel() {
        viewModel.configLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Success -> {
                    val data = result.data?.data
                    callPhone = data?.support?.callPhone.orEmpty().ifBlank { callPhone }
                    whatsappPhone = data?.support?.whatsappPhone.orEmpty().ifBlank { whatsappPhone }
                    issueCategories = data?.issueCategories.orEmpty()
                    val labels = issueCategories.map { it.label.orEmpty() }
                    binding.helpDeskIssueSpinner.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        labels,
                    )
                    if (issueCategories.isNotEmpty()) {
                        renderSelectedCategory()
                    }
                }
                is ApiResult.Error -> Toast.makeText(this, result.message?.errorMsg ?: "Failed to load help desk", Toast.LENGTH_SHORT).show()
                is ApiResult.Loading -> Unit
            }
        }

        viewModel.faqsLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.helpDeskLoading.visibility = View.VISIBLE
                    binding.helpDeskEmptyText.visibility = View.GONE
                }
                is ApiResult.Success -> {
                    binding.helpDeskLoading.visibility = View.GONE
                    val data = result.data?.data
                    val suggestions = data?.suggestions.orEmpty()
                    if (suggestions.isNotEmpty() && binding.helpDeskSearchInput.text?.isNotBlank() == true) {
                        binding.helpDeskSuggestionsLabel.visibility = View.VISIBLE
                        binding.helpDeskSuggestionsList.visibility = View.VISIBLE
                        suggestionAdapter.submitList(suggestions)
                    } else {
                        binding.helpDeskSuggestionsLabel.visibility = View.GONE
                        binding.helpDeskSuggestionsList.visibility = View.GONE
                    }

                    val mapped = data?.categories.orEmpty().mapNotNull { category ->
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
                        faqAdapter.submitCategories(emptyList())
                    } else {
                        binding.helpDeskEmptyText.visibility = View.GONE
                        faqAdapter.submitCategories(mapped)
                    }
                }
                is ApiResult.Error -> {
                    binding.helpDeskLoading.visibility = View.GONE
                    binding.helpDeskEmptyText.visibility = View.VISIBLE
                }
            }
        }

        viewModel.tutorialsLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.helpDeskTutorialLoading.visibility = View.VISIBLE
                    binding.helpDeskTutorialEmpty.visibility = View.GONE
                }
                is ApiResult.Success -> {
                    binding.helpDeskTutorialLoading.visibility = View.GONE
                    val tutorials = result.data?.data?.tutorials.orEmpty()
                    if (tutorials.isEmpty()) {
                        binding.helpDeskTutorialEmpty.visibility = View.VISIBLE
                    } else {
                        binding.helpDeskTutorialEmpty.visibility = View.GONE
                        tutorialAdapter.submitList(tutorials)
                    }
                }
                is ApiResult.Error -> {
                    binding.helpDeskTutorialLoading.visibility = View.GONE
                    binding.helpDeskTutorialEmpty.visibility = View.VISIBLE
                }
            }
        }

        viewModel.submitLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> binding.enquirySubmitButton.isEnabled = false
                is ApiResult.Success -> {
                    binding.enquirySubmitButton.isEnabled = true
                    Toast.makeText(this, result.data?.message ?: getString(R.string.enquiry_submitted), Toast.LENGTH_LONG).show()
                    binding.enquiryMessageInput.text?.clear()
                    selectedPhotoUri = null
                    binding.enquiryPhotoName.visibility = View.GONE
                }
                is ApiResult.Error -> {
                    binding.enquirySubmitButton.isEnabled = true
                    Toast.makeText(this, result.message?.errorMsg ?: "Submission failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun renderSelectedCategory() {
        binding.helpDeskFaqSection.visibility = if (selectedContentType == "faq") View.VISIBLE else View.GONE
        binding.helpDeskTutorialSection.visibility = if (selectedContentType == "tutorial") View.VISIBLE else View.GONE
        binding.helpDeskFormSection.visibility = if (selectedContentType == "form") View.VISIBLE else View.GONE

        when (selectedContentType) {
            "faq" -> viewModel.loadFaqs(selectedIssueKey, binding.helpDeskSearchInput.text?.toString()?.trim())
            "tutorial" -> viewModel.loadTutorials(selectedIssueKey)
        }
    }

    private fun submitComplaint() {
        val name = binding.enquiryNameInput.text?.toString()?.trim().orEmpty()
        val problem = binding.enquiryProblemSpinner.selectedItem?.toString().orEmpty()
        val message = binding.enquiryMessageInput.text?.toString()?.trim().orEmpty()

        when {
            name.isBlank() -> binding.enquiryNameInput.error = getString(R.string.required_field)
            message.isBlank() -> binding.enquiryMessageInput.error = getString(R.string.required_field)
            else -> viewModel.submitComplaint(name, problem, message, selectedPhotoUri)
        }
    }

    private fun dialPhone(phone: String) {
        val normalized = phone.filter { it.isDigit() || it == '+' }
        startActivity(Intent(Intent.ACTION_DIAL, "tel:+$normalized".toUri()))
    }

    private fun openWhatsApp(phone: String) {
        val digits = phone.filter { it.isDigit() }
        val uri = "https://wa.me/$digits?text=${Uri.encode("Namaste, mujhe Rojgaarwaala app se madad chahiye.")}".toUri()
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun openExternalUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
