package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo
import com.srijeesolution.rojgaarwaala.databinding.ItemGridVideoBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.VideoPlayerActivity
import com.srijeesolution.rojgaarwaala.utils.VideoNewTagUtils
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs

class GridVideosListAdapter : ListAdapter<TopVideo, GridVideosListAdapter.TopVideoViewHolder>(DIFF_CALLBACK) {
    class TopVideoViewHolder(val binding: ItemGridVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopVideoViewHolder {
        val binding = ItemGridVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopVideoViewHolder(binding)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onBindViewHolder(holder: TopVideoViewHolder, position: Int) {
        val video = getItem(position)
        val sharedPrefs = SharedPrefs(holder.binding.root.context)
        Glide.with(holder.binding.topVideoThumbnail.context)
            .load(video.thumbnail)
            .placeholder(R.drawable.no_image_placeholder)
            .into(holder.binding.topVideoThumbnail)

        VideoNewTagUtils.bindNewTagBadge(holder.binding.videoNewTag, video, sharedPrefs)
            
        holder.binding.root.setOnClickListener {
            val intent = Intent(holder.binding.root.context, VideoPlayerActivity::class.java)
            intent.putExtra("video_url", video.videoUrl)
            intent.putExtra("video_id", video.id)
            holder.binding.root.context.startActivity(intent)
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