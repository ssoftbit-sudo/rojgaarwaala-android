package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJob
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobMeta
import com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome
import com.srijeesolution.rojgaarwaala.data.repository.DayJobRepository
import com.srijeesolution.rojgaarwaala.databinding.ActivityDayJobHomeBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.MapPinActivity
import com.srijeesolution.rojgaarwaala.utils.DayJobFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 1 Day Job home. "Find Work": map of jobs near the worker, availability and the list.
 * "Hire Workers": the company's jobs and the big Add Job button.
 */
@AndroidEntryPoint
class DayJobHomeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_WORK = "work"
        const val MODE_HIRE = "hire"
        private const val PREFS = "day_job"

        fun intent(context: Context, mode: String) =
            Intent(context, DayJobHomeActivity::class.java).putExtra(EXTRA_MODE, mode)
    }

    @Inject lateinit var repository: DayJobRepository

    private lateinit var binding: ActivityDayJobHomeBinding
    private var mode = MODE_WORK
    private var meta: DayJobMeta? = null
    private var historyTab = false
    private var map: GoogleMap? = null
    private var pendingMapJobs: List<DayJob> = emptyList()
    private var center: LatLng? = null

    private val workerAdapter = DayJobAdapter(forCompany = false) { openJob(it) }
    private val companyAdapter = DayJobAdapter(forCompany = true) { openJob(it) }

    /** The rules dialog shows once per visit to this screen. */
    private var termsShown = false

    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    private val pickLocation = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val lat = data.getDoubleExtra(MapPinActivity.EXTRA_LAT, MapPinActivity.DEFAULT_LAT)
        val lng = data.getDoubleExtra(MapPinActivity.EXTRA_LNG, MapPinActivity.DEFAULT_LNG)
        prefs.edit().putFloat("lat", lat.toFloat()).putFloat("lng", lng.toFloat()).apply()
        loadNearby()
    }

    /** Asked once; with it, alerts follow the worker to the chowk instead of the home pin. */
    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        prefs.edit().putBoolean("location_asked", true).apply()
        loadNearby()
    }

    private val returnRefresh = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDayJobHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = intent.getStringExtra(EXTRA_MODE) ?: prefs.getString("mode", MODE_WORK) ?: MODE_WORK
        binding.dayJobBack.setOnClickListener { finish() }
        binding.dayJobModeWork.setOnClickListener { setMode(MODE_WORK) }
        binding.dayJobModeHire.setOnClickListener { setMode(MODE_HIRE) }
        binding.dayJobRefresh.setOnRefreshListener { refresh() }
        binding.dayJobRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.brand_color_yellow))

        binding.dayJobWorkerList.layoutManager = LinearLayoutManager(this)
        binding.dayJobWorkerList.adapter = workerAdapter
        binding.dayJobCompanyList.layoutManager = LinearLayoutManager(this)
        binding.dayJobCompanyList.adapter = companyAdapter

        binding.dayJobWorkerSave.setOnClickListener { saveWorker(available = true) }
        binding.dayJobEditTrades.setOnClickListener { showWorkerSetup(true) }
        binding.dayJobAvailable.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) saveWorker(available = checked)
        }
        binding.dayJobMyJobs.setOnClickListener {
            startActivity(Intent(this, DayJobMyJobsActivity::class.java))
        }
        binding.dayJobMyTeam.setOnClickListener { startActivity(Intent(this, DayJobTeamActivity::class.java)) }
        binding.dayJobAlertHours.setOnClickListener { chooseAlertHours() }
        binding.dayJobMyZone.setOnClickListener {
            val lat = if (prefs.contains("lat")) prefs.getFloat("lat", 0f).toDouble() else null
            val lng = if (prefs.contains("lng")) prefs.getFloat("lng", 0f).toDouble() else null
            returnRefresh.launch(DayJobZoneActivity.intent(this, lat, lng, meta?.myZones.orEmpty().map { it.id }))
        }
        binding.dayJobSpeak.isChecked = com.srijeesolution.rojgaarwaala.utils.DayJobSpeaker.isOn(this)
        binding.dayJobSpeak.setOnCheckedChangeListener { _, on ->
            com.srijeesolution.rojgaarwaala.utils.DayJobSpeaker.setOn(this, on)
            if (on) com.srijeesolution.rojgaarwaala.utils.DayJobSpeaker.speak(this, "नए काम की ख़बर अब बोलकर सुनाई जाएगी।")
        }
        binding.dayJobSetLocation.setOnClickListener { pickLocation.launch(Intent(this, MapPinActivity::class.java)) }
        binding.dayJobRegisterCompany.setOnClickListener { openCompanyForm() }
        binding.dayJobCompanyEdit.setOnClickListener { openCompanyForm() }
        binding.dayJobAddJob.setOnClickListener { returnRefresh.launch(Intent(this, DayJobPostActivity::class.java)) }
        binding.dayJobTabActive.setOnClickListener { historyTab = false; styleTabs(); loadCompanyJobs() }
        binding.dayJobTabHistory.setOnClickListener { historyTab = true; styleTabs(); loadCompanyJobs() }

        setupMap()
        setMode(mode)
    }

    override fun onResume() {
        super.onResume()
        if (meta != null) refresh()
    }

    private fun setMode(newMode: String) {
        mode = newMode
        prefs.edit().putString("mode", mode).apply()
        val work = mode == MODE_WORK
        styleToggle(binding.dayJobModeWork, work)
        styleToggle(binding.dayJobModeHire, !work)
        binding.dayJobWorkerPanel.visibility = if (work) View.VISIBLE else View.GONE
        binding.dayJobCompanyPanel.visibility = if (work) View.GONE else View.VISIBLE
        styleTabs()
        if (meta == null) loadMeta() else render()
    }

    private fun styleToggle(view: android.widget.TextView, on: Boolean) {
        view.setBackgroundResource(if (on) R.drawable.bg_day_job_toggle_on else android.R.color.transparent)
        view.setTextColor(ContextCompat.getColor(this, if (on) R.color.on_brand else R.color.search_hint))
    }

    private fun styleTabs() {
        styleToggle(binding.dayJobTabActive, !historyTab)
        styleToggle(binding.dayJobTabHistory, historyTab)
    }

    private fun refresh() {
        meta = null
        loadMeta()
    }

    private fun loadMeta() {
        binding.dayJobLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.meta()
            binding.dayJobLoading.visibility = View.GONE
            binding.dayJobRefresh.isRefreshing = false
            when (result) {
                is DayJobOutcome.Ok -> { meta = result.data; render() }
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobHomeActivity, result.message)
            }
        }
    }

    private fun render() {
        val meta = meta ?: return
        if (mode == MODE_WORK) {
            val registered = meta.worker.registered
            showWorkerSetup(!registered)
            binding.dayJobWorkerStatus.visibility = if (registered) View.VISIBLE else View.GONE
            binding.dayJobAvailable.isChecked = meta.worker.isAvailable
            binding.dayJobMyTrades.text = "आपका काम: " + meta.worker.trades.joinToString(", ") { key ->
                meta.trades.firstOrNull { it.key == key }?.label ?: key
            }
            binding.dayJobAlertHours.text = "ख़बर कब: " + DayJobFormat.alertHoursText(meta.worker.alertFrom, meta.worker.alertTo) + " ›"
            binding.dayJobMyZone.text = if (meta.myZones.isEmpty()) "📍 अपना चौक (ज़ोन) चुनें ›"
            else "📍 ज़ोन: " + meta.myZones.joinToString(", ") { it.name } + " ›"
            val heldUntil = meta.worker.heldBackUntil
            val rating = DayJobUi.ratingText(meta.worker.ratingAvg, meta.worker.ratingCount)
            if (rating.isNotBlank()) binding.dayJobMyTrades.append("  ·  $rating")
            // One yellow line for whatever stops new work right now; tapping a Job Done opens it.
            val warning = listOfNotNull(
                meta.pendingDone.takeIf { it.isNotEmpty() }?.let { "${it.size} काम का Job Done बाक़ी है। यहाँ दबाकर पूरा करें, तभी नया काम ले पाएँगे।" },
                meta.worker.bookingBlockMessage,
                heldUntil?.let { "आपने हाँ करके काम छोड़े, इसलिए नए काम की ख़बर इस दिन तक बंद है: " + DayJobFormat.dayText(it) + "।" },
            )
            binding.dayJobHeldBack.visibility = if (registered && warning.isNotEmpty()) View.VISIBLE else View.GONE
            binding.dayJobHeldBack.text = warning.joinToString("\n\n")
            binding.dayJobHeldBack.setOnClickListener {
                meta.pendingDone.firstOrNull()?.let { startActivity(DayJobDetailActivity.intent(this, it)) }
            }
            if (registered && meta.needsWorkerTerms && !termsShown) {
                termsShown = true
                DayJobUi.askTerms(this, "मज़दूर के नियम बदले हैं", meta.workerTerms) { agree("worker") }
            }
            loadNearby()
        } else {
            val company = meta.company
            binding.dayJobCompanyIntro.visibility = if (company == null) View.VISIBLE else View.GONE
            binding.dayJobCompanyHome.visibility = if (company == null) View.GONE else View.VISIBLE
            if (company != null) {
                binding.dayJobCompanyName.text = company.name
                val status = company.status.orEmpty()
                binding.dayJobCompanyStatus.text = when (status) {
                    "active" -> "जाँच पूरी"
                    "pending" -> "जाँच बाकी"
                    "rejected" -> "मंज़ूर नहीं"
                    "blocked" -> "रोक दी गई"
                    else -> status
                }
                binding.dayJobCompanyStatus.setTextColor(ContextCompat.getColor(this, when (status) {
                    "active" -> R.color.day_job_green
                    "pending" -> R.color.brand_color_yellow
                    else -> R.color.new_tag_red
                }))
                val note = when (status) {
                    "pending" -> "हम आपकी कंपनी की जाँच कर रहे हैं। काम अभी डाल सकते हैं, जाँच के बाद सबको दिखेगा।"
                    "rejected" -> company.adminNote ?: "आपकी कंपनी मंज़ूर नहीं हुई। जानकारी सुधारकर फिर भेजें।"
                    "blocked" -> "यह कंपनी रोक दी गई है। मदद के लिए हमसे बात करें।"
                    else -> null
                }
                val pending = meta.pendingDoneCompany
                val doneNote = if (pending.isEmpty()) null
                    else "${pending.size} काम का Job Done बाक़ी है। यहाँ दबाकर मज़दूरों को स्टार दें, तभी नया काम डाल पाएँगे।"
                val shown = listOfNotNull(doneNote, note).joinToString("\n\n").ifBlank { null }
                binding.dayJobCompanyNote.text = shown
                binding.dayJobCompanyNote.visibility = if (shown == null) View.GONE else View.VISIBLE
                binding.dayJobCompanyNote.setOnClickListener {
                    pending.firstOrNull()?.let { startActivity(DayJobDetailActivity.intent(this, it)) }
                }
                if (meta.needsCompanyTerms && !termsShown) {
                    termsShown = true
                    DayJobUi.askTerms(this, "कंपनी के नियम बदले हैं", meta.terms) { agree("company") }
                }
                val canPost = status == "active" || status == "pending"
                binding.dayJobAddJob.isEnabled = canPost
                binding.dayJobAddJob.alpha = if (canPost) 1f else 0.4f
                loadCompanyJobs()
            }
        }
    }

    private fun agree(audience: String) {
        lifecycleScope.launch {
            val result = repository.acceptTerms(audience)
            DayJobUi.toast(this@DayJobHomeActivity, (result as? DayJobOutcome.Ok)?.message ?: (result as? DayJobOutcome.Fail)?.message)
            refresh()
        }
    }

    private fun showWorkerSetup(show: Boolean) {
        val meta = meta ?: return
        binding.dayJobWorkerSetup.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) return
        val group = binding.dayJobWorkerTrades
        group.removeAllViews()
        meta.trades.forEach { trade ->
            group.addView(DayJobUi.chip(this, trade.label, trade.key in meta.worker.trades).apply { tag = trade.key })
        }
        binding.dayJobWorkerSave.text = if (meta.worker.registered) "सेव करें" else "काम की ख़बर पाना शुरू करें"
    }

    private fun saveWorker(available: Boolean, agreed: Boolean = false) {
        val meta = meta ?: return
        val chosen = if (binding.dayJobWorkerSetup.visibility == View.VISIBLE) {
            (0 until binding.dayJobWorkerTrades.childCount)
                .map { binding.dayJobWorkerTrades.getChildAt(it) as com.google.android.material.chip.Chip }
                .filter { it.isChecked }
                .map { it.tag as String }
        } else {
            meta.worker.trades
        }
        if (chosen.isEmpty()) {
            DayJobUi.toast(this, "कम से कम एक काम चुनें")
            return
        }
        // First time (or the rules changed): read and accept the rules, then save.
        if (meta.needsWorkerTerms && !agreed) {
            DayJobUi.askTerms(this, "मज़दूर के नियम", meta.workerTerms) { saveWorker(available, agreed = true) }
            if (meta.worker.registered) binding.dayJobAvailable.isChecked = meta.worker.isAvailable
            return
        }
        lifecycleScope.launch {
            when (val result = repository.saveWorker(chosen, available, acceptTerms = if (agreed) true else null)) {
                is DayJobOutcome.Ok -> {
                    DayJobUi.toast(this@DayJobHomeActivity, if (available) "पास के काम की ख़बर आपको मिलेगी" else "काम की ख़बर बंद")
                    refresh()
                }
                is DayJobOutcome.Fail -> {
                    DayJobUi.toast(this@DayJobHomeActivity, result.message)
                    binding.dayJobAvailable.isChecked = meta.worker.isAvailable
                }
            }
        }
    }

    private fun hasLocation(): Boolean =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Where the worker is now if the phone can tell us, else the pin they set here. */
    private fun loadNearby() {
        if (!hasLocation() && !prefs.getBoolean("location_asked", false)) {
            askLocation.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        if (hasLocation()) {
            try {
                val client = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
                // Show the list at once from the last known place; a fresh fix can take
                // a while, and the list is redrawn when it comes.
                client.lastLocation.addOnCompleteListener { last ->
                    val known = last.result
                    if (known != null) loadNearbyAt(known.latitude, known.longitude) else loadNearbyAt(null, null)
                    client.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                        .addOnCompleteListener { task ->
                            val here = task.result ?: return@addOnCompleteListener
                            val moved = known == null || here.distanceTo(known) > 200f
                            if (moved) loadNearbyAt(here.latitude, here.longitude)
                        }
                }
                return
            } catch (_: SecurityException) {
                // fall through to the saved pin
            }
        }
        loadNearbyAt(null, null)
    }

    private fun loadNearbyAt(currentLat: Double?, currentLng: Double?) {
        val savedLat = prefs.getFloat("lat", Float.NaN).takeUnless { it.isNaN() }?.toDouble()
        val savedLng = prefs.getFloat("lng", Float.NaN).takeUnless { it.isNaN() }?.toDouble()
        val lat = currentLat ?: savedLat
        val lng = currentLng ?: savedLng
        lifecycleScope.launch {
            when (val result = repository.nearby(lat, lng)) {
                is DayJobOutcome.Ok -> {
                    binding.dayJobSetLocation.visibility = View.GONE
                    val from = result.data
                    center = if (from.latitude != null && from.longitude != null) LatLng(from.latitude, from.longitude) else null
                    val jobs = from.jobs
                    workerAdapter.submit(jobs)
                    binding.dayJobWorkerEmpty.visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
                    binding.dayJobMapNote.text = if (jobs.isEmpty()) "" else "● 10 km में ${jobs.size} काम"
                    showOnMap(jobs)
                }
                is DayJobOutcome.Fail -> {
                    workerAdapter.submit(emptyList())
                    binding.dayJobWorkerEmpty.visibility = View.VISIBLE
                    binding.dayJobWorkerEmpty.text = result.message
                    binding.dayJobSetLocation.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun chooseAlertHours() {
        val meta = meta ?: return
        val options = arrayOf("कभी भी", "सुबह 6 – रात 8", "सुबह 5 – 11 (सिर्फ़ सुबह)", "अपना समय चुनें…")
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle("काम की ख़बर कब भेजें?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> saveAlertHours(null, null)
                    1 -> saveAlertHours("06:00", "20:00")
                    2 -> saveAlertHours("05:00", "11:00")
                    else -> pickTime("कब से", meta.worker.alertFrom ?: "06:00") { from ->
                        pickTime("कब तक", meta.worker.alertTo ?: "20:00") { to -> saveAlertHours(from, to) }
                    }
                }
            }
            .show()
    }

    private fun pickTime(title: String, current: String, then: (String) -> Unit) {
        val (h, m) = current.split(":").map { it.toIntOrNull() ?: 0 }.let { it[0] to it.getOrElse(1) { 0 } }
        android.app.TimePickerDialog(this, { _, hour, minute -> then("%02d:%02d".format(hour, minute)) }, h, m, false)
            .apply { setTitle(title) }
            .show()
    }

    private fun saveAlertHours(from: String?, to: String?) {
        val meta = meta ?: return
        lifecycleScope.launch {
            // Empty strings clear the hours on the server ("any time").
            when (val result = repository.saveWorker(meta.worker.trades, meta.worker.isAvailable, from ?: "", to ?: "")) {
                is DayJobOutcome.Ok -> { DayJobUi.toast(this@DayJobHomeActivity, "ख़बर का समय सेव हो गया"); refresh() }
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobHomeActivity, result.message)
            }
        }
    }

    private fun loadCompanyJobs() {
        lifecycleScope.launch {
            when (val result = repository.mine(historyTab)) {
                is DayJobOutcome.Ok -> {
                    companyAdapter.submit(result.data)
                    binding.dayJobCompanyEmpty.visibility = if (result.data.isEmpty()) View.VISIBLE else View.GONE
                    binding.dayJobCompanyEmpty.text = if (historyTab) "पूरे और रद्द हुए काम यहाँ दिखेंगे।" else "अभी कोई काम नहीं। मज़दूर चाहिए तो '+ काम डालें' दबाएँ।"
                }
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobHomeActivity, result.message)
            }
        }
    }

    private fun setupMap() {
        val fragment = SupportMapFragment.newInstance(
            GoogleMapOptions().mapToolbarEnabled(false).zoomControlsEnabled(false).compassEnabled(false),
        )
        supportFragmentManager.beginTransaction().replace(R.id.dayJobMap, fragment).commitNowAllowingStateLoss()
        fragment.getMapAsync { googleMap ->
            map = googleMap
            runCatching { googleMap.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark)) }
            googleMap.uiSettings.isMapToolbarEnabled = false
            googleMap.setOnInfoWindowClickListener { marker -> (marker.tag as? DayJob)?.let { openJob(it) } }
            showOnMap(pendingMapJobs)
        }
    }

    private fun showOnMap(jobs: List<DayJob>) {
        pendingMapJobs = jobs
        val googleMap = map ?: return
        googleMap.clear()
        val me = center
        if (me != null) {
            googleMap.addCircle(CircleOptions().center(me).radius(10_000.0).strokeWidth(2f)
                .strokeColor(0x66FFBB02).fillColor(0x14FFBB02))
            googleMap.addMarker(MarkerOptions().position(me).title("आप")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
        }
        val bounds = LatLngBounds.Builder()
        var any = false
        jobs.forEach { job ->
            val point = LatLng(job.latitude, job.longitude)
            googleMap.addMarker(
                MarkerOptions().position(point)
                    .title("${DayJobFormat.wageText(job.wagePerDay)} · ${job.title}")
                    .snippet(DayJobFormat.whenText(job.workDate, job.startTime) + " · खोलने के लिए छुएँ")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)),
            )?.tag = job
            bounds.include(point)
            any = true
        }
        me?.let { bounds.include(it); any = true }
        if (!any) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(MapPinActivity.DEFAULT_LAT, MapPinActivity.DEFAULT_LNG), 11f))
            return
        }
        binding.dayJobMap.post {
            runCatching {
                if (jobs.size + (if (me != null) 1 else 0) == 1) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me ?: LatLng(jobs[0].latitude, jobs[0].longitude), 13f))
                } else {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), DayJobUi.dp(this, 40)))
                }
            }
        }
    }

    private fun openCompanyForm() {
        returnRefresh.launch(Intent(this, DayJobCompanyActivity::class.java))
    }

    private fun openJob(job: DayJob) {
        returnRefresh.launch(DayJobDetailActivity.intent(this, job.id))
    }
}
