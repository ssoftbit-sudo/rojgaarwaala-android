package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo
import com.srijeesolution.rojgaarwaala.databinding.ItemTopVideoBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.VideoPlayerActivity

class TopVideosAdapter(private val videos: List<TopVideo>) : RecyclerView.Adapter<TopVideosAdapter.TopVideoViewHolder>() {
    class TopVideoViewHolder(val binding: ItemTopVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopVideoViewHolder {
        val binding = ItemTopVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopVideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopVideoViewHolder, position: Int) {
        val video = videos[position]
        holder.binding.topVideoTitle.text = video.title
        Glide.with(holder.binding.topVideoThumbnail.context)
            .load(video.thumbnail)
            .placeholder(R.drawable.no_image_placeholder)
            .into(holder.binding.topVideoThumbnail)
            
        holder.binding.root.setOnClickListener {
            val intent = Intent(holder.binding.root.context, VideoPlayerActivity::class.java)
            intent.putExtra("video_url", video.videoUrl)
            intent.putExtra("video_id", video.id)
            holder.binding.root.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = videos.size
} 