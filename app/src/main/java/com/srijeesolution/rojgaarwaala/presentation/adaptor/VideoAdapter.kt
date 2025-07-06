package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo
import com.srijeesolution.rojgaarwaala.databinding.ItemVideoBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.VideoPlayerActivity

class VideoAdapter(private val videos: List<TopVideo>) :
    RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(val binding: ItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        with(holder.binding) {
            videoTitle.text = video.title

            // Load thumbnail using Glide (use videoUrl as thumbnail for now)
            Glide.with(videoThumbnail.context)
                .load(video.thumbnail)
                .placeholder(R.drawable.thumbnail_background)
                .centerCrop()
                .into(videoThumbnail)

            root.setOnClickListener {
                val intent = Intent(root.context, VideoPlayerActivity::class.java)
                intent.putExtra("video_url", video.videoUrl)
                intent.putExtra("video_id", video.id)
                root.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = videos.size
}
