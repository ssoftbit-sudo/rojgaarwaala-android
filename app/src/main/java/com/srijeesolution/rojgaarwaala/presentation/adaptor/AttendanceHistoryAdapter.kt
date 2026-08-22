package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.AttendanceItem
import com.srijeesolution.rojgaarwaala.databinding.ItemAttendanceDayBinding
import com.srijeesolution.rojgaarwaala.utils.WageFormatter

class AttendanceHistoryAdapter(
    private var items: List<AttendanceItem> = emptyList(),
) : RecyclerView.Adapter<AttendanceHistoryAdapter.AttendanceViewHolder>() {

    fun submitList(newItems: List<AttendanceItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class AttendanceViewHolder(
        private val binding: ItemAttendanceDayBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttendanceItem) {
            binding.attendanceDateText.text = item.dateLabel ?: item.attendanceDate.orEmpty()
            binding.attendanceFactoryText.text = item.factoryName ?: "-"
            binding.attendanceStatusText.text =
                item.statusLabel ?: item.status?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }
                    ?: "-"
            binding.attendanceEarnedText.text = WageFormatter.format(item.earnedWage)

            val timing = buildString {
                item.punchInAt?.takeIf { it.isNotBlank() }?.let { append("In $it") }
                item.punchOutAt?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append("  •  ")
                    append("Out $it")
                }
            }
            binding.attendanceTimingText.text = timing
            binding.attendanceTimingText.visibility =
                if (timing.isBlank()) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = ItemAttendanceDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return AttendanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
