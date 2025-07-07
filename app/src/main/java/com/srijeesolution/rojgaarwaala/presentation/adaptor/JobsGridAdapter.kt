package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.JobItem
import com.srijeesolution.rojgaarwaala.databinding.ItemJobGridBinding

class JobsGridAdapter : RecyclerView.Adapter<JobsGridAdapter.JobViewHolder>() {
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
        holder.bind(job)
    }

    override fun getItemCount(): Int = jobs.size

    class JobViewHolder(private val binding: ItemJobGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(job: JobItem) {
            binding.jobTitle.text = "Title: ${job.jobTitle ?: "-"}"
            binding.jobCategory.text = "Category: ${job.jobCategory ?: "-"}"
            binding.jobDescription.text = "Description: ${job.jobDescription ?: "-"}"
        }
    }
} 