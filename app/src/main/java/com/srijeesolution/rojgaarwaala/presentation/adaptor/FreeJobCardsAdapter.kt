package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.utils.FreeJobFeed
import com.srijeesolution.rojgaarwaala.utils.FreeJobItem
import com.srijeesolution.rojgaarwaala.utils.JobMapPins
import com.srijeesolution.rojgaarwaala.utils.JobTitleCopy
import com.srijeesolution.rojgaarwaala.utils.TimeUtils

class FreeJobCardsAdapter(
    private val onClick: (FreeJobItem) -> Unit,
    private val onViewMap: (FreeJobItem) -> Unit,
) : RecyclerView.Adapter<FreeJobCardsAdapter.Holder>() {

    private val items = mutableListOf<FreeJobItem>()

    fun submit(newItems: List<FreeJobItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_free_job_card, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val job = item.job
        val company = FreeJobFeed.companyName(item)
        holder.avatar.text = company.firstOrNull()?.uppercaseChar()?.toString() ?: "R"
        holder.company.text = company
        holder.title.text = JobTitleCopy.display(job.title, holder.itemView.context.getString(R.string.free_job))
        val shift = job.shiftText?.trim().orEmpty()
        val posted = TimeUtils.formatPublishMeta(holder.itemView.context, job.publishDate, job.createdAt)
        holder.meta.text = listOf(shift, posted).filter { it.isNotBlank() }.joinToString("  ·  ")
        holder.meta.visibility = if (holder.meta.text.isNullOrBlank()) View.GONE else View.VISIBLE
        val salary = FreeJobFeed.salaryLine(job)
        holder.salary.text = salary
        holder.salary.visibility = if (salary.isBlank()) View.GONE else View.VISIBLE
        val place = FreeJobFeed.placeLine(job)
        val distance = JobMapPins.distanceLabel(job.distanceKm)
        holder.place.text = listOf(place, distance).filter { it.isNotBlank() }.joinToString(" · ")
            .ifBlank { holder.itemView.context.getString(R.string.free_job_location_available) }
        val canOpenMap = FreeJobFeed.mapGeoUri(job) != null
        holder.viewMap.visibility = if (canOpenMap) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onClick(item) }
        holder.viewMap.setOnClickListener { onViewMap(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.freeJobAvatar)
        val company: TextView = view.findViewById(R.id.freeJobCompany)
        val title: TextView = view.findViewById(R.id.freeJobTitle)
        val meta: TextView = view.findViewById(R.id.freeJobMeta)
        val salary: TextView = view.findViewById(R.id.freeJobSalary)
        val place: TextView = view.findViewById(R.id.freeJobPlace)
        val viewMap: TextView = view.findViewById(R.id.freeJobViewMap)
    }
}
