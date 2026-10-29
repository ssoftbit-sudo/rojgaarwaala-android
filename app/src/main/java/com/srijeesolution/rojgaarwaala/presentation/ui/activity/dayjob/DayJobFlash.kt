package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJob
import com.srijeesolution.rojgaarwaala.utils.DayJobFormat
import com.srijeesolution.rojgaarwaala.utils.DayJobSpeaker

/**
 * New work pops up as big Job Cards over the screen, one per job; the worker swipes
 * to the next. A new job has "हाँ" and "ना"; a job the leader picked them for has
 * "जाऊँगा" and "नहीं जाऊँगा". Each card shows once.
 */
object DayJobFlash {

    /** Sent inside the app when a new-job or team-pick push arrives. */
    const val ACTION_NEW_WORK = "com.srijeesolution.rojgaarwaala.DAY_JOB_NEW_WORK"

    private const val PREFS = "day_job"
    private const val KEY_SEEN = "flash_seen"

    class Actions(
        val onYes: (DayJob) -> Unit,
        val onNo: (DayJob) -> Unit,
        val onTeamYes: (DayJob) -> Unit,
        val onTeamNo: (DayJob) -> Unit,
        val onOpen: (DayJob) -> Unit,
    )

    /** New jobs to flash: open to the worker, not answered, not shown before. Team asks first. */
    fun pick(context: Context, nearby: List<DayJob>, mine: List<DayJob>): List<DayJob> {
        val seen = seen(context)
        val asks = mine.filter { it.myBooking?.teamAsk == true && "t${it.id}" !in seen }
        val fresh = nearby.filter {
            it.myBooking == null && !it.rejectedByMe && it.isAccepting && it.canTakeFrom == null && "n${it.id}" !in seen
        }
        return asks + fresh.filter { f -> asks.none { it.id == f.id } }
    }

    private fun seen(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY_SEEN, emptySet()).orEmpty()

