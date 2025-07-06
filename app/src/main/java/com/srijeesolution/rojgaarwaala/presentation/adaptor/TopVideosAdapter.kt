package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo

class TopVideosAdapter(private val videos: List<TopVideo>) : RecyclerView.Adapter<TopVideosAdapter.TopVideoViewHolder>() {
    class TopVideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.topVideoThumbnail)
        val title: TextView = itemView.findViewById(R.id.topVideoTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopVideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_top_video, parent, false)
        return TopVideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopVideoViewHolder, position: Int) {
        val video = videos[position]
        holder.title.text = video.title
        Glide.with(holder.thumbnail.context)
            .load(video.thumbnail) // You may want to use a thumbnail URL if available
            .placeholder(R.drawable.no_image_placeholder)
            .into(holder.thumbnail)
    }

    override fun getItemCount(): Int = videos.size
} 