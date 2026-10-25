package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem
import com.srijeesolution.rojgaarwaala.databinding.ItemImageCategoryBinding
import com.srijeesolution.rojgaarwaala.utils.FreeJobFeed
import com.srijeesolution.rojgaarwaala.utils.FreeJobItem

class ImagesCategoryAdapter(
    private val viewMode: FreeJobFeed.ViewMode,
    private val onImageClick: (ImageSubItem, Int) -> Unit,
    private val onViewAllClick: (ImageSubItem) -> Unit,
    private val onListClick: (FreeJobItem) -> Unit = {},
    private val onViewMap: (FreeJobItem) -> Unit = {},
) : RecyclerView.Adapter<ImagesCategoryAdapter.CategoryViewHolder>() {

    private val categories = mutableListOf<ImageSubItem>()

    fun submit(newCategories: List<ImageSubItem>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(private val binding: ItemImageCategoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: ImageSubItem) {
            binding.apply {
                categoryTitle.text = category.title.orEmpty().ifBlank { "Free Job" }
                categoryViewAll.visibility = View.VISIBLE
                categoryViewAll.setOnClickListener { onViewAllClick(category) }

                val allImages = category.images.orEmpty()
                val displayImages = if (viewMode == FreeJobFeed.ViewMode.TILE) {
                    FreeJobFeed.tilePreview(allImages)
                } else {
                    allImages
                }

                imagesRecyclerView.adapter = null
                imagesRecyclerView.layoutManager = null
                imagesRecyclerView.clearOnScrollListeners()
                while (imagesRecyclerView.itemDecorationCount > 0) {
                    imagesRecyclerView.removeItemDecorationAt(0)
                }

                if (viewMode == FreeJobFeed.ViewMode.LIST) {
                    imagesRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
                    val cards = FreeJobCardsAdapter(
                        onClick = onListClick,
                        onViewMap = onViewMap,
                    )
                    cards.submit(displayImages.map { FreeJobItem(job = it, categoryTitle = category.title, categoryIcon = category.iconFile) })
                    imagesRecyclerView.adapter = cards
                } else {
                    // One sideways strip per category, like Top Videos on Home.
                    imagesRecyclerView.layoutManager =
                        LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                    imagesRecyclerView.adapter = FreeJobTilesAdapter(
                        items = displayImages.map { FreeJobItem(job = it, categoryTitle = category.title, categoryIcon = category.iconFile) },
                        onClick = { _, imageIndex -> onImageClick(category, imageIndex) },
                        onViewMap = onViewMap,
                    )
                }

                imagesRecyclerView.setHasFixedSize(false)
                imagesRecyclerView.isNestedScrollingEnabled = false
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
