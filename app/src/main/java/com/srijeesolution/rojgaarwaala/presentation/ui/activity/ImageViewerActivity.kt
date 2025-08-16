package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.ActivityImageViewerBinding
import com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private var scheduledImage: ScheduledImage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get image data from intent
        scheduledImage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("scheduled_image", ScheduledImage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("scheduled_image")
        }
        
        setupToolbar()
        setupImage()
        setupClickListeners()
        
        // Ensure proper initial scaling after view is fully laid out
        binding.fullScreenImageView.viewTreeObserver.addOnGlobalLayoutListener {
            binding.fullScreenImageView.resetZoom()
        }
    }

    private fun setupToolbar() {
    }

    private fun setupImage() {
        scheduledImage?.let { image ->
            // Set title
            binding.imageTitle.text = image.title ?: ""
            binding.imageDescription.text = image.description ?: ""
            
            // Load full-screen image
            val imageUrl = "https://www.rojgaarwaala.com/${image.imagePath?.replace("\\/", "/")}"
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