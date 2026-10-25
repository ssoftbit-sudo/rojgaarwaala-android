package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
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

    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    private val pickLocation = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val lat = data.getDoubleExtra(MapPinActivity.EXTRA_LAT, MapPinActivity.DEFAULT_LAT)
        val lng = data.getDoubleExtra(MapPinActivity.EXTRA_LNG, MapPinActivity.DEFAULT_LNG)
        prefs.edit().putFloat("lat", lat.toFloat()).putFloat("lng", lng.toFloat()).apply()
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
            binding.dayJobMyTrades.text = "Your work: " + meta.worker.trades.joinToString(", ") { key ->
                meta.trades.firstOrNull { it.key == key }?.label ?: key
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
                    "active" -> "Verified"
                    "pending" -> "Verify pending"
                    "rejected" -> "Rejected"
                    "blocked" -> "Blocked"
                    else -> status
                }
                binding.dayJobCompanyStatus.setTextColor(ContextCompat.getColor(this, when (status) {
                    "active" -> R.color.day_job_green
                    "pending" -> R.color.brand_color_yellow
                    else -> R.color.new_tag_red
                }))
                val note = when (status) {
                    "pending" -> "We are checking your company. You can post now; jobs go live once verified."
                    "rejected" -> company.adminNote ?: "Your company was not approved. Edit the details and send again."
                    "blocked" -> "This company is blocked. Please contact support."
                    else -> null
                }
                binding.dayJobCompanyNote.text = note
                binding.dayJobCompanyNote.visibility = if (note == null) View.GONE else View.VISIBLE
                val canPost = status == "active" || status == "pending"
                binding.dayJobAddJob.isEnabled = canPost
                binding.dayJobAddJob.alpha = if (canPost) 1f else 0.4f
                loadCompanyJobs()
            }
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
        binding.dayJobWorkerSave.text = if (meta.worker.registered) "Save" else "Start getting jobs"
    }

    private fun saveWorker(available: Boolean) {
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
            DayJobUi.toast(this, "Choose at least one kind of work")
            return
        }
        lifecycleScope.launch {
            when (val result = repository.saveWorker(chosen, available)) {
                is DayJobOutcome.Ok -> {
                    DayJobUi.toast(this@DayJobHomeActivity, if (available) "You will get job alerts near you" else "Job alerts paused")
                    refresh()
                }
                is DayJobOutcome.Fail -> {
                    DayJobUi.toast(this@DayJobHomeActivity, result.message)
                    binding.dayJobAvailable.isChecked = meta.worker.isAvailable
                }
            }
        }
    }

    private fun loadNearby() {
        val savedLat = prefs.getFloat("lat", Float.NaN)
        val savedLng = prefs.getFloat("lng", Float.NaN)
        val lat = savedLat.takeUnless { it.isNaN() }?.toDouble()
        val lng = savedLng.takeUnless { it.isNaN() }?.toDouble()
        lifecycleScope.launch {
            when (val result = repository.nearby(lat, lng)) {
                is DayJobOutcome.Ok -> {
                    binding.dayJobSetLocation.visibility = View.GONE
                    val from = result.data
                    center = if (from.latitude != null && from.longitude != null) LatLng(from.latitude, from.longitude) else null
                    val jobs = from.jobs
                    workerAdapter.submit(jobs)
                    binding.dayJobWorkerEmpty.visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
                    binding.dayJobMapNote.text = if (jobs.isEmpty()) "" else "● ${jobs.size} ${if (jobs.size == 1) "job" else "jobs"} within 10 km"
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

    private fun loadCompanyJobs() {
        lifecycleScope.launch {
            when (val result = repository.mine(historyTab)) {
                is DayJobOutcome.Ok -> {
                    companyAdapter.submit(result.data)
                    binding.dayJobCompanyEmpty.visibility = if (result.data.isEmpty()) View.VISIBLE else View.GONE
                    binding.dayJobCompanyEmpty.text = if (historyTab) "Finished and cancelled jobs show here." else "No active jobs. Tap + Add Job to find workers."
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
            googleMap.addMarker(MarkerOptions().position(me).title("You")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
        }
        val bounds = LatLngBounds.Builder()
        var any = false
        jobs.forEach { job ->
            val point = LatLng(job.latitude, job.longitude)
            googleMap.addMarker(
                MarkerOptions().position(point)
                    .title("${DayJobFormat.wageText(job.wagePerDay)} · ${job.title}")
                    .snippet(DayJobFormat.whenText(job.workDate, job.startTime) + " · tap to open")
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
