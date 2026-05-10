package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage
import com.srijeesolution.rojgaarwaala.data.remote.model.StoriesResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.Story
import com.srijeesolution.rojgaarwaala.databinding.ActivityStoriesListBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.StoriesGridAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.SpaceItemDecoration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoriesListActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityStoriesListBinding
    private lateinit var viewModel: HomePageViewModel
    private var storiesAdapter: StoriesGridAdapter? = null
    private var timeGroupId: Int? = null
    private var timeGroupTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoriesListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get time group info from intent
        timeGroupId = intent.getIntExtra("time_group_id", -1)
        timeGroupTitle = intent.getStringExtra("time_group_title")
        
        viewModel = ViewModelProvider(this)[HomePageViewModel::class.java]
        
        setupViews()
        setupObservers()
        loadStories()
    }

    private fun setupViews() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = if (timeGroupTitle != null) "$timeGroupTitle Stories" else "All Stories"
        }
        
        // Setup RecyclerView with GridLayoutManager (2 columns for stories)
        binding.storiesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        
        // Add spacing decoration similar to top videos list
        binding.storiesRecyclerView.addItemDecoration(SpaceItemDecoration(8, 8))
        
        // Initialize adapter
        storiesAdapter = StoriesGridAdapter(emptyList()) { story ->
            onStoryClick(story)
        }
        binding.storiesRecyclerView.adapter = storiesAdapter
    }

    private fun setupObservers() {
        viewModel.storiesLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    // Show loading state if needed
                }
                is ApiResult.Success -> {
                    val data = result.data
                    if (data?.status == true && !data.data?.timeGroups.isNullOrEmpty()) {
                        displayAllStories(data.data?.timeGroups ?: emptyList())
                    } else {
                        showEmptyState()
                    }
                }
                is ApiResult.Error -> {
                    showError("Failed to load stories")
                }
            }
        }
    }

    private fun loadStories() {
        viewModel.getSectionStoriesGrouped()
    }

    private fun displayAllStories(timeGroups: List<com.srijeesolution.rojgaarwaala.data.remote.model.TimeGroup>) {
        val allStories = mutableListOf<Story>()
        
        if (timeGroupId != null && timeGroupId != -1) {
            // Show stories from specific time group only
            val specificTimeGroup = timeGroups.find { it.id == timeGroupId }
            specificTimeGroup?.stories?.let { stories ->
                allStories.addAll(stories)
            }
        } else {
            // Show all stories from all time groups
            timeGroups.forEach { timeGroup ->
                timeGroup.stories?.let { stories ->
                    allStories.addAll(stories)
                }
            }
        }
        
        if (allStories.isNotEmpty()) {
            storiesAdapter = StoriesGridAdapter(allStories) { story ->
                onStoryClick(story)
            }
            binding.storiesRecyclerView.adapter = storiesAdapter
        } else {
            showEmptyState()
        }
    }

    private fun onStoryClick(story: Story) {
        // Launch ImageViewerActivity to show the story image
        if (!story.imageUrl.isNullOrEmpty()) {
            val intent = android.content.Intent(this, ImageViewerActivity::class.java)
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
            android.widget.Toast.makeText(this, "No image available for this story", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEmptyState() {
        // Handle empty state
        binding.storiesRecyclerView.visibility = android.view.View.GONE
        // You can add a TextView to show "No stories found" message
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