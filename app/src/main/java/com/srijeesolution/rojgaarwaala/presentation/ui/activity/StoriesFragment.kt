package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.data.remote.model.CircleStory
import com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage
import com.srijeesolution.rojgaarwaala.data.remote.model.Story
import com.srijeesolution.rojgaarwaala.data.remote.model.TimeGroup
import com.srijeesolution.rojgaarwaala.data.remote.model.toCircleStory
import com.srijeesolution.rojgaarwaala.databinding.FragmentStoriesBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.StoriesCategoryAdapter
import com.srijeesolution.rojgaarwaala.presentation.adaptor.StoriesCircleAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.DeviceKeyUtils
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StoriesFragment : Fragment() {

    private var _binding: FragmentStoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomePageViewModel
    private var storiesAdapter: StoriesCategoryAdapter? = null
    private var circleAdapter: StoriesCircleAdapter? = null
    private var activeCircleStories: List<CircleStory> = emptyList()

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private var allTimeGroups: List<TimeGroup> = emptyList()
    private var isSearchMode = false

    private val storyViewerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deviceKey = DeviceKeyUtils.getOrCreateDeviceKey(sharedPrefs)
            viewModel.getActiveStories(deviceKey, forceRefresh = true)
        }
    }

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
        applyCachedStoriesIfAvailable()
        loadStories()
    }

    private fun applyCachedStoriesIfAvailable() {
        when (val grouped = viewModel.storiesLiveData.value) {
            is ApiResult.Success -> {
                val timeGroups = grouped.data?.data?.timeGroups
                if (grouped.data?.status == true && !timeGroups.isNullOrEmpty()) {
                    displayStories(timeGroups)
                }
            }
            else -> Unit
        }

        when (val active = viewModel.activeStoriesLiveData.value) {
            is ApiResult.Success -> {
                displayCircleStories(active.data?.data?.stories ?: emptyList())
            }
            else -> Unit
        }

        updateLoadingState()
    }

    private fun updateLoadingState() {
        val hasContent = allTimeGroups.isNotEmpty() || activeCircleStories.isNotEmpty()
        binding.storiesLoadingProgress.visibility =
            if (hasContent) View.GONE else View.VISIBLE
    }

    private fun setupViews() {
        binding.storiesRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.storiesRecyclerView.setHasFixedSize(true)
        binding.storiesRecyclerView.setItemViewCacheSize(20)

        storiesAdapter = StoriesCategoryAdapter(
            emptyList(),
            onStoryClick = { story -> onStoryClick(story) },
            onViewAllClick = { timeGroup -> onViewAllClick(timeGroup) }
        )
        binding.storiesRecyclerView.adapter = storiesAdapter

        binding.storyCirclesRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        circleAdapter = StoriesCircleAdapter(emptyList()) { story, index ->
            openCircleStoryViewer(index)
        }
        binding.storyCirclesRecyclerView.adapter = circleAdapter
    }

    private fun setupSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isEmpty()) {
                    isSearchMode = false
                    binding.clearSearchButton.visibility = View.GONE
                    showAllContent()
                } else {
                    isSearchMode = true
                    binding.clearSearchButton.visibility = View.VISIBLE
                    filterContent(query)
                }
            }
        })

        binding.clearSearchButton.setOnClickListener {
            binding.searchBar.setText("")
            binding.searchBar.clearFocus()
        }
    }

    private fun setupObservers() {
        viewModel.storiesLiveData.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is ApiResult.Loading -> updateLoadingState()
                is ApiResult.Success -> {
                    val data = result.data
                    if (data?.status == true && !data.data?.timeGroups.isNullOrEmpty()) {
                        displayStories(data.data?.timeGroups ?: emptyList())
                    } else if (activeCircleStories.isEmpty()) {
                        showEmptyState()
                    }
                    updateLoadingState()
                }
                is ApiResult.Error -> {
                    if (activeCircleStories.isEmpty()) {
                        showError("Failed to load stories")
                    }
                    updateLoadingState()
                }
            }
        })

        viewModel.activeStoriesLiveData.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is ApiResult.Success -> {
                    val stories = result.data?.data?.stories ?: emptyList()
                    displayCircleStories(stories)
                    updateLoadingState()
                }
                is ApiResult.Error -> {
                    if (activeCircleStories.isEmpty()) {
                        binding.storyCirclesRecyclerView.visibility = View.GONE
                    }
                    updateLoadingState()
                }
                is ApiResult.Loading -> updateLoadingState()
            }
        })
    }

    private fun loadStories() {
        val deviceKey = DeviceKeyUtils.getOrCreateDeviceKey(sharedPrefs)
        viewModel.getActiveStories(deviceKey, forceRefresh = true)
        viewModel.getSectionStoriesGrouped(forceRefresh = true)
    }

    private fun displayCircleStories(stories: List<CircleStory>) {
        activeCircleStories = stories
        if (stories.isEmpty()) {
            binding.storyCirclesRecyclerView.visibility = View.GONE
            return
        }

        binding.storyCirclesRecyclerView.visibility = View.VISIBLE
        circleAdapter?.updateStories(stories)
    }

    private fun openCircleStoryViewer(startIndex: Int) {
        val intent = Intent(context, StoryViewerActivity::class.java)
        intent.putParcelableArrayListExtra(
            StoryViewerActivity.EXTRA_STORIES,
            ArrayList(activeCircleStories)
        )
        intent.putExtra(StoryViewerActivity.EXTRA_START_INDEX, startIndex)
        storyViewerLauncher.launch(intent)
    }

    private fun filterContent(query: String) {
        val lowerQuery = query.lowercase()

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

        updateUIWithFilteredContent(filteredTimeGroups)
    }

    private fun showAllContent() {
        updateUIWithFilteredContent(allTimeGroups)
    }

    private fun updateUIWithFilteredContent(timeGroups: List<TimeGroup>) {
        val hasResults = timeGroups.isNotEmpty()

        if (isSearchMode && !hasResults) {
            binding.noResultsLayout.visibility = View.VISIBLE
            binding.searchResultsCount.visibility = View.GONE
            binding.storiesRecyclerView.visibility = View.GONE
            return
        } else {
            binding.noResultsLayout.visibility = View.GONE
        }

        if (isSearchMode) {
            val totalResults = timeGroups.sumOf { it.stories?.size ?: 0 }
            binding.searchResultsCount.text =
                "$totalResults result${if (totalResults != 1) "s" else ""} found"
            binding.searchResultsCount.visibility = View.VISIBLE
        } else {
            binding.searchResultsCount.visibility = View.GONE
        }

        if (timeGroups.isNotEmpty()) {
            binding.storiesRecyclerView.visibility = View.VISIBLE
            storiesAdapter = StoriesCategoryAdapter(
                timeGroups,
                onStoryClick = { story -> onStoryClick(story) },
                onViewAllClick = { timeGroup -> onViewAllClick(timeGroup) }
            )
            binding.storiesRecyclerView.adapter = storiesAdapter
        } else if (activeCircleStories.isEmpty()) {
            binding.storiesRecyclerView.visibility = View.GONE
        }
    }

    private fun displayStories(timeGroups: List<TimeGroup>) {
        val timeGroupsWithStories = timeGroups.filter { it.stories?.isNotEmpty() == true }
        allTimeGroups = timeGroupsWithStories

        if (timeGroupsWithStories.isNotEmpty()) {
            storiesAdapter = StoriesCategoryAdapter(
                timeGroupsWithStories,
                onStoryClick = { story -> onStoryClick(story) },
                onViewAllClick = { timeGroup -> onViewAllClick(timeGroup) }
            )
            binding.storiesRecyclerView.adapter = storiesAdapter
            binding.storiesRecyclerView.visibility = View.VISIBLE
        } else if (activeCircleStories.isEmpty()) {
            showEmptyState()
        }
    }

    private fun onStoryClick(story: Story) {
        when (story.mediaType) {
            "video", "link" -> {
                val intent = Intent(context, StoryViewerActivity::class.java)
                intent.putParcelableArrayListExtra(
                    StoryViewerActivity.EXTRA_STORIES,
                    arrayListOf(story.toCircleStory())
                )
                intent.putExtra(StoryViewerActivity.EXTRA_START_INDEX, 0)
                storyViewerLauncher.launch(intent)
            }
            else -> {
                if (!story.imageUrl.isNullOrEmpty()) {
                    val intent = Intent(context, ImageViewerActivity::class.java)
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
                    android.widget.Toast.makeText(
                        context,
                        "No image available for this story",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun onViewAllClick(timeGroup: TimeGroup) {
        val intent = Intent(context, StoriesListActivity::class.java)
        intent.putExtra("time_group_id", timeGroup.id)
        intent.putExtra("time_group_title", timeGroup.title)
        startActivity(intent)
    }

    private fun showEmptyState() {
        binding.storiesRecyclerView.visibility = View.GONE
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
