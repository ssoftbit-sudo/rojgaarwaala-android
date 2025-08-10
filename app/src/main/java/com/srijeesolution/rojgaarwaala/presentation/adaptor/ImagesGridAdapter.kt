package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage
import com.srijeesolution.rojgaarwaala.databinding.ItemImageGridBinding

class ImagesGridAdapter(
    private val images: List<ScheduledImage>,
    private val onImageClick: (ScheduledImage) -> Unit
) : RecyclerView.Adapter<ImagesGridAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemImageGridBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(image: ScheduledImage) {
            binding.apply {
                // Set title
               // imageTitle.text = image.title ?: ""
                
                // Load image using Glide
                val imageUrl = "https://www.rojgaarwaala.com/${image.imagePath?.replace("\\/", "/")}"
                Glide.with(imageView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.no_image_placeholder)
                    .error(R.drawable.no_image_placeholder)
                    .centerCrop()
                    .into(imageView)
                
                // Set click listener
                root.setOnClickListener {
                    onImageClick(image)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size
} 