package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo
import com.srijeesolution.rojgaarwaala.databinding.ItemVerticalVideoBinding
import com.srijeesolution.rojgaarwaala.databinding.ItemVideoBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.VideoPlayerActivity
import com.srijeesolution.rojgaarwaala.utils.TimeUtils
import com.srijeesolution.rojgaarwaala.utils.VideoNewTagUtils
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs

class VideoVerticalAdapter(
    private val videos: List<TopVideo>,
    private val onVideoClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<VideoVerticalAdapter.VideoViewHolder>() {

    class VideoViewHolder(val binding: ItemVerticalVideoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVerticalVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        val sharedPrefs = SharedPrefs(holder.binding.root.context)
        with(holder.binding) {
            // Set video title
            videoTitle.text = video.title ?: "Video Title"

            val viewsPart = video.views?.takeIf { it > 0 }?.let { formatViewCount(it) + " views" }
            val timePart = TimeUtils.getRelativeTimeSpanString(root.context, video.createdAt)
                .takeIf { it.isNotBlank() }
            videoMeta.text = listOfNotNull(viewsPart, timePart).joinToString(" • ")
                .ifBlank { root.context.getString(com.srijeesolution.rojgaarwaala.R.string.channel_placeholder) }
            videoDescription.visibility = View.GONE

            // Load thumbnail using Glide
            Glide.with(videoThumbnail.context)
                .load(video.thumbnail)
                .placeholder(R.drawable.thumbnail_background)
                .centerCrop()
                .into(videoThumbnail)

            VideoNewTagUtils.bindNewTagBadge(videoNewTag, video, sharedPrefs)

            root.setOnClickListener {
                Log.d("MANISH_JAIN","YES="+video.id+"NO ="+video.videoUrl)
                
                // If click listener is provided, use it (for same activity video switching)
                onVideoClick?.invoke(video.id ?: 0)
                
                // If no click listener, start new activity (for navigation from other screens)
                if (onVideoClick == null) {
                    val intent = Intent(root.context, VideoPlayerActivity::class.java)
                    intent.putExtra("video_url", video.videoUrl)
                    intent.putExtra("video_id", video.id)
                    root.context.startActivity(intent)
                }
            }
        }
    }
    
    override fun getItemCount(): Int = videos.size

    private fun formatViewCount(n: Int): String = when {
        n >= 1_000_000 -> {
            val v = n / 1_000_000.0
            if (v >= 10) "${(v).toInt()}M" else String.format("%.1fM", v).replace(".0M", "M")
        }
        n >= 1_000 -> {
            val v = n / 1_000.0
            if (v >= 10) "${v.toInt()}K" else String.format("%.1fK", v).replace(".0K", "K")
        }
        else -> n.toString()
    }
}
