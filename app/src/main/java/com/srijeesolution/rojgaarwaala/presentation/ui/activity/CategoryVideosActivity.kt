package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo
import com.srijeesolution.rojgaarwaala.databinding.ActivityCategoryVideosBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.GridVideosListAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.VideoListUtils
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
    private var categoryId = -1
    private var currentPage = 1
    private var hasMorePages = false
    private var isLoadingPage = false
    private val loadedVideos = mutableListOf<TopVideo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryVideosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        categoryId = resolveCategoryId()
        val categoryTitle = intent.getStringExtra("category_title") ?: ""
        val categoryIcon = intent.getStringExtra("category_icon")

        binding.categoryTitle.text = categoryTitle
        if (!categoryIcon.isNullOrEmpty() && categoryIcon != "null") {
            Glide.with(this).load(categoryIcon).into(binding.categoryIcon)
        }

        binding.videosRecyclerView.layoutManager = GridLayoutManager(this, 2)
        videosAdapter = GridVideosListAdapter()
        binding.videosRecyclerView.adapter = videosAdapter
        binding.videosRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0 || isLoadingPage || !hasMorePages) return
                val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val totalItems = layoutManager.itemCount
                if (lastVisible >= totalItems - 4) {
                    loadVideos(currentPage + 1, append = true)
                }
            }
        })

        isTopVideosScreen = categoryId == -1 && categoryTitle == "Top Videos"
        observeData()
        loadVideos(page = 1, append = false)
    }

    private fun resolveCategoryId(): Int {
        var finalCategoryId = intent.getIntExtra("category_id", -1)
        if (finalCategoryId == -1) {
            val categoryIdString = intent.getStringExtra("category_id")
            if (!categoryIdString.isNullOrEmpty()) {
                finalCategoryId = categoryIdString.toIntOrNull() ?: -1
            }
        }
        return finalCategoryId
    }

    private fun loadVideos(page: Int, append: Boolean) {
        if (isLoadingPage) return
        isLoadingPage = true
        currentPage = page
        if (!append) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.loadMoreProgress.visibility = View.VISIBLE
        }

        if (isTopVideosScreen) {
            viewModel.getTopVideos(page)
        } else if (categoryId != -1) {
            viewModel.getCategoryVideos(categoryId, page)
        } else {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun observeData() {
        if (isTopVideosScreen) {
            viewModel.topVideosLiveData.observe(this) { result ->
                handlePaginatedResult(
                    videos = (result as? ApiResult.Success)?.data?.data?.topVideos,
                    paginationHasMore = (result as? ApiResult.Success)?.data?.data?.pagination?.hasMore,
                    result = result,
                )
            }
        } else {
            viewModel.categoryVideosLiveData.observe(this) { result ->
                val success = result as? ApiResult.Success
                val videos = success?.data?.data?.videos
                val filtered = VideoLocationFilter.filterVideos(
                    videos ?: emptyList(),
                    sharedPrefs.getPrefs(SharedPrefsConstant.HOME_SELECTED_LOCATION, "").orEmpty().trim(),
                )
                handlePaginatedResult(
                    videos = filtered,
                    paginationHasMore = success?.data?.data?.pagination?.hasMore,
                    result = result,
                )
            }
        }
    }

    private fun handlePaginatedResult(
        videos: List<TopVideo>?,
        paginationHasMore: Boolean?,
        result: ApiResult<*>,
    ) {
        when (result) {
            is ApiResult.Loading -> Unit
            is ApiResult.Success -> {
                isLoadingPage = false
                binding.progressBar.visibility = View.GONE
                binding.loadMoreProgress.visibility = View.GONE

                val pageVideos = VideoListUtils.orderVideos(videos ?: emptyList())
                if (currentPage == 1) {
                    loadedVideos.clear()
                }
                loadedVideos.addAll(pageVideos)
                hasMorePages = paginationHasMore == true

                if (loadedVideos.isEmpty()) {
                    binding.noVideosText.visibility = View.VISIBLE
                    binding.videosRecyclerView.visibility = View.GONE
                } else {
                    binding.noVideosText.visibility = View.GONE
                    binding.videosRecyclerView.visibility = View.VISIBLE
                    videosAdapter.submitList(loadedVideos.toList())
                }
            }
            is ApiResult.Error -> {
                isLoadingPage = false
                binding.progressBar.visibility = View.GONE
                binding.loadMoreProgress.visibility = View.GONE
                if (loadedVideos.isEmpty()) {
                    binding.noVideosText.visibility = View.VISIBLE
                    binding.videosRecyclerView.visibility = View.GONE
                }
                Toast.makeText(this, result.message?.errorMsg ?: "Failed to load videos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        val fromNotification = intent.getBooleanExtra("from_notification", false) ||
            intent.getStringExtra("notification_type") != null ||
            intent.getStringExtra("type") != null

        if (fromNotification) {
            val cleanIntent = Intent(this, MainActivity::class.java)
            cleanIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(cleanIntent)
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
