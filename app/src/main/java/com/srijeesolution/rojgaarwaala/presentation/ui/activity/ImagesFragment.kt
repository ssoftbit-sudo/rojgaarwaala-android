package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageCategory
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage
import com.srijeesolution.rojgaarwaala.databinding.FragmentImagesBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.ImagesCategoryAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel

class ImagesFragment : Fragment() {

    private var _binding: FragmentImagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomePageViewModel
    private var imagesAdapter: ImagesCategoryAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[HomePageViewModel::class.java]
        setupViews()
        setupObservers()
        loadImages()
    }

    private fun setupViews() {
        // Setup RecyclerView with LinearLayoutManager for categories
        binding.imagesRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        
        // Initialize adapter
        imagesAdapter = ImagesCategoryAdapter(
            emptyList(),
            onImageClick = { category -> onImageClick(category) },
            onViewAllClick = { category -> onViewAllClick(category) }
        )
        binding.imagesRecyclerView.adapter = imagesAdapter
    }

    private fun setupObservers() {
        viewModel.imageListLiveData.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is ApiResult.Loading -> {
                    // Show loading state if needed
                }
                is ApiResult.Success -> {
                    val data = result.data
                    if (data?.status == true && !data.data?.categoryImages.isNullOrEmpty()) {
                        displayImages(data.data?.categoryImages ?: emptyList())
                    } else {
                        showEmptyState()
                    }
                }
                is ApiResult.Error -> {
                    showError("Failed to load images")
                }
            }
        })
    }

    private fun loadImages() {
        viewModel.getScheduledImages()
    }

    private fun displayImages(categories: List<ImageSubItem>) {
        // Filter categories that have images
        val categoriesWithImages = categories.filter { it.images?.isNotEmpty() == true }
        
        if (categoriesWithImages.isNotEmpty()) {
            imagesAdapter = ImagesCategoryAdapter(
                categoriesWithImages,
                onImageClick = { category -> onImageClick(category) },
                onViewAllClick = { category -> onViewAllClick(category) }
            )
            binding.imagesRecyclerView.adapter = imagesAdapter
        } else {
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        // You can customize this to show a proper empty state
        Toast.makeText(context, "No images available", Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun onImageClick(category: ImageSubItem) {
        // Launch full-screen image viewer with the first image from the category
        val firstImage = category.images?.firstOrNull()
        if (firstImage != null) {
            val intent = Intent(context, ImageViewerActivity::class.java)
            // Convert Images to ScheduledImage for compatibility
            val scheduledImage = ScheduledImage(
                id = firstImage.id,
                title = firstImage.title,
                description = firstImage.description,
                imagePath = firstImage.imageUrl,
                publishDate = firstImage.publishDate,
                status = null,
                createdAt = null,
                updatedAt = null
            )
            intent.putExtra("scheduled_image", scheduledImage)
            startActivity(intent)
        }
    }

    private fun onViewAllClick(category: ImageSubItem) {
        // Launch ImagesListActivity to show all images from this specific category
        val intent = Intent(context, ImagesListActivity::class.java)
        intent.putExtra("category_id", category.id)
        intent.putExtra("category_title", category.title)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 