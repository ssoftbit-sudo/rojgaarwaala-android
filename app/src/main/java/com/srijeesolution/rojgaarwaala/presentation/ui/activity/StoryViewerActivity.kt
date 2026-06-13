package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.srijeesolution.rojgaarwaala.data.remote.model.CircleStory
import com.srijeesolution.rojgaarwaala.databinding.ActivityStoryViewerBinding
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.DeviceKeyUtils
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@UnstableApi
class StoryViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoryViewerBinding
    private val viewModel: HomePageViewModel by viewModels()

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private var stories: ArrayList<CircleStory> = arrayListOf()
    private var currentIndex = 0
    private var exoPlayer: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var storyStartTime = 0L
    private var storyDurationMs = STORY_DURATION_MS
    private lateinit var deviceKey: String
    private var autoAdvanceStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        deviceKey = DeviceKeyUtils.getOrCreateDeviceKey(sharedPrefs)
        stories = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_STORIES, CircleStory::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_STORIES)
        } ?: arrayListOf()

        currentIndex = intent.getIntExtra(EXTRA_START_INDEX, 0).coerceIn(0, (stories.size - 1).coerceAtLeast(0))

        if (stories.isEmpty()) {
            finish()
            return
        }

        binding.storyCloseButton.setOnClickListener { finish() }
        showStoryAt(currentIndex)
    }

    private fun showStoryAt(index: Int) {
        stopCurrentStory()
        autoAdvanceStarted = false

        if (index >= stories.size) {
            finish()
            return
        }

        currentIndex = index
        val story = stories[index]
        binding.storyTitleText.text = story.title ?: ""
        viewModel.markStoryViewed(story.id ?: return, deviceKey)

        when (story.mediaType) {
            MEDIA_VIDEO -> showVideoStory(story)
            MEDIA_LINK -> showLinkStory(story)
            else -> showImageStory(story)
        }
    }

    private fun showImageStory(story: CircleStory) {
        binding.storyPlayerView.visibility = View.GONE
        binding.storyWebView.visibility = View.GONE
        binding.storyErrorText.visibility = View.GONE
        binding.storyImageView.visibility = View.VISIBLE

        val imageUrl = story.imageUrl ?: story.thumbnailUrl
        if (imageUrl.isNullOrBlank()) {
            showStoryLoadError(story, "Image unavailable")
            return
        }

        Glide.with(this)
            .load(imageUrl)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    showStoryLoadError(story, "Could not load image")
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.storyErrorText.visibility = View.GONE
                    binding.storyImageView.visibility = View.VISIBLE
                    return false
                }
            })
            .into(binding.storyImageView)

        storyDurationMs = STORY_DURATION_MS
        startAutoAdvance()
    }

    private fun showStoryLoadError(story: CircleStory, message: String) {
        binding.storyImageView.visibility = View.GONE
        binding.storyErrorText.visibility = View.VISIBLE
        binding.storyErrorText.text = buildString {
            append(message)
            story.title?.takeIf { it.isNotBlank() }?.let { append("\n\n").append(it) }
        }
        storyDurationMs = STORY_DURATION_MS
        startAutoAdvance()
    }

    private fun showVideoStory(story: CircleStory) {
        binding.storyImageView.visibility = View.GONE
        binding.storyPlayerView.visibility = View.VISIBLE
        binding.storyWebView.visibility = View.GONE

        val videoUrl = story.videoUrl
        if (videoUrl.isNullOrBlank()) {
            Toast.makeText(this, "Video unavailable", Toast.LENGTH_SHORT).show()
            advanceToNextStory()
            return
        }

        exoPlayer = ExoPlayer.Builder(this).build().also { player ->
            binding.storyPlayerView.player = player
            player.setMediaItem(MediaItem.fromUri(videoUrl))
            player.prepare()
            player.playWhenReady = true
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (!autoAdvanceStarted && playbackState == Player.STATE_READY) {
                        autoAdvanceStarted = true
                        storyDurationMs = if (player.duration > 0) {
                            minOf(STORY_DURATION_MS, player.duration)
                        } else {
                            STORY_DURATION_MS
                        }
                        startAutoAdvance()
                    }
                }
            })
        }

        handler.postDelayed({
            if (!autoAdvanceStarted) {
                autoAdvanceStarted = true
                storyDurationMs = STORY_DURATION_MS
                startAutoAdvance()
            }
        }, 500L)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showLinkStory(story: CircleStory) {
        binding.storyImageView.visibility = View.GONE
        binding.storyPlayerView.visibility = View.GONE
        binding.storyWebView.visibility = View.VISIBLE

        binding.storyWebView.settings.javaScriptEnabled = true
        binding.storyWebView.webViewClient = WebViewClient()

        val url = story.linkUrl
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "Link unavailable", Toast.LENGTH_SHORT).show()
            advanceToNextStory()
            return
        }

        binding.storyWebView.loadUrl(normalizeLinkUrl(url))
        storyDurationMs = STORY_DURATION_MS
        startAutoAdvance()
    }

    private fun normalizeLinkUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun startAutoAdvance() {
        storyStartTime = System.currentTimeMillis()
        binding.storyProgressBar.progress = 0

        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - storyStartTime
                val progress = ((elapsed.toFloat() / storyDurationMs) * 1000).toInt().coerceIn(0, 1000)
                binding.storyProgressBar.progress = progress

                if (elapsed >= storyDurationMs) {
                    advanceToNextStory()
                } else {
                    handler.postDelayed(this, 50L)
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun advanceToNextStory() {
        showStoryAt(currentIndex + 1)
    }

    private fun stopCurrentStory() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
        binding.storyErrorText.visibility = View.GONE

        exoPlayer?.release()
        exoPlayer = null
        binding.storyPlayerView.player = null
        binding.storyWebView.stopLoading()
        binding.storyWebView.loadUrl("about:blank")
    }

    override fun onStop() {
        exoPlayer?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        stopCurrentStory()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_STORIES = "circle_stories"
        const val EXTRA_START_INDEX = "start_index"
        private const val STORY_DURATION_MS = 10_000L
        private const val MEDIA_VIDEO = "video"
        private const val MEDIA_LINK = "link"
    }
}
