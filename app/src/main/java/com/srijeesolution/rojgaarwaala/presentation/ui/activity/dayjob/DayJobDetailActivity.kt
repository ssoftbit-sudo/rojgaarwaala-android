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
    /** True when the viewer leads a team, so they can take seats for it. */
    private var leadsTeam = false
    private var cancelReasons: List<String> = emptyList()
    /** Booking to start sharing for once location is allowed. */
    private var pendingShare: Pair<Int, Int>? = null
    private val askLocation = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val share = pendingShare ?: return@registerForActivityResult
        pendingShare = null
        if (granted[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) startSharing(share.first, share.second)
        else DayJobUi.toast(this, "लोकेशन की इजाज़त नहीं मिली, इसलिए कंपनी आपको नक्शे पर नहीं देख पाएगी।")
    }

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
            (repository.meta() as? DayJobOutcome.Ok)?.let { myTrades = it.data.worker.trades; cancelReasons = it.data.cancelReasons }
            (repository.team() as? DayJobOutcome.Ok)?.let { leadsTeam = it.data.role == "leader" }
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
            DayJobUi.ratingText(job.company.ratingAvg, job.company.ratingCount).ifBlank { null },
            job.company.paidRate?.let { "$it% मज़दूरों को समय पर पैसा मिला" },
        ).joinToString(" · ")
        binding.detailWage.text = DayJobFormat.wageText(job.wagePerDay)
        binding.detailWhen.text = "${DayJobFormat.whenText(job.workDate, job.startTime)} · ${job.hours} घंटे"
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
                    text = if (slot.left == 0) "${slot.needed} · भर गईं" else "${slot.filled}/${slot.needed} · ${slot.left} खाली"
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
            actions.addView(note("इस काम के लिए अब मज़दूर नहीं लिए जा रहे।"))
        } else {
            job.slots.filter { it.left > 0 }.forEach { slot ->
                if (slot.trade in myTrades) {
                    actions.addView(button("हाँ, ${slot.label} बनकर आऊँगा", 0) { confirmAccept(job, slot.trade, slot.label) })
                } else {
                    actions.addView(note("${slot.label}: यह जगह लेने के लिए 'काम ढूँढें' में 'बदलें' से इसे अपने काम में जोड़ें।"))
                }
            }
            if (job.slots.none { it.left > 0 }) actions.addView(note("सारी जगह भर गई हैं।"))
            else if (leadsTeam) actions.addView(button("टीम के साथ हाँ करें", 2) { pickTeam(job) })
        }
        actions.addView(button("रास्ता देखें", 1) { DayJobUi.directions(this, job) })
    }

    private fun renderBooked(job: DayJob) {
        val actions = binding.detailActions
        val trade = job.slots.firstOrNull { it.trade == job.myBooking?.trade }?.label ?: ""
        val booking = job.myBooking!!
        when {
            booking.doneByMe -> actions.addView(note(
                (if (booking.paidAnswer == "no") "आपने बताया कि पूरा पैसा नहीं मिला। हमारी टीम कंपनी से बात करेगी।"
                else "✓ Job Done" + (booking.myRating?.let { " · आपने कंपनी को $it ★ दिए" } ?: "")) +
                    (booking.companyRating?.let { "\nकंपनी ने आपको $it ★ दिए।" } ?: ""),
                R.color.day_job_green,
            ))
            booking.canMarkDone -> {
                actions.addView(note("काम पूरा हुआ? पूरा पैसा मिलने पर Job Done दबाएँ। तभी अगला काम ले पाएँगे।", R.color.white))
                actions.addView(button("पैसा मिल गया, Job Done", 2) {
                    DayJobUi.askStars(this, "Job Done", "${job.company.name} को कितने स्टार देंगे?") { stars ->
                        act { repository.workerDone(booking.id, true, stars) }
                    }
                })
                actions.addView(button("पूरा पैसा नहीं मिला", 1) {
                    AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
                        .setTitle("पूरा पैसा नहीं मिला?")
                        .setMessage("हमारी टीम कंपनी से बात करेगी। आप नया काम ले सकेंगे।")
                        .setPositiveButton("हाँ, नहीं मिला") { _, _ -> act { repository.workerDone(booking.id, false, null) } }
                        .setNegativeButton("रहने दें", null)
                        .show()
                })
            }
            booking.bookedByLeader && job.isAccepting && !booking.confirmAsked -> actions.addView(note(
                "${booking.leaderName ?: "आपके लीडर"} ने आपकी जगह पक्की की। आप $trade बनकर जा रहे हैं। न जा सकें तो नीचे 'नहीं जा सकता' दबाएँ।",
                R.color.day_job_green,
            ))
            booking.confirmAsked && !booking.confirmed && job.isAccepting -> {
                actions.addView(note("क्या आप इस काम पर आ रहे हैं?", R.color.white))
                actions.addView(button("हाँ, मैं आ रहा हूँ", 2) { act { repository.confirm(booking.id, true) } })
                actions.addView(button("नहीं, मेरी जगह किसी और को दें", 1) { act { repository.confirm(booking.id, false) } })
            }
            else -> actions.addView(note(
                (if (booking.confirmed) "✓ आपने पक्का किया। " else "") +
                    "आप $trade बनकर जा रहे हैं। समय पर पहुँचें, कुछ बदले तो कंपनी को Call करें।",
                R.color.day_job_green,
            ))
        }
        // On the way: nothing is shared until the worker taps this.
        val sharingHere = com.srijeesolution.rojgaarwaala.utils.DayJobTrackingService.activeBooking(this) == booking.id
        when {
            booking.arrived -> actions.addView(note("✓ आप काम की जगह पहुँच गए।", R.color.day_job_green))
            sharingHere -> {
                actions.addView(note("● कंपनी आपको नक्शे पर देख रही है। पहुँचते ही अपने-आप बंद हो जाएगा।", R.color.day_job_green))
                actions.addView(button("लोकेशन भेजना रोकें", 1) {
                    com.srijeesolution.rojgaarwaala.utils.DayJobTrackingService.stop(this)
                    binding.root.postDelayed({ load() }, 800)
                })
            }
            booking.canShareLocation -> actions.addView(button("घर से निकल पड़ा", 0) { askToShare(booking.id, job.id) })
        }
        actions.addView(button("कंपनी को Call करें", 2) { DayJobUi.call(this, job.company.phone) })
        actions.addView(button("रास्ता देखें", 1) { DayJobUi.directions(this, job) })
        // While "Are you coming?" is shown, its No already gives the seat back.
        val asking = booking.confirmAsked && !booking.confirmed
        if (job.isAccepting && !asking) {
            val byLeader = booking.bookedByLeader
            actions.addView(button(if (byLeader) "नहीं जा सकता" else "मेरी जगह छोड़ें", 1) {
                AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
                    .setTitle(if (byLeader) "आप नहीं जा सकते?" else "अपनी जगह छोड़ें?")
                    .setMessage(if (byLeader) "लीडर और कंपनी को ख़बर जाएगी, और कोई और यह जगह ले सकेगा।" else "कंपनी को ख़बर जाएगी और कोई और यह जगह ले सकेगा।")
                    .setPositiveButton("हाँ, छोड़ दें") { _, _ ->
                        DayJobUi.askReason(this, "जगह क्यों छोड़ रहे हैं?", cancelReasons) { reason ->
                            act { repository.cancelBooking(job.myBooking!!.id, reason) }
                        }
                    }
                    .setNegativeButton("नहीं", null)
                    .show()
            })
        }
    }

    private fun renderOwner(job: DayJob) {
        val actions = binding.detailActions
        if (job.status == "in_review") {
            actions.addView(note("कंपनी की जाँच पूरी होते ही यह काम सबको दिखेगा।"))
        } else if (job.notifiedCount > 0) {
            actions.addView(note("${job.radiusKm} km के ${job.notifiedCount} ${if (job.notifiedCount == 1) "मज़दूर" else "मज़दूरों"} को ख़बर भेजी गई।"))
        }
        if (job.canTrack && job.filledTotal > 0) {
            actions.addView(button("नक्शे पर देखें, कौन कहाँ है", 2) { startActivity(DayJobTrackActivity.intent(this, job.id)) })
        }
        val waitingDone = job.bookings.orEmpty().filter { it.canMarkDone }
        if (waitingDone.isNotEmpty()) {
            actions.addView(note("काम पूरा हुआ? मज़दूरों को स्टार देकर Job Done दबाएँ। तभी नया काम डाल पाएँगे।", R.color.white))
            if (waitingDone.size > 1) actions.addView(button("सबका Job Done (${waitingDone.size})", 2) {
                DayJobUi.askStars(this, "सबका Job Done", "सभी ${waitingDone.size} मज़दूरों को कितने स्टार देंगे? किसी को अलग देना हो तो नीचे उसके नाम पर Job Done दबाएँ।") { stars ->
                    act { repository.companyDoneAll(job.id, stars) }
                }
            })
        }
        actions.addView(button("यही काम दोबारा डालें", 0) { askRepost(job) })
        if (job.status in listOf("in_review", "open", "full", "closed")) {
            actions.addView(button("काम रद्द करें", 1) {
                AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
                    .setTitle("यह काम रद्द करें?")
                    .setMessage("जिसने भी हाँ कहा है, सबको ख़बर जाएगी।")
                    .setPositiveButton("काम रद्द करें") { _, _ -> act { repository.cancelJob(job.id) } }
                    .setNegativeButton("नहीं", null)
                    .show()
            })
        }

        val list = binding.detailWorkers
        list.removeAllViews()
        val coming = job.bookings.orEmpty().filter { it.status == "accepted" || it.status == "no_show" }
        binding.detailWorkersTitle.visibility = View.VISIBLE
        binding.detailWorkersTitle.text = "कौन आ रहा है (${job.filledTotal}/${job.neededTotal})"
        if (coming.isEmpty()) list.addView(note("अभी कोई नहीं। कोई हाँ करेगा तो आपको ख़बर आएगी।"))
        // A team shows as one block with a single Call, to its leader.
        val teams = coming.filter { it.teamName != null }.groupBy { it.teamName!! }
        teams.forEach { (name, people) ->
            list.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_day_job_card)
                setPadding(DayJobUi.dp(context, 12), DayJobUi.dp(context, 10), DayJobUi.dp(context, 8), DayJobUi.dp(context, 10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = DayJobUi.dp(context, 8) }
                addView(TextView(context).apply {
                    val lines = people.joinToString("\n") { b ->
                        "• ${b.name ?: "मज़दूर"} · ${b.tradeLabel.orEmpty()}" + when {
                            b.status == "no_show" -> " · नहीं आया"
                            b.companyDone -> " · Job Done ${b.companyRating ?: ""}★"
                            b.confirmed -> " · ✓"
                            else -> ""
                        }
                    }
                    text = "$name टीम (${people.size})\nलीडर: ${people.first().leaderName.orEmpty()}\n$lines"
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(context).apply {
                    text = "Call"
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.day_job_on_green))
                    setBackgroundResource(R.drawable.bg_day_job_green_button)
                    setPadding(DayJobUi.dp(context, 16), DayJobUi.dp(context, 8), DayJobUi.dp(context, 16), DayJobUi.dp(context, 8))
                    contentDescription = "लीडर को Call करें"
                    setOnClickListener { DayJobUi.call(context, people.first().callMobile) }
                })
            })
        }
        coming.filter { it.teamName == null }.forEach { b ->
            list.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_day_job_pill)
                setPadding(DayJobUi.dp(context, 12), DayJobUi.dp(context, 10), DayJobUi.dp(context, 8), DayJobUi.dp(context, 10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = DayJobUi.dp(context, 8) }
                addView(TextView(context).apply {
                    val state = when {
                        b.status == "no_show" -> " · नहीं आया"
                        b.companyDone -> " · Job Done, आपने ${b.companyRating ?: ""}★ दिए"
                        b.confirmed -> " · ✓ पक्का आएगा"
                        else -> ""
                    }
                    val rating = DayJobUi.ratingText(b.workerRatingAvg, b.workerRatingCount)
                    text = "${b.name ?: "मज़दूर"}${if (rating.isBlank()) "" else "  $rating"}\n${b.tradeLabel.orEmpty()}$state" +
                        if (b.notPaid) "\nमज़दूर ने कहा: पूरा पैसा नहीं मिला" else ""
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                if (b.status == "accepted" && b.workerId > 0) addView(TextView(context).apply {
                    text = if (b.favourite) "★" else "☆"
                    textSize = 22f
                    contentDescription = if (b.favourite) "पसंदीदा से हटाएँ" else "पसंदीदा में जोड़ें"
                    setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow))
                    setPadding(DayJobUi.dp(context, 8), 0, DayJobUi.dp(context, 8), 0)
                    setOnClickListener { act { repository.toggleFavourite(b.workerId) } }
                })
                if (b.canMarkDone) addView(TextView(context).apply {
                    text = "Job Done"
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow))
                    setPadding(DayJobUi.dp(context, 10), DayJobUi.dp(context, 8), DayJobUi.dp(context, 10), DayJobUi.dp(context, 8))
                    setOnClickListener {
                        DayJobUi.askStars(this@DayJobDetailActivity, "Job Done", "${b.name ?: "मज़दूर"} को कितने स्टार देंगे?") { stars ->
                            act { repository.companyDone(b.id, stars) }
                        }
                    }
                })
                if (b.canMarkNoShow) addView(TextView(context).apply {
                    text = "नहीं आया"
                    setTextColor(ContextCompat.getColor(context, R.color.new_tag_red))
                    setPadding(DayJobUi.dp(context, 10), DayJobUi.dp(context, 8), DayJobUi.dp(context, 10), DayJobUi.dp(context, 8))
                    setOnClickListener {
                        AlertDialog.Builder(this@DayJobDetailActivity, R.style.Theme_Rojgaarwaala_AlertDialog)
                            .setTitle("${b.name ?: "मज़दूर"} नहीं आया?")
                            .setMessage("तभी दबाएँ जब वह बिल्कुल नहीं आया। काम छोड़ने वालों को कम काम की ख़बर जाती है।")
                            .setPositiveButton("हाँ, नहीं आया") { _, _ -> act { repository.markNoShow(b.id) } }
                            .setNegativeButton("रहने दें", null)
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
            .setTitle("यह काम लेना है?")
            .setMessage("$label · ${DayJobFormat.wageText(job.wagePerDay)}\n${DayJobFormat.whenText(job.workDate, job.startTime)}\n${job.place}\n\nपक्का आना हो तभी हाँ करें।")
            .setPositiveButton("हाँ, मैं आऊँगा") { _, _ -> act { repository.accept(job.id, trade) } }
            .setNegativeButton("नहीं", null)
            .show()
    }

    /** The leader ticks who goes and as what; all seats are taken together or none. */
    private fun pickTeam(job: DayJob) {
        binding.detailLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.teamOptions(job.id)
            binding.detailLoading.visibility = View.GONE
            val people = (result as? DayJobOutcome.Ok)?.data ?: run {
                DayJobUi.toast(this@DayJobDetailActivity, (result as DayJobOutcome.Fail).message); return@launch
            }
            val ready = people.filter { it.available }
            if (ready.isEmpty()) {
                DayJobUi.toast(this@DayJobDetailActivity, "टीम में अभी कोई इस काम के लिए खाली नहीं है।")
                return@launch
            }
            // One row per person and work they can do, e.g. "Ramesh · हेल्पर".
            val rows = ready.flatMap { p -> p.trades.map { t -> Triple(p.userId, t.key, "${p.name ?: "मज़दूर"} · ${t.label}") } }
            val checked = BooleanArray(rows.size)
            val busy = people.filter { !it.available }.joinToString("\n") { "${it.name}: ${it.reason.orEmpty()}" }
            AlertDialog.Builder(this@DayJobDetailActivity, R.style.Theme_Rojgaarwaala_AlertDialog)
                .setTitle("कौन जाएगा?")
                .setMultiChoiceItems(rows.map { it.third }.toTypedArray(), checked) { _, i, on -> checked[i] = on }
                .setPositiveButton("सबकी जगह पक्की करें") { _, _ ->
                    val picks = rows.filterIndexed { i, _ -> checked[i] }.distinctBy { it.first }
                        .map { com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamPick(it.first, it.second) }
                    if (picks.isEmpty()) DayJobUi.toast(this@DayJobDetailActivity, "कम से कम एक व्यक्ति चुनें।")
                    else act { repository.acceptForTeam(job.id, picks) }
                }
                .setNegativeButton("रहने दें", null)
                .show()
            if (busy.isNotBlank()) DayJobUi.toast(this@DayJobDetailActivity, "नहीं जा सकते:\n$busy")
        }
    }

    private fun askToShare(bookingId: Int, jobId: Int) {
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle("काम पर निकल रहे हैं?")
            .setMessage("रास्ते में कंपनी आपको नक्शे पर देख सकेगी। काम की जगह पहुँचते ही लोकेशन भेजना अपने-आप बंद हो जाएगा। आप कभी भी रोक सकते हैं।")
            .setPositiveButton("हाँ, निकल पड़ा") { _, _ ->
                val fine = android.Manifest.permission.ACCESS_FINE_LOCATION
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, fine) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    startSharing(bookingId, jobId)
                } else {
                    pendingShare = bookingId to jobId
                    val perms = mutableListOf(fine, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (android.os.Build.VERSION.SDK_INT >= 33) perms += android.Manifest.permission.POST_NOTIFICATIONS
                    askLocation.launch(perms.toTypedArray())
                }
            }
            .setNegativeButton("नहीं", null)
            .show()
    }

    private fun startSharing(bookingId: Int, jobId: Int) {
        com.srijeesolution.rojgaarwaala.utils.DayJobTrackingService.start(this, bookingId, jobId)
        DayJobUi.toast(this, "ठीक है, कंपनी को ख़बर भेज दी। सुरक्षित पहुँचें।")
        binding.root.postDelayed({ load() }, 800)
    }

    private fun askRepost(job: DayJob) {
        val tomorrow = LocalDate.now().plusDays(1)
        DatePickerDialog(this, { _, y, m, d ->
            val date = LocalDate.of(y, m + 1, d).toString()
            act { repository.repost(job.id, date, null, null) }
        }, tomorrow.year, tomorrow.monthValue - 1, tomorrow.dayOfMonth).apply {
            setTitle("किस तारीख़ के लिए दोबारा डालें?")
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
