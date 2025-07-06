package com.srijeesolution.rojgaarwaala.presentation.ui.activity

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

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private lateinit var homePageViewModel: HomePageViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var bannerHandler: Handler? = null
    private var bannerRunnable: Runnable? = null
    private var bannerList: List<BannerList> = emptyList()
    private var currentBannerPage = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]
        
        // Set up profile icon click listener
        binding.profileIcon.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }
        
        observeHomePageData()
        callApi()
    }

    private fun callApi() {
        binding.loaderLayout.visibility = View.VISIBLE
        binding.homeProgressBar.visibility = View.VISIBLE
        binding.topVideosLabel.visibility = View.INVISIBLE
        binding.topVideosRecyclerView.visibility = View.INVISIBLE
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
                    bannerList = data?.bannerList ?: emptyList()
                    setupBannerSlider()
                    val topVideos = data?.topVideos ?: emptyList<TopVideo>()
                    val categoryList = data?.categoryList ?: emptyList<Category>()
                    val categoryVideos = data?.categoryVideos ?: emptyList<CategoryVideo>()

                    // Category Grid (with View All)
                    binding.categoryGridRecyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
                    binding.categoryGridRecyclerView.adapter = CategoryGridAdapter(categoryList) { cat ->
                        Toast.makeText(requireContext(), cat.title ?: "View All", Toast.LENGTH_SHORT).show()
                    }

                    // Top Videos
                    val topVideosAdapter = TopVideosAdapter(topVideos)
                    binding.topVideosRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    binding.topVideosRecyclerView.adapter = topVideosAdapter

                    // Dynamic Category Sections (only if videos are not empty)
                    binding.categorySectionsContainer.removeAllViews()
                    for (cat in categoryVideos) {
                        if (!cat.videos.isNullOrEmpty()) {
                            val sectionView = LayoutInflater.from(requireContext()).inflate(R.layout.item_category_section, binding.categorySectionsContainer, false)
                            val sectionTitle = sectionView.findViewById<TextView>(R.id.sectionTitle)
                            val sectionIcon = sectionView.findViewById<ImageView>(R.id.sectionIcon)
                            val sectionRecycler = sectionView.findViewById<RecyclerView>(R.id.sectionRecyclerView)
                            sectionTitle.text = cat.title
                            Glide.with(sectionIcon.context).load(cat.iconFile).placeholder(R.drawable.no_image_placeholder).into(sectionIcon)
                            sectionRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                            sectionRecycler.adapter = VideoAdapter(cat.videos ?: emptyList())
                            binding.categorySectionsContainer.addView(sectionView)
                        }
                    }
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

    private fun setupBannerSlider() {
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
}

class CategoryGridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val icon: ImageView = view.findViewById(R.id.categoryGridIcon)
    val title: TextView = view.findViewById(R.id.categoryGridTitle)
} 