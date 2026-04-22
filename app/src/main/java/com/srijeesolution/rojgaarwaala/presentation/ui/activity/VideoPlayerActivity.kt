package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.ActivityVideoPlayerBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.content.pm.ActivityInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.content.Intent
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.WindowManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import com.srijeesolution.rojgaarwaala.utils.VideoOptimizationUtils
import com.srijeesolution.rojgaarwaala.utils.VideoCacheManager

// ExoPlayer imports
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.common.C
import com.srijeesolution.rojgaarwaala.presentation.adaptor.VideoVerticalAdapter
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.core.widget.NestedScrollView

@AndroidEntryPoint
@UnstableApi
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private val viewModel: HomePageViewModel by viewModels()
    private var videoId: Int = -1
    private var likeCount = 0
    private var dislikeCount = 0
    private var hasIncrementedView = false
    private var currentVideoTitle: String? = null
    private var currentVideoUrl: String? = null
    private var currentVideoThumbnail: String? = null
    private var isFullscreen = false
    private var videoDuration = 0L
    private var isSeeking = false
    private var isPlaying = false
    
    // Store previous state for rollback on API error
    private var previousLikeState = false
    private var previousDislikeState = false
    private var previousLikeCount = 0
    private var previousDislikeCount = 0
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            updateVideoProgress()
            progressHandler.postDelayed(this, 1000) // Update every second
        }
    }
    private val autoHideHandler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable {
        binding.playPauseButton.visibility = View.GONE
    }
    
    // ExoPlayer components
    private var exoPlayer: ExoPlayer? = null
    private lateinit var bandwidthMeter: BandwidthMeter
    private var currentQualitySettings: VideoOptimizationUtils.AdaptiveQualitySettings? = null
    
    // Gesture detection for swipe to fullscreen
    private lateinit var gestureDetector: GestureDetector
    private var initialY = 0f
    private var isGestureInProgress = false
    
    // Video player scroll zoom variables
    private val minVideoPlayerHeight = 300 // dp
    private val scrollThreshold = 200 // scroll pixels to trigger full transition
    private var maxVideoPlayerHeight = 520 // dp - will be set from layout
    private var currentVideoPlayerHeight = 520 // dp - will be set from layout
    private var heightAnimator: ValueAnimator? = null

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prevent screenshots and screen recording
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        // Initialize ExoPlayer components
        initializeExoPlayer()

        // Setup top bar (Toolbar)
        val toolbar: Toolbar = binding.topBar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        videoId = intent.getIntExtra("video_id", -1)
        // Also check for string video_id (from notifications)
        if (videoId == -1) {
            val videoIdString = intent.getStringExtra("video_id")
            android.util.Log.d("VideoPlayerActivity", "Video ID from intent: $videoIdString")
            if (!videoIdString.isNullOrEmpty()) {
                try {
                    videoId = videoIdString.toInt()
                    android.util.Log.d("VideoPlayerActivity", "Converted video ID: $videoId")
                } catch (e: NumberFormatException) {
                    android.util.Log.e("VideoPlayerActivity", "Failed to convert video ID: $videoIdString", e)
                    Toast.makeText(this, "Invalid video ID format", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
            }
        }
        
        android.util.Log.d("VideoPlayerActivity", "Final video ID: $videoId")
        if (videoId == -1) {
            Toast.makeText(this, "Invalid video ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRelatedVideosRecycler()
        setupActionRow()
        setupCustomVideoPlayerControls()
        setupGestureDetector()
        setupScrollZoomEffect()
        observeVideoDetails()
        observeLikeDislikeActions()
        viewModel.getVideoDetails(videoId)
    }

    private fun initializeExoPlayer() {
        try {
                    // Initialize bandwidth meter for adaptive streaming
        bandwidthMeter = VideoOptimizationUtils.getBandwidthMeter(this)
        
        // Set initial quality settings
        currentQualitySettings = VideoOptimizationUtils.getAdaptiveQualitySettings(this)
        
        // Add bandwidth monitoring for dynamic quality adjustment
        bandwidthMeter.addEventListener(Handler(Looper.getMainLooper())) { totalLoadTime, totalBytesLoaded, bitrateEstimate ->
            adjustVideoQuality(bitrateEstimate)
        }
            
            // Create zero-buffer load control for instant playback
            val loadControl = VideoOptimizationUtils.getZeroBufferLoadControl()
            
            // Create ExoPlayer with optimized settings
            exoPlayer = ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .setBandwidthMeter(bandwidthMeter)
                .build()
            
                    // Use instant HTTP data source factory for zero delay
        val httpDataSourceFactory = VideoOptimizationUtils.getInstantHttpDataSourceFactory()
        
        val dataSourceFactory = CacheDataSource.Factory()
            .setCache(VideoCacheManager.getCache(this))
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            
            // Set up player listeners
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        binding.progressBar.visibility = View.GONE
                        binding.videoThumbnailOverlay.visibility = View.GONE
                        isPlaying = true
                        
                        // Start playing immediately when ready
                        exoPlayer?.playWhenReady = true
                        
                        // Get video duration
                        videoDuration = exoPlayer?.duration ?: 0
                        
                        // Increment view count
                        if (!hasIncrementedView) {
                            viewModel.incrementVideoView(videoId)
                            hasIncrementedView = true
                        }
                    }
                    Player.STATE_BUFFERING -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        progressHandler.removeCallbacks(progressRunnable)
                        autoHideHandler.removeCallbacks(autoHideRunnable)
                    }
                    Player.STATE_IDLE -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("VideoPlayerActivity", "Player error: ${error.message}")
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@VideoPlayerActivity, "Error playing video. Please try again.", Toast.LENGTH_SHORT).show()
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@VideoPlayerActivity.isPlaying = isPlaying
            }
        })
        
        // Attach player to PlayerView
        binding.customVideoView.player = exoPlayer
        
        // Configure PlayerView to show controls on tap and auto-hide after 2 seconds
        binding.customVideoView.controllerShowTimeoutMs = 2000
        binding.customVideoView.useController = true
        
        // Ensure PlayerView is clickable and can receive touch events
        binding.customVideoView.isClickable = true
        binding.customVideoView.isFocusable = true
        
        // Hide settings button from PlayerView controller
        binding.customVideoView.findViewById<View>(androidx.media3.ui.R.id.exo_settings)?.visibility = View.GONE
        
        // Optimize for cached content
        optimizeForCachedContent()
        
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Error initializing ExoPlayer: ${e.message}")
            Toast.makeText(this, "Error initializing video player", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Adjust video quality based on current bandwidth
     */
    private fun adjustVideoQuality(currentBitrate: Long) {
        val newQualitySettings = when {
            currentBitrate >= 1_500_000 -> VideoOptimizationUtils.AdaptiveQualitySettings.HIGH_QUALITY
            currentBitrate >= 800_000 -> VideoOptimizationUtils.AdaptiveQualitySettings.MEDIUM_QUALITY
            else -> VideoOptimizationUtils.AdaptiveQualitySettings.LOW_QUALITY
        }
        
        // Only adjust if quality has changed
        if (currentQualitySettings != newQualitySettings) {
            currentQualitySettings = newQualitySettings
            Log.d("VideoPlayerActivity", "Quality adjusted to: ${newQualitySettings.name} (${currentBitrate / 1000}kbps)")
            
            // Show quality change notification to user
            showQualityChangeNotification(newQualitySettings)
        }
    }
    
    /**
     * Show quality change notification to user
     */
    private fun showQualityChangeNotification(quality: VideoOptimizationUtils.AdaptiveQualitySettings) {
        val qualityText = when (quality) {
            VideoOptimizationUtils.AdaptiveQualitySettings.HIGH_QUALITY -> "HD"
            VideoOptimizationUtils.AdaptiveQualitySettings.MEDIUM_QUALITY -> "Standard"
            VideoOptimizationUtils.AdaptiveQualitySettings.LOW_QUALITY -> "Low"
        }

    //    Toast.makeText(this, "Video quality: $qualityText", Toast.LENGTH_SHORT).show()
    }

    private fun setupActionRow() {
        binding.likeButton.setOnClickListener {
            if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                handleLikeAction()
            } else {
                // User is not logged in, navigate to login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Please login to like videos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.dislikeButton.setOnClickListener {
            if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                handleDislikeAction()
            } else {
                // User is not logged in, navigate to login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Please login to dislike videos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.shareButton.setOnClickListener {
            shareVideo()
        }

        // Setup fullscreen action controls with synchronized functionality
        binding.fullscreenLikeButton.setOnClickListener {
            if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                handleLikeAction()
            } else {
                // User is not logged in, navigate to login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Please login to like videos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fullscreenDislikeButton.setOnClickListener {
            if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                handleDislikeAction()
            } else {
                // User is not logged in, navigate to login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Please login to dislike videos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fullscreenShareButton.setOnClickListener {
            shareVideo()
        }
    }
    
    /**
     * Handle like action with optimistic UI update
     */
    private fun handleLikeAction() {
        val wasLiked = sharedPrefs.isVideoLiked(videoId)
        val wasDisliked = sharedPrefs.isVideoDisliked(videoId)
        
        // Store previous state for potential rollback
        previousLikeState = wasLiked
        previousDislikeState = wasDisliked
        previousLikeCount = likeCount
        previousDislikeCount = dislikeCount
        
        // Optimistic UI update - update immediately
        if (wasLiked) {
            // Unlike: decrease like count
            likeCount = maxOf(0, likeCount - 1)
            sharedPrefs.setVideoLiked(videoId, false)
        } else {
            // Like: increase like count
            likeCount += 1
            sharedPrefs.setVideoLiked(videoId, true)
            
            // If previously disliked, decrease dislike count
            if (wasDisliked) {
                dislikeCount = maxOf(0, dislikeCount - 1)
                sharedPrefs.setVideoDisliked(videoId, false)
            }
            
            // Show confetti only when user likes the video (not when unliking)
            binding.confettiView.spawnBurstAtCenter()
        }
        
        // Update UI immediately
        updateLikeDislikeCounts()
        updateLikeDislikeUI()
        // Make API call in background
        viewModel.likeVideo(videoId)
    }
    
    /**
     * Handle dislike action with optimistic UI update
     */
    private fun handleDislikeAction() {
        val wasLiked = sharedPrefs.isVideoLiked(videoId)
        val wasDisliked = sharedPrefs.isVideoDisliked(videoId)
        
        // Store previous state for potential rollback
        previousLikeState = wasLiked
        previousDislikeState = wasDisliked
        previousLikeCount = likeCount
        previousDislikeCount = dislikeCount
        
        // Optimistic UI update - update immediately
        if (wasDisliked) {
            // Undislike: decrease dislike count
            dislikeCount = maxOf(0, dislikeCount - 1)
            sharedPrefs.setVideoDisliked(videoId, false)
        } else {
            // Dislike: increase dislike count
            dislikeCount += 1
            sharedPrefs.setVideoDisliked(videoId, true)
            
            // If previously liked, decrease like count
            if (wasLiked) {
                likeCount = maxOf(0, likeCount - 1)
                sharedPrefs.setVideoLiked(videoId, false)
            }
        }
        
        // Update UI immediately
        updateLikeDislikeCounts()
        updateLikeDislikeUI()
        
        // Make API call in background
        viewModel.unlikeVideo(videoId)
    }

    private fun updateLikeDislikeCounts() {
        binding.likeCount.text = likeCount.toString()
        binding.dislikeCount.text = dislikeCount.toString()
        
        // Update fullscreen controls with synchronized counts
        binding.fullscreenLikeCount.text = likeCount.toString()
        binding.fullscreenDislikeCount.text = dislikeCount.toString()
    }

    private fun updateLikeDislikeUI() {
        val isLiked = sharedPrefs.isVideoLiked(videoId)
        val isDisliked = sharedPrefs.isVideoDisliked(videoId)
        // Update like button
        if (isLiked) {
            binding.likeButton.setImageResource(R.drawable.ic_thumb_up_filled)
            binding.likeButton.alpha = 1.0f
            // Update fullscreen like button
            binding.fullscreenLikeButton.setImageResource(R.drawable.ic_thumb_up_filled)
            binding.fullscreenLikeButton.alpha = 1.0f
        } else {
            binding.likeButton.setImageResource(R.drawable.ic_thumb_up_outline)
            binding.likeButton.alpha = 0.7f
            // Update fullscreen like button
            binding.fullscreenLikeButton.setImageResource(R.drawable.ic_thumb_up_outline)
            binding.fullscreenLikeButton.alpha = 0.7f
        }
        // Update dislike button
        if (isDisliked) {
            binding.dislikeButton.setImageResource(R.drawable.ic_thumb_down_filled)
            binding.dislikeButton.alpha = 1.0f
            // Update fullscreen dislike button
            binding.fullscreenDislikeButton.setImageResource(R.drawable.ic_thumb_down_filled)
            binding.fullscreenDislikeButton.alpha = 1.0f
        } else {
            binding.dislikeButton.setImageResource(R.drawable.ic_thumb_down_outline)
            binding.dislikeButton.alpha = 0.7f
            // Update fullscreen dislike button
            binding.fullscreenDislikeButton.setImageResource(R.drawable.ic_thumb_down_outline)
            binding.fullscreenDislikeButton.alpha = 0.7f
        }
    }

    private fun observeVideoDetails() {
        viewModel.videoDetailsLiveData.observe(this, Observer { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val data = result.data?.data
                    if (data != null) {
                        bindVideoDetails(data)
                    } else {
                        Toast.makeText(this, "No video data found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (result.message != null) {
                        Toast.makeText(this, "" + result.message, Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            }
        })
    }

    private fun observeLikeDislikeActions() {
        // Observe like video action
        viewModel.likeVideoLiveData.observe(this, Observer { result ->
            when (result) {
                is ApiResult.Success<*> -> {
                    // API call succeeded - optimistic update was correct
                    // No need to refresh video details, UI already updated
                    // This prevents video from restarting
                }
                is ApiResult.Error -> {
                    // Rollback optimistic update on error
                    sharedPrefs.setVideoLiked(videoId, previousLikeState)
                    sharedPrefs.setVideoDisliked(videoId, previousDislikeState)
                    likeCount = previousLikeCount
                    dislikeCount = previousDislikeCount
                    updateLikeDislikeCounts()
                    updateLikeDislikeUI()
                    //Toast.makeText(this, "Failed to like video", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        })

        // Observe unlike video action
        viewModel.unlikeVideoLiveData.observe(this, Observer { result ->
            when (result) {
                is ApiResult.Success<*> -> {
                    // API call succeeded - optimistic update was correct
                    // No need to refresh video details, UI already updated
                    // This prevents video from restarting
                }
                is ApiResult.Error -> {
                    // Rollback optimistic update on error
                    sharedPrefs.setVideoLiked(videoId, previousLikeState)
                    sharedPrefs.setVideoDisliked(videoId, previousDislikeState)
                    likeCount = previousLikeCount
                    dislikeCount = previousDislikeCount
                    updateLikeDislikeCounts()
                    updateLikeDislikeUI()
                    //Toast.makeText(this, "Failed to dislike video", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        })
    }

    private fun bindVideoDetails(data: com.srijeesolution.rojgaarwaala.data.remote.model.VideoDetailsData) {
        // Show thumbnail overlay while loading
        val thumbnailUrl = data.thumbnail
        if (!thumbnailUrl.isNullOrEmpty()) {
            binding.videoThumbnailOverlay.visibility = View.VISIBLE
            Glide.with(this)
                .load(thumbnailUrl)
                .centerCrop()
                .into(binding.videoThumbnailOverlay)
        } else {
            binding.videoThumbnailOverlay.visibility = View.GONE
        }
        // Play video (decoder-safe mode: disable extra preload players to avoid NO_MEMORY on some devices).
        if (data.videoUrl != null) {
            // Reset view count flag for new video
            hasIncrementedView = false
            startVideoWithCacheCheck(data.videoUrl)
        }
        // Like/Dislike/Share/Views
        likeCount = data.likes ?: 0
        dislikeCount = data.unlikes ?: 0
        updateLikeDislikeCounts()
        updateLikeDislikeUI()
        binding.viewsCount.text = "${data.views ?: 0} views"
        binding.fullscreenViewsCount.text = "${data.views ?: 0} views"
        // Related videos
        val related = data.relatedVideos ?: emptyList()
        Log.d("VideoPlayerActivity", "Related videos count: ${related.size}")
        
        // Always show related videos section (it will show empty list if no data)
        binding.relatedVideosLabel.visibility = View.VISIBLE
        binding.relatedVideosRecyclerView.visibility = View.VISIBLE
        
        // Create new adapter with related videos (empty list if no data)
        val newAdapter = VideoVerticalAdapter(related) { videoId ->
            // Stop current video and load new one
            stopCurrentVideo()
            this.videoId = videoId
            viewModel.getVideoDetails(videoId)
        }
        binding.relatedVideosRecyclerView.adapter = newAdapter
        
        if (related.isEmpty()) {
            Log.d("VideoPlayerActivity", "No related videos available - showing empty list")
        }
        currentVideoTitle = data.title
        currentVideoUrl = data.stream_url?:data.videoUrl
        currentVideoThumbnail = data.thumbnail
    }

    /**
     * Refresh related videos display
     */
    private fun refreshRelatedVideosDisplay() {
        val relatedVideos = viewModel.videoDetailsLiveData.value?.data?.data?.relatedVideos ?: emptyList()
        Log.d("VideoPlayerActivity", "Refreshing related videos display: ${relatedVideos.size}")
        
        if (!isFullscreen) {
            // Keep related videos section visible (it will show empty list if no data)
            binding.relatedVideosLabel.visibility = View.VISIBLE
            binding.relatedVideosRecyclerView.visibility = View.VISIBLE
            
            // Create new adapter with related videos (empty list if no data)
            val newAdapter = VideoVerticalAdapter(relatedVideos) { videoId ->
                // Stop current video and load new one with zero buffering
                stopCurrentVideo()
                this.videoId = videoId
                viewModel.getVideoDetails(videoId)
            }
            binding.relatedVideosRecyclerView.adapter = newAdapter
        } else {
            // Only hide when in fullscreen
            binding.relatedVideosLabel.visibility = View.GONE
            binding.relatedVideosRecyclerView.visibility = View.GONE
        }
    }

    /**
     * Setup scroll-based zoom effect for video player
     */
    private fun setupScrollZoomEffect() {
        // Get the actual height from layout
        binding.videoPlayerFrame.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.videoPlayerFrame.viewTreeObserver.removeOnGlobalLayoutListener(this)
                
                // Get the actual height in dp from the layout
                val heightInPixels = binding.videoPlayerFrame.height
                maxVideoPlayerHeight = (heightInPixels / resources.displayMetrics.density).toInt()
                currentVideoPlayerHeight = maxVideoPlayerHeight
                
                // Initialize spacer height to show some content initially
                // Use 80% of video height so related videos are partially visible
                val initialSpacerHeight = (heightInPixels * 0.08).toInt()
                val spacerLayoutParams = binding.spacerView.layoutParams
                spacerLayoutParams.height = initialSpacerHeight
                binding.spacerView.layoutParams = spacerLayoutParams
                
                // Initially show fullscreen button since video is at full size
                binding.fullscreenButton.visibility = View.VISIBLE
            }
        })
        
        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (isFullscreen) return@setOnScrollChangeListener // Don't apply zoom in fullscreen
            
            // Calculate scroll progress (0.0 to 1.0)
            val scrollProgress = (scrollY.toFloat() / scrollThreshold).coerceIn(0f, 1f)
            
            // Calculate target height based on scroll progress
            val targetHeight = (maxVideoPlayerHeight - (maxVideoPlayerHeight - minVideoPlayerHeight) * scrollProgress).toInt()
            
            // Control fullscreen button visibility based on video size
            // Show when video is large (near top), hide when video is small (scrolled down)
            val shouldShowFullscreenButton = targetHeight >= (maxVideoPlayerHeight * 0.8) // Show when video is 80% or larger
            binding.fullscreenButton.visibility = if (shouldShowFullscreenButton) View.VISIBLE else View.GONE
            
            // Only animate if the target height has changed significantly
            if (kotlin.math.abs(targetHeight - currentVideoPlayerHeight) > 5) {
                animateVideoPlayerHeight(targetHeight)
            }
        }
    }
    
    /**
     * Animate video player height with smooth transition
     */
    private fun animateVideoPlayerHeight(targetHeight: Int) {
        // Cancel any running animation
        heightAnimator?.cancel()
        
        val startHeight = currentVideoPlayerHeight
        
        heightAnimator = ValueAnimator.ofInt(startHeight, targetHeight).apply {
            duration = 200 // Smooth but quick animation
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animator ->
                val animatedHeight = animator.animatedValue as Int
                currentVideoPlayerHeight = animatedHeight
                
                val densityPixels = (animatedHeight * resources.displayMetrics.density).toInt()
                
                // Update the video player frame height
                val videoLayoutParams = binding.videoPlayerFrame.layoutParams
                videoLayoutParams.height = densityPixels
                binding.videoPlayerFrame.layoutParams = videoLayoutParams
                
                // Update the spacer height - use a percentage to keep some content visible
                // When video is full size (520dp), spacer is 80% so content shows
                // When video is minimized (300dp), spacer is smaller so more content shows
                val spacerHeightPercentage = if (animatedHeight > minVideoPlayerHeight + 50) 0.08 else 0.08
                val spacerHeight = (densityPixels * spacerHeightPercentage).toInt()
                val spacerLayoutParams = binding.spacerView.layoutParams
                spacerLayoutParams.height = spacerHeight
                binding.spacerView.layoutParams = spacerLayoutParams
            }
            
            start()
        }
    }

    private fun setupRelatedVideosRecycler() {
        binding.relatedVideosRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.relatedVideosRecyclerView.adapter = VideoVerticalAdapter(emptyList()) { videoId ->
            // Stop current video and load new one with zero buffering
            stopCurrentVideo()
            this.videoId = videoId
            viewModel.getVideoDetails(videoId)
        }
        
        // Ensure related videos section is visible by default
        binding.relatedVideosLabel.visibility = View.VISIBLE
        binding.relatedVideosRecyclerView.visibility = View.VISIBLE
        
        // Disable nested scrolling to let NestedScrollView handle all scrolling
        binding.relatedVideosRecyclerView.isNestedScrollingEnabled = false
    }

    private fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be")
    }

    private fun playCustomVideo(videoUrl: String) {
        try {
            stopCurrentVideo()
            
            val mediaItem = MediaItem.Builder().setUri(videoUrl).build()
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            
            // Start playing immediately without waiting for buffer
            exoPlayer?.playWhenReady = true
            
            // Force start playback
            Handler(Looper.getMainLooper()).postDelayed({
                exoPlayer?.play()
            }, 100) // Start after 100ms
            
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Error playing video: ${e.message}")
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Stop the currently playing video
     */
    private fun stopCurrentVideo() {
        try {
            // Stop playback
            exoPlayer?.stop()
            
            // Reset state
            isPlaying = false
            videoDuration = 0
            
            // Stop progress tracking
            progressHandler.removeCallbacks(progressRunnable)
            autoHideHandler.removeCallbacks(autoHideRunnable)
            
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Error stopping video: ${e.message}")
        }
    }

    /**
     * Preload video for better streaming performance
     */
    private fun preloadVideo(url: String) {
        Log.d("VideoPlayerActivity", "Skipping decoder preload for stability: $url")
    }

    /**
     * Preload related videos for faster switching
     */
    private fun preloadRelatedVideos(videos: List<com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo>) {
        Log.d("VideoPlayerActivity", "Related decoder preload disabled for device stability. Count=${videos.size}")
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val regex = Regex("(?:v=|be/|embed/|shorts/)([a-zA-Z0-9_-]{11})")
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun setupCustomVideoPlayerControls() {
        val fullscreenButton = binding.fullscreenButton

        // Setup fullscreen button for custom fullscreen handling
        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }

        // Smart touch handling for video area with gesture detection
        binding.customVideoView.setOnTouchListener { _, event ->
            // Handle gestures for fullscreen
            val gestureHandled = gestureDetector.onTouchEvent(event)
            
            // Always return false to let ExoPlayer handle regular taps
            // Only consume events if a specific gesture was detected
            if (gestureHandled) {
                true // Consume the event only if gesture was handled
            } else {
                false // Let the event pass through to ExoPlayer for tap-to-show controls
            }
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                initialY = e.y
                isGestureInProgress = false
                return false // Don't consume the down event
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1 != null) {
                    val deltaY = e2.y - e1.y
                    val minSwipeDistance = 150f // Increased minimum distance for swipe detection
                    
                    if (Math.abs(deltaY) > minSwipeDistance) {
                        isGestureInProgress = true
                        
                        if (deltaY < 0) {
                            // Swipe up - enter fullscreen
                            if (!isFullscreen) {
                                toggleFullscreen()
                            }
                        } else {
                            // Swipe down - exit fullscreen
                            if (isFullscreen) {
                                toggleFullscreen()
                            }
                        }
                        return true
                    }
                }
                return false
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null) {
                    val minVelocity = 800f // Increased minimum velocity for fling detection
                    
                    if (Math.abs(velocityY) > minVelocity) {
                        isGestureInProgress = true
                        
                        if (velocityY < 0) {
                            // Fling up - enter fullscreen
                            if (!isFullscreen) {
                                toggleFullscreen()
                            }
                        } else {
                            // Fling down - exit fullscreen
                            if (isFullscreen) {
                                toggleFullscreen()
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun updateVideoProgress() {
        // This method is no longer needed since ExoPlayer handles progress tracking
        // Keeping it for potential future use
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = (milliseconds / 1000).toLong()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            enterFullscreen()
        } else {
            exitFullscreen()
        }
    }

    private fun enterFullscreen() {
        // Hide UI elements
        binding.topBar.visibility = View.GONE
        binding.actionRow.visibility = View.GONE
        binding.viewsCount.visibility = View.GONE
        binding.relatedVideosLabel.visibility = View.GONE
        binding.relatedVideosRecyclerView.visibility = View.GONE
        
        // Hide normal video progress controls (fullscreen controls are hidden since ExoPlayer has built-in controls)
        binding.videoSeekBar.visibility = View.GONE
        binding.currentTimeText.visibility = View.GONE
        binding.totalTimeText.visibility = View.GONE
        binding.fullscreenProgressControls.visibility = View.GONE
        
        // Show fullscreen action controls
        binding.fullscreenActionControls.visibility = View.VISIBLE
        
        // Make video player frame take full screen
        val frameParams = binding.videoPlayerFrame.layoutParams as LinearLayout.LayoutParams
        frameParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        frameParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.videoPlayerFrame.layoutParams = frameParams
        
        // Make video player take full screen
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.gravity = android.view.Gravity.CENTER
        binding.customVideoView.layoutParams = layoutParams
        
        // Hide system UI
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        
        // Change fullscreen button icon
        binding.fullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit)
    }

    private fun exitFullscreen() {
        // Show UI elements
        binding.topBar.visibility = View.VISIBLE
        binding.actionRow.visibility = View.GONE // Keep hidden
        binding.viewsCount.visibility = View.VISIBLE
        
        // Refresh related videos display
        refreshRelatedVideosDisplay()
        
        // Show normal video progress controls and hide fullscreen ones
        binding.videoSeekBar.visibility = View.VISIBLE
        binding.currentTimeText.visibility = View.VISIBLE
        binding.totalTimeText.visibility = View.VISIBLE
        binding.fullscreenProgressControls.visibility = View.GONE
        
        // Keep fullscreen action controls visible
        binding.fullscreenActionControls.visibility = View.VISIBLE
        
        // Restore video player frame to original size
        val frameParams = binding.videoPlayerFrame.layoutParams as LinearLayout.LayoutParams
        frameParams.height = 260.dpToPx()
        frameParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.videoPlayerFrame.layoutParams = frameParams
        
        // Restore video player size with proper centering
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.gravity = android.view.Gravity.CENTER
        binding.customVideoView.layoutParams = layoutParams
        
        // Show system UI
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        
        // Change fullscreen button icon
        binding.fullscreenButton.setImageResource(R.drawable.ic_fullscreen)
    }
    
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onBackPressed() {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            // Check if user came from notification
            val fromNotification = intent.getBooleanExtra("from_notification", false) ||
                                  intent.getStringExtra("notification_type") != null ||
                                  intent.getStringExtra("type") != null
            
            if (fromNotification) {
                // User came from notification - go to MainActivity and clear stack
                // Create a clean intent without notification flags
                val cleanIntent = Intent(this, MainActivity::class.java)
                cleanIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(cleanIntent)
                finish()
            } else {
                // Normal back navigation
                super.onBackPressed()
            }
        }
    }

    private fun shareVideo() {
        val title = currentVideoTitle ?: "Check out this video!"
        val url = currentVideoUrl ?: ""
        val appDetails = "\n\nWatch this video on Rojgaarwaala! Download the app: https://play.google.com/store/apps/details?id=com.srijeesolution.rojgaarwaala"
        val shareText = "$title\n$url$appDetails"
        
        // If thumbnail is available, share with image
        if (!currentVideoThumbnail.isNullOrEmpty()) {
            shareVideoWithThumbnail(shareText, currentVideoThumbnail!!)
        } else {
            // Fallback to text-only sharing
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            startActivity(android.content.Intent.createChooser(intent, "Share video via"))
        }
    }
    
    private fun shareVideoWithThumbnail(shareText: String, thumbnailUrl: String) {
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE
        
        // Download thumbnail in background thread
        Thread {
            try {
                // Download thumbnail
                val url = URL(thumbnailUrl)
                val connection = url.openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                // Save bitmap to cache directory
                val cacheDir = cacheDir
                val imageFile = File(cacheDir, "share_thumbnail_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
                outputStream.close()
                
                // Create FileProvider URI
                val imageUri = FileProvider.getUriForFile(
                    this@VideoPlayerActivity,
                    "${packageName}.fileprovider",
                    imageFile
                )
                
                // Share on main thread
                Handler(Looper.getMainLooper()).post {
                    binding.progressBar.visibility = View.GONE
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                    intent.type = "image/*"
                    intent.putExtra(android.content.Intent.EXTRA_STREAM, imageUri)
                    intent.putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(android.content.Intent.createChooser(intent, "Share video via"))
                }
            } catch (e: Exception) {
                Log.e("VideoPlayerActivity", "Error sharing video with thumbnail: ${e.message}")
                // Fallback to text-only sharing on error
                Handler(Looper.getMainLooper()).post {
                    binding.progressBar.visibility = View.GONE
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    startActivity(android.content.Intent.createChooser(intent, "Share video via"))
                }
            }
        }.start()
    }

    override fun onPause() {
        super.onPause()
        // Pause video when activity is paused
        exoPlayer?.pause()
        isPlaying = false
        progressHandler.removeCallbacks(progressRunnable)
        autoHideHandler.removeCallbacks(autoHideRunnable)
    }
    
    override fun onResume() {
        super.onResume()
        // Resume video if it was playing before pause
        if (exoPlayer?.isPlaying == false && isPlaying) {
            exoPlayer?.play()
            progressHandler.post(progressRunnable)
            startAutoHideTimer()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacks(progressRunnable)
        autoHideHandler.removeCallbacks(autoHideRunnable)
        
        // Cancel height animation
        heightAnimator?.cancel()
        heightAnimator = null
        
        // Release ExoPlayer resources
        exoPlayer?.release()
        exoPlayer = null
        
        // Note: Cache is managed by VideoCacheManager singleton
        // It will be automatically cleaned up when the app is killed
    }

    private fun startAutoHideTimer() {
        autoHideHandler.removeCallbacks(autoHideRunnable)
        autoHideHandler.postDelayed(autoHideRunnable, 3000) // Auto-hide after 3 seconds
    }

    /**
     * Configure ExoPlayer for zero buffering and immediate playback
     */
    private fun configureForZeroBuffering() {
        exoPlayer?.let { player ->
            // Set minimum buffer to absolute minimum
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            
            // Disable buffering indicators
            binding.customVideoView.setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
            
            // Set immediate playback
            player.playWhenReady = true
        }
    }
    
    /**
     * Start video with zero buffering
     */
    private fun startVideoWithZeroBuffering(videoUrl: String) {
        try {
            stopCurrentVideo()
            
            val mediaItem = MediaItem.Builder().setUri(videoUrl).build()
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            
            // Configure for zero buffering
            configureForZeroBuffering()
            
            // Force immediate start
            Handler(Looper.getMainLooper()).postDelayed({
                exoPlayer?.play()
            }, 50) // Start after 50ms
            
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Error starting video: ${e.message}")
        }
    }
    
    /**
     * Start video with cache status check
     */
    private fun startVideoWithCacheCheck(videoUrl: String) {
        try {
            stopCurrentVideo()
            
            // Check if video is cached
            val isCached = VideoCacheManager.isVideoCached(videoUrl)
            Log.d("VideoPlayerActivity", "Video cached: $isCached for URL: $videoUrl")
            
            // Show cache status to user
            if (isCached) {
                Toast.makeText(this, "Playing from cache", Toast.LENGTH_SHORT).show()
            }
            
            val mediaItem = MediaItem.Builder().setUri(videoUrl).build()
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            
            // Configure for zero buffering
            configureForZeroBuffering()
            
            // Force immediate start
            Handler(Looper.getMainLooper()).postDelayed({
                exoPlayer?.play()
            }, 50) // Start after 50ms
            
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Error starting video: ${e.message}")
        }
    }
    
    /**
     * Completely disable buffering and start immediate playback
     */
    private fun startVideoInstantly(videoUrl: String) {
        try {
            stopCurrentVideo()
            
            // Hide progress bar immediately
            binding.progressBar.visibility = View.GONE
            
            val mediaItem = MediaItem.Builder().setUri(videoUrl).build()
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            
            // Set immediate playback without any buffering
            exoPlayer?.playWhenReady = true
            
            // Force start immediately
            Handler(Looper.getMainLooper()).postDelayed({
                exoPlayer?.play()
                // Hide thumbnail immediately
                binding.videoThumbnailOverlay.visibility = View.GONE
            }, 25) // Start after 25ms - ultra fast
            
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Error starting video instantly: ${e.message}")
        }
    }

    /**
     * Show cache statistics for debugging
     */
    private fun showCacheStats() {
        try {
            val cacheStats = VideoCacheManager.getCacheStats()
            Log.d("VideoPlayerActivity", "Cache Stats: $cacheStats")
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Error getting cache stats: ${e.message}")
        }
    }
    
    /**
     * Optimize video player for cached content
     */
    private fun optimizeForCachedContent() {
        try {
            // Show cache stats
            showCacheStats()
            
            // Configure ExoPlayer for optimal cached playback
            exoPlayer?.let { player ->
                // Set video scaling for better performance
                player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                
                // Enable hardware acceleration
                binding.customVideoView.useArtwork = false
                binding.customVideoView.setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
            }
            
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Error optimizing for cached content: ${e.message}")
        }
    }
}
