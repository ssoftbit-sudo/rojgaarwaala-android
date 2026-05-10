package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.JobItem
import com.srijeesolution.rojgaarwaala.databinding.ItemJobGridBinding

class JobsGridAdapter(
    private val onEdit: ((JobItem) -> Unit)? = null,
    private val onDelete: ((JobItem) -> Unit)? = null,
    private val showActions: Boolean = false
) : RecyclerView.Adapter<JobsGridAdapter.JobViewHolder>() {
    private var jobs: List<JobItem> = emptyList()

    fun submitList(newJobs: List<JobItem>) {
        jobs = newJobs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ItemJobGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]
        holder.bind(job, showActions, onEdit, onDelete)
    }

    override fun getItemCount(): Int = jobs.size

    class JobViewHolder(private val binding: ItemJobGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(job: JobItem, showActions: Boolean, onEdit: ((JobItem) -> Unit)?, onDelete: ((JobItem) -> Unit)?) {
            binding.jobTitle.text = "Title: ${job.jobTitle ?: "-"}"
            binding.jobCategory.text = "Category: ${job.jobCategory ?: "-"}"
            binding.jobDescription.text = "Description: ${job.jobDescription ?: "-"}"
            if (showActions) {
                binding.editJobIcon.visibility = View.VISIBLE
                binding.deleteJobIcon.visibility = View.VISIBLE
                binding.editJobIcon.setOnClickListener { onEdit?.invoke(job) }
                binding.deleteJobIcon.setOnClickListener { onDelete?.invoke(job) }
            } else {
                binding.editJobIcon.visibility = View.GONE
                binding.deleteJobIcon.visibility = View.GONE
            }
        }
    }
} 