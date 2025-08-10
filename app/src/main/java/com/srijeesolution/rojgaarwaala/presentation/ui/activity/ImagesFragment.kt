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
import com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage
import com.srijeesolution.rojgaarwaala.databinding.FragmentImagesBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.ImagesGridAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel

class ImagesFragment : Fragment() {

    private var _binding: FragmentImagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomePageViewModel
    private var imagesAdapter: ImagesGridAdapter? = null

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
        // Setup RecyclerView with GridLayoutManager (3 columns)
        binding.imagesRecyclerView.layoutManager = GridLayoutManager(context, 3)
        
        // Initialize adapter
        imagesAdapter = ImagesGridAdapter(emptyList()) { image ->
            onImageClick(image)
        }
        binding.imagesRecyclerView.adapter = imagesAdapter
    }

    private fun setupObservers() {
        viewModel.scheduledImagesLiveData.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is ApiResult.Loading -> {
                    // Show loading state if needed
                }
                is ApiResult.Success -> {
                    val data = result.data
                    if (data?.success == true && !data.data.isNullOrEmpty()) {
                        displayImages(data.data)
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
        viewModel.getScheduledImagesGrouped()
    }

    private fun displayImages(categories: List<ImageCategory>) {
        // For now, we'll display all images from all categories in a single grid
        // You can modify this to show categories separately if needed
        val allImages = mutableListOf<ScheduledImage>()
        categories.forEach { category ->
            category.images?.let { images ->
                allImages.addAll(images)
            }
        }
        
        if (allImages.isNotEmpty()) {
            imagesAdapter = ImagesGridAdapter(allImages) { image ->
                onImageClick(image)
            }
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

    private fun onImageClick(image: ScheduledImage) {
        // Launch full-screen image viewer
        val intent = Intent(context, ImageViewerActivity::class.java)
        intent.putExtra("scheduled_image", image)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 