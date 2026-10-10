package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.ItemFreeJobTileBinding
import com.srijeesolution.rojgaarwaala.utils.FreeJobFeed
import com.srijeesolution.rojgaarwaala.utils.FreeJobItem
import com.srijeesolution.rojgaarwaala.utils.JobMapPins
import com.srijeesolution.rojgaarwaala.utils.TimeUtils

class FreeJobTilesAdapter(
    private val items: List<FreeJobItem>,
    private val onClick: (FreeJobItem, Int) -> Unit,
    private val onViewMap: (FreeJobItem) -> Unit,
) : RecyclerView.Adapter<FreeJobTilesAdapter.Holder>() {

    inner class Holder(private val binding: ItemFreeJobTileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FreeJobItem) {
            val job = item.job
            val imageUrl = job.imageUrl.orEmpty()
            if (imageUrl.isNotEmpty()) {
                Glide.with(binding.freeJobTileImage.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.no_image_placeholder)
                    .error(R.drawable.no_image_placeholder)
                    .centerCrop()
                    .dontAnimate()
                    .into(binding.freeJobTileImage)
            } else {
                binding.freeJobTileImage.setImageResource(R.drawable.no_image_placeholder)
            }

            val title = job.title.orEmpty()
            binding.freeJobTileTitle.text = title
            binding.freeJobTileTitle.visibility = if (title.isBlank()) View.GONE else View.VISIBLE

            val distance = JobMapPins.distanceLabel(job.distanceKm).takeIf { it.isNotBlank() }
            val posted = TimeUtils.formatPublishMeta(binding.root.context, job.publishDate, job.createdAt)
                .takeIf { it.isNotBlank() }
            val meta = listOfNotNull(distance, posted).joinToString(" · ")
            binding.freeJobTileMeta.text = meta
            binding.freeJobTileMeta.visibility = if (meta.isBlank()) View.GONE else View.VISIBLE
            binding.freeJobTileCaption.visibility =
                if (title.isBlank() && meta.isBlank()) View.GONE else View.VISIBLE

            // Kept in the layout when there is no pin, so every tile in the strip is the same height.
            binding.freeJobTileMap.visibility =
                if (FreeJobFeed.mapGeoUri(job) != null) View.VISIBLE else View.INVISIBLE
            binding.freeJobTileMap.setOnClickListener { onViewMap(item) }

            binding.freeJobTileCard.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onClick(item, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemFreeJobTileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
