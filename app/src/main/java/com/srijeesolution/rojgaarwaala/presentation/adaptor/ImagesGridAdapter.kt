package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.databinding.ItemImageGridBinding

class ImagesGridAdapter(
    private val images: List<ImageData>,
    private val onImageClick: (ImageData) -> Unit
) : RecyclerView.Adapter<ImagesGridAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemImageGridBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(image: ImageData) {
            binding.apply {
                // Set title
                imageTitle.text = image.title ?: ""
                
                // Load image using Glide
                val imageUrl = image.imageUrl ?: ""
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