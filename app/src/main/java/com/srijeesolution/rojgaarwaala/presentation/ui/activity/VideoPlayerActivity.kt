package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private val viewModel: HomePageViewModel by viewModels()
    private var videoId: Int = -1

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
        binding.likeButton.setOnClickListener {
            Toast.makeText(this, "Liked!", Toast.LENGTH_SHORT).show()
        }
        binding.dislikeButton.setOnClickListener {
            Toast.makeText(this, "Disliked!", Toast.LENGTH_SHORT).show()
        }
        binding.shareButton.setOnClickListener {
            Toast.makeText(this, "Share clicked!", Toast.LENGTH_SHORT).show()
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
        binding.likeCount.text = (data.likes ?: 0).toString()
        binding.dislikeCount.text = (data.unlikes ?: 0).toString()
        binding.viewsCount.text = "${data.views ?: 0} views"
        // Related videos
        val related = data.relatedVideos ?: emptyList()
        (binding.relatedVideosRecyclerView.adapter as? VideoAdapter)?.let {
            binding.relatedVideosRecyclerView.adapter = VideoAdapter(related)
        } ?: run {
            binding.relatedVideosRecyclerView.adapter = VideoAdapter(related)
        }
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
            Toast.makeText(this, "Fullscreen coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
