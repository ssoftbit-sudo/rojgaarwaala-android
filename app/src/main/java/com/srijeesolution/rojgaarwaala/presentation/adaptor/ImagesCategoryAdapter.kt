package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem
import com.srijeesolution.rojgaarwaala.databinding.ItemImageCategoryBinding

class ImagesCategoryAdapter(
    private val categories: List<ImageSubItem>,
    private val onImageClick: (ImageSubItem) -> Unit,
    private val onViewAllClick: (ImageSubItem) -> Unit
) : RecyclerView.Adapter<ImagesCategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(private val binding: ItemImageCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: ImageSubItem) {
            binding.apply {
                // Set category title
                categoryTitle.text = category.title ?: ""
                
                // Setup View All click listener
                categoryViewAll.setOnClickListener {
                    onViewAllClick(category)
                }
                
                // Setup nested RecyclerView for images with GridLayoutManager
                imagesRecyclerView.layoutManager = GridLayoutManager(itemView.context, 3)
                
                val imagesAdapter = ImagesGridAdapter(category.images ?: emptyList()) { image ->
                    // Handle image click - convert to ImageSubItem for compatibility
                    val tempCategory = ImageSubItem(
                        id = category.id,
                        title = category.title,
                        iconFile = category.iconFile,
                        images = listOf(image)
                    )
                    onImageClick(tempCategory)
                }
                
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