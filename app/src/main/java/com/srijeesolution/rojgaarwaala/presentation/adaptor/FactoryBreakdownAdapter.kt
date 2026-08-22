package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.FactoryBreakdownItem
import com.srijeesolution.rojgaarwaala.databinding.ItemFactoryBreakdownBinding
import com.srijeesolution.rojgaarwaala.utils.WageFormatter

class FactoryBreakdownAdapter(
    private var items: List<FactoryBreakdownItem> = emptyList(),
) : RecyclerView.Adapter<FactoryBreakdownAdapter.BreakdownViewHolder>() {

    fun submitList(newItems: List<FactoryBreakdownItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class BreakdownViewHolder(
        private val binding: ItemFactoryBreakdownBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FactoryBreakdownItem) {
            binding.breakdownFactoryText.text = item.factoryName ?: "-"
            binding.breakdownEarnedText.text = WageFormatter.format(item.totalEarned)
            binding.breakdownDaysText.text = "Present ${item.presentDays ?: 0}  •  " +
                "Half day ${item.halfDays ?: 0}  •  Absent ${item.absentDays ?: 0}"
            binding.breakdownWageText.text = "Daily wage ${WageFormatter.format(item.dailyWage)}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreakdownViewHolder {
        val binding = ItemFactoryBreakdownBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return BreakdownViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BreakdownViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
