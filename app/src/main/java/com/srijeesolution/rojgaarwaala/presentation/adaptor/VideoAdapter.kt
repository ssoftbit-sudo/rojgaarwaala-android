package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo
import com.srijeesolution.rojgaarwaala.databinding.ItemVideoBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.ApplyFormActivity
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.VideoPlayerActivity
import com.srijeesolution.rojgaarwaala.utils.TimeUtils

class VideoAdapter(
    private val onVideoClick: ((Int) -> Unit)? = null
) : ListAdapter<TopVideo, VideoAdapter.VideoViewHolder>(DIFF_CALLBACK) {

    class VideoViewHolder(val binding: ItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = getItem(position)
        with(holder.binding) {
            videoTitle.text = video.title

            // Set relative time
            videoTime.text = TimeUtils.getRelativeTimeSpanString(root.context, video.createdAt)

            // Load thumbnail using Glide (use videoUrl as thumbnail for now)
            Glide.with(videoThumbnail.context)
                .load(video.thumbnail)
                .placeholder(R.drawable.thumbnail_background)
                .centerCrop()
                .into(videoThumbnail)

            // Apply button click
            applyButton.setOnClickListener {
                val intent = Intent(root.context, ApplyFormActivity::class.java)
                intent.putExtra("video_id", video.id)
                intent.putExtra("video_title", video.title)
                root.context.startActivity(intent)
            }

            // Call button click
            callButton.setOnClickListener {
                val phoneNumber = video.phoneNumber ?: video.user?.mobile
                if (!phoneNumber.isNullOrEmpty()) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                    }
                    root.context.startActivity(intent)
                }
            }

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
