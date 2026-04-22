package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem
import com.srijeesolution.rojgaarwaala.databinding.ItemImageCategoryBinding
import com.srijeesolution.rojgaarwaala.utils.SpaceItemDecoration

class ImagesCategoryAdapter(
    private val categories: List<ImageSubItem>,
    private val onImageClick: (ImageSubItem, Int) -> Unit,
    private val onViewAllClick: (ImageSubItem) -> Unit
) : RecyclerView.Adapter<ImagesCategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(private val binding: ItemImageCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: ImageSubItem) {
            binding.apply {
                // Set category title
                categoryTitle.text = category.title ?: ""
                
                // Limit to first 4 images for display
                val allImages = category.images ?: emptyList()
                val displayImages = allImages.take(4)
                
                // Show View All button only if there are more than 4 images
                if (allImages.size > 4) {
                    categoryViewAll.visibility = View.VISIBLE
                    // Setup View All click listener
                    categoryViewAll.setOnClickListener {
                        onViewAllClick(category)
                    }
                } else {
                    categoryViewAll.visibility = View.GONE
                }
                
                // Clear previous adapter and decorations to avoid showing extra items
                imagesRecyclerView.adapter = null
                imagesRecyclerView.layoutManager = null
                imagesRecyclerView.clearOnScrollListeners()
                
                // Setup nested RecyclerView for images with GridLayoutManager (2 columns like stories)
                imagesRecyclerView.layoutManager = GridLayoutManager(itemView.context, 2)
                
                // Remove all existing decorations and add fresh one
                while (imagesRecyclerView.itemDecorationCount > 0) {
                    imagesRecyclerView.removeItemDecorationAt(0)
                }
                imagesRecyclerView.addItemDecoration(SpaceItemDecoration(8, 8))
                
                // Optimize for smooth scrolling
                imagesRecyclerView.setHasFixedSize(true)
                imagesRecyclerView.isNestedScrollingEnabled = false
                
                // Create adapter with exactly 4 items (or fewer if less than 4 available)
                val imagesAdapter = ImagesGridAdapter { image, imageIndex ->
                    // Since displayImages is the first 4 images from allImages, the index matches directly
                    onImageClick(category, imageIndex)
                }
                imagesAdapter.submitList(displayImages)
                imagesRecyclerView.adapter = imagesAdapter
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemImageCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size
} 