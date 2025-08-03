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
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant

@AndroidEntryPoint
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
    private var videoDuration = 0
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

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        observeVideoDetails()
        viewModel.getVideoDetails(videoId)
    }

    private fun setupActionRow() {
        binding.likeButton.setOnClickListener {
            if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                viewModel.likeVideo(videoId)
                viewModel.likeVideoLiveData.observe(this, Observer { result ->
                    when (result) {
                        is ApiResult.Success<*> -> {
                            likeCount++
                            updateLikeDislikeCounts()
                            Toast.makeText(this, "Video liked!", Toast.LENGTH_SHORT).show()
                        }
                        is ApiResult.Error -> {
                            Toast.makeText(this, "Failed to like video", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                })
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
                viewModel.unlikeVideoLiveData.observe(this, Observer { result ->
                    when (result) {
                        is ApiResult.Success<*> -> {
                            dislikeCount++
                            updateLikeDislikeCounts()
                            Toast.makeText(this, "Video disliked!", Toast.LENGTH_SHORT).show()
                        }
                        is ApiResult.Error -> {
                            Toast.makeText(this, "Failed to dislike video", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                })
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
                viewModel.likeVideoLiveData.observe(this, Observer { result ->
                    when (result) {
                        is ApiResult.Success<*> -> {
                            likeCount++
                            updateLikeDislikeCounts()
                            Toast.makeText(this, "Video liked!", Toast.LENGTH_SHORT).show()
                        }
                        is ApiResult.Error -> {
                            Toast.makeText(this, "Failed to like video", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                })
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
                viewModel.unlikeVideoLiveData.observe(this, Observer { result ->
                    when (result) {
                        is ApiResult.Success<*> -> {
                            dislikeCount++
                            updateLikeDislikeCounts()
                            Toast.makeText(this, "Video disliked!", Toast.LENGTH_SHORT).show()
                        }
                        is ApiResult.Error -> {
                            Toast.makeText(this, "Failed to dislike video", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                })
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
        // Play video
        if (data.videoUrl != null) {
            playCustomVideo(data.videoUrl)
        }
        // Like/Dislike/Share/Views
        likeCount = data.likes ?: 0
        dislikeCount = data.unlikes ?: 0
        updateLikeDislikeCounts()
        binding.viewsCount.text = "${data.views ?: 0} views"
        binding.fullscreenViewsCount.text = "${data.views ?: 0} views"
        // Related videos
        val related = data.relatedVideos ?: emptyList()
        if (related.isNotEmpty()) {
            // Show related videos section when data is available
            binding.relatedVideosLabel.visibility = View.VISIBLE
            binding.relatedVideosRecyclerView.visibility = View.VISIBLE
            (binding.relatedVideosRecyclerView.adapter as? VideoAdapter)?.let {
                binding.relatedVideosRecyclerView.adapter = VideoAdapter(related)
            } ?: run {
                binding.relatedVideosRecyclerView.adapter = VideoAdapter(related)
            }
        } else {
            // Hide related videos section when no data
            binding.relatedVideosLabel.visibility = View.GONE
            binding.relatedVideosRecyclerView.visibility = View.GONE
        }
        currentVideoTitle = data.title
        currentVideoUrl = data.stream_url?:data.videoUrl
    }

    private fun setupRelatedVideosRecycler() {
        binding.relatedVideosRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.relatedVideosRecyclerView.adapter = VideoAdapter(emptyList())
    }

    private fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be")
    }

    private fun playCustomVideo(url: String) {
        val videoView = binding.customVideoView
        val playPauseButton = binding.playPauseButton
        val thumbnailOverlay = binding.videoThumbnailOverlay
        val seekBar = binding.videoSeekBar
        val currentTimeText = binding.currentTimeText
        val totalTimeText = binding.totalTimeText
        
        videoView.visibility = View.VISIBLE
        
        // Ensure video is centered in normal view
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.gravity = android.view.Gravity.CENTER
        videoView.layoutParams = layoutParams
        
        videoView.setVideoURI(Uri.parse(url))
        
        // Set up prepared listener with progress tracking
        videoView.setOnPreparedListener { mediaPlayer ->
            // Hide thumbnail and start video
            thumbnailOverlay.visibility = View.GONE
            playPauseButton.setImageResource(R.drawable.ic_pause_circle)
            playPauseButton.visibility = View.VISIBLE
            isPlaying = true
            
            // Get video duration and update total time for both normal and fullscreen
            videoDuration = mediaPlayer.duration
            totalTimeText.text = formatTime(videoDuration)
            currentTimeText.text = "0:00"
            binding.fullscreenTotalTimeText.text = formatTime(videoDuration)
            binding.fullscreenCurrentTimeText.text = "0:00"
            seekBar.progress = 0
            binding.fullscreenVideoSeekBar.progress = 0
            
            // Start the video
            mediaPlayer.start()
            
            // Start progress tracking
            progressHandler.post(progressRunnable)
            
            // Auto-hide play button after 3 seconds
            startAutoHideTimer()
        }
        
        videoView.setOnCompletionListener {
            playPauseButton.setImageResource(R.drawable.ic_play_circle)
            playPauseButton.visibility = View.VISIBLE
            isPlaying = false
            progressHandler.removeCallbacks(progressRunnable)
            autoHideHandler.removeCallbacks(autoHideRunnable)
            seekBar.progress = 0
            currentTimeText.text = "0:00"
        }
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val regex = Regex("(?:v=|be/|embed/|shorts/)([a-zA-Z0-9_-]{11})")
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun setupCustomVideoPlayerControls() {
        val videoView = binding.customVideoView
        val playPauseButton = binding.playPauseButton
        val fullscreenButton = binding.fullscreenButton
        val thumbnailOverlay = binding.videoThumbnailOverlay
        val seekBar = binding.videoSeekBar
        val currentTimeText = binding.currentTimeText
        val totalTimeText = binding.totalTimeText
        val fullscreenSeekBar = binding.fullscreenVideoSeekBar
        val fullscreenCurrentTimeText = binding.fullscreenCurrentTimeText
        val fullscreenTotalTimeText = binding.fullscreenTotalTimeText

        // Setup SeekBar with improved performance (normal mode)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && videoDuration > 0) {
                    val newPosition = (progress * videoDuration) / 100
                    currentTimeText.text = formatTime(newPosition)
                    fullscreenCurrentTimeText.text = formatTime(newPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeeking = true
                progressHandler.removeCallbacks(progressRunnable)
                autoHideHandler.removeCallbacks(autoHideRunnable)
                
                // Show play button during seeking
                if (isPlaying) {
                    playPauseButton.setImageResource(R.drawable.ic_pause_circle)
                    playPauseButton.visibility = View.VISIBLE
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (videoDuration > 0) {
                    val newPosition = (seekBar?.progress ?: 0) * videoDuration / 100
                    videoView.seekTo(newPosition)
                    
                    // Resume progress tracking immediately after seek
                    if (isPlaying) {
                        progressHandler.post(progressRunnable)
                        startAutoHideTimer()
                    }
                }
                isSeeking = false
            }
        })

        // Setup Fullscreen SeekBar with synchronized functionality
        fullscreenSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && videoDuration > 0) {
                    val newPosition = (progress * videoDuration) / 100
                    fullscreenCurrentTimeText.text = formatTime(newPosition)
                    currentTimeText.text = formatTime(newPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeeking = true
                progressHandler.removeCallbacks(progressRunnable)
                autoHideHandler.removeCallbacks(autoHideRunnable)
                
                // Show play button during seeking
                if (isPlaying) {
                    playPauseButton.setImageResource(R.drawable.ic_pause_circle)
                    playPauseButton.visibility = View.VISIBLE
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (videoDuration > 0) {
                    val newPosition = (seekBar?.progress ?: 0) * videoDuration / 100
                    videoView.seekTo(newPosition)
                    
                    // Resume progress tracking immediately after seek
                    if (isPlaying) {
                        progressHandler.post(progressRunnable)
                        startAutoHideTimer()
                    }
                }
                isSeeking = false
            }
        })

        playPauseButton.setOnClickListener {
            if (videoView.isPlaying) {
                videoView.pause()
                playPauseButton.setImageResource(R.drawable.ic_play_circle)
                playPauseButton.visibility = View.VISIBLE
                isPlaying = false
                progressHandler.removeCallbacks(progressRunnable)
                autoHideHandler.removeCallbacks(autoHideRunnable)
            } else {
                // Hide thumbnail overlay when user clicks play
                thumbnailOverlay.visibility = View.GONE
                videoView.start()
                playPauseButton.setImageResource(R.drawable.ic_pause_circle)
                playPauseButton.visibility = View.VISIBLE
                isPlaying = true
                progressHandler.post(progressRunnable)
                
                // Auto-hide play button after 3 seconds
                startAutoHideTimer()
                
                // Increment view count only once per session
                if (!hasIncrementedView) {
                    viewModel.incrementVideoView(videoId)
                    binding.viewsCount.text = "${(binding.viewsCount.text.toString().split(" ")[0].toIntOrNull() ?: 0) + 1} views"
                    hasIncrementedView = true
                }
            }
        }

        // Smart touch handling for video area
        videoView.setOnTouchListener { _, _ ->
            if (isPlaying) {
                // Show pause button when video is playing
                playPauseButton.setImageResource(R.drawable.ic_pause_circle)
                playPauseButton.visibility = View.VISIBLE
                startAutoHideTimer()
            } else {
                // Show play button when video is paused
                playPauseButton.setImageResource(R.drawable.ic_play_circle)
                playPauseButton.visibility = View.VISIBLE
                startAutoHideTimer()
            }
            false
        }

        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }
    }

    private fun updateVideoProgress() {
        if (!isSeeking) {
            val videoView = binding.customVideoView
            val seekBar = binding.videoSeekBar
            val currentTimeText = binding.currentTimeText
            val fullscreenSeekBar = binding.fullscreenVideoSeekBar
            val fullscreenCurrentTimeText = binding.fullscreenCurrentTimeText
            
            if (videoView.isPlaying && videoDuration > 0) {
                val currentPosition = videoView.currentPosition
                val progress = (currentPosition * 100) / videoDuration
                
                // Update both normal and fullscreen progress controls
                seekBar.progress = progress
                currentTimeText.text = formatTime(currentPosition)
                fullscreenSeekBar.progress = progress
                fullscreenCurrentTimeText.text = formatTime(currentPosition)
            }
        }
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
        
        // Hide normal video progress controls and show fullscreen ones
        binding.videoSeekBar.visibility = View.GONE
        binding.currentTimeText.visibility = View.GONE
        binding.totalTimeText.visibility = View.GONE
        binding.fullscreenProgressControls.visibility = View.VISIBLE
        
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
        
        // Only show related videos if they were visible before fullscreen
        val relatedVideos = viewModel.videoDetailsLiveData.value?.data?.data?.relatedVideos ?: emptyList()
        if (relatedVideos.isNotEmpty()) {
            binding.relatedVideosLabel.visibility = View.VISIBLE
            binding.relatedVideosRecyclerView.visibility = View.VISIBLE
        }
        
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

    override fun onDestroy() {
        super.onDestroy()
        progressHandler.removeCallbacks(progressRunnable)
        autoHideHandler.removeCallbacks(autoHideRunnable)
    }

    private fun startAutoHideTimer() {
        autoHideHandler.removeCallbacks(autoHideRunnable)
        autoHideHandler.postDelayed(autoHideRunnable, 3000) // Auto-hide after 3 seconds
    }
}
