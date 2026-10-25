package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJob
import com.srijeesolution.rojgaarwaala.utils.DayJobFormat

/** Small shared pieces for the 1 Day Job screens. */
internal object DayJobUi {

    fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()

    /** Amber when chosen, dark otherwise; same look as the resume builder chips. */
    fun chip(context: Context, text: String, checked: Boolean): Chip = Chip(context).apply {
        this.text = text
        isCheckable = true
        isChecked = checked
        isCheckedIconVisible = false
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        chipBackgroundColor = ColorStateList(states, intArrayOf(
            ContextCompat.getColor(context, R.color.brand_color_yellow), Color.parseColor("#1C1C1C"),
        ))
        setTextColor(ColorStateList(states, intArrayOf(ContextCompat.getColor(context, R.color.on_brand), Color.WHITE)))
        chipStrokeWidth = dp(context, 1).toFloat()
        chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#444444"))
    }

    /** Status pill: green when people are coming, amber while open, red/grey when it ended badly. */
    fun styleStatus(view: TextView, status: String) {
        view.text = DayJobFormat.statusText(status)
        val color = when (status) {
            "open" -> R.color.brand_color_yellow
            "full", "closed", "completed" -> R.color.day_job_green
            "in_review" -> R.color.search_hint
            else -> R.color.new_tag_red
        }
        view.setTextColor(ContextCompat.getColor(view.context, color))
    }

    fun call(context: Context, phone: String?) {
        val number = phone?.filter { it.isDigit() || it == '+' }.orEmpty()
        if (number.isBlank()) {
            Toast.makeText(context, "No phone number", Toast.LENGTH_SHORT).show()
            return
        }
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    fun directions(context: Context, job: DayJob) {
        val uri = Uri.parse("google.navigation:q=${job.latitude},${job.longitude}")
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${job.latitude},${job.longitude}"))
        runCatching { context.startActivity(intent) }.onFailure { runCatching { context.startActivity(fallback) } }
    }

    fun toast(context: Context, message: String?) {
        if (!message.isNullOrBlank()) Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

/** Cards for jobs: a worker sees wage, distance and seats; a company sees how many are coming. */
internal class DayJobAdapter(
    private val forCompany: Boolean,
    private val onClick: (DayJob) -> Unit,
) : RecyclerView.Adapter<DayJobAdapter.Holder>() {

    private val items = mutableListOf<DayJob>()

    fun submit(jobs: List<DayJob>) {
        items.clear()
        items.addAll(jobs)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_day_job, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val job = items[position]
        holder.title.text = job.title
        holder.wage.text = DayJobFormat.wageText(job.wagePerDay)
        holder.slots.text = DayJobFormat.slotsText(job)
        val meta = listOf(
            if (forCompany) "" else job.company.name,
            DayJobFormat.whenText(job.workDate, job.startTime),
            if (forCompany) job.place else DayJobFormat.distanceText(job.distanceKm),
        ).filter { it.isNotBlank() }
        holder.meta.text = meta.joinToString(" · ")
        val booked = job.myBooking?.status == "accepted"
        when {
            !forCompany && booked -> {
                holder.badge.text = "You're going"
                holder.badge.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.day_job_green))
            }
            else -> DayJobUi.styleStatus(holder.badge, job.status)
        }
        holder.seats.text = if (forCompany) DayJobFormat.comingText(job) else DayJobFormat.seatsLeftText(job)
        holder.itemView.setOnClickListener { onClick(job) }
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.itemDayJobTitle)
        val wage: TextView = view.findViewById(R.id.itemDayJobWage)
        val slots: TextView = view.findViewById(R.id.itemDayJobSlots)
        val meta: TextView = view.findViewById(R.id.itemDayJobMeta)
        val badge: TextView = view.findViewById(R.id.itemDayJobBadge)
        val seats: TextView = view.findViewById(R.id.itemDayJobSeats)
    }
}
