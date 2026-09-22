package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem
import com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage
import com.srijeesolution.rojgaarwaala.databinding.FragmentImagesBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.FreeJobCardsAdapter
import com.srijeesolution.rojgaarwaala.presentation.adaptor.ImagesCategoryAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.MainToolbarViewModel
import com.srijeesolution.rojgaarwaala.utils.FreeJobFeed
import com.srijeesolution.rojgaarwaala.utils.FreeJobItem
import com.srijeesolution.rojgaarwaala.utils.HomeLocationDefaults
import com.srijeesolution.rojgaarwaala.utils.ImageLocationFilter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ImagesFragment : Fragment() {

    private var _binding: FragmentImagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomePageViewModel
    private lateinit var mainToolbarViewModel: MainToolbarViewModel

    private var allCategories: List<ImageSubItem> = emptyList()
    private var viewMode = FreeJobFeed.ViewMode.LIST
    private var sort = FreeJobFeed.Sort.NEWEST
    private var hasLoaded = false
    private var currentPage = 1
    private var hasMorePages = false
    private var isLoadingPage = false
    private var serverTotal: Int? = null
    private val searchHandler = Handler(Looper.getMainLooper())
    private val searchReload = Runnable { reloadFromStart() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[HomePageViewModel::class.java]
        mainToolbarViewModel = ViewModelProvider(requireActivity())[MainToolbarViewModel::class.java]
        setupClicks()
        setupSearch()
        binding.imagesRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.imagesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || isLoadingPage || !hasMorePages) return
                val manager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (manager.findLastVisibleItemPosition() >= manager.itemCount - 3) {
                    loadPage(currentPage + 1, append = true)
                }
            }
        })
        mainToolbarViewModel.selectedLocation.observe(viewLifecycleOwner) {
            if (allCategories.isNotEmpty()) render()
        }
        setupObservers()
        reloadFromStart()
    }

    private fun setupClicks() {
        binding.freeJobViewList.setOnClickListener { setViewMode(FreeJobFeed.ViewMode.LIST) }
        binding.freeJobViewTile.setOnClickListener { setViewMode(FreeJobFeed.ViewMode.TILE) }
        binding.freeJobSortChip.setOnClickListener { showSortMenu() }
    }

    private fun setupSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                binding.clearSearchButton.visibility = if (query.isBlank()) View.GONE else View.VISIBLE
                searchHandler.removeCallbacks(searchReload)
                searchHandler.postDelayed(searchReload, 400)
            }
        })
        binding.clearSearchButton.setOnClickListener {
            binding.searchBar.setText("")
        }
    }

    private fun setupObservers() {
        viewModel.imageListLiveData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ApiResult.Loading -> Unit
                is ApiResult.Success -> {
                    val incoming = result.data?.data?.categoryImages
                        .orEmpty()
                        .filter { it.images?.isNotEmpty() == true }
                    val pagination = result.data?.data?.pagination
                    val page = pagination?.currentPage ?: currentPage
                    hasLoaded = true
                    isLoadingPage = false
                    hideProgress()
                    serverTotal = pagination?.total
                    hasMorePages = pagination?.hasMore == true
                    currentPage = page
                    allCategories = if (page <= 1) {
                        incoming
                    } else {
                        FreeJobFeed.mergeCategories(allCategories, incoming)
                    }
                    render()
                }
                is ApiResult.Error -> {
                    isLoadingPage = false
                    hideProgress()
                    Toast.makeText(context, "Failed to load jobs", Toast.LENGTH_SHORT).show()
                    if (!hasLoaded) showEmpty()
                }
            }
        }
    }

    private fun reloadFromStart() {
        currentPage = 1
        hasMorePages = false
        serverTotal = null
        loadPage(1, append = false)
    }

    private fun loadPage(page: Int, append: Boolean) {
        if (isLoadingPage) return
        isLoadingPage = true
        currentPage = page
        if (append) {
            binding.freeJobLoadMoreProgress.visibility = View.VISIBLE
        } else {
            binding.freeJobProgress.visibility = View.VISIBLE
            binding.noResultsLayout.visibility = View.GONE
        }
        viewModel.getScheduledImages(
            page = page,
            perPage = PAGE_SIZE,
            sort = if (sort == FreeJobFeed.Sort.NEWEST) "newest" else "oldest",
            title = binding.searchBar.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun hideProgress() {
        binding.freeJobProgress.visibility = View.GONE
        binding.freeJobLoadMoreProgress.visibility = View.GONE
    }

    private fun setViewMode(mode: FreeJobFeed.ViewMode) {
        viewMode = mode
        render()
    }

    private fun showSortMenu() {
        val popup = PopupMenu(requireContext(), binding.freeJobSortChip)
        popup.menu.add(0, 1, 0, R.string.free_job_newest)
        popup.menu.add(0, 2, 1, R.string.free_job_oldest)
        popup.setOnMenuItemClickListener { item ->
            sort = if (item.itemId == 1) FreeJobFeed.Sort.NEWEST else FreeJobFeed.Sort.OLDEST
            reloadFromStart()
            true
        }
        popup.show()
    }

    private fun districtQuery(): String {
        val loc = mainToolbarViewModel.selectedLocation.value.orEmpty().trim()
        return if (HomeLocationDefaults.skipsDistrictFilter(loc)) "" else loc
    }

    private fun filteredCategories(): List<ImageSubItem> {
        val query = binding.searchBar.text?.toString()?.trim().orEmpty().lowercase()
        val locationQuery = districtQuery()
        if (query.isEmpty() && locationQuery.isEmpty()) return allCategories
        return allCategories.mapNotNull { category ->
            val images = category.images.orEmpty().filter { image ->
                val searchOk = query.isEmpty() ||
                    image.title?.lowercase()?.contains(query) == true ||
                    image.description?.lowercase()?.contains(query) == true ||
                    category.title?.lowercase()?.contains(query) == true
                val locationOk = !FreeJobFeed.hasLocation(image) ||
                    locationQuery.isEmpty() ||
                    ImageLocationFilter.matches(image, locationQuery)
                searchOk && locationOk
            }
            if (images.isEmpty()) null else category.copy(images = images)
        }
    }

    private fun render() {
        val categories = filteredCategories()
        val items = FreeJobFeed.flatten(categories)
        val located = FreeJobFeed.sortedItems(FreeJobFeed.withLocation(items), sort)
        val posterCategories = FreeJobFeed.sortedCategories(
            FreeJobFeed.categoriesWithoutLocation(categories),
            sort,
        )
        val visibleCount = if (viewMode == FreeJobFeed.ViewMode.LIST) {
            located.size
        } else {
            posterCategories.sumOf { it.images.orEmpty().size }
        }
        val count = if (districtQuery().isEmpty()) {
            serverTotal ?: visibleCount
        } else {
            visibleCount
        }
        binding.freeJobCountLabel.text = getString(R.string.free_job_count_label, count)
        binding.freeJobSortChip.text = getString(
            if (sort == FreeJobFeed.Sort.NEWEST) R.string.free_job_newest else R.string.free_job_oldest,
        )
        styleViewToggle()

        if (viewMode == FreeJobFeed.ViewMode.LIST) {
            bindList(located)
        } else {
            bindTiles(posterCategories)
        }
    }

    private fun bindList(cards: List<FreeJobItem>) {
        if (cards.isEmpty()) {
            showEmpty()
            return
        }
        binding.noResultsLayout.visibility = View.GONE
        binding.imagesRecyclerView.visibility = View.VISIBLE
        binding.imagesRecyclerView.adapter = FreeJobCardsAdapter(
            cards,
            onClick = { item -> openLocatedJob(item) },
            onViewMap = { item -> openJobOnMap(item) },
        )
    }

    private fun bindTiles(images: List<ImageSubItem>) {
        if (images.isEmpty()) {
            showEmpty()
            return
        }
        binding.noResultsLayout.visibility = View.GONE
        binding.imagesRecyclerView.visibility = View.VISIBLE
        binding.imagesRecyclerView.adapter = ImagesCategoryAdapter(
            images,
            onImageClick = { category, imageIndex -> onPosterClick(category, imageIndex) },
            onViewAllClick = { category -> onViewAllClick(category) },
        )
    }

    private fun showEmpty() {
        binding.imagesRecyclerView.adapter = null
        binding.imagesRecyclerView.visibility = View.GONE
        binding.noResultsLayout.visibility = if (hasLoaded) View.VISIBLE else View.GONE
    }

    private fun styleViewToggle() {
        val listSelected = viewMode == FreeJobFeed.ViewMode.LIST
        val yellow = ContextCompat.getColor(requireContext(), R.color.brand_color_yellow)
        val muted = ContextCompat.getColor(requireContext(), R.color.search_hint)
        binding.freeJobViewList.setBackgroundResource(
            if (listSelected) R.drawable.bg_free_job_view_selected else android.R.color.transparent,
        )
        binding.freeJobViewTile.setBackgroundResource(
            if (listSelected) android.R.color.transparent else R.drawable.bg_free_job_view_selected,
        )
        binding.freeJobViewList.setColorFilter(if (listSelected) yellow else muted)
        binding.freeJobViewTile.setColorFilter(if (listSelected) muted else yellow)
    }

    private fun openLocatedJob(item: FreeJobItem) {
        startActivity(
            Intent(context, ImageViewerActivity::class.java).apply {
                putParcelableArrayListExtra(
                    "scheduled_images",
                    arrayListOf(toScheduledImage(item.job)),
                )
                putExtra("current_index", 0)
                putExtra(ImageViewerActivity.EXTRA_IMAGE_CATEGORY, item.categoryTitle ?: getString(R.string.free_job))
            },
        )
    }

    private fun openJobOnMap(item: FreeJobItem) {
        val geo = FreeJobFeed.mapGeoUri(item.job)
        if (geo == null) {
            Toast.makeText(context, getString(R.string.free_job_map_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(geo)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, getString(R.string.free_job_map_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    private fun onPosterClick(category: ImageSubItem, imageIndex: Int) {
        val images = category.images ?: emptyList()
        if (imageIndex !in images.indices) return
        startActivity(
            Intent(context, ImageViewerActivity::class.java).apply {
                putParcelableArrayListExtra(
                    "scheduled_images",
                    arrayListOf(toScheduledImage(images[imageIndex])),
                )
                putExtra("current_index", 0)
                putExtra(ImageViewerActivity.EXTRA_IMAGE_CATEGORY, category.title)
            },
        )
    }

    private fun onViewAllClick(category: ImageSubItem) {
        startActivity(
            Intent(context, ImagesListActivity::class.java).apply {
                putExtra("category_id", category.id)
                putExtra("category_title", category.title)
                putExtra("filter_location", districtQuery())
            },
        )
    }

    private fun toScheduledImage(imageData: ImageData): ScheduledImage {
        return ScheduledImage(
            id = imageData.id,
            title = imageData.title,
            description = imageData.description,
            imagePath = imageData.imageUrl,
            location = imageData.location,
            publishDate = imageData.publishDate,
            status = null,
            createdAt = imageData.createdAt,
            updatedAt = null,
            phoneNumber = imageData.phoneNumber,
            areaName = imageData.areaName,
            salaryText = imageData.salaryText,
            shiftText = imageData.shiftText,
            latitude = imageData.latitude,
            longitude = imageData.longitude,
            distanceKm = imageData.distanceKm,
        )
    }

    override fun onDestroyView() {
        searchHandler.removeCallbacks(searchReload)
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
