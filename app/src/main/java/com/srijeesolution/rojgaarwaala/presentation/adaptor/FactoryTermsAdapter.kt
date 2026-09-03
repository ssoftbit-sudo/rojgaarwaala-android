package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.FactoryTermItem
import com.srijeesolution.rojgaarwaala.databinding.ItemFactoryTermBinding

class FactoryTermsAdapter(
    private var items: List<FactoryTermItem> = emptyList(),
) : RecyclerView.Adapter<FactoryTermsAdapter.TermViewHolder>() {

    fun submitList(newItems: List<FactoryTermItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class TermViewHolder(
        private val binding: ItemFactoryTermBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FactoryTermItem, position: Int) {
            binding.termIndexText.text = (position + 1).toString()
            binding.termTitleText.text = item.title ?: "-"
            val description = item.description.orEmpty()
            binding.termDescriptionText.text = description
            binding.termDescriptionText.visibility =
                if (description.isBlank()) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TermViewHolder {
        val binding = ItemFactoryTermBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TermViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TermViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size
}
