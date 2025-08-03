package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.databinding.ActivityCategoryVideosBinding
import com.srijeesolution.rojgaarwaala.presentation.adaptor.TopVideosAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.GridVideosListAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryVideosActivity : ComponentActivity() {
    private lateinit var binding: ActivityCategoryVideosBinding
    private val viewModel: HomePageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryVideosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val categoryId = intent.getIntExtra("category_id", -1)
        val categoryTitle = intent.getStringExtra("category_title") ?: ""
        val categoryIcon = intent.getStringExtra("category_icon")

        // Also check for string category_id (from notifications)
        var finalCategoryId = categoryId
        if (categoryId == -1) {
            val categoryIdString = intent.getStringExtra("category_id")
            android.util.Log.d("CategoryVideosActivity", "Category ID from intent: $categoryIdString")
            if (!categoryIdString.isNullOrEmpty()) {
                try {
                    finalCategoryId = categoryIdString.toInt()
                    android.util.Log.d("CategoryVideosActivity", "Converted category ID: $finalCategoryId")
                } catch (e: NumberFormatException) {
                    android.util.Log.e("CategoryVideosActivity", "Failed to convert category ID: $categoryIdString", e)
                    Toast.makeText(this, "Invalid category ID format", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
            }
        }

        android.util.Log.d("CategoryVideosActivity", "Final category ID: $finalCategoryId")
        binding.categoryTitle.text = categoryTitle
        if (!categoryIcon.isNullOrEmpty()) {
            Glide.with(this).load(categoryIcon).into(binding.categoryIcon)
        }

        binding.videosRecyclerView.layoutManager = GridLayoutManager(this, 2)

        if (finalCategoryId == -1 && categoryTitle == "Top Videos") {
            // Handle top videos case
            viewModel.getHomePageData("") // Get all home page data to extract top videos
        } else if (finalCategoryId != -1) {
            viewModel.getCategoryVideos(finalCategoryId)
        } else {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
        }
        observeData()
    }

    private fun observeData() {
        val categoryId = intent.getIntExtra("category_id", -1)
        val categoryTitle = intent.getStringExtra("category_title") ?: ""
        
        // Get the final category ID (same logic as onCreate)
        var finalCategoryId = categoryId
        if (categoryId == -1) {
            val categoryIdString = intent.getStringExtra("category_id")
            if (!categoryIdString.isNullOrEmpty()) {
                try {
                    finalCategoryId = categoryIdString.toInt()
                } catch (e: NumberFormatException) {
                    // Handle error if needed
                }
            }
        }
        
        if (finalCategoryId == -1 && categoryTitle == "Top Videos") {
            // Observe home page data for top videos
            viewModel.homepageLiveData.observe(this) { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is ApiResult.Success -> {
                        binding.progressBar.visibility = View.GONE
                        val topVideos = result.data?.dataObj?.topVideos ?: emptyList()
                        if (topVideos.isEmpty()) {
                            binding.noVideosText.visibility = View.VISIBLE
                            binding.videosRecyclerView.visibility = View.GONE
                        } else {
                            binding.noVideosText.visibility = View.GONE
                            binding.videosRecyclerView.visibility = View.VISIBLE
                            binding.videosRecyclerView.adapter = GridVideosListAdapter(topVideos)
                        }
                    }
                    is ApiResult.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, result.message?.toString() ?: "Unknown error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Observe category videos data
            viewModel.categoryVideosLiveData.observe(this) { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is ApiResult.Success -> {
                        binding.progressBar.visibility = View.GONE
                        val videos = result.data?.data?.videos ?: emptyList()
                        if (videos.isEmpty()) {
                            binding.noVideosText.visibility = View.VISIBLE
                            binding.videosRecyclerView.visibility = View.GONE
                        } else {
                            binding.noVideosText.visibility = View.GONE
                            binding.videosRecyclerView.visibility = View.VISIBLE
                            binding.videosRecyclerView.adapter = GridVideosListAdapter(videos)
                        }
                    }
                    is ApiResult.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, result.message?.toString() ?: "Unknown error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    override fun onBackPressed() {
        // Check if user came from notification
        val fromNotification = intent.getBooleanExtra("from_notification", false) ||
                              intent.getStringExtra("notification_type") != null ||
                              intent.getStringExtra("type") != null
        
        if (fromNotification) {
            // User came from notification - go to MainActivity and clear stack
            // Create a clean intent without notification flags
            val cleanIntent = Intent(this, MainActivity::class.java)
            cleanIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(cleanIntent)
            finish()
        } else {
            // Normal back navigation
            super.onBackPressed()
        }
    }
} 