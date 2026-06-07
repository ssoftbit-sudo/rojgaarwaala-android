package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage
import com.srijeesolution.rojgaarwaala.databinding.ActivityImageViewerBinding
import com.srijeesolution.rojgaarwaala.utils.LocationDisplayUtils
import com.srijeesolution.rojgaarwaala.utils.LocationSuggestions
import com.srijeesolution.rojgaarwaala.utils.TimeUtils
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private var scheduledImage: ScheduledImage? = null
    private var imageList: ArrayList<ScheduledImage>? = null
    private var currentIndex: Int = 0
    private var descriptionExpanded = false
    private var imageCategory: String? = null
    private var imageLocation: String? = null
    private var likeCount = 0
    private var dislikeCount = 0

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setupEdgeToEdge()

        imageCategory = intent.getStringExtra(EXTRA_IMAGE_CATEGORY)
        imageLocation = intent.getStringExtra(EXTRA_IMAGE_LOCATION)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    binding.fullScreenImageView.resetZoom()
                    finish()
                }
            }
        )

        if (intent.hasExtra("scheduled_images")) {
            imageList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra("scheduled_images", ScheduledImage::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra("scheduled_images")
            }
            currentIndex = intent.getIntExtra("current_index", 0)

            if (imageList != null && imageList!!.isNotEmpty()) {
                if (currentIndex < 0 || currentIndex >= imageList!!.size) {
                    currentIndex = 0
                }
                scheduledImage = imageList!![currentIndex]
            }
        } else {
            scheduledImage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("scheduled_image", ScheduledImage::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("scheduled_image")
            }
        }

        setupToolbar()
        setupImage()
        setupClickListeners()
        setupNavigationArrows()

        binding.fullScreenImageView.viewTreeObserver.addOnGlobalLayoutListener {
            binding.fullScreenImageView.resetZoom()
        }
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_background)
        ViewCompat.setOnApplyWindowInsetsListener(binding.imageViewerRoot) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }
    }

    private fun setupToolbar() {
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
            } else {
                false
            }
        }
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    /** Stable key for prefs — uses image id or path hash when id is missing. */
    private fun currentImageKey(): Int {
        val id = scheduledImage?.id
        if (id != null && id > 0) return id
        val path = scheduledImage?.imagePath?.trim().orEmpty()
        if (path.isNotEmpty()) return path.hashCode()
        return "image_$currentIndex".hashCode()
    }

    private fun resolveImageLocation(image: ScheduledImage): String {
        image.location?.trim()?.takeIf { it.isNotEmpty() }?.let {
            return LocationDisplayUtils.formatForDisplay(it)
        }
        imageLocation?.trim()?.takeIf { it.isNotEmpty() }?.let {
            return LocationDisplayUtils.formatForDisplay(it)
        }
        val desc = image.description.orEmpty()
        Regex("(?i)(?:location|loc\\.?)\\s*:\\s*([^\\n\\r]+)").find(desc)?.groupValues
            ?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let {
                return LocationDisplayUtils.formatForDisplay(it)
            }
        val lower = desc.lowercase()
        for (city in LocationSuggestions.districtList) {
            if (lower.contains(city.lowercase())) return city
        }
        return ""
    }

    private fun loadReactionCountsFromPrefs() {
        val key = currentImageKey()
        likeCount = sharedPrefs.getImageLikeCount(key)
        dislikeCount = sharedPrefs.getImageDislikeCount(key)
    }

    private fun saveReactionCountsToPrefs() {
        val key = currentImageKey()
        sharedPrefs.setImageLikeCount(key, likeCount)
        sharedPrefs.setImageDislikeCount(key, dislikeCount)
    }

    private fun setupImage() {
        scheduledImage?.let { image ->
            binding.videoDetailTitle.text = image.title.orEmpty()

            val category = imageCategory?.trim().orEmpty()
            if (category.isNotEmpty()) {
                binding.videoSubtitle.visibility = View.VISIBLE
                binding.videoSubtitle.text = category
            } else {
                binding.videoSubtitle.visibility = View.GONE
            }

            val publishMeta = TimeUtils.formatPublishMeta(
                this,
                image.publishDate,
                image.createdAt
            )
            binding.uploadTimeLabel.text = publishMeta.ifEmpty { "Recently posted" }

            val location = resolveImageLocation(image)
            if (location.isNotEmpty()) {
                binding.videoLocationLabel.visibility = View.VISIBLE
                binding.videoLocationLabel.text = location
            } else {
                binding.videoLocationLabel.visibility = View.GONE
            }

            loadReactionCountsFromPrefs()
            refreshLikeDislikeUi()

            binding.progressBar.visibility = View.VISIBLE
            Glide.with(this)
                .load(image.imagePath)
                .placeholder(R.drawable.no_image_placeholder)
                .error(R.drawable.no_image_placeholder)
                .listener(
                    object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.progressBar.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.progressBar.visibility = View.GONE
                            return false
                        }
                    }
                )
                .into(binding.fullScreenImageView)

            binding.fullScreenImageView.postDelayed({
                binding.fullScreenImageView.resetZoom()
            }, 100)
        }
    }

    private fun refreshLikeDislikeUi() {
        val key = currentImageKey()
        binding.likeCount.text = likeCount.toString()
        binding.dislikeCount.text = dislikeCount.toString()
        val isLiked = sharedPrefs.isImageLiked(key)
        val isDisliked = sharedPrefs.isImageDisliked(key)
        if (isLiked) {
            binding.likeButton.setImageResource(R.drawable.ic_thumb_up_filled)
            binding.likeButton.alpha = 1f
        } else {
            binding.likeButton.setImageResource(R.drawable.ic_thumb_up_outline)
            binding.likeButton.alpha = 0.7f
        }
        if (isDisliked) {
            binding.dislikeButton.setImageResource(R.drawable.ic_thumb_down_filled)
            binding.dislikeButton.alpha = 1f
        } else {
            binding.dislikeButton.setImageResource(R.drawable.ic_thumb_down_outline)
            binding.dislikeButton.alpha = 0.7f
        }
    }

    private fun handleLikeAction() {
        val key = currentImageKey()
        if (!sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            Toast.makeText(this, "Please login to like", Toast.LENGTH_SHORT).show()
            return
        }
        val wasLiked = sharedPrefs.isImageLiked(key)
        val wasDisliked = sharedPrefs.isImageDisliked(key)
        if (wasLiked) {
            likeCount = max(0, likeCount - 1)
            sharedPrefs.setImageLiked(key, false)
        } else {
            likeCount += 1
            sharedPrefs.setImageLiked(key, true)
            if (wasDisliked) {
                dislikeCount = max(0, dislikeCount - 1)
                sharedPrefs.setImageDisliked(key, false)
            }
        }
        saveReactionCountsToPrefs()
        refreshLikeDislikeUi()
    }

    private fun handleDislikeAction() {
        val key = currentImageKey()
        if (!sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            Toast.makeText(this, "Please login to dislike", Toast.LENGTH_SHORT).show()
            return
        }
        val wasLiked = sharedPrefs.isImageLiked(key)
        val wasDisliked = sharedPrefs.isImageDisliked(key)
        if (wasDisliked) {
            dislikeCount = max(0, dislikeCount - 1)
            sharedPrefs.setImageDisliked(key, false)
        } else {
            dislikeCount += 1
            sharedPrefs.setImageDisliked(key, true)
            if (wasLiked) {
                likeCount = max(0, likeCount - 1)
                sharedPrefs.setImageLiked(key, false)
            }
        }
        saveReactionCountsToPrefs()
        refreshLikeDislikeUi()
    }

    private fun openApplyForImage() {
        val id = scheduledImage?.id ?: 0
        if (id <= 0) {
            Toast.makeText(this, "Image details are missing", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, ApplyFormActivity::class.java).apply {
                putExtra("scheduled_image_id", id)
                putExtra("video_title", scheduledImage?.title ?: "Job Opportunity")
            }
        )
    }

    private fun dialPosterPhone() {
        val phone = scheduledImage?.phoneNumber?.takeIf { it.isNotBlank() }
        if (phone == null) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phone") })
    }

    private fun setupClickListeners() {
        binding.fullScreenImageView.setOnSwipeListener { isLeftSwipe ->
            if (isLeftSwipe) navigateToNext() else navigateToPrevious()
        }

        binding.leftArrowButton.setOnClickListener { navigateToPrevious() }
        binding.rightArrowButton.setOnClickListener { navigateToNext() }

        binding.likeColumn.setOnClickListener { handleLikeAction() }
        binding.dislikeColumn.setOnClickListener { handleDislikeAction() }
        binding.shareButton.setOnClickListener { shareImage() }
        binding.inlineCallColumn.setOnClickListener { dialPosterPhone() }
        binding.inlineApplyColumn.setOnClickListener { openApplyForImage() }
    }

    private fun setupNavigationArrows() {
        val hasMultipleImages = imageList != null && imageList!!.size > 1
        if (hasMultipleImages) {
            binding.leftArrowButton.visibility = View.VISIBLE
            binding.rightArrowButton.visibility = View.VISIBLE
            updateArrowVisibility()
        } else {
            binding.leftArrowButton.visibility = View.GONE
            binding.rightArrowButton.visibility = View.GONE
        }
    }

    private fun updateArrowVisibility() {
        binding.leftArrowButton.visibility =
            if (currentIndex > 0) View.VISIBLE else View.GONE
        binding.rightArrowButton.visibility =
            if (imageList != null && currentIndex < imageList!!.size - 1) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun navigateToPrevious() {
        if (imageList != null && currentIndex > 0) {
            currentIndex--
            loadImageAtIndex(currentIndex)
        }
    }

    private fun navigateToNext() {
        if (imageList != null && currentIndex < imageList!!.size - 1) {
            currentIndex++
            loadImageAtIndex(currentIndex)
        }
    }

    private fun loadImageAtIndex(index: Int) {
        if (imageList != null && index >= 0 && index < imageList!!.size) {
            binding.fullScreenImageView.resetZoom()
            scheduledImage = imageList!![index]
            setupImage()
            updateArrowVisibility()
        }
    }

    private fun shareImage() {
        scheduledImage?.let { image ->
            val title = image.title ?: "Check out this image!"
            val description = image.description ?: ""
            val imageUrl = "https://www.rojgaarwaala.com/${image.imagePath?.replace("\\/", "/")}"
            val appDetails =
                "\n\nView this image on Rojgaarwaala! Download the app: https://rojgaarwaala.com"
            val shareText = "$title\n$description\n$imageUrl$appDetails"

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(sendIntent, "Share image via"))
        }
    }

    companion object {
        const val EXTRA_IMAGE_CATEGORY = "extra_image_category"
        const val EXTRA_IMAGE_LOCATION = "extra_image_location"
    }
}
