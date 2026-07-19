package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.content.Intent
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
import com.srijeesolution.rojgaarwaala.databinding.ItemViewMoreVideoBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.VideoPlayerActivity
import com.srijeesolution.rojgaarwaala.utils.VideoListUtils
import com.srijeesolution.rojgaarwaala.utils.VideoNewTagUtils
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs

class VideoAdapter(
    private val onVideoClick: ((Int) -> Unit)? = null,
    private val onViewMoreClick: (() -> Unit)? = null,
) : ListAdapter<TopVideo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    class VideoViewHolder(val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root)
    class ViewMoreViewHolder(val binding: ItemViewMoreVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (VideoListUtils.isViewMoreItem(getItem(position))) VIEW_TYPE_VIEW_MORE else VIEW_TYPE_VIDEO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_VIEW_MORE) {
            ViewMoreViewHolder(
                ItemViewMoreVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        } else {
            VideoViewHolder(
                ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val video = getItem(position)
        if (holder is ViewMoreViewHolder) {
            holder.binding.root.setOnClickListener { onViewMoreClick?.invoke() }
            return
        }

        holder as VideoViewHolder
        val sharedPrefs = SharedPrefs(holder.binding.root.context)
        with(holder.binding) {
            Glide.with(videoThumbnail.context)
                .load(video.thumbnail)
                .placeholder(R.drawable.thumbnail_background)
                .centerCrop()
                .into(videoThumbnail)

            VideoNewTagUtils.bindNewTagBadge(videoNewTag, video, sharedPrefs)

            root.setOnClickListener {
                Log.d("MANISH_JAIN", "YES=" + video.id + "NO =" + video.videoUrl)
                onVideoClick?.invoke(video.id ?: 0)

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
        private const val VIEW_TYPE_VIDEO = 0
        private const val VIEW_TYPE_VIEW_MORE = 1

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
