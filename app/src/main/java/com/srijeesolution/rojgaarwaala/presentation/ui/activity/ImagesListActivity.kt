package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage
import com.srijeesolution.rojgaarwaala.databinding.ActivityImagesListBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.ImagesGridAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.SpaceItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList

@AndroidEntryPoint
class ImagesListActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityImagesListBinding
    private lateinit var viewModel: HomePageViewModel
    private var imagesAdapter: ImagesGridAdapter? = null
    private var categoryId: Int? = null
    private var categoryTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagesListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get category info from intent
        categoryId = intent.getIntExtra("category_id", -1)
        categoryTitle = intent.getStringExtra("category_title")
        
        viewModel = ViewModelProvider(this)[HomePageViewModel::class.java]
        
        setupViews()
        setupObservers()
        loadImages()
    }

    private fun setupViews() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = if (categoryTitle != null) "$categoryTitle Images" else "All Images"
        }
        
        // Setup RecyclerView with GridLayoutManager (2 columns like stories)
        binding.imagesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        
        // Add spacing decoration similar to stories
        binding.imagesRecyclerView.addItemDecoration(SpaceItemDecoration(8, 8))
        
        // Optimize for smooth scrolling
        binding.imagesRecyclerView.setHasFixedSize(true)
        binding.imagesRecyclerView.setItemViewCacheSize(20)
        
        // Initialize adapter
        imagesAdapter = ImagesGridAdapter(emptyList()) { image, imageIndex ->
            onImageClick(image, imageIndex)
        }
        binding.imagesRecyclerView.adapter = imagesAdapter
    }

    private fun setupObservers() {
        viewModel.imageListLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    // Show loading state if needed
                }
                is ApiResult.Success -> {
                    val data = result.data
                    if (data?.status == true && !data.data?.categoryImages.isNullOrEmpty()) {
                        displayAllImages(data.data?.categoryImages ?: emptyList())
                    } else {
                        showEmptyState()
                    }
                }
                is ApiResult.Error -> {
                    showError("Failed to load images")
                }
            }
        }
    }

    private fun loadImages() {
        viewModel.getScheduledImages()
    }

    private fun displayAllImages(categories: List<com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem>) {
        val allImages = mutableListOf<ImageData>()
        
        if (categoryId != null && categoryId != -1) {
            // Show images from specific category only
            val specificCategory = categories.find { it.id == categoryId }
            specificCategory?.images?.let { images ->
                allImages.addAll(images)
            }
        } else {
            // Show all images from all categories
            categories.forEach { category ->
                category.images?.let { images ->
                    allImages.addAll(images)
                }
            }
        }
        
        // Store the list for navigation
        allImagesList = allImages
        
        if (allImages.isNotEmpty()) {
            imagesAdapter = ImagesGridAdapter(allImages) { image, imageIndex ->
                onImageClick(image, imageIndex)
            }
            binding.imagesRecyclerView.adapter = imagesAdapter
        } else {
            showEmptyState()
        }
    }

    private var allImagesList: List<ImageData> = emptyList()
    
    private fun onImageClick(image: ImageData, imageIndex: Int) {
        // Launch full-screen image viewer with all images from the category
        if (!image.imageUrl.isNullOrEmpty() && imageIndex >= 0 && imageIndex < allImagesList.size) {
            val intent = android.content.Intent(this, ImageViewerActivity::class.java)
            
            // Convert all ImageData to ScheduledImage list
            val scheduledImages = allImagesList.map { imageData ->
                ScheduledImage(
                    id = imageData.id,
                    title = imageData.title,
                    description = imageData.description,
                    imagePath = imageData.imageUrl,
                    publishDate = imageData.publishDate,
                    status = null,
                    createdAt = null,
                    updatedAt = null
                )
            }
            
            // Pass the list and current index
            intent.putParcelableArrayListExtra("scheduled_images", ArrayList(scheduledImages))
            intent.putExtra("current_index", imageIndex)
            startActivity(intent)
        } else {
            // Show toast if no image available
            android.widget.Toast.makeText(this, "No image available", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEmptyState() {
        // Handle empty state
        binding.imagesRecyclerView.visibility = android.view.View.GONE
        // You can add a TextView to show "No images found" message
    }

    private fun showError(message: String) {
        // Handle error state
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 