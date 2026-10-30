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
        private const val EXTRA_ACCEPT = "accept"
        fun intent(context: Context, id: Int) = Intent(context, DayJobDetailActivity::class.java).putExtra(EXTRA_ID, id)

        /** Opens the job and goes straight to "हाँ" (from the flash card). */
        fun acceptIntent(context: Context, id: Int) = intent(context, id).putExtra(EXTRA_ACCEPT, true)
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
    private var workerTerms: List<String> = emptyList()
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
            (repository.meta() as? DayJobOutcome.Ok)?.let { myTrades = it.data.worker.trades; cancelReasons = it.data.cancelReasons; workerTerms = it.data.workerTerms }
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
            var tick = 0
            while (isActive) {
                updateHoldTimer()
                if (tick++ % 30 == 0) {
                    job?.let { binding.detailCloses.text = if (it.isAccepting) DayJobFormat.closesInText(it.acceptClosesAt) else DayJobFormat.statusText(it.status) }
                }
                delay(1_000)
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

        renderJobCard(job, isOwner)
        renderSlots(job)
        renderPhotos(job)
        renderMap(job)
        binding.detailActions.removeAllViews()
        holdTimer = null
        when {
            isOwner -> renderOwner(job)
            booked -> renderBooked(job)
            else -> renderWorker(job)
        }
    }

    /** A worker sees one big card with every fact, and can have it read out. */
    private fun renderJobCard(job: DayJob, isOwner: Boolean) {
        val facts = DayJobFormat.jobCardFacts(job)
        binding.detailFactsCard.visibility = if (isOwner) View.GONE else View.VISIBLE
        binding.detailListen.visibility = binding.detailFactsCard.visibility
        listOf(binding.detailWage, binding.detailWhen, binding.detailPlace).forEach {
            it.visibility = if (isOwner) View.VISIBLE else View.GONE
        }
        if (isOwner) return
        binding.detailFactsCard.text = DayJobUi.factsText(facts)
        binding.detailListen.setOnClickListener {
            com.srijeesolution.rojgaarwaala.utils.DayJobSpeaker.speak(this, DayJobFormat.spokenCard(job, facts), always = true)
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
        holdTimer = null
        job.teamTurn?.let { turn ->
            renderTeamTurn(job, turn)
            return
        }
        job.canTakeFrom?.let { from ->
            actions.addView(note("यह काम ${DayJobFormat.dayText(from)} ${DayJobFormat.clockText(from)} से ले सकेंगे। 20 rating के बाद 3 दिन आगे तक का काम ले सकते हैं।", R.color.brand_color_yellow))
            if (!job.rejectedByMe) actions.addView(button("✗  नहीं, यह काम नहीं", 1) { act { repository.reject(job.id) } })
            actions.addView(button("🧭  रास्ता देखें", 1) { DayJobUi.directions(this, job) })
            return
        }
        if (job.reservedForMe) {
            actions.addView(note("${job.company.name} ने आपको फिर बुलाया है। यह काम ${DayJobFormat.clockText(job.reservedUntil)} तक सिर्फ़ आपके लिए है।", R.color.day_job_green))
        } else if (job.rejectedByMe && job.isAccepting) {
            actions.addView(note("आपने इस काम को मना किया था। जगह खाली है तो अब भी ले सकते हैं।"))
        }
        if (!job.isAccepting) {
            actions.addView(note("इस काम के लिए अब मज़दूर नहीं लिए जा रहे।"))
        } else {
            job.slots.filter { it.left > 0 }.forEach { slot ->
                if (slot.trade in myTrades) {
                    actions.addView(button("✓  हाँ, ${slot.label} बनकर आऊँगा", 0) { confirmAccept(job, slot.trade, slot.label) })
                } else {
                    actions.addView(note("${slot.label}: यह जगह लेने के लिए 'काम ढूँढें' में 'बदलें' से इसे अपने काम में जोड़ें।"))
                }
            }
            if (intent.getBooleanExtra(EXTRA_ACCEPT, false)) {
                intent.removeExtra(EXTRA_ACCEPT)
                job.slots.filter { it.left > 0 && it.trade in myTrades }.let { open ->
                    when {
                        open.size == 1 -> confirmAccept(job, open[0].trade, open[0].label)
                        leadsTeam && open.isNotEmpty() -> Unit
                        open.size > 1 -> DayJobUi.toast(this, "आप किस काम के लिए आएँगे? नीचे चुनें।")
                    }
                }
            }
            if (job.slots.none { it.left > 0 }) actions.addView(note("सारी जगह भर गई हैं।"))
            else if (leadsTeam) actions.addView(button("👥  टीम के साथ हाँ करें", 2) { pickTeam(job) })
            if (!job.rejectedByMe) actions.addView(button("✗  नहीं, यह काम नहीं", 1) { act { repository.reject(job.id) } })
        }
        actions.addView(button("🧭  रास्ता देखें", 1) { DayJobUi.directions(this, job) })
    }

    private fun renderBooked(job: DayJob) {
        val actions = binding.detailActions
        val trade = job.slots.firstOrNull { it.trade == job.myBooking?.trade }?.label ?: ""
        val booking = job.myBooking!!
        if (booking.onHold) {
            renderHold(job, booking)
            return
        }
        holdTimer = null
        when (booking.otStatus) {
            "asked" -> {
                actions.addView(note("कंपनी पूछ रही है: ${otHoursText(booking.otHours)} और रुक सकते हैं? ₹${booking.otAmount ?: 0} extra मिलेंगे।", R.color.brand_color_yellow))
                actions.addView(button("✓  हाँ, रुकूँगा", 2) { act { repository.answerOvertime(booking.id, true) } })
                actions.addView(button("✗  नहीं रुक सकता", 1) { act { repository.answerOvertime(booking.id, false) } })
            }
            "accepted" -> actions.addView(note("✓ OT: ${otHoursText(booking.otHours)} · ₹${booking.otAmount ?: 0} extra", R.color.day_job_green))
            "requested" -> actions.addView(note("आपने ${otHoursText(booking.otHours)} OT (₹${booking.otAmount ?: 0}) के लिए पूछा है। कंपनी के जवाब का इंतज़ार।", R.color.brand_color_yellow))
            "declined" -> actions.addView(note("कंपनी को अभी OT नहीं चाहिए।"))
        }
        if (booking.canRequestOvertime) {
            actions.addView(button("⏱  काम बाक़ी है? OT के लिए पूछें", 1) {
                val choices = listOf(1.0, 1.5, 2.0, 3.0, 4.0)
                DayJobUi.pickOne(this, "कितने घंटे और रुक सकते हैं?", choices.map { "${otHoursText(it)} · ₹${(booking.hourlyRate * it).toInt()} extra" }) { i ->
                    act { repository.requestOvertime(booking.id, choices[i]) }
                }
            })
        }
        when {
            booking.doneByMe -> actions.addView(note(
                (if (booking.paidAnswer == "no") "आपने बताया कि पूरा पैसा नहीं मिला। हमारी टीम कंपनी से बात करेगी।"
                else "✓ Job Done" + (booking.myRating?.let { " · आपने कंपनी को $it ★ दिए" } ?: "")) +
                    (booking.companyRating?.let { "\nकंपनी ने आपको $it ★ दिए।" } ?: ""),
                R.color.day_job_green,
            ))
            booking.canMarkDone -> {
                actions.addView(note("काम पूरा हुआ? पूरा पैसा मिलने पर Job Done दबाएँ। तभी अगला काम ले पाएँगे।", R.color.white))
                actions.addView(button("✅  पैसा मिल गया, Job Done", 2) {
                    DayJobUi.askStars(this, "Job Done", "${job.company.name} को कितने स्टार देंगे?") { stars ->
                        act { repository.workerDone(booking.id, true, stars) }
                    }
                })
                actions.addView(button("⚠️  पूरा पैसा नहीं मिला", 1) {
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
                actions.addView(button("⏹  लोकेशन भेजना रोकें", 1) {
                    com.srijeesolution.rojgaarwaala.utils.DayJobTrackingService.stop(this)
                    binding.root.postDelayed({ load() }, 800)
                })
            }
            booking.canShareLocation -> actions.addView(button("🛵  घर से निकल पड़ा", 0) { askToShare(booking.id, job.id) })
        }
        actions.addView(button("📞  कंपनी को Call करें", 2) { DayJobUi.call(this, job.company.phone) })
        actions.addView(button("🧭  रास्ता देखें", 1) { DayJobUi.directions(this, job) })
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
        } else if (job.teamsTurnNow) {
            actions.addView(note("अभी पास की टीमों से एक-एक करके पूछ रहे हैं। बची जगह के लिए फिर सबको ख़बर जाएगी।", R.color.brand_color_yellow))
        } else if (job.reservedUntil != null) {
            actions.addView(note("यह काम ${DayJobFormat.clockText(job.reservedUntil)} तक सिर्फ़ उसी मज़दूर के लिए रखा है जिसे आपने फिर बुलाया। उसके बाद सबको ख़बर जाएगी।", R.color.day_job_green))
        } else if (job.notifiedCount > 0) {
            actions.addView(note("${job.radiusKm} km के ${job.notifiedCount} ${if (job.notifiedCount == 1) "मज़दूर" else "मज़दूरों"} को ख़बर भेजी गई।"))
        }
        if (job.canTrack && job.filledTotal > 0) {
            actions.addView(button("🗺  नक्शे पर देखें, कौन कहाँ है", 2) { startActivity(DayJobTrackActivity.intent(this, job.id)) })
        }
        if (job.canAskOvertime) {
            actions.addView(button("⏱  OT माँगें (ज़्यादा देर रुकें)", 0) { askOvertime(job) })
        }
        val waitingDone = job.bookings.orEmpty().filter { it.canMarkDone }
        if (waitingDone.isNotEmpty()) {
            actions.addView(note("काम पूरा हुआ? मज़दूरों को स्टार देकर Job Done दबाएँ। तभी नया काम डाल पाएँगे।", R.color.white))
            if (waitingDone.size > 1) actions.addView(button("✅  सबका Job Done (${waitingDone.size})", 2) {
                DayJobUi.askStars(this, "सबका Job Done", "सभी ${waitingDone.size} मज़दूरों को कितने स्टार देंगे? किसी को अलग देना हो तो नीचे उसके नाम पर Job Done दबाएँ।") { stars ->
                    act { repository.companyDoneAll(job.id, stars) }
                }
            })
        }
        actions.addView(button("🔁  यही काम दोबारा डालें", 0) { askRepost(job) })
        if (job.canCancel ?: (job.status in listOf("in_review", "open", "full", "closed"))) {
            actions.addView(button("🚫  काम रद्द करें", 1) {
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
        // Free people of the teams coming: the company can call them for empty seats.
        job.teamFreeMembers.orEmpty().forEach { m ->
            list.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(DayJobUi.dp(context, 12), DayJobUi.dp(context, 4), DayJobUi.dp(context, 8), DayJobUi.dp(context, 8))
                addView(TextView(context).apply {
                    text = "${m.name ?: "सदस्य"} · ${m.teamName} टीम · खाली"
                    setTextColor(ContextCompat.getColor(context, R.color.search_hint))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(context).apply {
                    text = "बुलाएँ"
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow))
                    setPadding(DayJobUi.dp(context, 12), DayJobUi.dp(context, 6), DayJobUi.dp(context, 12), DayJobUi.dp(context, 6))
                    setOnClickListener { act { repository.inviteMember(job.id, m.userId) } }
                })
            })
        }
        coming.filter { it.teamName == null }.forEach { b ->
            list.addView(LinearLayout(this).apply {
                // Name on top, actions on a row of their own so neither gets squeezed.
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_day_job_pill)
                setPadding(DayJobUi.dp(context, 12), DayJobUi.dp(context, 10), DayJobUi.dp(context, 8), DayJobUi.dp(context, 10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = DayJobUi.dp(context, 8) }
                addView(TextView(context).apply {
                    val state = when {
                        b.status == "no_show" -> " · नहीं आया"
                        b.onHold -> " · बुक कर रहा है (4 मिनट)"
                        b.otStatus == "requested" -> " · OT माँगा: ${otHoursText(b.otHours)}, ₹${b.otAmount ?: 0}"
                        b.otStatus == "asked" -> " · OT पूछा, जवाब बाक़ी"
                        b.otStatus == "accepted" -> " · OT हाँ: ${otHoursText(b.otHours)}, ₹${b.otAmount ?: 0}"
                        b.otStatus == "declined" -> " · OT नहीं"
                        b.companyDone -> " · Job Done, आपने ${b.companyRating ?: ""}★ दिए"
                        b.confirmed -> " · ✓ पक्का आएगा"
                        else -> ""
                    }
                    val rating = DayJobUi.ratingText(b.workerRatingAvg, b.workerRatingCount)
                    text = "${b.name ?: "मज़दूर"}${if (rating.isBlank()) "" else "  $rating"}\n${b.tradeLabel.orEmpty()}$state" +
                        if (b.notPaid) "\nमज़दूर ने कहा: पूरा पैसा नहीं मिला" else ""
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                })
                val actionsRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .apply { topMargin = DayJobUi.dp(context, 6) }
                }
                // The star sits beside the name, so the action row keeps room for Call.
                val nameView = getChildAt(0)
                removeView(nameView)
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    nameView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(nameView)
                    if (b.status == "accepted" && b.workerId > 0) addView(TextView(context).apply {
                        text = if (b.favourite) "★" else "☆"
                        textSize = 22f
                        contentDescription = if (b.favourite) "पसंदीदा से हटाएँ" else "पसंदीदा में जोड़ें"
                        setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow))
                        setPadding(DayJobUi.dp(context, 8), 0, DayJobUi.dp(context, 8), 0)
                        setOnClickListener { act { repository.toggleFavourite(b.workerId) } }
                    })
                }, 0)
                addView(actionsRow)
                // Less frequent actions go on a row above, so the main row never runs out of room.
                val extraRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                }
                addView(extraRow, childCount - 1)
                if (b.otStatus == "requested") extraRow.addView(TextView(context).apply {
                    text = "OT हाँ/ना"
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow))
                    setPadding(DayJobUi.dp(context, 10), DayJobUi.dp(context, 8), DayJobUi.dp(context, 10), DayJobUi.dp(context, 8))
                    setOnClickListener { decideOvertime(b) }
                })
                if (b.canRebook) extraRow.addView(TextView(context).apply {
                    text = if (b.companyDone) "फिर बुलाएँ" else "कल फिर बुलाएँ"
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.day_job_green))
                    setPadding(DayJobUi.dp(context, 10), DayJobUi.dp(context, 8), DayJobUi.dp(context, 10), DayJobUi.dp(context, 8))
                    setOnClickListener { askRebook(b.id, b.name ?: "मज़दूर") }
                })
                if (b.canMarkDone) actionsRow.addView(TextView(context).apply {
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
                if (b.canMarkNoShow) actionsRow.addView(TextView(context).apply {
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
                actionsRow.addView(TextView(context).apply {
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

    /** "हाँ" holds the seat for 4 minutes; the Booking Summary then asks for "अभी Book करें". */
    @Suppress("UNUSED_PARAMETER")
    private fun confirmAccept(job: DayJob, trade: String, label: String) {
        act(failAsDialog = true) { repository.accept(job.id, trade) }
    }

    /** The leader's turn: 1 minute to say yes for the team, then 2 minutes to pick who goes. */
    private fun renderTeamTurn(job: DayJob, turn: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamTurn) {
        val actions = binding.detailActions
        val seats = job.neededTotal - job.filledTotal
        if (turn.status == "yes") {
            actions.addView(note("आपकी टीम की बारी है। $seats लोग चाहिए। लोग चुनकर पक्का करें।", R.color.day_job_green))
            val left = note("", R.color.brand_color_yellow).apply { textSize = 18f; tag = turn.pickBy to " में लोग चुनें, नहीं तो काम अगली टीम को जाएगा।" }
            holdTimer = left
            updateHoldTimer()
            actions.addView(left)
            actions.addView(button("✓  टीम के लोग चुनें", 2) { pickTeam(job) })
        } else {
            actions.addView(note("आपकी टीम के लिए काम आया है: $seats लोग चाहिए। सबसे पहले आपको पूछा जा रहा है।", R.color.white))
            val left = note("", R.color.brand_color_yellow).apply { textSize = 18f; tag = turn.respondBy to " में हाँ करें, नहीं तो अगली टीम को जाएगा।" }
            holdTimer = left
            updateHoldTimer()
            actions.addView(left)
            actions.addView(button("✓  हाँ, टीम भेजूँगा", 2) { act { repository.answerTeamTurn(job.id, true) } })
            actions.addView(button("✗  नहीं", 1) { act { repository.answerTeamTurn(job.id, false) } })
        }
        actions.addView(button("🧭  रास्ता देखें", 1) { DayJobUi.directions(this, job) })
    }

    /** Booking Summary for a held seat, with the minutes left and a big Book button. */
    /** The leader picked this member: just "जाऊँगा" or "नहीं जाऊँगा". */
    private fun renderTeamAsk(job: DayJob, booking: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobBooking) {
        val actions = binding.detailActions
        actions.addView(note("${booking.leaderName ?: "आपके लीडर"} ने आपको यह काम के लिए चुना है। क्या आप जाएँगे?", R.color.brand_color_yellow).apply { textSize = 18f })
        val left = note("", R.color.brand_color_yellow).apply { tag = booking.heldUntil to " में बताएँ, नहीं तो जगह छूट जाएगी।" }
        holdTimer = left
        updateHoldTimer()
        actions.addView(left)
        actions.addView(button("✅  जाऊँगा", 2) {
            binding.detailLoading.visibility = View.VISIBLE
            lifecycleScope.launch {
                val result = repository.book(booking.id)
                binding.detailLoading.visibility = View.GONE
                when (result) {
                    is DayJobOutcome.Ok -> {
                        DayJobUi.toast(this@DayJobDetailActivity, result.message)
                        com.srijeesolution.rojgaarwaala.utils.DayJobSpeaker.speak(this@DayJobDetailActivity,
                            "बधाई हो! काम बुक हो गया है। घर से निकलते समय, घर से निकल पड़ा दबाइए।")
                    }
                    is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobDetailActivity, result.message)
                }
                load()
            }
        })
        actions.addView(button("❌  नहीं जाऊँगा", 1) { act { repository.cancelBooking(booking.id) } })
        actions.addView(button("🧭  रास्ता देखें", 1) { DayJobUi.directions(this, job) })
    }

    private fun renderHold(job: DayJob, booking: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobBooking) {
        val actions = binding.detailActions
        if (booking.teamAsk) {
            renderTeamAsk(job, booking)
            return
        }
        val trade = job.slots.firstOrNull { it.trade == booking.trade }?.label.orEmpty()
        actions.addView(TextView(this).apply {
            text = "Booking Summary\n\n" +
                "काम: ${job.title} · $trade\n" +
                "मज़दूरी: ${DayJobFormat.wageText(job.wagePerDay)}\n" +
                "कब: ${DayJobFormat.whenText(job.workDate, job.startTime)} · ${job.hours} घंटे\n" +
                (job.place.ifBlank { job.district.orEmpty() }.takeIf { it.isNotBlank() }?.let { "कहाँ: $it\n" } ?: "") +
                "कंपनी: ${job.company.name}"
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = 16f
            setBackgroundResource(R.drawable.bg_day_job_card)
            setPadding(DayJobUi.dp(context, 16), DayJobUi.dp(context, 14), DayJobUi.dp(context, 16), DayJobUi.dp(context, 14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = DayJobUi.dp(context, 12) }
        })
        val left = note("", R.color.brand_color_yellow).apply { textSize = 18f; tag = booking.heldUntil to " में Book करें, नहीं तो जगह छूट जाएगी।" }
        holdTimer = left
        updateHoldTimer()
        actions.addView(left)
        actions.addView(button("✅  अभी Book करें", 2) {
            binding.detailLoading.visibility = View.VISIBLE
            lifecycleScope.launch {
                val result = repository.book(booking.id)
                binding.detailLoading.visibility = View.GONE
                when (result) {
                    is DayJobOutcome.Ok -> {
                        DayJobUi.toast(this@DayJobDetailActivity, result.message)
                        com.srijeesolution.rojgaarwaala.utils.DayJobSpeaker.speak(this@DayJobDetailActivity,
                            "बधाई हो! काम बुक हो गया है। घर से निकलते समय, घर से निकल पड़ा दबाइए।")
                    }
                    is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobDetailActivity, result.message)
                }
                load()
            }
        })
        actions.addView(button("📞  कंपनी को Call करें", 1) { DayJobUi.call(this, job.company.phone) })
        actions.addView(button("नहीं चाहिए, छोड़ दें", 1) { act { repository.cancelBooking(booking.id) } })
    }

    private var holdTimer: TextView? = null

    /** "3:42 में Book करें…" (any countdown line on screen); at zero the screen reloads. */
    private fun updateHoldTimer() {
        val view = holdTimer ?: return
        val (iso, suffix) = (view.tag as? Pair<*, *>) ?: return
        val until = (iso as? String)?.let { runCatching { java.time.OffsetDateTime.parse(it).toInstant() }.getOrNull() } ?: return
        val seconds = java.time.Duration.between(java.time.Instant.now(), until).seconds
        if (seconds <= 0) {
            holdTimer = null
            view.text = "समय ख़त्म।"
            load()
            return
        }
        view.text = String.format(java.util.Locale.US, "%d:%02d", seconds / 60, seconds % 60) + suffix
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
                .setPositiveButton("सबसे पूछें") { _, _ ->
                    val picks = rows.filterIndexed { i, _ -> checked[i] }.distinctBy { it.first }
                        .map { com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamPick(it.first, it.second) }
                    if (picks.isEmpty()) {
                        DayJobUi.toast(this@DayJobDetailActivity, "कम से कम एक व्यक्ति चुनें।")
                    } else {
                        // The leader agrees to the rules for everyone they send.
                        val rules = workerTerms.ifEmpty { listOf("समय पर पहुँचें और पूरा पैसा मिलने पर ही Job Done दबाएँ।") }
                        DayJobUi.askTerms(this@DayJobDetailActivity, "टीम के ${picks.size} लोगों के लिए नियम", rules + "ये नियम टीम के हर व्यक्ति को बता दें।") {
                            act { repository.acceptForTeam(job.id, picks) }
                        }
                    }
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

    private fun otHoursText(hours: Double?): String {
        val h = hours ?: 0.0
        val whole = h.toInt()
        return (if (whole > 0) "$whole" else "") + (if (h - whole >= 0.5) "½" else "") + " घंटे"
    }

    /** Hours on a picker; the app works out the money (day's wage ÷ hours × extra), the company can change it. */
    private fun askOvertime(job: DayJob) {
        val choices = listOf(1.0, 1.5, 2.0, 3.0, 4.0)
        DayJobUi.pickOne(this, "कितने घंटे और रुकना है?", choices.map { "${otHoursText(it)} · ₹${(job.hourlyRate * it).toInt()} हर मज़दूर" }) { i ->
            val hours = choices[i]
            val suggested = (job.hourlyRate * hours).toInt()
            val input = android.widget.EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(suggested.toString())
                setSelectAllOnFocus(true)
                selectAll()
            }
            AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
                .setTitle("हर मज़दूर को कितने रुपये?")
                .setMessage("मज़दूरी ₹${job.wagePerDay} ÷ ${job.hours} घंटे × ${otHoursText(hours)} = ₹$suggested। चाहें तो बदलें।")
                .setView(input)
                .setPositiveButton("OT माँगें") { _, _ ->
                    val amount = input.text.toString().toIntOrNull()
                    if (amount == null || amount > 5000) DayJobUi.toast(this, "OT के रुपये ₹0 से ₹5000 तक लिखें।")
                    else act { repository.askOvertime(job.id, hours, amount) }
                }
                .setNegativeButton("रहने दें", null)
                .show()
        }
    }

    /** A worker offered OT: yes (amount editable) or no. */
    private fun decideOvertime(b: com.srijeesolution.rojgaarwaala.data.remote.model.DayJobBooking) {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText((b.otAmount ?: 0).toString())
            // Selected, so typing a new amount replaces the suggestion.
            setSelectAllOnFocus(true)
            selectAll()
        }
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle("${b.name ?: "मज़दूर"}: ${otHoursText(b.otHours)} OT")
            .setMessage("कितने रुपये देंगे? चाहें तो बदलें।")
            .setView(input)
            .setPositiveButton("हाँ, OT करें") { _, _ ->
                val amount = input.text.toString().toIntOrNull()
                if (amount == null || amount > 5000) DayJobUi.toast(this, "OT के रुपये ₹0 से ₹5000 तक लिखें।")
                else act { repository.decideOvertime(b.id, true, amount) }
            }
            .setNegativeButton("नहीं") { _, _ -> act { repository.decideOvertime(b.id, false, null) } }
            .show()
    }

    /** Same worker, a new date; the job is theirs for an hour. */
    private fun askRebook(bookingId: Int, name: String) {
        val tomorrow = LocalDate.now().plusDays(1)
        DatePickerDialog(this, { _, y, m, d ->
            act { repository.rebook(bookingId, LocalDate.of(y, m + 1, d).toString()) }
        }, tomorrow.year, tomorrow.monthValue - 1, tomorrow.dayOfMonth).apply {
            setTitle("$name को किस दिन बुलाएँ?")
            datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.maxDate = System.currentTimeMillis() + 14L * 24 * 3600 * 1000
        }.show()
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
    /** [failAsDialog]: a refusal the worker must read (e.g. Job Done pending) stays on screen instead of a toast. */
    private fun act(failAsDialog: Boolean = false, block: suspend () -> DayJobOutcome<Any?>) {
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
                is DayJobOutcome.Fail -> if (failAsDialog) {
                    AlertDialog.Builder(this@DayJobDetailActivity, R.style.Theme_Rojgaarwaala_AlertDialog)
                        .setTitle("काम नहीं ले सके")
                        .setMessage(result.message)
                        .setPositiveButton("ठीक है", null)
                        .show()
                    com.srijeesolution.rojgaarwaala.utils.DayJobSpeaker.speak(this@DayJobDetailActivity, result.message)
                } else {
                    DayJobUi.toast(this@DayJobDetailActivity, result.message)
                }
            }
        }
    }
}
