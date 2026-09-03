package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.TimeGroup
import com.srijeesolution.rojgaarwaala.databinding.ItemStoryCategoryBinding
import com.srijeesolution.rojgaarwaala.utils.SpaceItemDecoration

class StoriesCategoryAdapter(
    private val timeGroups: List<TimeGroup>,
    private val onStoryClick: (com.srijeesolution.rojgaarwaala.data.remote.model.Story) -> Unit,
    private val onViewAllClick: (TimeGroup) -> Unit
) : RecyclerView.Adapter<StoriesCategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(private val binding: ItemStoryCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(timeGroup: TimeGroup) {
            binding.apply {
                // Set time group title
                categoryTitle.text = timeGroup.title ?: ""
                
                // Setup View All click listener
                categoryViewAll.setOnClickListener {
                    onViewAllClick(timeGroup)
                }
                
                // Setup nested RecyclerView for stories with GridLayoutManager
                if (storiesRecyclerView.layoutManager == null) {
                    storiesRecyclerView.layoutManager = GridLayoutManager(itemView.context, 2)
                    storiesRecyclerView.addItemDecoration(SpaceItemDecoration(8, 8))
                    storiesRecyclerView.setHasFixedSize(true)
                    storiesRecyclerView.isNestedScrollingEnabled = false
                }

                val storiesAdapter = StoriesGridAdapter(timeGroup.stories ?: emptyList()) { story ->
                    onStoryClick(story)
                }

                storiesRecyclerView.adapter = storiesAdapter
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemStoryCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(timeGroups[position])
    }

    override fun getItemCount(): Int = timeGroups.size
} 