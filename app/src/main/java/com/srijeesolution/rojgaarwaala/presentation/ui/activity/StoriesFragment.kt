package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage
import com.srijeesolution.rojgaarwaala.data.remote.model.StoriesResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.TimeGroup
import com.srijeesolution.rojgaarwaala.databinding.FragmentStoriesBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.StoriesCategoryAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoriesFragment : Fragment() {

    private var _binding: FragmentStoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomePageViewModel
    private var storiesAdapter: StoriesCategoryAdapter? = null
    
    // Search related variables
    private var allTimeGroups: List<TimeGroup> = emptyList()
    private var isSearchMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[HomePageViewModel::class.java]
        
        setupViews()
        setupSearch()
        setupObservers()
        loadStories()
    }

    private fun setupViews() {
        // Setup RecyclerView with LinearLayoutManager for time groups
        binding.storiesRecyclerView.layoutManager = LinearLayoutManager(context)
        
        // Optimize for smooth scrolling
        binding.storiesRecyclerView.setHasFixedSize(true)
        binding.storiesRecyclerView.setItemViewCacheSize(20)
        
        // Initialize adapter
        storiesAdapter = StoriesCategoryAdapter(
            emptyList(),
            onStoryClick = { story -> onStoryClick(story) },
            onViewAllClick = { timeGroup -> onViewAllClick(timeGroup) }
        )
        binding.storiesRecyclerView.adapter = storiesAdapter
    }

    private fun setupSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isEmpty()) {
                    // Show all content when search is empty
                    isSearchMode = false
                    binding.clearSearchButton.visibility = View.GONE
                    showAllContent()
                } else {
                    // Filter content based on search query
                    isSearchMode = true
                    binding.clearSearchButton.visibility = View.VISIBLE
                    filterContent(query)
                }
            }
        })
        
        // Clear search button click listener
        binding.clearSearchButton.setOnClickListener {
            binding.searchBar.setText("")
            binding.searchBar.clearFocus()
        }
    }

    private fun setupObservers() {
        viewModel.storiesLiveData.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is ApiResult.Loading -> {
                    // Show loading state if needed
                }
                is ApiResult.Success -> {
                    val data = result.data
                    if (data?.status == true && !data.data?.timeGroups.isNullOrEmpty()) {
                        displayStories(data.data?.timeGroups ?: emptyList())
                    } else {
                        showEmptyState()
                    }
                }
                is ApiResult.Error -> {
                    showError("Failed to load stories")
                }
            }
        })
    }

    private fun loadStories() {
        viewModel.getSectionStoriesGrouped()
    }

    private fun filterContent(query: String) {
        val lowerQuery = query.lowercase()
        
        // Filter time groups and their stories
        val filteredTimeGroups = allTimeGroups.mapNotNull { timeGroup ->
            val filteredStories = timeGroup.stories?.filter { story ->
                story.title?.lowercase()?.contains(lowerQuery) == true ||
                story.description?.lowercase()?.contains(lowerQuery) == true ||
                timeGroup.title?.lowercase()?.contains(lowerQuery) == true
            } ?: emptyList()
            
            if (filteredStories.isNotEmpty()) {
                timeGroup.copy(stories = filteredStories)
            } else null
        }
        
        // Update UI with filtered results
        updateUIWithFilteredContent(filteredTimeGroups)
    }

    private fun showAllContent() {
        updateUIWithFilteredContent(allTimeGroups)
    }

    private fun updateUIWithFilteredContent(timeGroups: List<TimeGroup>) {
        // Check if we have any results
        val hasResults = timeGroups.isNotEmpty()
        
        if (isSearchMode && !hasResults) {
            // Show no results message
            binding.noResultsLayout.visibility = View.VISIBLE
            binding.searchResultsCount.visibility = View.GONE
            binding.storiesRecyclerView.visibility = View.GONE
            return
        } else {
            binding.noResultsLayout.visibility = View.GONE
        }
        
        // Show search results count
        if (isSearchMode) {
            val totalResults = timeGroups.sumOf { it.stories?.size ?: 0 }
            binding.searchResultsCount.text = "$totalResults result${if (totalResults != 1) "s" else ""} found"
            binding.searchResultsCount.visibility = View.VISIBLE
        } else {
            binding.searchResultsCount.visibility = View.GONE
        }
        
        // Update RecyclerView with filtered time groups
        if (timeGroups.isNotEmpty()) {
            binding.storiesRecyclerView.visibility = View.VISIBLE
            storiesAdapter = StoriesCategoryAdapter(
                timeGroups,
                onStoryClick = { story -> onStoryClick(story) },
                onViewAllClick = { timeGroup -> onViewAllClick(timeGroup) }
            )
            binding.storiesRecyclerView.adapter = storiesAdapter
        } else {
            binding.storiesRecyclerView.visibility = View.GONE
        }
    }

    private fun displayStories(timeGroups: List<TimeGroup>) {
        // Filter time groups that have stories
        val timeGroupsWithStories = timeGroups.filter { it.stories?.isNotEmpty() == true }
        
        // Store all time groups for search functionality
        allTimeGroups = timeGroupsWithStories
        
        if (timeGroupsWithStories.isNotEmpty()) {
            storiesAdapter = StoriesCategoryAdapter(
                timeGroupsWithStories,
                onStoryClick = { story -> onStoryClick(story) },
                onViewAllClick = { timeGroup -> onViewAllClick(timeGroup) }
            )
            binding.storiesRecyclerView.adapter = storiesAdapter
        } else {
            showEmptyState()
        }
    }

    private fun onStoryClick(story: com.srijeesolution.rojgaarwaala.data.remote.model.Story) {
        // Launch ImageViewerActivity to show the story image
        if (!story.imageUrl.isNullOrEmpty()) {
            val intent = Intent(context, ImageViewerActivity::class.java)
            // Convert Story to ScheduledImage for compatibility with ImageViewerActivity
            val scheduledImage = ScheduledImage(
                id = story.id,
                title = story.title,
                description = story.description,
                imagePath = story.imageUrl,
                publishDate = story.publishDate,
                status = null,
                createdAt = story.createdAt,
                updatedAt = null
            )
            intent.putExtra("scheduled_image", scheduledImage)
            startActivity(intent)
        } else {
            // Show toast if no image available
            android.widget.Toast.makeText(context, "No image available for this story", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun onViewAllClick(timeGroup: TimeGroup) {
        // Launch StoriesListActivity to show all stories from this time group
        val intent = Intent(context, StoriesListActivity::class.java)
        intent.putExtra("time_group_id", timeGroup.id)
        intent.putExtra("time_group_title", timeGroup.title)
        startActivity(intent)
    }

    private fun showEmptyState() {
        // Handle empty state
        binding.storiesRecyclerView.visibility = View.GONE
        // You can add a TextView to show "No stories found" message
    }

    private fun showError(message: String) {
        // Handle error state
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 