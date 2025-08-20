package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.Story
import com.srijeesolution.rojgaarwaala.databinding.ItemStoryGridBinding

class StoriesGridAdapter(
    private val stories: List<Story>,
    private val onStoryClick: (Story) -> Unit
) : RecyclerView.Adapter<StoriesGridAdapter.StoryViewHolder>() {

    inner class StoryViewHolder(private val binding: ItemStoryGridBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(story: Story) {
            binding.apply {
                // Set title
                storyTitle.text = story.title ?: ""
                
                // Set description
                storyDescription.text = story.description ?: ""
                
                // Load image using Glide
                val imageUrl = story.imageUrl ?: ""
                if (imageUrl.isNotEmpty()) {
                    Glide.with(storyImage.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.no_image_placeholder)
                        .error(R.drawable.no_image_placeholder)
                        .centerCrop()
                        .into(storyImage)
                } else {
                    // Set placeholder if no image
                    storyImage.setImageResource(R.drawable.no_image_placeholder)
                }
                
                // Set click listener
                root.setOnClickListener {
                    onStoryClick(story)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ItemStoryGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(stories[position])
    }

    override fun getItemCount(): Int = stories.size
} 