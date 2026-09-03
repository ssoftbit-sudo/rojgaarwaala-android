package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.CircleStory
import com.srijeesolution.rojgaarwaala.databinding.ActivityStoryViewerBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.DeviceKeyUtils
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
@UnstableApi
class StoryViewerActivity : AppCompatActivity(),
    com.srijeesolution.rojgaarwaala.utils.ManualEdgeToEdge {

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
    private var storyDurationMs = IMAGE_STORY_DURATION_MS
    private lateinit var deviceKey: String
    private var autoAdvanceStarted = false
    private var storiesUpdated = false
    private var pendingLikeStoryId: Int? = null
    private var fullDescription: String = ""
    private var descriptionExpanded = false
    private var progressPausedForReadMore = false
    private var pausedElapsedMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        applySystemBarInsets()

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

        binding.storyBackButton.setOnClickListener { finishWithResult() }
        binding.storyCloseButton.setOnClickListener { finishWithResult() }
        binding.storyLikeButton.setOnClickListener { toggleStoryLike() }
        binding.storyReadMoreButton.setOnClickListener { toggleDescriptionExpanded() }
        onBackPressedDispatcher.addCallback(this) {
            finishWithResult()
        }

        observeStoryLikeResponses()
        showStoryAt(currentIndex)
    }

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.storyTopChrome.setPadding(
                binding.storyTopChrome.paddingLeft,
                bars.top,
                binding.storyTopChrome.paddingRight,
                binding.storyTopChrome.paddingBottom
            )
            binding.storyBottomOverlay.setPadding(
                binding.storyBottomOverlay.paddingLeft,
                binding.storyBottomOverlay.paddingTop,
                binding.storyBottomOverlay.paddingRight,
                bars.bottom.coerceAtLeast(dp(12))
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun observeStoryLikeResponses() {
        viewModel.likeStoryLiveData.observe(this, Observer { result ->
            handleStoryLikeApiResult(result, expectedLiked = true)
        })
        viewModel.unlikeStoryLiveData.observe(this, Observer { result ->
            handleStoryLikeApiResult(result, expectedLiked = false)
        })
        viewModel.storyLikeStatusLiveData.observe(this, Observer { result ->
            if (result is ApiResult.Success && result.data?.status == true) {
                applyLikeStateFromApi(result.data.data)
            }
        })
    }

    private fun handleStoryLikeApiResult(
        result: ApiResult<com.srijeesolution.rojgaarwaala.data.remote.model.StoryLikeApiModel>,
        expectedLiked: Boolean
    ) {
        val storyId = pendingLikeStoryId
        pendingLikeStoryId = null
        binding.storyLikeButton.isEnabled = true

        when (result) {
            is ApiResult.Success -> {
                if (result.data?.status == true) {
                    applyLikeStateFromApi(result.data.data)
                } else {
                    Toast.makeText(this, result.data?.message ?: "Could not update like", Toast.LENGTH_SHORT).show()
                }
            }
            is ApiResult.Error -> {
                Toast.makeText(this, "Could not update like", Toast.LENGTH_SHORT).show()
                storyId?.let { refreshStoryLikeStatus(it) }
            }
            else -> Unit
        }
    }

    private fun applyLikeStateFromApi(
        data: com.srijeesolution.rojgaarwaala.data.remote.model.StoryReactionData?
    ) {
        val storyId = data?.storyId ?: return
        val index = stories.indexOfFirst { it.id == storyId }
        if (index >= 0) {
            stories[index] = stories[index].copy(
                likeCount = data.likeCount ?: stories[index].likeCount,
                isLiked = data.isLiked ?: stories[index].isLiked
            )
            if (index == currentIndex) {
                updateLikeButton(stories[index])
            }
        }
    }

    private fun showStoryAt(index: Int) {
        stopCurrentStory()
        autoAdvanceStarted = false

        if (index >= stories.size) {
            finishWithResult()
            return
        }

        currentIndex = index
        val story = stories[index]
        updateStoryHeader(story)
        updateStoryOverlay(story)
        updateLikeButton(story)
        viewModel.markStoryViewed(story.id ?: return, deviceKey)
        storiesUpdated = true

        if (isLoggedIn()) {
            refreshStoryLikeStatus(story.id)
        }

        when (story.mediaType) {
            MEDIA_VIDEO -> showVideoStory(story)
            MEDIA_LINK -> showLinkStory(story)
            else -> showImageStory(story)
        }
    }

    private fun updateStoryHeader(story: CircleStory) {
        val author = story.createdBy?.trim().takeUnless { it.isNullOrBlank() } ?: "Rojgaarwaala"
        binding.storyAuthorName.text = author
        binding.storyAvatarText.text = author.firstOrNull()?.uppercaseChar()?.toString() ?: "R"
        binding.storyTimeText.text = formatStoryTime(story.publishDate ?: story.createdAt)
    }

    private fun updateStoryOverlay(story: CircleStory) {
        val title = story.title?.trim().orEmpty()
        fullDescription = story.description?.trim().orEmpty()
        descriptionExpanded = false
        progressPausedForReadMore = false

        if (title.isBlank()) {
            binding.storyTitleText.visibility = View.GONE
        } else {
            binding.storyTitleText.visibility = View.VISIBLE
            binding.storyTitleText.text = title
        }

        renderDescriptionCollapsed()
    }

    private fun renderDescriptionCollapsed() {
        if (fullDescription.isBlank()) {
            binding.storyDescriptionScroll.visibility = View.GONE
            binding.storyDescriptionText.visibility = View.GONE
            binding.storyReadMoreButton.visibility = View.GONE
            binding.storyDescriptionScroll.layoutParams = binding.storyDescriptionScroll.layoutParams.apply {
                height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            }
            return
        }

        binding.storyDescriptionScroll.visibility = View.VISIBLE
        binding.storyDescriptionText.visibility = View.VISIBLE
        binding.storyDescriptionScroll.layoutParams = binding.storyDescriptionScroll.layoutParams.apply {
            height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        }

        binding.storyDescriptionText.maxLines = DESCRIPTION_COLLAPSED_LINES
        binding.storyDescriptionText.ellipsize = android.text.TextUtils.TruncateAt.END
        binding.storyDescriptionText.text = fullDescription

        binding.storyDescriptionText.post {
            val needsReadMore = binding.storyDescriptionText.lineCount > DESCRIPTION_COLLAPSED_LINES ||
                fullDescription.length > DESCRIPTION_COLLAPSE_CHAR_LIMIT ||
                fullDescription.lines().size > DESCRIPTION_COLLAPSED_LINES
            if (needsReadMore) {
                binding.storyReadMoreButton.visibility = View.VISIBLE
                binding.storyReadMoreButton.text = "Read more"
            } else {
                binding.storyReadMoreButton.visibility = View.GONE
            }
        }
    }

    private fun toggleDescriptionExpanded() {
        descriptionExpanded = !descriptionExpanded
        if (descriptionExpanded) {
            binding.storyDescriptionText.maxLines = Integer.MAX_VALUE
            binding.storyDescriptionText.ellipsize = null
            binding.storyDescriptionText.text = fullDescription
            binding.storyReadMoreButton.text = "Read less"
            val maxHeight = (resources.displayMetrics.heightPixels * 0.38f).toInt()
            binding.storyDescriptionScroll.layoutParams = binding.storyDescriptionScroll.layoutParams.apply {
                height = maxHeight
            }
            pauseStoryForReadMore()
        } else {
            renderDescriptionCollapsed()
            resumeStoryAfterReadMore()
        }
        binding.storyDescriptionScroll.requestLayout()
    }

    private fun pauseStoryForReadMore() {
        if (progressPausedForReadMore) return
        progressPausedForReadMore = true
        pausedElapsedMs = (System.currentTimeMillis() - storyStartTime).coerceAtLeast(0L)
        progressRunnable?.let { handler.removeCallbacks(it) }
        exoPlayer?.pause()
    }

    private fun resumeStoryAfterReadMore() {
        if (!progressPausedForReadMore) return
        progressPausedForReadMore = false
        storyStartTime = System.currentTimeMillis() - pausedElapsedMs
        exoPlayer?.playWhenReady = true
        startAutoAdvance(resumeFromPaused = true)
    }

    private fun updateLikeButton(story: CircleStory) {
        val liked = story.isLiked == true
        binding.storyLikeButton.setImageResource(
            if (liked) R.drawable.ic_story_like_filled else R.drawable.ic_story_like
        )

        val count = story.likeCount ?: 0
        if (count > 0) {
            binding.storyLikeCountText.visibility = View.VISIBLE
            binding.storyLikeCountText.text = count.toString()
        } else {
            binding.storyLikeCountText.visibility = View.GONE
        }
    }

    private fun toggleStoryLike() {
        if (!isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            Toast.makeText(this, "Please login to like stories", Toast.LENGTH_SHORT).show()
            return
        }

        val storyId = stories.getOrNull(currentIndex)?.id ?: return
        val currentlyLiked = stories[currentIndex].isLiked == true
        pendingLikeStoryId = storyId
        binding.storyLikeButton.isEnabled = false

        if (currentlyLiked) {
            viewModel.unlikeStory(storyId)
        } else {
            viewModel.likeStory(storyId)
        }
    }

    private fun refreshStoryLikeStatus(storyId: Int?) {
        if (storyId == null || !isLoggedIn()) return
        viewModel.getStoryLikeStatus(storyId)
    }

    private fun isLoggedIn(): Boolean {
        return sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)
    }

    private fun formatStoryTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""

        return try {
            val parsers = listOf(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            )
            val date = parsers.firstNotNullOfOrNull { parser ->
                runCatching { parser.parse(raw) }.getOrNull()
            } ?: return raw

            val today = Calendar.getInstance()
            val storyCal = Calendar.getInstance().apply { time = date }
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

            if (today.get(Calendar.YEAR) == storyCal.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == storyCal.get(Calendar.DAY_OF_YEAR)
            ) {
                "Today, ${timeFormat.format(date)}"
            } else {
                SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(date)
            }
        } catch (_: Exception) {
            raw
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

        // fitCenter keeps full poster visible (centerCrop was zooming/cropping permanent ads)
        Glide.with(this)
            .load(imageUrl)
            .fitCenter()
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

        storyDurationMs = IMAGE_STORY_DURATION_MS
        startAutoAdvance()
    }

    private fun showStoryLoadError(story: CircleStory, message: String) {
        binding.storyImageView.visibility = View.GONE
        binding.storyErrorText.visibility = View.VISIBLE
        binding.storyErrorText.text = buildString {
            append(message)
            story.title?.takeIf { it.isNotBlank() }?.let { append("\n\n").append(it) }
        }
        storyDurationMs = IMAGE_STORY_DURATION_MS
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
                    when (playbackState) {
                        Player.STATE_READY -> {
                            if (!autoAdvanceStarted) {
                                autoAdvanceStarted = true
                                storyDurationMs = player.duration
                                    .takeIf { it > 0 }
                                    ?: FALLBACK_VIDEO_DURATION_MS
                                startAutoAdvance()
                            }
                        }
                        Player.STATE_ENDED -> advanceToNextStory()
                    }
                }
            })
        }

        handler.postDelayed({
            if (!autoAdvanceStarted) {
                autoAdvanceStarted = true
                storyDurationMs = FALLBACK_VIDEO_DURATION_MS
                startAutoAdvance()
            }
        }, 1500L)
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
        storyDurationMs = IMAGE_STORY_DURATION_MS
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

    private fun startAutoAdvance(resumeFromPaused: Boolean = false) {
        if (!resumeFromPaused) {
            storyStartTime = System.currentTimeMillis()
            binding.storyProgressBar.progress = 0
        }

        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = object : Runnable {
            override fun run() {
                if (progressPausedForReadMore) return
                val elapsed = System.currentTimeMillis() - storyStartTime
                val progress = ((elapsed.toFloat() / storyDurationMs) * 1000).toInt().coerceIn(0, 1000)
                binding.storyProgressBar.progress = progress

                if (elapsed >= storyDurationMs) {
                    if (stories.getOrNull(currentIndex)?.mediaType != MEDIA_VIDEO) {
                        advanceToNextStory()
                    }
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
        progressPausedForReadMore = false
        descriptionExpanded = false
        binding.storyErrorText.visibility = View.GONE

        exoPlayer?.release()
        exoPlayer = null
        binding.storyPlayerView.player = null
        binding.storyWebView.stopLoading()
        binding.storyWebView.loadUrl("about:blank")
    }

    private fun finishWithResult() {
        if (storiesUpdated) {
            setResult(RESULT_OK)
        }
        finish()
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
        private const val IMAGE_STORY_DURATION_MS = 10_000L
        private const val FALLBACK_VIDEO_DURATION_MS = 30_000L
        private const val MEDIA_VIDEO = "video"
        private const val MEDIA_LINK = "link"
        private const val DESCRIPTION_COLLAPSED_LINES = 3
        private const val DESCRIPTION_COLLAPSE_CHAR_LIMIT = 120
    }
}
