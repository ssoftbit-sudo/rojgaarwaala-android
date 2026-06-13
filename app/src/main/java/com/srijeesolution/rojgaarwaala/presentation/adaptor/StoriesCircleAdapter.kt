package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.CircleStory
import com.srijeesolution.rojgaarwaala.databinding.ItemStoryCircleBinding

class StoriesCircleAdapter(
    private var stories: List<CircleStory>,
    private val onStoryClick: (CircleStory, Int) -> Unit
) : RecyclerView.Adapter<StoriesCircleAdapter.CircleViewHolder>() {

    fun updateStories(newStories: List<CircleStory>) {
        stories = newStories
        notifyDataSetChanged()
    }

    inner class CircleViewHolder(private val binding: ItemStoryCircleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(story: CircleStory) {
            val ringDrawable = if (story.seen == true) {
                R.drawable.story_circle_ring_seen
            } else {
                R.drawable.story_circle_ring_unseen
            }
            binding.storyRingContainer.setBackgroundResource(ringDrawable)

            val thumbUrl = story.thumbnailUrl ?: story.imageUrl
            Glide.with(binding.storyCircleImage.context)
                .load(thumbUrl)
                .placeholder(R.drawable.story_circle_ring_seen)
                .error(R.drawable.story_circle_ring_seen)
                .circleCrop()
                .into(binding.storyCircleImage)

            binding.storyCircleTitle.text = story.title ?: ""

            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStoryClick(story, position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CircleViewHolder {
        val binding = ItemStoryCircleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CircleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CircleViewHolder, position: Int) {
        holder.bind(stories[position])
    }

    override fun getItemCount(): Int = stories.size
}
