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
        // Set initial like/dislike states from SharedPreferences
        updateLikeDislikeUI()
        updateLikeDislikeCounts()

        binding.likeButton.setOnClickListener {
            if (!sharedPrefs.isVideoLiked(videoId)) {
                viewModel.likeVideo(videoId)
                sharedPrefs.setVideoLiked(videoId, true)
                if (sharedPrefs.isVideoDisliked(videoId)) {
                    sharedPrefs.setVideoDisliked(videoId, false)
                    dislikeCount = (dislikeCount - 1).coerceAtLeast(0)
                }
                likeCount += 1
                updateLikeDislikeUI()
                updateLikeDislikeCounts()
            }
        }
        binding.dislikeButton.setOnClickListener {
            if (!sharedPrefs.isVideoDisliked(videoId)) {
                viewModel.unlikeVideo(videoId)
                sharedPrefs.setVideoDisliked(videoId, true)
                if (sharedPrefs.isVideoLiked(videoId)) {
                    sharedPrefs.setVideoLiked(videoId, false)
                    likeCount = (likeCount - 1).coerceAtLeast(0)
                }
                dislikeCount += 1
                updateLikeDislikeUI()
                updateLikeDislikeCounts()
            }
        }
        binding.shareButton.setOnClickListener {
            shareVideo()
        }
    }

    private fun updateLikeDislikeCounts() {
        binding.likeCount.text = likeCount.toString()
        binding.dislikeCount.text = dislikeCount.toString()
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
        currentVideoUrl = data.videoUrl
    }

    private fun setupRelatedVideosRecycler() {
        binding.relatedVideosRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.relatedVideosRecyclerView.adapter = VideoAdapter(emptyList())
    }

    private fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be")
    }

    private fun playCustomVideo(url: String) {
        with(binding.customVideoView) {
            visibility = View.VISIBLE
            
            // Ensure video is centered in normal view
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutParams.gravity = android.view.Gravity.CENTER
            this.layoutParams = layoutParams
            
            setVideoURI(Uri.parse(url))
            setOnPreparedListener { it.start() }
            setOnCompletionListener { pause() }
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
        var isPlaying = false

        playPauseButton.setOnClickListener {
            if (videoView.isPlaying) {
                videoView.pause()
                playPauseButton.setImageResource(R.drawable.ic_play_circle)
                playPauseButton.visibility = View.VISIBLE
                isPlaying = false
            } else {
                // Hide thumbnail overlay when user clicks play
                thumbnailOverlay.visibility = View.GONE
                videoView.start()
                playPauseButton.setImageResource(R.drawable.ic_pause_circle)
                playPauseButton.visibility = View.GONE
                isPlaying = true
                // Increment view count only once per session
                if (!hasIncrementedView) {
                    viewModel.incrementVideoView(videoId)
                    binding.viewsCount.text = "${(binding.viewsCount.text.toString().split(" ")[0].toIntOrNull() ?: 0) + 1} views"
                    hasIncrementedView = true
                }
            }
        }

        videoView.setOnPreparedListener {
            // Hide thumbnail and start video
            thumbnailOverlay.visibility = View.GONE
            playPauseButton.setImageResource(R.drawable.ic_play_circle)
            playPauseButton.visibility = View.VISIBLE
            isPlaying = false
        }

        videoView.setOnCompletionListener {
            playPauseButton.setImageResource(R.drawable.ic_play_circle)
            playPauseButton.visibility = View.VISIBLE
            isPlaying = false
        }

        videoView.setOnTouchListener { _, _ ->
            playPauseButton.visibility = if (isPlaying) View.VISIBLE else View.GONE
            false
        }

        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }
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
            super.onBackPressed()
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
    }
}
