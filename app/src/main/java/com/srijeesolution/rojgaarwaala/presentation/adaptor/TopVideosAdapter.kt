package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo
import com.srijeesolution.rojgaarwaala.databinding.ItemTopVideoBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.ApplyFormActivity
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.VideoPlayerActivity
import com.srijeesolution.rojgaarwaala.utils.TimeUtils

class TopVideosAdapter : ListAdapter<TopVideo, TopVideosAdapter.TopVideoViewHolder>(DIFF_CALLBACK) {
    class TopVideoViewHolder(val binding: ItemTopVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopVideoViewHolder {
        val binding = ItemTopVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopVideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopVideoViewHolder, position: Int) {
        val video = getItem(position)
        with(holder.binding) {
            Glide.with(topVideoThumbnail.context)
                .load(video.thumbnail)
                .placeholder(R.drawable.no_image_placeholder)
                .into(topVideoThumbnail)

            // Video thumbnail click
            root.setOnClickListener {
                val intent = Intent(root.context, VideoPlayerActivity::class.java)
                intent.putExtra("video_url", video.videoUrl)
                intent.putExtra("video_id", video.id)
                root.context.startActivity(intent)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TopVideo>() {
            override fun areItemsTheSame(oldItem: TopVideo, newItem: TopVideo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TopVideo, newItem: TopVideo): Boolean {
                return oldItem == newItem
            }
        }
    }
} 