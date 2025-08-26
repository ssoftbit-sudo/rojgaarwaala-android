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
import com.srijeesolution.rojgaarwaala.presentation.adaptor.VideoAdapter
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
    private var isFullscreen = false
    private var videoDuration = 0L
    private var isSeeking = false
    private var isPlaying = false
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

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        
        Toast.makeText(this, "Video quality: $qualityText", Toast.LENGTH_SHORT).show()
    }

    private fun setupActionRow() {
        binding.likeButton.setOnClickListener {
            if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                viewModel.likeVideo(videoId)
            } else {
                // User is not logged in, navigate to login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Please login to like videos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.dislikeButton.setOnClickListener {
            if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                viewModel.unlikeVideo(videoId)
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
                viewModel.likeVideo(videoId)
            } else {
                // User is not logged in, navigate to login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Please login to like videos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fullscreenDislikeButton.setOnClickListener {
            if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                viewModel.unlikeVideo(videoId)
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
        } else {
            binding.likeButton.setImageResource(R.drawable.ic_thumb_up_outline)
            binding.likeButton.alpha = 0.7f
        }
        // Update dislike button
        if (isDisliked) {
            binding.dislikeButton.setImageResource(R.drawable.ic_thumb_down_filled)
            binding.dislikeButton.alpha = 1.0f
        } else {
            binding.dislikeButton.setImageResource(R.drawable.ic_thumb_down_outline)
            binding.dislikeButton.alpha = 0.7f
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
                    // After successful like, refresh video details to get updated counts
                    viewModel.getVideoDetails(videoId)
                    //Toast.makeText(this, "Video liked!", Toast.LENGTH_SHORT).show()
                }
                is ApiResult.Error -> {
                    //Toast.makeText(this, "Failed to like video", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        })

        // Observe unlike video action
        viewModel.unlikeVideoLiveData.observe(this, Observer { result ->
            when (result) {
                is ApiResult.Success<*> -> {
                    // After successful unlike, refresh video details to get updated counts
                    viewModel.getVideoDetails(videoId)
                    //Toast.makeText(this, "Video disliked!", Toast.LENGTH_SHORT).show()
                }
                is ApiResult.Error -> {
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
        // Play video with preloading for better performance
        if (data.videoUrl != null) {
            // Reset view count flag for new video
            hasIncrementedView = false
            
            // Preload the video for better streaming
            preloadVideo(data.videoUrl)
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
        
        if (related.isNotEmpty()) {
            // Show related videos section when data is available
            binding.relatedVideosLabel.visibility = View.VISIBLE
            binding.relatedVideosRecyclerView.visibility = View.VISIBLE
            
            // Create new adapter with related videos
            val newAdapter = VideoAdapter(related) { videoId ->
                // Stop current video and load new one
                stopCurrentVideo()
                this.videoId = videoId
                viewModel.getVideoDetails(videoId)
            }
            binding.relatedVideosRecyclerView.adapter = newAdapter
            
            // Preload related videos for faster switching
            preloadRelatedVideos(related)
        } else {
            // Hide related videos section when no data
            binding.relatedVideosLabel.visibility = View.GONE
            binding.relatedVideosRecyclerView.visibility = View.GONE
            Log.d("VideoPlayerActivity", "No related videos available")
        }
        currentVideoTitle = data.title
        currentVideoUrl = data.stream_url?:data.videoUrl
    }

    /**
     * Refresh related videos display
     */
    private fun refreshRelatedVideosDisplay() {
        val relatedVideos = viewModel.videoDetailsLiveData.value?.data?.data?.relatedVideos ?: emptyList()
        Log.d("VideoPlayerActivity", "Refreshing related videos display: ${relatedVideos.size}")
        
        if (relatedVideos.isNotEmpty() && !isFullscreen) {
            binding.relatedVideosLabel.visibility = View.VISIBLE
            binding.relatedVideosRecyclerView.visibility = View.VISIBLE
            
            // Create new adapter with related videos
            val newAdapter = VideoAdapter(relatedVideos) { videoId ->
                // Stop current video and load new one with zero buffering
                stopCurrentVideo()
                this.videoId = videoId
                viewModel.getVideoDetails(videoId)
            }
            binding.relatedVideosRecyclerView.adapter = newAdapter
        } else {
            binding.relatedVideosLabel.visibility = View.GONE
            binding.relatedVideosRecyclerView.visibility = View.GONE
        }
    }

    private fun setupRelatedVideosRecycler() {
        binding.relatedVideosRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.relatedVideosRecyclerView.adapter = VideoAdapter(emptyList()) { videoId ->
            // Stop current video and load new one with zero buffering
            stopCurrentVideo()
            this.videoId = videoId
            viewModel.getVideoDetails(videoId)
        }
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
        try {
            // Use zero-buffer load control for instant preloading
            val loadControl = VideoOptimizationUtils.getZeroBufferLoadControl()
            
            // Create a temporary ExoPlayer instance for preloading
            val preloadPlayer = ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build()
            
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(url))
                .build()
            preloadPlayer.setMediaItem(mediaItem)
            preloadPlayer.prepare()
            
            // Preload for a very short duration then release
            Handler(Looper.getMainLooper()).postDelayed({
                preloadPlayer.release()
            }, 1000) // Ultra-fast preload time
            
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Error preloading video: ${e.message}")
        }
    }

    /**
     * Preload related videos for faster switching
     */
    private fun preloadRelatedVideos(videos: List<com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo>) {
        try {
            // Preload first 2 related videos for faster switching
            videos.take(2).forEach { video ->
                video.videoUrl?.let { url ->
                    Handler(Looper.getMainLooper()).postDelayed({
                        preloadVideo(url)
                    }, 2000) // Delay preloading to not interfere with current video
                }
            }
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Error preloading related videos: ${e.message}")
        }
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
        binding.actionRow.visibility = View.VISIBLE
        binding.viewsCount.visibility = View.VISIBLE
        
        // Refresh related videos display
        refreshRelatedVideosDisplay()
        
        // Show normal video progress controls and hide fullscreen ones
        binding.videoSeekBar.visibility = View.VISIBLE
        binding.currentTimeText.visibility = View.VISIBLE
        binding.totalTimeText.visibility = View.VISIBLE
        binding.fullscreenProgressControls.visibility = View.GONE
        
        // Hide fullscreen action controls
        binding.fullscreenActionControls.visibility = View.GONE
        
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
        val appDetails = "\n\nWatch this video on Rojgaarwaala! Download the app: https://rojgaarwaala.com"
        val shareText = "$title\n$url$appDetails"
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        startActivity(android.content.Intent.createChooser(intent, "Share video via"))
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
