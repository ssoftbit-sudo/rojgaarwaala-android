package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.widget.Toast
import java.util.ArrayList
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
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.MainToolbarViewModel
import com.srijeesolution.rojgaarwaala.utils.HomeLocationDefaults
import com.srijeesolution.rojgaarwaala.utils.ImageLocationFilter
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ImagesFragment : Fragment() {

    private var _binding: FragmentImagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomePageViewModel
    private lateinit var mainToolbarViewModel: MainToolbarViewModel

    @Inject
    lateinit var sharedPrefs: SharedPrefs
    private var imagesAdapter: ImagesCategoryAdapter? = null
    
    // Search related variables
    private var allCategories: List<ImageSubItem> = emptyList()
    private var isSearchMode = false

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
        mainToolbarViewModel = ViewModelProvider(requireActivity())[MainToolbarViewModel::class.java]
        setupViews()
        setupSearch()
        observeMainToolbarLocation()
        setupObservers()
        loadImages()
    }

    private fun observeMainToolbarLocation() {
        mainToolbarViewModel.selectedLocation.observe(viewLifecycleOwner) {
            if (allCategories.isNotEmpty()) {
                applyFilters(binding.searchBar.text?.toString()?.trim().orEmpty())
            }
        }
    }

    private fun districtFilterQuery(): String {
        val loc = mainToolbarViewModel.selectedLocation.value.orEmpty().trim()
        return if (HomeLocationDefaults.skipsDistrictFilter(loc)) "" else loc
    }

    private fun applyFilters(textQuery: String) {
        val locationQuery = districtFilterQuery()
        if (textQuery.isEmpty() && locationQuery.isEmpty()) {
            isSearchMode = false
            showAllContent()
        } else {
            isSearchMode = true
            filterContent(textQuery, locationQuery)
        }
    }

    private fun setupViews() {
        // Setup RecyclerView with LinearLayoutManager for categories
        binding.imagesRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        
        // Optimize for smooth scrolling
        binding.imagesRecyclerView.setHasFixedSize(true)
        binding.imagesRecyclerView.setItemViewCacheSize(20)
        
        // Initialize adapter
        imagesAdapter = ImagesCategoryAdapter(
            emptyList(),
            onImageClick = { category, imageIndex -> onImageClick(category, imageIndex) },
            onViewAllClick = { category -> onViewAllClick(category) }
        )
        binding.imagesRecyclerView.adapter = imagesAdapter
    }

    private fun setupSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                binding.clearSearchButton.visibility =
                    if (query.isEmpty() && districtFilterQuery().isEmpty()) View.GONE else View.VISIBLE
                applyFilters(query)
            }
        })
        
        // Clear search button click listener
        binding.clearSearchButton.setOnClickListener {
            binding.searchBar.setText("")
            binding.searchBar.clearFocus()
        }
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

    private fun filterContent(query: String, locationQuery: String = "") {
        val lowerQuery = query.lowercase()
        val hasSearch = query.isNotEmpty()
        val hasLocation = locationQuery.isNotEmpty()

        val filteredCategories = allCategories.mapNotNull { category ->
            val filteredImages = category.images?.filter { image ->
                val searchOk = !hasSearch || image.title?.lowercase()?.contains(lowerQuery) == true ||
                    image.description?.lowercase()?.contains(lowerQuery) == true ||
                    category.title?.lowercase()?.contains(lowerQuery) == true
                val locationOk = !hasLocation || ImageLocationFilter.matches(image, locationQuery)
                searchOk && locationOk
            } ?: emptyList()

            if (filteredImages.isNotEmpty()) {
                category.copy(images = filteredImages)
            } else {
                null
            }
        }

        updateUIWithFilteredContent(filteredCategories)
    }

    private fun showAllContent() {
        updateUIWithFilteredContent(allCategories)
    }

    private fun updateUIWithFilteredContent(categories: List<ImageSubItem>) {
        // Check if we have any results
        val hasResults = categories.isNotEmpty()
        
        if (isSearchMode && !hasResults) {
            // Show no results message
            binding.noResultsLayout.visibility = View.VISIBLE
            binding.searchResultsCount.visibility = View.GONE
            binding.imagesRecyclerView.visibility = View.GONE
            return
        } else {
            binding.noResultsLayout.visibility = View.GONE
        }
        
        // Show search results count
        if (isSearchMode) {
            val totalResults = categories.sumOf { it.images?.size ?: 0 }
            binding.searchResultsCount.text = "$totalResults result${if (totalResults != 1) "s" else ""} found"
            binding.searchResultsCount.visibility = View.VISIBLE
        } else {
            binding.searchResultsCount.visibility = View.GONE
        }
        
        // Update RecyclerView with filtered categories
        if (categories.isNotEmpty()) {
            binding.imagesRecyclerView.visibility = View.VISIBLE
            imagesAdapter = ImagesCategoryAdapter(
                categories,
                onImageClick = { category, imageIndex -> onImageClick(category, imageIndex) },
                onViewAllClick = { category -> onViewAllClick(category) }
            )
            binding.imagesRecyclerView.adapter = imagesAdapter
        } else {
            binding.imagesRecyclerView.visibility = View.GONE
        }
    }

    private fun displayImages(categories: List<ImageSubItem>) {
        // Filter categories that have images
        val categoriesWithImages = categories.filter { it.images?.isNotEmpty() == true }
        
        allCategories = categoriesWithImages

        if (categoriesWithImages.isEmpty()) {
            showEmptyState()
            return
        }

        applyFilters(binding.searchBar.text?.toString()?.trim().orEmpty())
    }

    private fun showEmptyState() {
        // Handle empty state
        binding.imagesRecyclerView.visibility = View.GONE
        // You can add a TextView to show "No images found" message
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun onImageClick(category: ImageSubItem, imageIndex: Int) {
        // Launch full-screen image viewer with all images from the category
        val images = category.images ?: emptyList()
        if (images.isNotEmpty() && imageIndex >= 0 && imageIndex < images.size) {
            val intent = Intent(context, ImageViewerActivity::class.java)
            
            // Convert all ImageData to ScheduledImage list
            val scheduledImages = images.map { imageData ->
                ScheduledImage(
                    id = imageData.id,
                    title = imageData.title,
                    description = imageData.description,
                    imagePath = imageData.imageUrl,
                    location = imageData.location,
                    publishDate = imageData.publishDate,
                    status = null,
                    createdAt = imageData.createdAt,
                    updatedAt = null,
                    phoneNumber = imageData.phoneNumber
                )
            }
            
            // Pass the list and current index
            intent.putParcelableArrayListExtra("scheduled_images", ArrayList(scheduledImages))
            intent.putExtra("current_index", imageIndex)
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_CATEGORY, category.title)
            startActivity(intent)
        }
    }

    private fun onViewAllClick(category: ImageSubItem) {
        // Launch ImagesListActivity to show all images from this specific category
        val intent = Intent(context, ImagesListActivity::class.java)
        intent.putExtra("category_id", category.id)
        intent.putExtra("category_title", category.title)
        intent.putExtra("filter_location", districtFilterQuery())
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 