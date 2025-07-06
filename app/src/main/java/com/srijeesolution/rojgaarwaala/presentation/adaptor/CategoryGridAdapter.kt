package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.Category

class CategoryGridAdapter(
    categories: List<Category>,
    private val onItemClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryGridAdapter.CategoryGridViewHolder>() {
    private val gridItems = ArrayList<Category>(categories).apply {
        add(Category(id = -1, title = "View All", iconFile = null))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryGridViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category_grid, parent, false)
        return CategoryGridViewHolder(v)
    }

    override fun onBindViewHolder(holder: CategoryGridViewHolder, position: Int) {
        val cat = gridItems[position]
        holder.title.text = cat.title
        if (cat.iconFile != null) {
            Glide.with(holder.icon.context).load(cat.iconFile).placeholder(R.drawable.circle_image_placeholder).into(holder.icon)
        } else {
            holder.icon.setImageResource(R.drawable.ic_chevron_right)
        }
        holder.itemView.setOnClickListener { onItemClick(cat) }
    }

    override fun getItemCount() = gridItems.size

    class CategoryGridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.categoryGridIcon)
        val title: TextView = view.findViewById(R.id.categoryGridTitle)
    }
} 