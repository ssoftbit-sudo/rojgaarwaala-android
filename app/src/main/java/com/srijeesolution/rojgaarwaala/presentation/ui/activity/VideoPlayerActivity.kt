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
import com.srijeesolution.rojgaarwaala.utils.TimeUtils

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
    private var currentContactNumber: String? = null
    private var isFullscreen = false
    private var videoDuration = 0L
    private var isSeeking = false
    private var isSeekingOverlay = false
    private var isPlaying = false
    private var descriptionExpanded = false
    
    // Store previous state for rollback on API error
    private var previousLikeState = false
    private var previousDislikeState = false
    private var previousLikeCount = 0
    private var previousDislikeCount = 0
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            updatePlaybackProgress()
            progressHandler.postDelayed(this, 500)
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
    
    private var maxVideoPlayerHeight = 260 // dp — updated when compact height applied
    private var currentVideoPlayerHeight = 260
    private var preFullscreenVideoHeightPx: Int = 0

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
        toolbar.inflateMenu(R.menu.menu_video_player)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_search) {
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                )
                true
            } else false
        }
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
        setupPlaybackOverlay()
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
                        updatePlayPauseIcon()
                        progressHandler.post(progressRunnable)
                        
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
                        updatePlayPauseIcon()
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
                updatePlayPauseIcon()
                if (isPlaying) {
                    progressHandler.post(progressRunnable)
                } else {
                    progressHandler.removeCallbacks(progressRunnable)
                }
            }
        })
        
        // Attach player to PlayerView (custom overlay controls; XML sets use_controller=false)
        binding.customVideoView.player = exoPlayer
        binding.customVideoView.useController = false
        binding.customVideoView.setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
        
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

    private fun setupPlaybackOverlay() {
        binding.playPauseOverlay.setOnClickListener {
            val p = exoPlayer ?: return@setOnClickListener
            if (p.isPlaying) p.pause() else p.play()
        }
        binding.skipBackButton.setOnClickListener {
            exoPlayer?.let {
                val t = (it.currentPosition - 10_000L).coerceAtLeast(0L)
                it.seekTo(t)
            }
        }
        binding.skipForwardButton.setOnClickListener {
            exoPlayer?.let {
                val d = it.duration
                val t = it.currentPosition + 10_000L
                val end = if (d > 0 && d != C.TIME_UNSET) d else t
                it.seekTo(t.coerceAtMost(end))
            }
        }
        binding.videoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val dur = exoPlayer?.duration ?: return
                    if (dur > 0 && dur != C.TIME_UNSET) {
                        val pos = (progress / 1000.0 * dur).toLong()
                        binding.currentTimeText.text = formatTime(pos.toInt().coerceAtLeast(0))
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeekingOverlay = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeekingOverlay = false
                val dur = exoPlayer?.duration ?: return
                if (dur > 0 && dur != C.TIME_UNSET) {
                    val pos = (binding.videoSeekBar.progress / 1000.0 * dur).toLong()
                    exoPlayer?.seekTo(pos)
                }
            }
        })
    }

    private fun updatePlayPauseIcon() {
        val playing = exoPlayer?.isPlaying == true
        binding.playPauseOverlay.setImageResource(
            if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    private fun updatePlaybackProgress() {
        val player = exoPlayer ?: return
        val duration = player.duration
        if (duration <= 0 || duration == C.TIME_UNSET) return
        if (!isSeekingOverlay) {
            val pos = player.currentPosition
            val prog = ((pos * 1000L) / duration).toInt().coerceIn(0, 1000)
            binding.videoSeekBar.progress = prog
            binding.currentTimeText.text = formatTime(pos.toInt().coerceAtLeast(0))
            binding.totalTimeText.text = formatTime(duration.toInt().coerceAtLeast(0))
        }
    }

    private fun formatCompactViews(count: Int): String = when {
        count >= 1_000_000 -> {
            val v = count / 1_000_000.0
            if (v >= 10) "${v.toInt()}M" else String.format("%.1fM", v).replace(".0M", "M")
        }
        count >= 1_000 -> {
            val v = count / 1_000.0
            if (v >= 10) "${v.toInt()}K" else String.format("%.1fK", v).replace(".0K", "K")
        }
        else -> count.toString()
    }

    private fun toggleDescriptionExpanded() {
        descriptionExpanded = !descriptionExpanded
        if (descriptionExpanded) {
            binding.videoDetailDescription.maxLines = Int.MAX_VALUE
            binding.expandDescriptionButton.rotation = 270f
        } else {
            binding.videoDetailDescription.maxLines = 3
            binding.expandDescriptionButton.rotation = 90f
        }
    }

    private fun openApplyForVideo() {
        if (videoId <= 0) {
            Toast.makeText(this, "Video details are loading", Toast.LENGTH_SHORT).show()
            return
        }
        val applyIntent = Intent(this, ApplyFormActivity::class.java)
        applyIntent.putExtra("video_id", videoId)
        applyIntent.putExtra("video_title", currentVideoTitle ?: "Job Opportunity")
        startActivity(applyIntent)
    }

    private fun dialPosterPhone() {
        val phoneNumber = currentContactNumber
        if (phoneNumber.isNullOrBlank()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phoneNumber") }
        )
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

        binding.fullscreenApplyButton.setOnClickListener { openApplyForVideo() }
        binding.fullscreenCallButton.setOnClickListener { dialPosterPhone() }
        binding.inlineApplyColumn.setOnClickListener { openApplyForVideo() }
        binding.inlineCallColumn.setOnClickListener { dialPosterPhone() }

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

        binding.expandDescriptionButton.setOnClickListener { toggleDescriptionExpanded() }
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
        if (wasLiked) {
            viewModel.removeVideoReaction(videoId)
        } else {
            viewModel.likeVideo(videoId)
        }
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
        
        if (wasDisliked) {
            viewModel.removeVideoReaction(videoId)
        } else {
            viewModel.unlikeVideo(videoId)
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
        viewModel.likeVideoLiveData.observe(this, Observer { result ->
            when (result) {
                is ApiResult.Success -> applyReactionFromApi(result.data?.data)
                is ApiResult.Error -> handleReactionError(result)
                else -> {}
            }
        })
        viewModel.unlikeVideoLiveData.observe(this, Observer { result ->
            when (result) {
                is ApiResult.Success -> applyReactionFromApi(result.data?.data)
                is ApiResult.Error -> handleReactionError(result)
                else -> {}
            }
        })
        viewModel.removeVideoReactionLiveData.observe(this, Observer { result ->
            when (result) {
                is ApiResult.Success -> applyReactionFromApi(result.data?.data)
                is ApiResult.Error -> handleReactionError(result)
                else -> {}
            }
        })
    }

    private fun handleReactionError(result: ApiResult.Error<*>) {
        rollbackReaction()
        if (result.message?.statusCode == 401) {
            sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false))
            sharedPrefs.removeSharedPrefs(SharedPrefsConstant.USER_AUTH_TOKEN)
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun applyReactionFromApi(data: com.srijeesolution.rojgaarwaala.data.remote.model.VideoReactionData?) {
        data ?: return
        data.likeCount?.let { likeCount = it }
        data.unlikeCount?.let { dislikeCount = it }
        data.isLiked?.let { sharedPrefs.setVideoLiked(videoId, it) }
        data.isUnliked?.let { sharedPrefs.setVideoDisliked(videoId, it) }
        updateLikeDislikeCounts()
        updateLikeDislikeUI()
    }

    private fun rollbackReaction() {
        sharedPrefs.setVideoLiked(videoId, previousLikeState)
        sharedPrefs.setVideoDisliked(videoId, previousDislikeState)
        likeCount = previousLikeCount
        dislikeCount = previousDislikeCount
        updateLikeDislikeCounts()
        updateLikeDislikeUI()
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
        val relativeUploadTime = TimeUtils.getRelativeTimeSpanString(this, data.createdAt)
        val vCompact = formatCompactViews(data.views ?: 0)
        val metaLine = if (relativeUploadTime.isNotEmpty()) {
            "$vCompact views • $relativeUploadTime"
        } else {
            "$vCompact views"
        }
        binding.uploadTimeLabel.text = metaLine
        binding.fullscreenViewsCount.text = metaLine
        binding.viewsCount.text = metaLine

        binding.videoDetailTitle.text = data.title.orEmpty()
        val cat = data.category?.title?.trim().orEmpty()
        val descFirst = data.description?.lineSequence()?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }.orEmpty()
        when {
            cat.isNotEmpty() -> {
                binding.videoSubtitle.visibility = View.VISIBLE
                binding.videoSubtitle.text = cat
            }
            descFirst.isNotEmpty() && descFirst != data.title?.trim() -> {
                binding.videoSubtitle.visibility = View.VISIBLE
                binding.videoSubtitle.text = descFirst
            }
            else -> binding.videoSubtitle.visibility = View.GONE
        }
        val location = data.locationHint?.trim().orEmpty()
        if (location.isNotEmpty()) {
            binding.videoLocationLabel.visibility = View.VISIBLE
            binding.videoLocationLabel.text = location
        } else {
            binding.videoLocationLabel.visibility = View.GONE
        }
        binding.videoDetailDescription.text = data.description.orEmpty()
        val hasDesc = !data.description.isNullOrBlank()
        binding.videoDetailDescription.visibility = if (hasDesc) View.VISIBLE else View.GONE
        val needsExpand = hasDesc && data.description.orEmpty().trim().lines().size > 3
        binding.expandDescriptionButton.visibility =
            if (needsExpand) View.VISIBLE else View.GONE
        descriptionExpanded = false
        binding.videoDetailDescription.maxLines = 3
        binding.expandDescriptionButton.rotation = 90f

        // Related videos
        val related = data.relatedVideos ?: emptyList()
        Log.d("VideoPlayerActivity", "Related videos count: ${related.size}")
        
        // Always show related videos section (it will show empty list if no data)
        binding.relatedVideosLabel.setText(R.string.up_next)
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
        currentContactNumber = data.user?.mobile
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

    /** ~34% of screen height (mockup: video ~30%), at least [R.dimen.video_player_min_height]. */
    private fun setupScrollZoomEffect() {
        binding.videoPlayerFrame.post {
            val minPx = resources.getDimensionPixelSize(R.dimen.video_player_min_height)
            val targetPx =
                (resources.displayMetrics.heightPixels * 0.34f).toInt().coerceAtLeast(minPx)
            val lp = binding.videoPlayerFrame.layoutParams as LinearLayout.LayoutParams
            lp.height = targetPx
            binding.videoPlayerFrame.layoutParams = lp
            maxVideoPlayerHeight = (targetPx / resources.displayMetrics.density).toInt()
            currentVideoPlayerHeight = maxVideoPlayerHeight
            binding.fullscreenButton.visibility = View.VISIBLE
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
        preFullscreenVideoHeightPx = binding.videoPlayerFrame.layoutParams.height

        binding.nestedScrollView.visibility = View.GONE
        binding.topBar.visibility = View.GONE
        binding.actionRow.visibility = View.GONE
        binding.viewsCount.visibility = View.GONE
        binding.relatedVideosLabel.visibility = View.GONE
        binding.relatedVideosRecyclerView.visibility = View.GONE

        binding.fullscreenProgressControls.visibility = View.GONE
        binding.videoSeekBar.visibility = View.VISIBLE
        binding.currentTimeText.visibility = View.VISIBLE
        binding.totalTimeText.visibility = View.VISIBLE
        binding.playerControlsOverlay.visibility = View.VISIBLE

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
        
        binding.fullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit)
    }

    private fun exitFullscreen() {
        binding.nestedScrollView.visibility = View.VISIBLE
        binding.topBar.visibility = View.VISIBLE
        binding.actionRow.visibility = View.GONE
        binding.viewsCount.visibility = View.GONE

        refreshRelatedVideosDisplay()

        binding.videoSeekBar.visibility = View.VISIBLE
        binding.currentTimeText.visibility = View.VISIBLE
        binding.totalTimeText.visibility = View.VISIBLE
        binding.fullscreenProgressControls.visibility = View.GONE
        binding.playerControlsOverlay.visibility = View.VISIBLE

        binding.fullscreenActionControls.visibility = View.GONE
        
        // Restore video player frame to original size
        val frameParams = binding.videoPlayerFrame.layoutParams as LinearLayout.LayoutParams
        frameParams.height = if (preFullscreenVideoHeightPx > 0) {
            preFullscreenVideoHeightPx
        } else {
            260.dpToPx()
        }
        frameParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.videoPlayerFrame.layoutParams = frameParams

        // Sync scroll-zoom state with restored height so UI stays consistent.
        currentVideoPlayerHeight = (frameParams.height / resources.displayMetrics.density).toInt()
        
        // Restore video player size with proper centering
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.gravity = android.view.Gravity.CENTER
        binding.customVideoView.layoutParams = layoutParams
        
        // Show system UI
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        
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
                Log.i("VideoPlayerActivity", "Playing from cache")
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
