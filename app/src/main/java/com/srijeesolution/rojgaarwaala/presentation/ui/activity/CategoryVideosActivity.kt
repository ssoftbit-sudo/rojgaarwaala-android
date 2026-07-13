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
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo
import com.srijeesolution.rojgaarwaala.databinding.ActivityCategoryVideosBinding
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.GridVideosListAdapter
import com.srijeesolution.rojgaarwaala.utils.VideoLocationFilter
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CategoryVideosActivity : ComponentActivity() {
    private lateinit var binding: ActivityCategoryVideosBinding
    private val viewModel: HomePageViewModel by viewModels()
    @Inject lateinit var sharedPrefs: SharedPrefs
    private lateinit var videosAdapter: GridVideosListAdapter
    private var isTopVideosScreen = false

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
        videosAdapter = GridVideosListAdapter()
        binding.videosRecyclerView.adapter = videosAdapter

        isTopVideosScreen = finalCategoryId == -1 && categoryTitle == "Top Videos"

        if (isTopVideosScreen) {
            viewModel.getTopVideos()
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
            viewModel.topVideosLiveData.observe(this) { result ->
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
                            videosAdapter.submitList(orderVideos(topVideos))
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
                        val location = sharedPrefs.getPrefs(
                            SharedPrefsConstant.HOME_SELECTED_LOCATION,
                            "",
                        ).orEmpty().trim()
                        val videos = VideoLocationFilter.filterVideos(
                            result.data?.data?.videos ?: emptyList(),
                            location,
                        )
                        if (videos.isEmpty()) {
                            binding.noVideosText.visibility = View.VISIBLE
                            binding.videosRecyclerView.visibility = View.GONE
                        } else {
                            binding.noVideosText.visibility = View.GONE
                            binding.videosRecyclerView.visibility = View.VISIBLE
                            videosAdapter.submitList(orderVideos(videos))
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

    private fun orderVideos(videos: List<TopVideo>): List<TopVideo> {
        return videos.sortedWith(
            compareBy<TopVideo> { it.sortOrder ?: Int.MAX_VALUE }
                .thenByDescending { it.createdAt.orEmpty() }
        )
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