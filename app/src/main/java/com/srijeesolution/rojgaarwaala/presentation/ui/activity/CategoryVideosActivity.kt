package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
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

        binding.categoryTitle.text = categoryTitle
        if (!categoryIcon.isNullOrEmpty()) {
            Glide.with(this).load(categoryIcon).into(binding.categoryIcon)
        }

        binding.videosRecyclerView.layoutManager = GridLayoutManager(this, 2)

        if (categoryId != -1) {
            viewModel.getCategoryVideos(categoryId)
        } else {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
        }
        observeData()
    }

    private fun observeData() {
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
                        binding.videosRecyclerView.adapter = TopVideosAdapter(videos)
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