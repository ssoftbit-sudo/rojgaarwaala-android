package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.databinding.ItemImageGridBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.ApplyFormActivity
import com.srijeesolution.rojgaarwaala.utils.TimeUtils

class ImagesGridAdapter(
    private val onImageClick: (ImageData, Int) -> Unit
) : ListAdapter<ImageData, ImagesGridAdapter.ImageViewHolder>(DIFF_CALLBACK) {

    inner class ImageViewHolder(private val binding: ItemImageGridBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(image: ImageData) {
            binding.apply {
                // Set title
                imageTitle.text = image.title ?: ""
                
                // Set relative time
                imageTime.text = TimeUtils.getRelativeTimeSpanString(root.context, image.publishDate)
                
                // Load image using Glide with optimizations for smooth scrolling
                val imageUrl = image.imageUrl ?: ""
                if (imageUrl.isNotEmpty()) {
                    Glide.with(imageView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.no_image_placeholder)
                        .error(R.drawable.no_image_placeholder)
                        .centerCrop()
                        .dontAnimate() // Disable animations for smoother scrolling
                        .skipMemoryCache(false) // Use memory cache for better performance
                        .into(imageView)
                } else {
                    // Set placeholder if no image
                    imageView.setImageResource(R.drawable.no_image_placeholder)
                }
                
                // Set click listener
                root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onImageClick(image, position)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ImageData>() {
            override fun areItemsTheSame(oldItem: ImageData, newItem: ImageData): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ImageData, newItem: ImageData): Boolean {
                return oldItem == newItem
            }
        }
    }
} 