package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.Video
import com.srijeesolution.rojgaarwaala.databinding.ItemVideoBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.VideoPlayerActivity

class VideoAdapter(private val videos: List<Video>) :
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

            // Log the full video URL
            Log.d("VideoAdapter", "Video URL: ${video.video_url}")

            // Extract YouTube video ID
            val videoId = extractYouTubeVideoId(video.video_url)
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

            // Load thumbnail using Glide
            Glide.with(videoThumbnail.context)
                .load(thumbnailUrl)
                .placeholder(R.drawable.thumbnail_background)
                .centerCrop()
                .into(videoThumbnail)

            /*root.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.video_url))
                root.context.startActivity(intent)
            }*/
            root.setOnClickListener {
                val intent = Intent(root.context, VideoPlayerActivity::class.java)
                intent.putExtra("video_url", video.video_url)
                root.context.startActivity(intent)

            }

        }
    }
    private fun extractYouTubeVideoId(url: String): String? {
        val regex = Regex("(?:v=|be/|embed/|shorts/)([a-zA-Z0-9_-]{11})")
        val match = regex.find(url)
        return match?.groupValues?.get(1)
    }


    override fun getItemCount(): Int = videos.size
}
