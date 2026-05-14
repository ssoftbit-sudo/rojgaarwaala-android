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
import com.srijeesolution.rojgaarwaala.utils.TimeUtils
import java.util.ArrayList

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private var scheduledImage: ScheduledImage? = null
    private var imageList: ArrayList<ScheduledImage>? = null
    private var currentIndex: Int = 0
    private var descriptionExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setupEdgeToEdge()

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

    private fun setupImage() {
        scheduledImage?.let { image ->
            binding.videoDetailTitle.text = image.title.orEmpty()
            val desc = image.description.orEmpty()
            binding.videoDetailDescription.text = desc
            val hasDesc = desc.isNotBlank()
            binding.videoDetailDescription.visibility = if (hasDesc) View.VISIBLE else View.GONE

            val descFirst =
                desc.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
            val titleTrim = image.title?.trim().orEmpty()


            val relativeUploadTime =
                TimeUtils.getRelativeTimeSpanString(this, image.createdAt ?: image.publishDate)
            binding.uploadTimeLabel.text =
                if (relativeUploadTime.isNotEmpty()) {
                    relativeUploadTime
                } else {
                    getString(R.string.channel_placeholder)
                }

            val needsExpand = hasDesc && desc.trim().lines().size > 3
           descriptionExpanded = false
            binding.videoDetailDescription.maxLines = 3

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


    private fun openApplyForImage() {
        val id = scheduledImage?.id ?: 0
        if (id <= 0) {
            Toast.makeText(this, "Image details are missing", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, ApplyFormActivity::class.java).apply {
                putExtra("video_id", id)
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
}
