package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.FragmentHomeBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.ImagePagerAdapter
import com.srijeesolution.rojgaarwaala.data.remote.model.BannerList
import com.srijeesolution.rojgaarwaala.data.remote.model.Category
import com.srijeesolution.rojgaarwaala.data.remote.model.CategoryVideo
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.MainToolbarViewModel
import com.srijeesolution.rojgaarwaala.presentation.adaptor.TopVideosAdapter
import com.srijeesolution.rojgaarwaala.presentation.adaptor.VideoAdapter
import com.srijeesolution.rojgaarwaala.presentation.adaptor.CategoryGridAdapter
import dagger.hilt.android.AndroidEntryPoint
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import android.os.Handler
import android.os.Looper
import androidx.viewpager2.widget.ViewPager2
import com.srijeesolution.rojgaarwaala.utils.VideoCacheManager
import com.srijeesolution.rojgaarwaala.utils.HomeLocationDefaults
import com.srijeesolution.rojgaarwaala.utils.VideoListUtils
import com.srijeesolution.rojgaarwaala.utils.VideoLocationFilter
import android.util.Log

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private lateinit var homePageViewModel: HomePageViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var bannerHandler: Handler? = null
    private var bannerRunnable: Runnable? = null
    private var bannerList: List<BannerList> = emptyList()
    private var currentBannerPage = 0
    
    // Search related variables
    private var allTopVideos: List<TopVideo> = emptyList()
    private var allCategoryList: List<Category> = emptyList()
    private var allCategoryVideos: List<CategoryVideo> = emptyList()
    private var isSearchMode = false
    private var isEmployee = false
    private var topVideosHasMore = false
    private lateinit var topVideosAdapter: TopVideosAdapter

    private lateinit var mainToolbarViewModel: MainToolbarViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]
        mainToolbarViewModel = ViewModelProvider(requireActivity())[MainToolbarViewModel::class.java]

        topVideosAdapter = TopVideosAdapter(onViewMoreClick = { openTopVideosList() })
        observeMainToolbarFilters()
        
        // Set up View All click listeners
        setupViewAllClickListeners()
        
        observeHomePageData()
        callApi()
    }

    private fun observeMainToolbarFilters() {
        mainToolbarViewModel.searchQuery.observe(viewLifecycleOwner) {
            if (hasHomeListData()) applyFromToolbarState()
        }
        mainToolbarViewModel.selectedLocation.observe(viewLifecycleOwner) {
            if (hasHomeListData()) applyFromToolbarState()
        }
    }

    private fun hasHomeListData(): Boolean =
        allCategoryList.isNotEmpty() || allTopVideos.isNotEmpty() || allCategoryVideos.isNotEmpty()

    private fun districtFilterQuery(): String {
        val loc = mainToolbarViewModel.selectedLocation.value.orEmpty().trim()
        return if (HomeLocationDefaults.skipsDistrictFilter(loc)) "" else loc
    }

    private fun hasActiveTextSearch(): Boolean =
        mainToolbarViewModel.searchQuery.value.orEmpty().trim().isNotEmpty()

    private fun applyFromToolbarState() {
        val q = mainToolbarViewModel.searchQuery.value.orEmpty().trim()
        val loc = districtFilterQuery()
        if (q.isEmpty() && loc.isEmpty()) {
            isSearchMode = false
            showAllContent()
        } else {
            isSearchMode = true
            filterContent(q, loc)
        }
    }

    private fun setupViewAllClickListeners() {
        binding.topVideosViewAll.setOnClickListener { openTopVideosList() }
        binding.employeeAttendanceCard.setOnClickListener {
            startActivity(Intent(requireContext(), AttendanceDashboardActivity::class.java))
        }
    }

    private fun openTopVideosList() {
        val intent = Intent(requireContext(), CategoryVideosActivity::class.java)
        intent.putExtra("category_id", -1)
        intent.putExtra("category_title", "Top Videos")
        intent.putExtra("category_icon", "null")
        startActivity(intent)
    }

    private fun openCategoryVideosList(category: CategoryVideo) {
        val intent = Intent(requireContext(), CategoryVideosActivity::class.java)
        intent.putExtra("category_id", category.id ?: -1)
        intent.putExtra("category_title", category.title)
        intent.putExtra("category_icon", category.iconFile)
        startActivity(intent)
    }

    private fun filterContent(query: String, locationQuery: String = "") {
        val lowerQuery = query.lowercase()
        val hasSearch = query.isNotEmpty()
        val hasLocation = locationQuery.isNotEmpty()

        // Top videos: never filter by toolbar location; search text still applies
        val filteredTopVideos = if (hasSearch) {
            allTopVideos.filter { matchesSearch(it, lowerQuery) }
        } else {
            allTopVideos
        }

        // Category rows: filter by location (and optional search)
        val filteredCategoryVideos = allCategoryVideos.mapNotNull { categoryVideo ->
            val filteredVideos = categoryVideo.videos?.filter { video ->
                val searchOk = !hasSearch || matchesSearch(video, lowerQuery, categoryVideo.title)
                val locationOk = !hasLocation || VideoLocationFilter.matches(video, locationQuery)
                searchOk && locationOk
            } ?: emptyList()

            if (filteredVideos.isNotEmpty()) {
                categoryVideo.copy(videos = ArrayList(filteredVideos))
            } else {
                null
            }
        }

        val filteredCategories = when {
            hasLocation && !hasSearch -> {
                val idsWithVideos = filteredCategoryVideos.mapNotNull { it.id }.toSet()
                allCategoryList.filter { it.id in idsWithVideos }
            }
            hasSearch -> {
                allCategoryList.filter { category ->
                    category.title?.lowercase()?.contains(lowerQuery) == true ||
                        category.id in filteredCategoryVideos.mapNotNull { it.id }.toSet()
                }
            }
            else -> allCategoryList
        }

        updateUIWithFilteredContent(filteredCategories, filteredTopVideos, filteredCategoryVideos)
    }

    private fun matchesSearch(video: TopVideo, lowerQuery: String, categoryTitle: String? = null): Boolean {
        return video.title?.lowercase()?.contains(lowerQuery) == true ||
            video.description?.lowercase()?.contains(lowerQuery) == true ||
            video.user?.name?.lowercase()?.contains(lowerQuery) == true ||
            categoryTitle?.lowercase()?.contains(lowerQuery) == true
    }

    private fun showAllContent() {
        updateUIWithFilteredContent(allCategoryList, allTopVideos, allCategoryVideos)
    }

    private fun updateUIWithFilteredContent(
        categories: List<Category>,
        topVideos: List<TopVideo>,
        categoryVideos: List<CategoryVideo>
    ) {
        // Attendance entry point is only for linked employees, and is irrelevant while searching
        binding.employeeAttendanceCard.visibility =
            if (isEmployee && !isSearchMode) View.VISIBLE else View.GONE

        // Check if we have any results
        val hasResults = categories.isNotEmpty() || topVideos.isNotEmpty() || categoryVideos.isNotEmpty()
        
        if (isSearchMode && !hasResults) {
            // Show no results message
            binding.noResultsLayout.visibility = View.VISIBLE
            binding.searchResultsCount.visibility = View.GONE
            binding.topVideosLabel.visibility = View.GONE
            binding.topVideosRecyclerView.visibility = View.GONE
            binding.categoryGridRecyclerView.visibility = View.GONE
            binding.categorySectionsContainer.visibility = View.GONE
            return
        } else {
            binding.noResultsLayout.visibility = View.GONE
        }
        
        // Show search results count
        if (isSearchMode) {
            val totalResults = categories.size + topVideos.size + categoryVideos.sumOf { it.videos?.size ?: 0 }
            binding.searchResultsCount.text = "$totalResults result${if (totalResults != 1) "s" else ""} found"
            binding.searchResultsCount.visibility = View.VISIBLE
        } else {
            binding.searchResultsCount.visibility = View.GONE
        }
        
        // Update category grid
        val displayCategories = if (categories.size > 7) categories.take(7) else categories
        val showViewAll = categories.size > 7
        binding.categoryGridRecyclerView.visibility = View.VISIBLE
        binding.categoryGridRecyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.categoryGridRecyclerView.adapter = CategoryGridAdapter(
            displayCategories,
            onItemClick = fun(cat) {
                if (cat.id == -1) {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, CategoriesFragment())
                        .addToBackStack(null)
                        .commit()
                    return
                }
                // Open CategoryVideosActivity for real categories
                val intent = android.content.Intent(requireContext(), CategoryVideosActivity::class.java)
                intent.putExtra("category_id", cat.id)
                intent.putExtra("category_title", cat.title)
                intent.putExtra("category_icon", cat.iconFile)
                startActivity(intent)
            },
            showViewAll = showViewAll
        )

        // Update top videos
        if (topVideos.isNotEmpty()) {
            val orderedTopVideos = VideoListUtils.orderVideos(topVideos)
            val showTopViewMore = if (hasActiveTextSearch()) {
                false
            } else {
                topVideosHasMore
            }
            binding.topVideosLabel.visibility = View.VISIBLE
            binding.topVideosRecyclerView.visibility = View.VISIBLE
            binding.topVideosViewAll.visibility = if (showTopViewMore) View.VISIBLE else View.GONE
            binding.topVideosRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.topVideosRecyclerView.adapter = topVideosAdapter
            topVideosAdapter.submitList(
                VideoListUtils.withViewMoreTile(orderedTopVideos, showTopViewMore)
            )
        } else {
            binding.topVideosLabel.visibility = View.GONE
            binding.topVideosRecyclerView.visibility = View.GONE
            binding.topVideosViewAll.visibility = View.GONE
        }

        // Update category sections
        if (categoryVideos.isNotEmpty()) {
            binding.categorySectionsContainer.visibility = View.VISIBLE
            binding.categorySectionsContainer.removeAllViews()
            for (cat in categoryVideos) {
                if (!cat.videos.isNullOrEmpty()) {
                    val sectionView = LayoutInflater.from(requireContext()).inflate(R.layout.item_category_section, binding.categorySectionsContainer, false)
                    val sectionTitle = sectionView.findViewById<TextView>(R.id.sectionTitle)
                    val sectionIcon = sectionView.findViewById<ImageView>(R.id.sectionIcon)
                    val sectionViewAll = sectionView.findViewById<TextView>(R.id.sectionViewAll)
                    val sectionRecycler = sectionView.findViewById<RecyclerView>(R.id.sectionRecyclerView)
                    
                    sectionTitle.text = cat.title
                    Glide.with(sectionIcon.context).load(cat.iconFile).placeholder(R.drawable.no_image_placeholder).into(sectionIcon)

                    val orderedVideos = VideoListUtils.orderVideos(cat.videos ?: emptyList())
                    val sourceCategory = allCategoryVideos.find { it.id == cat.id } ?: cat
                    val sourcePreviewCount = sourceCategory.videos?.size ?: orderedVideos.size
                    val showCategoryViewMore = if (hasActiveTextSearch()) {
                        false
                    } else {
                        VideoListUtils.inferHasMore(
                            previewCount = sourcePreviewCount,
                            hasMore = sourceCategory.hasMore,
                            total = sourceCategory.videoTotal,
                        )
                    }
                    sectionViewAll.visibility = if (showCategoryViewMore) View.VISIBLE else View.GONE

                    sectionViewAll.setOnClickListener { openCategoryVideosList(cat) }

                    sectionRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    val adapter = VideoAdapter(onViewMoreClick = { openCategoryVideosList(cat) })
                    sectionRecycler.adapter = adapter
                    adapter.submitList(
                        VideoListUtils.withViewMoreTile(orderedVideos, showCategoryViewMore)
                    )
                    binding.categorySectionsContainer.addView(sectionView)
                }
            }
        } else {
            binding.categorySectionsContainer.visibility = View.GONE
        }
    }

    private fun callApi() {
        binding.loaderLayout.visibility = View.VISIBLE
        binding.homeProgressBar.visibility = View.VISIBLE
        binding.topVideosLabel.visibility = View.INVISIBLE
        binding.topVideosRecyclerView.visibility = View.INVISIBLE
        binding.topVideosViewAll.visibility = View.INVISIBLE
        binding.categoryGridRecyclerView.visibility = View.INVISIBLE
        binding.categorySectionsContainer.visibility = View.INVISIBLE
        homePageViewModel.getHomePageData("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerHandler?.removeCallbacksAndMessages(null)
        _binding = null
    }

    private fun observeHomePageData() {
        homePageViewModel.homepageLiveData.observe(viewLifecycleOwner) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> {
                    binding.loaderLayout.visibility = View.VISIBLE
                    binding.homeProgressBar.visibility = View.VISIBLE
                    binding.topVideosLabel.visibility = View.INVISIBLE
                    binding.topVideosRecyclerView.visibility = View.INVISIBLE
                    binding.categoryGridRecyclerView.visibility = View.INVISIBLE
                    binding.categorySectionsContainer.visibility = View.INVISIBLE
                }
                is ApiResult.Success -> {
                    binding.loaderLayout.visibility = View.GONE
                    binding.homeProgressBar.visibility = View.GONE
                    binding.topVideosLabel.visibility = View.VISIBLE
                    binding.topVideosRecyclerView.visibility = View.VISIBLE
                    binding.categoryGridRecyclerView.visibility = View.VISIBLE
                    binding.categorySectionsContainer.visibility = View.VISIBLE
                    
                    val data = apiResponse.data?.dataObj
                    isEmployee = data?.userDetails?.isEmployee == true
                    bannerList = orderBanners(data?.bannerList ?: emptyList())
                    setupBannerSlider()
                    
                    // Store all data for search functionality
                    allTopVideos = VideoListUtils.orderVideos(data?.topVideos ?: emptyList())
                    topVideosHasMore = VideoListUtils.inferHasMore(
                        previewCount = allTopVideos.size,
                        hasMore = data?.topVideosHasMore,
                        total = data?.topVideosTotal,
                    )
                    allCategoryList = data?.categoryList ?: emptyList<Category>()
                    allCategoryVideos = (data?.categoryVideos ?: emptyList()).map { category ->
                        val orderedVideos = VideoListUtils.orderVideos(category.videos ?: emptyList())
                        category.copy(videos = ArrayList(orderedVideos))
                    }

                    // Start background video preloading for better user experience
                    startBackgroundVideoPreloading()

                    // Apply toolbar search/location if any
                    applyFromToolbarState()
                }
                is ApiResult.Error -> {
                    binding.loaderLayout.visibility = View.GONE
                    binding.homeProgressBar.visibility = View.GONE
                    binding.topVideosLabel.visibility = View.VISIBLE
                    binding.topVideosRecyclerView.visibility = View.VISIBLE
                    binding.categoryGridRecyclerView.visibility = View.VISIBLE
                    binding.categorySectionsContainer.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "Failed to load home data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun orderBanners(banners: List<BannerList>): List<BannerList> {
        return banners.sortedWith(
            compareBy<BannerList> { it.position ?: Int.MAX_VALUE }
                .thenBy { it.id ?: Int.MAX_VALUE }
        )
    }

    private fun setupBannerSlider() {
        currentBannerPage = 0
        if (bannerList.isEmpty()) {
            binding.bannerViewPager.visibility = View.GONE
            binding.bannerIndicator.visibility = View.GONE
            bannerHandler?.removeCallbacksAndMessages(null)
            return
        }
        binding.bannerViewPager.visibility = View.VISIBLE
        binding.bannerIndicator.visibility = View.VISIBLE
        val bannerAdapter = ImagePagerAdapter(ArrayList(bannerList))
        binding.bannerViewPager.adapter = bannerAdapter
        setupBannerIndicator()
        startBannerAutoSlide()
        binding.bannerViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentBannerPage = position
                updateBannerIndicator(position)
            }
        })
    }

    private fun setupBannerIndicator() {
        val indicatorLayout = binding.bannerIndicator
        indicatorLayout.removeAllViews()
        val size = bannerList.size
        for (i in 0 until size) {
            val dot = ImageView(requireContext())
            val params = LinearLayout.LayoutParams(16, 16)
            params.marginEnd = 8
            dot.layoutParams = params
            dot.setImageResource(if (i == 0) R.drawable.indicator_dot_selected else R.drawable.indicator_dot_unselected)
            indicatorLayout.addView(dot)
        }
    }

    private fun updateBannerIndicator(selected: Int) {
        val indicatorLayout = binding.bannerIndicator
        for (i in 0 until indicatorLayout.childCount) {
            val dot = indicatorLayout.getChildAt(i) as ImageView
            dot.setImageResource(if (i == selected) R.drawable.indicator_dot_selected else R.drawable.indicator_dot_unselected)
        }
    }

    private fun startBannerAutoSlide() {
        bannerHandler?.removeCallbacksAndMessages(null)
        if (bannerList.size <= 1) return
        bannerHandler = Handler(Looper.getMainLooper())
        bannerRunnable = object : Runnable {
            override fun run() {
                val nextPage = (currentBannerPage + 1) % bannerList.size
                binding.bannerViewPager.setCurrentItem(nextPage, true)
                bannerHandler?.postDelayed(this, 3500)
            }
        }
        bannerHandler?.postDelayed(bannerRunnable!!, 3500)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startBackgroundVideoPreloading() {
        try {
            // Collect all video URLs from top videos and category videos
            val videoUrls = mutableListOf<String>()
            
            // Add top videos
            allTopVideos.forEach { video ->
                video.videoUrl?.let { url ->
                    if (url.isNotEmpty()) {
                        videoUrls.add(url)
                    }
                }
            }
            
            // Add category videos (limit to first 10 videos per category to avoid overwhelming)
            allCategoryVideos.forEach { categoryVideo ->
                categoryVideo.videos?.take(VideoListUtils.PREVIEW_LIMIT)?.forEach { video ->
                    video.videoUrl?.let { url ->
                        if (url.isNotEmpty()) {
                            videoUrls.add(url)
                        }
                    }
                }
            }
            
            // Start background preloading
            if (videoUrls.isNotEmpty()) {
                Log.d("HomeFragment", "Starting background preload for ${videoUrls.size} videos")
                VideoCacheManager.preloadVideosInBackground(requireContext(), videoUrls)
            }
            
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error starting background preload: ${e.message}")
        }
    }
}

class CategoryGridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val icon: ImageView = view.findViewById(R.id.categoryGridIcon)
    val title: TextView = view.findViewById(R.id.categoryGridTitle)
} 