    private fun markSeen(context: Context, job: DayJob) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Keep the list short; old ids no longer come back.
        val next = (seen(context) + key(job)).toList().takeLast(300).toSet()
        prefs.edit().putStringSet(KEY_SEEN, next).apply()
    }

    private fun key(job: DayJob) = if (job.myBooking?.teamAsk == true) "t${job.id}" else "n${job.id}"

    fun show(context: Context, jobs: List<DayJob>, actions: Actions): Dialog? {
        if (jobs.isEmpty()) return null
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dp = { v: Int -> DayJobUi.dp(context, v) }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE000000.toInt())
            setPadding(0, dp(24), 0, dp(16))
        }
        val counter = text(context, "", 15f, R.color.brand_color_yellow, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(8), 0)
            addView(text(context, "🔔 नया काम", 20f, R.color.white, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(text(context, "✕ बंद करें", 16f, R.color.search_hint, bold = true).apply {
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setOnClickListener { dialog.dismiss() }
            })
        })
        root.addView(counter)

        val list = jobs.toMutableList()
        val pager = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        fun updateCounter(at: Int) {
            counter.text = if (list.size > 1) "${at + 1} / ${list.size}  ·  अगला काम देखने के लिए स्वाइप करें ›" else ""
            list.getOrNull(at)?.let { markSeen(context, it) }
        }
        lateinit var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
        fun answered(job: DayJob, then: (DayJob) -> Unit) {
            val at = list.indexOfFirst { it.id == job.id }
            if (at >= 0) {
                list.removeAt(at)
                adapter.notifyItemRemoved(at)
            }
            if (list.isEmpty()) dialog.dismiss() else updateCounter(at.coerceAtMost(list.size - 1).coerceAtLeast(0))
            then(job)
        }
        adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = list.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(ScrollView(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(parent.width.takeIf { it > 0 } ?: ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val scroll = holder.itemView as ScrollView
                scroll.removeAllViews()
                scroll.addView(page(context, list[position], actions = Actions(
                    onYes = { answered(it, actions.onYes) },
                    onNo = { answered(it, actions.onNo) },
                    onTeamYes = { answered(it, actions.onTeamYes) },
                    onTeamNo = { answered(it, actions.onTeamNo) },
                    onOpen = { dialog.dismiss(); actions.onOpen(it) },
                )))
            }
        }
        pager.adapter = adapter
        PagerSnapHelper().attachToRecyclerView(pager)
        pager.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, state: Int) {
                if (state == RecyclerView.SCROLL_STATE_IDLE) {
                    (rv.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition().takeIf { it >= 0 }?.let { updateCounter(it) }
                }
            }
        })
        root.addView(pager)
        dialog.setContentView(root)
        dialog.show()
        updateCounter(0)
        // Read the first card aloud, like the alert does.
        DayJobSpeaker.speak(context, spokenIntro(list[0]))
        return dialog
    }

    private fun spokenIntro(job: DayJob): String =
        if (job.myBooking?.teamAsk == true) "${job.myBooking.leaderName ?: "लीडर"} ने आपको काम के लिए चुना है। ${job.title}। जाऊँगा या नहीं जाऊँगा दबाइए।"
        else "आपके लिए नया काम आया है। ${job.title}, ${job.wagePerDay} रुपये रोज़।"

    private fun page(context: Context, job: DayJob, actions: Actions): View {
        val dp = { v: Int -> DayJobUi.dp(context, v) }
        val teamAsk = job.myBooking?.teamAsk == true
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(16))
            setBackgroundResource(R.drawable.bg_day_job_card)
            layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            job.photos.firstOrNull()?.let { url ->
                addView(ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(170)).apply { topMargin = dp(12) }
                    Glide.with(context).load(url).into(this)
                })
            }
            if (teamAsk) {
                addView(text(context, "${job.myBooking?.leaderName ?: "आपके लीडर"} ने आपको चुना है", 17f, R.color.day_job_green, bold = true).apply { setPadding(0, dp(12), 0, 0) })
            }
            addView(text(context, job.title, 22f, R.color.white, bold = true).apply { setPadding(0, dp(12), 0, dp(4)) })
            addView(TextView(context).apply {
                text = DayJobUi.factsText(DayJobFormat.jobCardFacts(job))
                textSize = 17f
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setLineSpacing(dp(5).toFloat(), 1f)
                setBackgroundResource(R.drawable.bg_day_job_highlight)
                setPadding(dp(14), dp(12), dp(14), dp(12))
            })
            addView(button(context, "🔊  सुनें", 1) { DayJobSpeaker.speak(context, DayJobFormat.spokenCard(job, DayJobFormat.jobCardFacts(job)), always = true) })
            if (teamAsk) {
                addView(button(context, "✅  जाऊँगा", 2) { actions.onTeamYes(job) })
                addView(button(context, "❌  नहीं जाऊँगा", 3) { actions.onTeamNo(job) })
            } else if (job.teamTurn != null) {
                addView(button(context, "👥  टीम की बारी: खोलें", 2) { actions.onOpen(job) })
            } else {
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(button(context, "❌  ना", 3) { actions.onNo(job) }.apply {
                        layoutParams = LinearLayout.LayoutParams(0, dp(60), 1f).apply { marginEnd = dp(6) }
                    })
                    addView(button(context, "✅  हाँ", 2) { actions.onYes(job) }.apply {
                        layoutParams = LinearLayout.LayoutParams(0, dp(60), 1f).apply { marginStart = dp(6) }
                    })
                })
            }
            addView(text(context, "पूरी जानकारी देखें ›", 15f, R.color.brand_color_yellow, bold = true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(4))
                setOnClickListener { actions.onOpen(job) }
            })
        }.let { card ->
            // Cards sit inside a page-wide frame so the next one peeks only after a swipe.
            LinearLayout(context).apply {
                setPadding(dp(16), 0, dp(16), 0)
                addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }
    }

    private fun text(context: Context, value: String, size: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(ContextCompat.getColor(context, color))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    /** 1 outline, 2 green, 3 red-ish outline. */
    private fun button(context: Context, label: String, style: Int, onClick: () -> Unit) = TextView(context).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 19f
        setTypeface(typeface, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DayJobUi.dp(context, 56)).apply { topMargin = DayJobUi.dp(context, 10) }
        when (style) {
            2 -> { setBackgroundResource(R.drawable.bg_day_job_green_button); setTextColor(ContextCompat.getColor(context, R.color.day_job_on_green)) }
            3 -> { setBackgroundResource(R.drawable.bg_free_job_map_button); setTextColor(0xFFFF6B6B.toInt()) }
            else -> { setBackgroundResource(R.drawable.bg_free_job_map_button); setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow)) }
        }
        setOnClickListener { onClick() }
    }
}
