package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.content.Intent
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.Category
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.CategoryVideosActivity

class CategoryGridAdapter(
    categories: List<Category>,
    private val onItemClick: (Category) -> Unit,
    showViewAll: Boolean = true
) : RecyclerView.Adapter<CategoryGridAdapter.CategoryGridViewHolder>() {
    private val gridItems = ArrayList<Category>(categories).apply {
        if (showViewAll) {
            add(Category(id = -1, title = "View All", iconFile = null))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryGridViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category_grid, parent, false)
        return CategoryGridViewHolder(v)
    }

    override fun onBindViewHolder(holder: CategoryGridViewHolder, position: Int) {
        val cat = gridItems[position]
        if (!TextUtils.isEmpty(cat.title)) {
            holder.title.text = cat.title
            holder.title.visibility=View.VISIBLE
        }else{
            holder.title.visibility=View.GONE
        }
        if (cat.iconFile != null) {
            Glide.with(holder.icon.context).load(cat.iconFile).placeholder(R.drawable.circle_image_placeholder).into(holder.icon)
        } else {
            holder.icon.setImageResource(R.drawable.ic_category_placeholder)
        }
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, CategoryVideosActivity::class.java)
            intent.putExtra("category_id", cat.id)
            intent.putExtra("category_title", cat.title)
            intent.putExtra("category_icon", cat.iconFile)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = gridItems.size

    class CategoryGridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.categoryGridIcon)
        val title: TextView = view.findViewById(R.id.categoryGridTitle)
    }
} 