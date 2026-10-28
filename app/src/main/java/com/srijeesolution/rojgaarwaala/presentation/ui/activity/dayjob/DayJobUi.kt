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
            Toast.makeText(context, "फ़ोन नंबर नहीं है", Toast.LENGTH_SHORT).show()
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

    /** The rules as a list, with "मैं मानता हूँ"; nothing happens until it is pressed. */
    fun askTerms(context: Context, title: String, rules: List<String>, onAgree: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(context, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle(title)
            .setMessage(rules.joinToString("\n\n") { "• $it" })
            .setCancelable(false)
            .setPositiveButton("मैं मानता हूँ") { _, _ -> onAgree() }
            .setNegativeButton("बाद में", null)
            .show()
    }

    /** 1 to 5 big stars; Job Done waits for a choice. */
    fun askStars(context: Context, title: String, message: String, onPick: (Int) -> Unit) {
        val bar = android.widget.RatingBar(context).apply {
            numStars = 5
            stepSize = 1f
            rating = 0f
            progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brand_color_yellow))
            progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#8A8A8A"))
            layoutParams = android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val box = android.widget.LinearLayout(context).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(dp(context, 16), dp(context, 12), dp(context, 16), 0)
            addView(bar)
        }
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle(title)
            .setMessage(message)
            .setView(box)
            .setPositiveButton("Job Done", null)
            .setNegativeButton("रहने दें", null)
            .show()
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val stars = bar.rating.toInt()
            if (stars < 1) {
                toast(context, "1 से 5 स्टार चुनें")
            } else {
                dialog.dismiss()
                onPick(stars)
            }
        }
    }

    /** Why the worker leaves the seat; the list comes from the admin. */
    fun askReason(context: Context, title: String, reasons: List<String>, onPick: (String?) -> Unit) {
        if (reasons.isEmpty()) {
            onPick(null)
            return
        }
        androidx.appcompat.app.AlertDialog.Builder(context, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle(title)
            .setAdapter(object : android.widget.ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, reasons) {
                // The dialog theme's list text is too faint on the dark background.
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                    (super.getView(position, convertView, parent) as TextView).apply {
                        setTextColor(Color.WHITE)
                        textSize = 17f
                    }
            }) { _, i -> onPick(reasons[i]) }
            .setNegativeButton("रहने दें", null)
            .show()
    }

    /** "★ 4.3 (12)", or empty before the first rating. */
    fun ratingText(avg: Double?, count: Int): String =
        if (avg == null || count == 0) "" else "★ " + String.format(java.util.Locale.US, "%.1f", avg) + " ($count)"
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
                holder.badge.text = "आप जा रहे हैं"
                holder.badge.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.day_job_green))
            }
            !forCompany && job.reservedForMe -> {
                holder.badge.text = "आपके लिए रखा है"
                holder.badge.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.day_job_green))
            }
            !forCompany && job.rejectedByMe -> {
                holder.badge.text = "आपने मना किया"
                holder.badge.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.search_hint))
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
