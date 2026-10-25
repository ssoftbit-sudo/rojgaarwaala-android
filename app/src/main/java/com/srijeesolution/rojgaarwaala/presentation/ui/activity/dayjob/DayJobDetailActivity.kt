package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJob
import com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome
import com.srijeesolution.rojgaarwaala.data.repository.DayJobRepository
import com.srijeesolution.rojgaarwaala.databinding.ActivityDayJobDetailBinding
import com.srijeesolution.rojgaarwaala.utils.DayJobFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * One job, map on top like a ride app. What the buttons do depends on who looks:
 * a worker can accept a seat, a worker who is coming can call or cancel, and the
 * company sees who is coming and can cancel or post the job again.
 */
@AndroidEntryPoint
class DayJobDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ID = "day_job_id"
        fun intent(context: Context, id: Int) = Intent(context, DayJobDetailActivity::class.java).putExtra(EXTRA_ID, id)
    }

    @Inject lateinit var repository: DayJobRepository

    private lateinit var binding: ActivityDayJobDetailBinding
    private var job: DayJob? = null
    /** Work the viewer chose as a worker; seats for other work are shown but not offered. */
    private var myTrades: List<String> = emptyList()
    private var mapReady: SupportMapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDayJobDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.detailBack.setOnClickListener { finish() }
        val fragment = SupportMapFragment.newInstance(GoogleMapOptions().liteMode(false).mapToolbarEnabled(false))
        supportFragmentManager.beginTransaction().replace(R.id.detailMap, fragment).commitNowAllowingStateLoss()
        mapReady = fragment
        load()
        tickCountdown()
    }

    private fun load() {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        binding.detailLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            (repository.meta() as? DayJobOutcome.Ok)?.let { myTrades = it.data.worker.trades }
            val result = repository.show(id)
            binding.detailLoading.visibility = View.GONE
            when (result) {
                is DayJobOutcome.Ok -> result.data?.let { render(it) }
                is DayJobOutcome.Fail -> { DayJobUi.toast(this@DayJobDetailActivity, result.message); finish() }
            }
        }
    }

    private fun tickCountdown() {
        lifecycleScope.launch {
            while (isActive) {
                job?.let { binding.detailCloses.text = if (it.isAccepting) DayJobFormat.closesInText(it.acceptClosesAt) else DayJobFormat.statusText(it.status) }
                delay(30_000)
            }
        }
    }

    private fun render(job: DayJob) {
        this.job = job
        setResult(RESULT_OK)
        val isOwner = job.bookings != null
        val booked = job.myBooking?.status == "accepted"

        binding.detailTitle.text = job.title
        binding.detailCompany.text = listOfNotNull(
            job.company.name,
            job.company.paidRate?.let { "$it% workers paid on time" },
        ).joinToString(" · ")
        binding.detailWage.text = DayJobFormat.wageText(job.wagePerDay)
        binding.detailWhen.text = "${DayJobFormat.whenText(job.workDate, job.startTime)} · ${job.hours} hours"
        binding.detailPlace.text = listOfNotNull(job.place.ifBlank { null }, job.district).joinToString(" · ")
        binding.detailDistance.text = if (isOwner) "" else DayJobFormat.distanceText(job.distanceKm)
        binding.detailCloses.text = if (job.isAccepting) DayJobFormat.closesInText(job.acceptClosesAt) else DayJobFormat.statusText(job.status)
        DayJobUi.styleStatus(binding.detailStatus, job.status)
        binding.detailStatus.text = if (isOwner) "${DayJobFormat.statusText(job.status)} · ${DayJobFormat.comingText(job)}" else DayJobFormat.statusText(job.status)
        binding.detailDescription.text = job.description.orEmpty()
        binding.detailDescription.visibility = if (job.description.isNullOrBlank()) View.GONE else View.VISIBLE

        renderSlots(job)
        renderPhotos(job)
        renderMap(job)
        binding.detailActions.removeAllViews()
        when {
            isOwner -> renderOwner(job)
            booked -> renderBooked(job)
            else -> renderWorker(job)
        }
    }

    private fun renderSlots(job: DayJob) {
        val box = binding.detailSlots
        box.removeAllViews()
        job.slots.forEach { slot ->
            box.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, DayJobUi.dp(context, 4), 0, DayJobUi.dp(context, 4))
                addView(TextView(context).apply {
                    text = slot.label
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    textSize = 15f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(context).apply {
                    text = if (slot.left == 0) "${slot.needed} · full" else "${slot.filled}/${slot.needed} · ${slot.left} left"
                    setTextColor(ContextCompat.getColor(context, if (slot.left == 0) R.color.day_job_green else R.color.search_hint))
                    textSize = 14f
                })
            })
        }
    }

    private fun renderPhotos(job: DayJob) {
        binding.detailPhotosScroll.visibility = if (job.photos.isEmpty()) View.GONE else View.VISIBLE
        val box = binding.detailPhotos
        box.removeAllViews()
        val size = DayJobUi.dp(this, 90)
        job.photos.forEach { url ->
            box.addView(ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(size * 4 / 3, size).apply { marginEnd = DayJobUi.dp(context, 8) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                setBackgroundResource(R.drawable.bg_day_job_card)
                Glide.with(this).load(url).into(this)
            })
        }
    }

    private fun renderMap(job: DayJob) {
        mapReady?.getMapAsync { map ->
            runCatching { map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark)) }
            map.clear()
            val point = LatLng(job.latitude, job.longitude)
            map.addMarker(MarkerOptions().position(point).title(job.place.ifBlank { job.title })
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 14f))
            map.uiSettings.isMapToolbarEnabled = false
        }
    }

    // ------------------------------------------------------------ buttons

    private fun button(text: String, style: Int, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        textSize = 16f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, DayJobUi.dp(context, 50)).apply {
            bottomMargin = DayJobUi.dp(context, 10)
        }
        when (style) {
            0 -> { setBackgroundResource(R.drawable.bg_brand_button); setTextColor(ContextCompat.getColor(context, R.color.on_brand)) }
            1 -> { setBackgroundResource(R.drawable.bg_free_job_map_button); setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow)) }
            else -> { setBackgroundResource(R.drawable.bg_day_job_green_button); setTextColor(ContextCompat.getColor(context, R.color.day_job_on_green)) }
        }
        setOnClickListener { onClick() }
    }

    private fun renderWorker(job: DayJob) {
        val actions = binding.detailActions
        if (!job.isAccepting) {
            actions.addView(note("This job is not taking workers now."))
        } else {
            job.slots.filter { it.left > 0 }.forEach { slot ->
                if (slot.trade in myTrades) {
                    actions.addView(button("Accept as ${slot.label}", 0) { confirmAccept(job, slot.trade, slot.label) })
                } else {
                    actions.addView(note("${slot.label}: add it to your work (Edit on Find Work) to take this seat."))
                }
            }
            if (job.slots.none { it.left > 0 }) actions.addView(note("All seats are taken."))
        }
        actions.addView(button("Directions", 1) { DayJobUi.directions(this, job) })
    }

    private fun renderBooked(job: DayJob) {
        val actions = binding.detailActions
        val trade = job.slots.firstOrNull { it.trade == job.myBooking?.trade }?.label ?: ""
        val booking = job.myBooking!!
        when {
            booking.canAnswerPayment -> {
                actions.addView(note("Did you get full payment for this job?", R.color.white))
                actions.addView(button("Yes, I got full payment", 2) { act { repository.answerPaid(booking.id, true) } })
                actions.addView(button("No, not paid fully", 1) { act { repository.answerPaid(booking.id, false) } })
            }
            booking.paidAnswer != null -> actions.addView(note("Thanks for telling us about the payment.", R.color.day_job_green))
            booking.confirmAsked && !booking.confirmed && job.isAccepting -> {
                actions.addView(note("Are you coming for this job?", R.color.white))
                actions.addView(button("Yes, I am coming", 2) { act { repository.confirm(booking.id, true) } })
                actions.addView(button("No, give my seat to someone", 1) { act { repository.confirm(booking.id, false) } })
            }
            else -> actions.addView(note(
                (if (booking.confirmed) "✓ You confirmed. " else "") +
                    "You're going as $trade. Reach on time and call the company if anything changes.",
                R.color.day_job_green,
            ))
        }
        actions.addView(button("Call company", 2) { DayJobUi.call(this, job.company.phone) })
        actions.addView(button("Directions", 1) { DayJobUi.directions(this, job) })
        // While "Are you coming?" is shown, its No already gives the seat back.
        val asking = booking.confirmAsked && !booking.confirmed
        if (job.isAccepting && !asking) {
            actions.addView(button("Cancel my seat", 1) {
                AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
                    .setTitle("Cancel your seat?")
                    .setMessage("The company will be told and someone else can take it.")
                    .setPositiveButton("Yes, cancel") { _, _ -> act { repository.cancelBooking(job.myBooking!!.id) } }
                    .setNegativeButton("No", null)
                    .show()
            })
        }
    }

    private fun renderOwner(job: DayJob) {
        val actions = binding.detailActions
        if (job.status == "in_review") {
            actions.addView(note("This job goes live once your company is verified."))
        } else if (job.notifiedCount > 0) {
            actions.addView(note("Sent to ${job.notifiedCount} ${if (job.notifiedCount == 1) "worker" else "workers"} within ${job.radiusKm} km."))
        }
        actions.addView(button("Post again", 0) { askRepost(job) })
        if (job.status in listOf("in_review", "open", "full", "closed")) {
            actions.addView(button("Cancel job", 1) {
                AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
                    .setTitle("Cancel this job?")
                    .setMessage("Everyone who accepted will get a notification.")
                    .setPositiveButton("Cancel job") { _, _ -> act { repository.cancelJob(job.id) } }
                    .setNegativeButton("Keep", null)
                    .show()
            })
        }

        val list = binding.detailWorkers
        list.removeAllViews()
        val coming = job.bookings.orEmpty().filter { it.status == "accepted" || it.status == "no_show" }
        binding.detailWorkersTitle.visibility = View.VISIBLE
        binding.detailWorkersTitle.text = "Who is coming (${job.filledTotal}/${job.neededTotal})"
        if (coming.isEmpty()) list.addView(note("No one yet. You will get a notification when someone accepts."))
        coming.forEach { b ->
            list.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_day_job_pill)
                setPadding(DayJobUi.dp(context, 12), DayJobUi.dp(context, 10), DayJobUi.dp(context, 8), DayJobUi.dp(context, 10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = DayJobUi.dp(context, 8) }
                addView(TextView(context).apply {
                    val state = when {
                        b.status == "no_show" -> " · Did not come"
                        b.confirmed -> " · ✓ Confirmed"
                        else -> ""
                    }
                    text = "${b.name ?: "Worker"}\n${b.tradeLabel.orEmpty()}$state"
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                if (b.canMarkNoShow) addView(TextView(context).apply {
                    text = "Didn't come"
                    setTextColor(ContextCompat.getColor(context, R.color.new_tag_red))
                    setPadding(DayJobUi.dp(context, 10), DayJobUi.dp(context, 8), DayJobUi.dp(context, 10), DayJobUi.dp(context, 8))
                    setOnClickListener {
                        AlertDialog.Builder(this@DayJobDetailActivity, R.style.Theme_Rojgaarwaala_AlertDialog)
                            .setTitle("${b.name ?: "Worker"} did not come?")
                            .setMessage("Only mark this if they did not come at all. Workers who miss jobs get fewer alerts.")
                            .setPositiveButton("Yes, did not come") { _, _ -> act { repository.markNoShow(b.id) } }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                })
                addView(TextView(context).apply {
                    text = "Call"
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.day_job_on_green))
                    setBackgroundResource(R.drawable.bg_day_job_green_button)
                    setPadding(DayJobUi.dp(context, 16), DayJobUi.dp(context, 8), DayJobUi.dp(context, 16), DayJobUi.dp(context, 8))
                    setOnClickListener { DayJobUi.call(context, b.mobile) }
                })
            })
        }
    }

    private fun note(text: String, color: Int = R.color.search_hint) = TextView(this).apply {
        this.text = text
        setTextColor(ContextCompat.getColor(context, color))
        textSize = 14f
        setPadding(0, 0, 0, DayJobUi.dp(context, 12))
    }

    private fun confirmAccept(job: DayJob, trade: String, label: String) {
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle("Accept this job?")
            .setMessage("$label · ${DayJobFormat.wageText(job.wagePerDay)}\n${DayJobFormat.whenText(job.workDate, job.startTime)}\n${job.place}\n\nOnly accept if you will surely come.")
            .setPositiveButton("Yes, I will come") { _, _ -> act { repository.accept(job.id, trade) } }
            .setNegativeButton("No", null)
            .show()
    }

    private fun askRepost(job: DayJob) {
        val tomorrow = LocalDate.now().plusDays(1)
        DatePickerDialog(this, { _, y, m, d ->
            val date = LocalDate.of(y, m + 1, d).toString()
            act { repository.repost(job.id, date, null, null) }
        }, tomorrow.year, tomorrow.monthValue - 1, tomorrow.dayOfMonth).apply {
            setTitle("Post again for which date?")
            datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.maxDate = System.currentTimeMillis() + 14L * 24 * 3600 * 1000
        }.show()
    }

    /** Runs an action, shows the server's message, and reloads (or opens the new job). */
    private fun act(block: suspend () -> DayJobOutcome<Any?>) {
        binding.detailLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = block()
            binding.detailLoading.visibility = View.GONE
            when (result) {
                is DayJobOutcome.Ok -> {
                    DayJobUi.toast(this@DayJobDetailActivity, result.message)
                    val newJob = result.data as? DayJob
                    if (newJob != null && newJob.id != job?.id) {
                        startActivity(DayJobDetailActivity.intent(this@DayJobDetailActivity, newJob.id))
                        finish()
                    } else {
                        load()
                    }
                }
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobDetailActivity, result.message)
            }
        }
    }
}
