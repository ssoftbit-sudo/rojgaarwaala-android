package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.JobApplicationDto
import com.srijeesolution.rojgaarwaala.databinding.ItemAppliedJobBinding

class AppliedJobsAdapter(
    private var applications: List<JobApplicationDto>,
    private val onItemClick: (JobApplicationDto) -> Unit,
) : RecyclerView.Adapter<AppliedJobsAdapter.AppliedJobViewHolder>() {

    fun updateApplications(newApplications: List<JobApplicationDto>) {
        applications = newApplications
        notifyDataSetChanged()
    }

    inner class AppliedJobViewHolder(
        private val binding: ItemAppliedJobBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(application: JobApplicationDto, position: Int) {
            binding.appliedJobIndex.text = (position + 1).toString()
            binding.appliedJobTitle.text = application.jobTitle ?: "Job Application"
            binding.appliedJobMeta.text = buildString {
                application.categoryName?.takeIf { it.isNotBlank() }?.let { append(it) }
                application.appliedAt?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append(" • ")
                    append("Applied $it")
                }
            }.ifBlank { formatStatus(application.status) }

            binding.root.setOnClickListener {
                onItemClick(application)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppliedJobViewHolder {
        val binding = ItemAppliedJobBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return AppliedJobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppliedJobViewHolder, position: Int) {
        holder.bind(applications[position], position)
    }

    override fun getItemCount(): Int = applications.size

    private fun formatStatus(status: String?): String {
        return status?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Applied"
    }
}
