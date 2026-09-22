package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.Story
import com.srijeesolution.rojgaarwaala.databinding.ItemStoryListBinding
import com.srijeesolution.rojgaarwaala.utils.TimeUtils

class StoriesListAdapter(
    private val stories: List<Story>,
    private val groupTitle: (Story) -> String?,
    private val onStoryClick: (Story) -> Unit,
) : RecyclerView.Adapter<StoriesListAdapter.Holder>() {

    inner class Holder(private val binding: ItemStoryListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(story: Story) {
            binding.storyListTitle.text = story.title.orEmpty().ifBlank { "Story" }
            val posted = TimeUtils.formatPublishMeta(
                binding.root.context,
                story.publishDate,
                story.createdAt,
            )
            binding.storyListMeta.text = listOfNotNull(
                groupTitle(story)?.takeIf { it.isNotBlank() },
                posted.takeIf { it.isNotBlank() },
            ).joinToString("  ·  ")
            val imageUrl = story.imageUrl.orEmpty()
            if (imageUrl.isNotEmpty()) {
                Glide.with(binding.storyListImage.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.no_image_placeholder)
                    .error(R.drawable.no_image_placeholder)
                    .centerCrop()
                    .dontAnimate()
                    .into(binding.storyListImage)
            } else {
                binding.storyListImage.setImageResource(R.drawable.no_image_placeholder)
            }
            binding.root.setOnClickListener { onStoryClick(story) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemStoryListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(stories[position])
    }

    override fun getItemCount(): Int = stories.size
}
