package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.databinding.ActivityAddJobBinding
import com.srijeesolution.rojgaarwaala.databinding.ActivityLoginBinding
import com.srijeesolution.rojgaarwaala.databinding.ActivityRegisterBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddJobActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddJobBinding
    @Inject
    lateinit var sharedPrefs: SharedPrefs
    private lateinit var homePageViewModel: HomePageViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddJobBinding.inflate(layoutInflater)
        setContentView(binding.root)
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        observeJobSubmitData()
        binding.submitBtn.setOnClickListener {
            validateLogin()
        }
        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)  // Show back arrow
        supportActionBar?.title = "Add Job"                // Optional if title is set in XML

        binding.toolBar.setNavigationOnClickListener {
            finish()
        }
    }


    private fun observeJobSubmitData() {
        homePageViewModel.jobSubmitLiveData.observe(this) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                }
                is ApiResult.Success -> {
                    if (apiResponse.data?.dataObj != null) {
                        binding.progressBar.visibility= View.GONE
                        Toast.makeText(this,"Job added successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }else{
                        Toast.makeText(this,""+apiResponse.data?.message, Toast.LENGTH_SHORT).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility= View.GONE
                    Toast.makeText(this,"Failed Job Submit", Toast.LENGTH_SHORT).show()
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

        homePageViewModel.onSubmitJob(requestBody)
    }
}