package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.FragmentAddJobBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.app.AlertDialog

import com.srijeesolution.rojgaarwaala.R

@AndroidEntryPoint
class AddJobFragment : Fragment() {
    private var _binding: FragmentAddJobBinding? = null
    private val binding get() = _binding!!
    @Inject
    lateinit var sharedPrefs: SharedPrefs
    private lateinit var homePageViewModel: HomePageViewModel
    private var updateJobId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddJobBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        // Check for update mode
        arguments?.let { args ->
            updateJobId = args.getInt("job_id", -1).takeIf { it != -1 }
            binding.jobTitle.setText(args.getString("job_title", ""))
            binding.jobDescription.setText(args.getString("job_description", ""))
            binding.jobCategory.setText(args.getString("job_category", ""))
            binding.jobResponsibility.setText(args.getString("job_responsibility", ""))
            if (updateJobId != null) {
                binding.submitBtn.text = "Update Job"
            }
        }

        observeJobSubmitData()
        observeJobUpdateData()
        setupCategoryDropdown()
        binding.submitBtn.setOnClickListener {
            hideKeyboard()
            binding.submitBtn.isEnabled = false
            binding.submitBtn.text = if (updateJobId != null) "Updating..." else "Submitting..."
            validateLogin()
        }
        binding.viewAllJobsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.rootLayout, ViewAllJobsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        view?.let { v ->
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    private fun observeJobSubmitData() {
        homePageViewModel.jobSubmitLiveData.observe(viewLifecycleOwner) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility= View.GONE
                    binding.submitBtn.isEnabled = true
                    binding.submitBtn.text = "Submit Job"
                    if (apiResponse.data?.dataObj != null) {
                        Toast.makeText(requireContext(),"Job added successfully!", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.selectTabFromFragment(0)
                    }else{
                        Toast.makeText(requireContext(),""+apiResponse.data?.message, Toast.LENGTH_SHORT).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility= View.GONE
                    binding.submitBtn.isEnabled = true
                    binding.submitBtn.text = "Submit Job"
                    Toast.makeText(requireContext(),"Failed Job Submit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeJobUpdateData() {
        homePageViewModel.updateJobLiveData.observe(viewLifecycleOwner) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility= View.GONE
                    binding.submitBtn.isEnabled = true
                    binding.submitBtn.text = "Update Job"
                    Toast.makeText(requireContext(),"Job updated successfully!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.rootLayout, ViewAllJobsFragment())
                        .commit()
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility= View.GONE
                    binding.submitBtn.isEnabled = true
                    binding.submitBtn.text = "Update Job"
                    Toast.makeText(requireContext(),"Failed to update job", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupCategoryDropdown() {
        binding.jobCategory.isFocusable = false
        binding.jobCategory.isClickable = true
        binding.jobCategory.setOnClickListener {
            // Show loading
            binding.progressBar.visibility = View.VISIBLE
            // Fetch categories
            homePageViewModel.getCategoriesData()
            observeCategoriesDropdown()
        }
    }

    private fun observeCategoriesDropdown() {
        homePageViewModel.categoriesLiveData.observe(viewLifecycleOwner) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val data = apiResponse.data?.dataObj
                    val categories = data?.categories ?: emptyList<com.srijeesolution.rojgaarwaala.data.remote.model.Category>()
                    val titles = categories.mapNotNull { it.title }
                    if (titles.isNotEmpty()) {
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("Select Category")
                        builder.setItems(titles.toTypedArray()) { dialog, which ->
                            binding.jobCategory.setText(titles[which])
                        }
                        builder.show()
                    } else {
                        Toast.makeText(requireContext(), "No categories found", Toast.LENGTH_SHORT).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validateLogin() {
        val jobTitle = binding.jobTitle.text.toString().trim()
        val jobDescription = binding.jobDescription.text.toString().trim()
        val jobCategory = binding.jobCategory.text.toString().trim()
        val jobResponsibility = binding.jobResponsibility.text.toString().trim()

        if (jobTitle.isEmpty()) {
            binding.jobTitle.error = "Title is required"
            return
        }

        if (jobDescription.isEmpty()) {
            binding.jobDescription.error = "Description is required"
            return
        }

        if (jobCategory.isEmpty()) {
            binding.jobCategory.error = "Category is required"
            return
        }

        if (jobResponsibility.isEmpty()) {
            binding.jobResponsibility.error = "Responsibility is required"
            return
        }

        // ✅ If all fields are valid
        onSuccess(jobTitle, jobDescription, jobCategory, jobResponsibility)
    }

    private fun onSuccess(
        jobTitle: String,
        jobDescription: String,
        jobCategory: String,
        jobResponsibility: String) {
        binding.progressBar.visibility = View.VISIBLE

        val requestBody = HashMap<String, String>().apply {
            this["job_title"] = jobTitle
            this["job_description"] = jobDescription
            this["job_category"] = jobCategory
            this["job_responsibility"] = jobResponsibility
        }

        if (updateJobId != null) {
            homePageViewModel.updateJob(updateJobId!!, requestBody)
        } else {
            homePageViewModel.onSubmitJob(requestBody)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 