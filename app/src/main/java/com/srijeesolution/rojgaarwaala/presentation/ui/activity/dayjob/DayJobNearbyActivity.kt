package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJob
import com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome
import com.srijeesolution.rojgaarwaala.data.repository.DayJobRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * All work near the worker on its own page. It opens on the swipe Job Cards; closing
 * them leaves the list, and "कार्ड में देखें" brings them back.
 */
@AndroidEntryPoint
class DayJobNearbyActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_LAT = "lat"
        private const val EXTRA_LNG = "lng"

        fun intent(context: Context, lat: Double?, lng: Double?) =
            Intent(context, DayJobNearbyActivity::class.java).apply {
                lat?.let { putExtra(EXTRA_LAT, it) }
                lng?.let { putExtra(EXTRA_LNG, it) }
            }
    }

    @Inject lateinit var repository: DayJobRepository

    private val adapter = DayJobAdapter(forCompany = false) { openJob(it) }
    private lateinit var loading: ProgressBar
    private lateinit var empty: TextView
    private var jobs: List<DayJob> = emptyList()
    private var cards: android.app.Dialog? = null
    private var cardsShown = false

    private val returnRefresh = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { load() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildScreen())
        load()
    }

    override fun onDestroy() {
        cards?.dismiss()
        super.onDestroy()
    }

    private fun buildScreen(): View {
        val dp = { v: Int -> DayJobUi.dp(this, v) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.app_background))
        }
        // Margins, not padding: the theme's system-bar insets replace a root's padding.
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(inner, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            setMargins(dp(16), dp(12), dp(16), 0)
        })
        inner.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                text = "←"
                textSize = 26f
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setPadding(0, dp(4), dp(16), dp(4))
                setOnClickListener { finish() }
            })
            addView(TextView(context).apply {
                text = "आपके पास के काम"
                textSize = 21f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.white))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        })
        inner.addView(TextView(this).apply {
            text = "🃏  कार्ड में देखें"
            gravity = Gravity.CENTER
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow))
            setBackgroundResource(R.drawable.bg_free_job_map_button)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(14); bottomMargin = dp(8) }
            setOnClickListener { showCards() }
        })
        loading = ProgressBar(this).apply { visibility = View.GONE }
        inner.addView(loading)
        empty = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 16f
            setPadding(dp(16), dp(32), dp(16), dp(32))
            setTextColor(ContextCompat.getColor(context, R.color.search_hint))
            visibility = View.GONE
        }
        inner.addView(empty)
        inner.addView(RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@DayJobNearbyActivity.adapter
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        return root
    }

    private fun load() {
        val lat = if (intent.hasExtra(EXTRA_LAT)) intent.getDoubleExtra(EXTRA_LAT, 0.0) else null
        val lng = if (intent.hasExtra(EXTRA_LNG)) intent.getDoubleExtra(EXTRA_LNG, 0.0) else null
        loading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.nearby(lat, lng)
            loading.visibility = View.GONE
            when (result) {
                is DayJobOutcome.Ok -> {
                    jobs = result.data.jobs
                    adapter.submit(jobs)
                    empty.text = "अभी आपके पास कोई काम नहीं है।"
                    empty.visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
                    // The page opens on the cards once; after that the list stays.
                    if (!cardsShown) {
                        cardsShown = true
                        showCards()
                    }
                }
                is DayJobOutcome.Fail -> {
                    empty.text = result.message
                    empty.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showCards() {
        if (jobs.isEmpty() || cards?.isShowing == true || isFinishing) return
        cards = DayJobFlash.show(this, jobs, DayJobFlash.Actions(
            onYes = { returnRefresh.launch(DayJobDetailActivity.acceptIntent(this, it.id)) },
            onNo = { job -> lifecycleScope.launch { repository.reject(job.id); load() } },
            onTeamYes = { job -> job.myBooking?.let { b -> answer { repository.book(b.id) } } },
            onTeamNo = { job -> job.myBooking?.let { b -> answer { repository.cancelBooking(b.id) } } },
            onOpen = { openJob(it) },
        ), title = "📋 पास के काम", remember = false)
    }

    private fun answer(block: suspend () -> DayJobOutcome<*>) {
        lifecycleScope.launch {
            when (val result = block()) {
                is DayJobOutcome.Ok -> DayJobUi.toast(this@DayJobNearbyActivity, result.message)
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobNearbyActivity, result.message)
            }
            load()
        }
    }

    private fun openJob(job: DayJob) {
        returnRefresh.launch(DayJobDetailActivity.intent(this, job.id))
    }
}
