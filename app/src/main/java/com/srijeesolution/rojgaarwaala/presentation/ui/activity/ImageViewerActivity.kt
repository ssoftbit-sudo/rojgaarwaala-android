package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prevent screenshots and screen recording
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        // Get image data from intent - support both old (single image) and new (list) formats
        if (intent.hasExtra("scheduled_images")) {
            // New format: list of images with current index
            imageList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra("scheduled_images", ScheduledImage::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra("scheduled_images")
            }
            currentIndex = intent.getIntExtra("current_index", 0)
            
            // Validate index
            if (imageList != null && imageList!!.isNotEmpty()) {
                if (currentIndex < 0 || currentIndex >= imageList!!.size) {
                    currentIndex = 0
                }
                scheduledImage = imageList!![currentIndex]
            }
        } else {
            // Old format: single image (backward compatibility)
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
        
        // Ensure proper initial scaling after view is fully laid out
        binding.fullScreenImageView.viewTreeObserver.addOnGlobalLayoutListener {
            binding.fullScreenImageView.resetZoom()
        }
    }

    private fun setupToolbar() {}

    private fun setupImage() {
        scheduledImage?.let { image ->
            // Set title
            binding.imageTitle.text = image.title ?: ""
            binding.imageDescription.text = image.description ?: ""
            val uploadTime = TimeUtils.getRelativeTimeSpanString(this, image.createdAt ?: image.publishDate)
            binding.imageUploadTime.text = if (uploadTime.isNotEmpty()) uploadTime else "Recently uploaded"
            
            // Load full-screen image
            val imageUrl = image.imagePath ?: ""
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.no_image_placeholder)
                .error(R.drawable.no_image_placeholder)
                .into(binding.fullScreenImageView)
                
            // Reset zoom after a short delay to ensure proper initial scaling
            binding.fullScreenImageView.postDelayed({
                binding.fullScreenImageView.resetZoom()
            }, 100)
        }
    }

    private fun setupClickListeners() {
        // Set up zoomable image view click listener
        binding.fullScreenImageView.setOnImageClickListener {
            toggleUI()
        }
        
        // Set up swipe listener for navigation
        binding.fullScreenImageView.setOnSwipeListener { isLeftSwipe ->
            if (isLeftSwipe) {
                // Swipe left - next image
                navigateToNext()
            } else {
                // Swipe right - previous image
                navigateToPrevious()
            }
        }
        
        // Set up navigation arrow click listeners
        binding.leftArrowButton.setOnClickListener {
            navigateToPrevious()
        }
        
        binding.rightArrowButton.setOnClickListener {
            navigateToNext()
        }

        binding.fullscreenApplyButton.setOnClickListener {
            val applyIntent = Intent(this, ApplyFormActivity::class.java)
            applyIntent.putExtra("video_id", scheduledImage?.id ?: 0)
            applyIntent.putExtra("video_title", scheduledImage?.title ?: "Job Opportunity")
            startActivity(applyIntent)
        }

        binding.fullscreenCallButton.setOnClickListener {
            val phone = scheduledImage?.phoneNumber?.takeIf { it.isNotBlank() }
            if (phone.isNullOrEmpty()) {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            startActivity(dialIntent)
        }

        binding.fullscreenShareButton.setOnClickListener {
            shareImage()
        }
    }
    
    private fun setupNavigationArrows() {
        // Show navigation arrows only if there are multiple images
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
        // Hide left arrow if at first image
        binding.leftArrowButton.visibility = if (currentIndex > 0) View.VISIBLE else View.GONE
        
        // Hide right arrow if at last image
        binding.rightArrowButton.visibility = if (imageList != null && currentIndex < imageList!!.size - 1) View.VISIBLE else View.GONE
    }
    
    private fun navigateToPrevious() {
        if (imageList != null && currentIndex > 0) {
            currentIndex--
            loadImageAtIndex(currentIndex)
            updateArrowVisibility()
        }
    }
    
    private fun navigateToNext() {
        if (imageList != null && currentIndex < imageList!!.size - 1) {
            currentIndex++
            loadImageAtIndex(currentIndex)
            updateArrowVisibility()
        }
    }
    
    private fun loadImageAtIndex(index: Int) {
        if (imageList != null && index >= 0 && index < imageList!!.size) {
            // Reset zoom before loading new image
            binding.fullScreenImageView.resetZoom()
            scheduledImage = imageList!![index]
            setupImage()
        }
    }

    private fun toggleUI() {
       /* val isVisible = binding.toolbar.visibility == View.VISIBLE
        
        if (isVisible) {
            // Hide UI
            binding.toolbar.animate().alpha(0f).setDuration(300).withEndAction {
                binding.toolbar.visibility = View.GONE
            }
            binding.imageInfoContainer.animate().alpha(0f).setDuration(300).withEndAction {
                binding.imageInfoContainer.visibility = View.GONE
            }
            binding.shareButton.animate().alpha(0f).setDuration(300).withEndAction {
                binding.shareButton.visibility = View.GONE
            }
        } else {
            // Show UI
            binding.toolbar.visibility = View.VISIBLE
            binding.toolbar.alpha = 0f
            binding.toolbar.animate().alpha(1f).setDuration(300)
            
            binding.imageInfoContainer.visibility = View.VISIBLE
            binding.imageInfoContainer.alpha = 0f
            binding.imageInfoContainer.animate().alpha(1f).setDuration(300)
            
            binding.shareButton.visibility = View.VISIBLE
            binding.shareButton.alpha = 0f
            binding.shareButton.animate().alpha(1f).setDuration(300)
        }*/
    }

    private fun shareImage() {
        scheduledImage?.let { image ->
            val title = image.title ?: "Check out this image!"
            val description = image.description ?: ""
            val imageUrl = "https://www.rojgaarwaala.com/${image.imagePath?.replace("\\/", "/")}"
            val appDetails = "\n\nView this image on Rojgaarwaala! Download the app: https://rojgaarwaala.com"
            val shareText = "$title\n$description\n$imageUrl$appDetails"
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            startActivity(android.content.Intent.createChooser(intent, "Share image via"))
        }
    }

    override fun onBackPressed() {
        // Reset zoom before going back
        binding.fullScreenImageView.resetZoom()
        super.onBackPressed()
    }
} 