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
                // Load image using Glide with optimizations for smooth scrolling
                val imageUrl = image.imageUrl ?: ""
                if (imageUrl.isNotEmpty()) {
                    Glide.with(imageView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.no_image_placeholder)
                        .error(R.drawable.no_image_placeholder)
                        .centerCrop()
                        .dontAnimate()
                        .skipMemoryCache(false)
                        .into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.no_image_placeholder)
                }
                val title = image.title.orEmpty()
                imageTitle.text = title
                imageTitle.visibility = if (title.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
                val distance = image.distanceKm?.let { "📍 आपसे $it किमी दूर" }
                val whenPosted = TimeUtils.formatPublishMeta(root.context, image.publishDate, image.createdAt)
                val meta = listOfNotNull(distance, whenPosted.takeIf { it.isNotBlank() }).joinToString(" · ")
                imageTime.text = meta
                imageTime.visibility = if (meta.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
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