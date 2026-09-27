package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem
import com.srijeesolution.rojgaarwaala.databinding.ItemImageCategoryBinding
import com.srijeesolution.rojgaarwaala.utils.FreeJobFeed
import com.srijeesolution.rojgaarwaala.utils.FreeJobItem
import com.srijeesolution.rojgaarwaala.utils.SpaceItemDecoration

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
                    allImages.take(TILE_PREVIEW)
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
                    cards.submit(displayImages.map { FreeJobItem(job = it, categoryTitle = category.title) })
                    imagesRecyclerView.adapter = cards
                } else {
                    imagesRecyclerView.layoutManager = GridLayoutManager(itemView.context, 2)
                    imagesRecyclerView.addItemDecoration(SpaceItemDecoration(8, 8))
                    val imagesAdapter = ImagesGridAdapter { _, imageIndex ->
                        onImageClick(category, imageIndex)
                    }
                    imagesAdapter.submitList(displayImages)
                    imagesRecyclerView.adapter = imagesAdapter
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

    companion object {
        private const val TILE_PREVIEW = 6
    }
}
