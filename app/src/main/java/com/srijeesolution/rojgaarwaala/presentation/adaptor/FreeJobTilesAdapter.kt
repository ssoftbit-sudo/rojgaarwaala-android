package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.ItemFreeJobTileBinding
import com.srijeesolution.rojgaarwaala.utils.FreeJobItem

class FreeJobTilesAdapter(
    private val items: List<FreeJobItem>,
    private val onClick: (FreeJobItem) -> Unit,
) : RecyclerView.Adapter<FreeJobTilesAdapter.Holder>() {

    inner class Holder(private val binding: ItemFreeJobTileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FreeJobItem) {
            val imageUrl = item.job.imageUrl.orEmpty()
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
            binding.root.setOnClickListener { onClick(item) }
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
