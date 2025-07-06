package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.Category
import com.srijeesolution.rojgaarwaala.databinding.ItemCategoryBinding

class CategoryAdapter(private val categories: List<Category>) :
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        with(holder.binding) {
            categoryTitle.text = category.title

            val videoAdapter = VideoAdapter(category.videos)
            videoRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, RecyclerView.HORIZONTAL, false)
            videoRecyclerView.adapter = videoAdapter
        }
    }

    override fun getItemCount(): Int = categories.size
}
