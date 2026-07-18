package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.JobApplicationTimelineEntry
import com.srijeesolution.rojgaarwaala.databinding.ItemApplicationTimelineBinding

class ApplicationTimelineAdapter(
    private var entries: List<JobApplicationTimelineEntry>,
) : RecyclerView.Adapter<ApplicationTimelineAdapter.TimelineViewHolder>() {

    fun updateEntries(newEntries: List<JobApplicationTimelineEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    inner class TimelineViewHolder(
        private val binding: ItemApplicationTimelineBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: JobApplicationTimelineEntry, isLast: Boolean) {
            binding.timelineStatusText.text = formatStatusLabel(entry.status)
            binding.timelineDateText.text = entry.at.orEmpty()
            binding.timelineNoteText.text = entry.note.orEmpty()
            binding.timelineNoteText.visibility =
                if (entry.note.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.timelineConnector.visibility = if (isLast) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val binding = ItemApplicationTimelineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TimelineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        holder.bind(entries[position], position == entries.lastIndex)
    }

    override fun getItemCount(): Int = entries.size

    private fun formatStatusLabel(status: String?): String {
        return when (status?.lowercase()) {
            "applied" -> "Applied"
            "under_review" -> "Under Review"
            "interview_scheduled" -> "Interview Scheduled"
            "selected" -> "Selected"
            "rejected" -> "Not Selected"
            "pending_payment" -> "Submitted"
            "failed" -> "Failed"
            else -> status?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Update"
        }
    }
}
