package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.srijeesolution.rojgaarwaala.BuildConfig
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeDashboardData
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeFactory
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchResponse
import com.srijeesolution.rojgaarwaala.databinding.ActivityAttendanceDashboardBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorMapper
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import com.srijeesolution.rojgaarwaala.utils.AttendanceHindi
import com.srijeesolution.rojgaarwaala.utils.FactoryGeofenceMonitor
import com.srijeesolution.rojgaarwaala.utils.GeofenceEvaluator
import com.srijeesolution.rojgaarwaala.utils.LocationHelper
import com.srijeesolution.rojgaarwaala.utils.PunchElapsedFormatter
import com.srijeesolution.rojgaarwaala.utils.PunchOutOutcome
import com.srijeesolution.rojgaarwaala.utils.WageFormatter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttendanceDashboardActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PUNCH = "extra_punch"
        const val PUNCH_IN = "in"
        const val PUNCH_OUT = "out"

        val COLOR_INSIDE_STROKE = Color.argb(255, 76, 175, 80)
        val COLOR_INSIDE_FILL = Color.argb(60, 76, 175, 80)
        val COLOR_OUTSIDE_STROKE = Color.argb(255, 229, 57, 53)
        val COLOR_OUTSIDE_FILL = Color.argb(50, 229, 57, 53)

        const val FACTORY_ONLY_ZOOM = 16f
        const val MAP_BOUNDS_PADDING_PX = 80
    }

    private lateinit var binding: ActivityAttendanceDashboardBinding
    private val viewModel: EmployeeAttendanceViewModel by viewModels()

    // Registered during construction so the permission / settings launchers are ready
    // before the activity reaches STARTED.
    private val locationHelper = LocationHelper(this)

    /** Set when the backend rejects a punch with a code that can never succeed for this user. */
    private var punchBlockedByError = false
    private var punchInProgress = false
    private var skipNextResumeRefresh = false

    private var googleMap: GoogleMap? = null

    /** Latest dashboard state, kept so a new GPS fix can re-render without another API call. */
    private var todayFactory: EmployeeFactory? = null
    private var employeeInactive = false
    private var serverCanPunchIn = false
    private var serverCanPunchOut = false
    private var attendanceMarked = false
    private var dashboardLoaded = false

    private var lastFix: LocationHelper.Result.Success? = null
    private var locationPermissionAsked = false

    /** Set while the terms gate is on screen, so the dashboard does not reopen it. */
    private var awaitingTermsAcceptance = false

    private var pendingPunchAction: String? = null
    private var pendingPunchConsumed = false

    private var punchInAtMillis: Long? = null
    private var punchOutAtMillis: Long? = null
    private val elapsedHandler = Handler(Looper.getMainLooper())
    private val elapsedTick = object : Runnable {
        override fun run() {
            renderPunchElapsed()
            elapsedHandler.postDelayed(this, 1000)
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) FactoryGeofenceMonitor.register(this, todayFactory)
    }

    private val termsGate = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        awaitingTermsAcceptance = false
        if (result.resultCode == RESULT_OK) {
            viewModel.loadDashboard()
        } else {
            // Declining is a choice to leave: attendance is unusable without the terms.
            finish()
        }
    }

    /**
     * Set when location tracking cannot start at all (permission refused, GPS off). The punch
     * button stays tappable in that case so the existing punch flow can raise the system
     * prompts, rather than locking the employee out of a screen they cannot fix from here.
     */
    private var trackingError: LocationHelper.Result.Error? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigationRows()

        pendingPunchAction = intent.getStringExtra(EXTRA_PUNCH)

        binding.backButton.setOnClickListener { finish() }
        binding.refreshButton.setOnClickListener { viewModel.loadDashboard() }
        binding.errorRetryButton.setOnClickListener { viewModel.loadDashboard() }
        binding.punchInButton.setOnClickListener { startPunchFlow(isPunchIn = true) }
        binding.punchOutButton.setOnClickListener { confirmPunchOut() }

        setupMap()
        observeDashboard()
        observePunchResults()

        skipNextResumeRefresh = true
        viewModel.loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        startLocationTracking()
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false
            return
        }
        if (!punchInProgress) viewModel.loadDashboard()
        startPunchElapsedTicker()
    }

    override fun onPause() {
        stopPunchElapsedTicker()
        locationHelper.stopLocationUpdates()
        super.onPause()
    }

    override fun onDestroy() {
        stopPunchElapsedTicker()
        locationHelper.cancel()
        super.onDestroy()
    }

    private fun setupMap() {
        // Without a Maps key the map renders as a blank grey box, so the distance and status
        // text stand in for it instead.
        if (!BuildConfig.HAS_MAPS_KEY) return

        binding.geofenceMapContainer.visibility = View.VISIBLE
        val fragment = SupportMapFragment.newInstance(
            // Lite mode draws a static bitmap: cheap, and it cannot fight the surrounding
            // ScrollView for touch gestures.
            GoogleMapOptions()
                .liteMode(true)
                .mapToolbarEnabled(false),
        )
        supportFragmentManager.beginTransaction()
            .replace(R.id.geofenceMapContainer, fragment)
            .commit()

        fragment.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isMapToolbarEnabled = false
            renderGeofence()
        }
    }

    private fun openTermsGate() {
        if (awaitingTermsAcceptance) return
        awaitingTermsAcceptance = true
        // Stay masked so wages and attendance are never briefly readable behind the gate.
        binding.loadingOverlay.visibility = View.VISIBLE
        termsGate.launch(
            Intent(this, FactoryTermsActivity::class.java)
                .putExtra(FactoryTermsActivity.EXTRA_REQUIRE_ACCEPTANCE, true),
        )
    }

    private fun startLocationTracking() {
        // onResume runs again as soon as the permission dialog closes, so asking more than
        // once per visit would trap a refusing user in a loop of system prompts.
        if (!locationHelper.hasLocationPermission()) {
            if (locationPermissionAsked) {
                trackingError = LocationHelper.Result.Error(
                    LocationHelper.Failure.PERMISSION_DENIED,
                    LocationHelper.MESSAGE_PERMISSION,
                )
                renderGeofence()
                return
            }
            locationPermissionAsked = true
        }

        locationHelper.trackLocation { result ->
            when (result) {
                is LocationHelper.Result.Success -> {
                    trackingError = null
                    lastFix = result
                }
                is LocationHelper.Result.Error -> trackingError = result
            }
            renderGeofence()
        }
    }

    private fun setupNavigationRows() {
        binding.historyRow.navRowTitle.text = "हाजिरी इतिहास"
        binding.historyRow.navRowSubtitle.text = "किसी भी महीने की रोज़ की हाजिरी"
        binding.historyRow.root.setOnClickListener {
            startActivity(Intent(this, AttendanceHistoryActivity::class.java))
        }

        binding.wageRow.navRowTitle.text = "महीने की मजदूरी"
        binding.wageRow.navRowSubtitle.text = "कमाई, एडवांस और बैलेंस"
        binding.wageRow.root.setOnClickListener {
            startActivity(Intent(this, MonthlyWageActivity::class.java))
        }

        binding.paymentsRow.navRowTitle.text = "पेमेंट इतिहास"
        binding.paymentsRow.navRowSubtitle.text = "एडवांस, सैलरी और बोनस"
        binding.paymentsRow.root.setOnClickListener {
            startActivity(Intent(this, PaymentHistoryActivity::class.java))
        }

        binding.termsRow.navRowTitle.text = "फैक्ट्री के नियम"
        binding.termsRow.navRowSubtitle.text = "आपकी फैक्ट्री के नियम"
        binding.termsRow.root.setOnClickListener {
            startActivity(Intent(this, FactoryTermsActivity::class.java))
        }

        binding.otRow.navRowTitle.text = "ओवरटाइम रिक्वेस्ट"
        binding.otRow.navRowSubtitle.text = "ओवरटाइम के घंटे अप्रूव करवाएं"
        binding.otRow.root.setOnClickListener {
            startActivity(
                Intent(this, AttendanceRequestActivity::class.java)
                    .putExtra(AttendanceRequestActivity.EXTRA_MODE, AttendanceRequestActivity.MODE_OT),
            )
        }

        binding.missedPunchRow.navRowTitle.text = "मिस्ड पंच"
        binding.missedPunchRow.navRowSubtitle.text = "पंच लगाना भूल गए? रिक्वेस्ट भेजें"
        binding.missedPunchRow.root.setOnClickListener {
            startActivity(
                Intent(this, AttendanceRequestActivity::class.java)
                    .putExtra(AttendanceRequestActivity.EXTRA_MODE, AttendanceRequestActivity.MODE_MISSED),
            )
        }

        binding.bulkAttendanceRow.navRowTitle.text = "कई लोगों की हाजिरी"
        binding.bulkAttendanceRow.navRowSubtitle.text = "बिना फोन वाले वर्कर की हाजिरी लगाएं"
        binding.bulkAttendanceRow.root.setOnClickListener {
            startActivity(Intent(this, BulkAttendanceActivity::class.java))
        }
    }

    private fun observeDashboard() {
        viewModel.dashboardLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.loadingOverlay.visibility = View.VISIBLE
                    binding.errorStateLayout.visibility = View.GONE
                }
                is ApiResult.Success -> {
                    binding.loadingOverlay.visibility = View.GONE
                    val payload = result.data?.data
                    if (payload == null) {
                        // A 2xx with no payload would otherwise render an empty screen with
                        // no punch button and no explanation, which reads as a dead screen.
                        binding.errorStateLayout.visibility = View.VISIBLE
                        binding.errorStateText.text =
                            result.data?.message?.takeIf { it.isNotBlank() }
                                ?: AttendanceErrorMapper.GENERIC_MESSAGE
                    } else {
                        binding.errorStateLayout.visibility = View.GONE
                        bindDashboard(payload)
                    }
                }
                is ApiResult.Error -> {
                    binding.loadingOverlay.visibility = View.GONE
                    val parsed = AttendanceErrorParser.parse(result.message)
                    binding.errorStateLayout.visibility = View.VISIBLE
                    binding.errorStateText.text = parsed.message
                }
            }
        }
    }

    private fun bindDashboard(data: EmployeeDashboardData?) {
        val employee = data?.employee
        val today = data?.today
        val summary = data?.monthSummary

        binding.greetingText.text = AttendanceHindi.greeting(data?.greeting)
        binding.employeeNameText.text = employee?.name ?: "-"
        val code = employee?.employeeCode.orEmpty()
        binding.employeeCodeText.text = if (code.isBlank()) "" else "कर्मचारी कोड: $code"
        binding.employeeCodeText.visibility = if (code.isBlank()) View.GONE else View.VISIBLE

        binding.todayDateText.text = today?.dateLabel ?: today?.date ?: "आज"
        binding.todayStatusChip.text = when {
            today?.attendanceMarked == true -> AttendanceHindi.status(today.statusLabel ?: "Marked")
            !today?.statusLabel.isNullOrBlank() -> AttendanceHindi.status(today?.statusLabel)
            else -> AttendanceHindi.status("Not Marked")
        }

        binding.factoryNameText.text = today?.factory?.name ?: "आज कोई फैक्ट्री नहीं मिली"
        val dailyWage = today?.dailyWage
        binding.dailyWageText.text =
            if (dailyWage == null) "" else "रोज़ की मजदूरी ${WageFormatter.format(dailyWage)}"
        binding.dailyWageText.visibility = if (dailyWage == null) View.GONE else View.VISIBLE

        binding.punchInTimeText.text = today?.punchInAt?.takeIf { it.isNotBlank() } ?: "--"
        binding.punchOutTimeText.text = today?.punchOutAt?.takeIf { it.isNotBlank() } ?: "--"
        punchInAtMillis = PunchElapsedFormatter.parseClock(today?.date, today?.punchInAt)
        punchOutAtMillis = PunchElapsedFormatter.parseClock(today?.date, today?.punchOutAt)
        if (punchInAtMillis != null) startPunchElapsedTicker() else stopPunchElapsedTicker()
        binding.todayEarnedText.text = WageFormatter.format(today?.earnedWage)

        binding.monthLabelText.text = summary?.monthLabel ?: summary?.month.orEmpty()
        binding.presentDaysText.text = (summary?.presentDays ?: 0).toString()
        binding.absentDaysText.text = (summary?.absentDays ?: 0).toString()
        binding.halfDaysText.text = (summary?.halfDays ?: 0).toString()
        binding.monthTotalEarnedText.text = WageFormatter.format(summary?.totalEarned)
        binding.monthRemainingBalanceText.text = WageFormatter.format(summary?.remainingBalance)

        // The gate comes first: an employee who has not agreed must not see, or act on, a
        // screen they are not yet entitled to use.
        if (data?.terms?.acceptanceRequired == true) {
            openTermsGate()
            return
        }

        employeeInactive = employee?.isActive == false
        todayFactory = today?.factory
        serverCanPunchIn = today?.canPunchIn == true
        serverCanPunchOut = today?.canPunchOut == true
        attendanceMarked = today?.attendanceMarked == true
        dashboardLoaded = true

        binding.bulkAttendanceRow.root.visibility =
            if (employee?.canBulkAttendance == true) View.VISIBLE else View.GONE

        if (employeeInactive) {
            showPunchMessage("आपका अकाउंट बंद है।")
        } else if (today?.hasActiveAssignment == false) {
            showPunchMessage(AttendanceErrorMapper.message(AttendanceErrorMapper.NO_ACTIVE_ASSIGNMENT))
        } else {
            hidePunchMessage()
        }

        renderGeofence()
        requestBackgroundLocationIfNeeded()
        maybeStartPendingPunch()
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            return
        }
        FactoryGeofenceMonitor.register(this, todayFactory)
    }

    private fun maybeStartPendingPunch() {
        if (pendingPunchConsumed || punchInProgress || employeeInactive) return
        when (pendingPunchAction) {
            PUNCH_IN -> if (serverCanPunchIn) {
                pendingPunchConsumed = true
                startPunchFlow(isPunchIn = true)
            }
            PUNCH_OUT -> if (serverCanPunchOut) {
                pendingPunchConsumed = true
                confirmPunchOut()
            }
        }
    }

    /**
     * Single place that turns "where the server says we are" plus "where GPS says we are" into
     * the geofence card and the punch button state. Called on every dashboard load and every
     * new fix, so the two can never drift apart.
     */
    private fun renderGeofence() {
        if (!dashboardLoaded) return

        val evaluation = GeofenceEvaluator.evaluate(
            factory = todayFactory,
            latitude = lastFix?.latitude,
            longitude = lastFix?.longitude,
            accuracy = lastFix?.accuracy,
        )

        val hasPunchAction = serverCanPunchIn || serverCanPunchOut
        val showCard = todayFactory != null && hasPunchAction &&
            !employeeInactive && !punchBlockedByError

        binding.geofenceCard.visibility = if (showCard) View.VISIBLE else View.GONE
        if (showCard) bindGeofenceCard(evaluation)

        updatePunchButtons(evaluation)
        updateMap(evaluation)
    }

    private fun bindGeofenceCard(evaluation: GeofenceEvaluator.Evaluation) {
        binding.geofenceStatusChip.text = evaluation.label
        binding.geofenceStatusChip.setBackgroundResource(
            if (evaluation.allowed) R.drawable.bg_punch_in_button else R.drawable.bg_attendance_status_chip,
        )

        // A blocked fix would otherwise show a stale distance as if it were usable.
        binding.geofenceDistanceText.text = evaluation.distanceMetres
            ?.takeIf { trackingError == null }
            ?.let { GeofenceEvaluator.formatDistance(it) }
            .orEmpty()

        binding.geofenceMessageText.text = trackingError?.message ?: evaluation.message
        binding.geofenceMessageText.setTextColor(
            ContextCompat.getColor(
                this,
                if (evaluation.allowed && trackingError == null) R.color.color_green else R.color.gray_light_3,
            ),
        )
    }

    private fun updatePunchButtons(evaluation: GeofenceEvaluator.Evaluation) {
        if (employeeInactive || punchBlockedByError || punchInProgress) {
            binding.punchInButton.visibility = View.GONE
            binding.punchOutButton.visibility = View.GONE
            binding.dutyHintText.visibility = View.GONE
            binding.dayCompleteText.visibility = View.GONE
            return
        }

        // When tracking never started, the punch flow itself raises the permission and GPS
        // dialogs, so the button has to stay tappable for the employee to recover.
        val locationUndetermined = trackingError != null ||
            evaluation.status == GeofenceEvaluator.Status.LOCATING
        val enabled = evaluation.allowed || locationUndetermined

        // Both punches are gated the same way because AttendanceService enforces the
        // geofence on both: an employee leaves by the gate they entered.
        applyPunchButtonState(binding.punchInButton, visible = serverCanPunchIn, enabled = enabled)
        applyPunchButtonState(binding.punchOutButton, visible = serverCanPunchOut, enabled = enabled)
        binding.dutyHintText.visibility = if (serverCanPunchIn) View.VISIBLE else View.GONE

        binding.dayCompleteText.visibility =
            if (!serverCanPunchIn && !serverCanPunchOut && attendanceMarked) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun applyPunchButtonState(button: View, visible: Boolean, enabled: Boolean) {
        button.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) return

        button.isEnabled = enabled
        button.isClickable = enabled
        button.alpha = if (enabled) 1f else 0.6f
        if (!enabled) {
            button.setBackgroundResource(R.drawable.bg_punch_disabled_button)
        } else if (button.id == R.id.punchInButton) {
            button.setBackgroundResource(R.drawable.bg_punch_in_button)
        } else {
            button.setBackgroundResource(R.drawable.bg_punch_out_button)
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateMap(evaluation: GeofenceEvaluator.Evaluation) {
        val map = googleMap ?: return
        val factory = todayFactory
        val factoryLat = factory?.latitude
        val factoryLng = factory?.longitude

        map.clear()
        if (factoryLat == null || factoryLng == null) {
            binding.geofenceMapContainer.visibility = View.GONE
            return
        }
        binding.geofenceMapContainer.visibility = View.VISIBLE

        val factoryPoint = LatLng(factoryLat, factoryLng)
        map.addMarker(
            MarkerOptions()
                .position(factoryPoint)
                .title(factory.name ?: "फैक्ट्री"),
        )
        map.addCircle(
            CircleOptions()
                .center(factoryPoint)
                .radius(evaluation.radiusMetres.toDouble())
                .strokeWidth(3f)
                .strokeColor(if (evaluation.allowed) COLOR_INSIDE_STROKE else COLOR_OUTSIDE_STROKE)
                .fillColor(if (evaluation.allowed) COLOR_INSIDE_FILL else COLOR_OUTSIDE_FILL),
        )

        val fix = lastFix
        if (fix == null || trackingError != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(factoryPoint, FACTORY_ONLY_ZOOM))
            return
        }

        val here = LatLng(fix.latitude, fix.longitude)
        map.addMarker(
            MarkerOptions()
                .position(here)
                .title("आप")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
        )

        val bounds = LatLngBounds.Builder().include(factoryPoint).include(here).build()
        // Lite mode has no laid-out map view to measure yet, so padding is passed explicitly.
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, MAP_BOUNDS_PADDING_PX))
    }

    private fun confirmPunchOut() {
        if (punchInProgress) return
        val outcome = PunchOutOutcome.evaluate(
            punchInAtMillis,
            System.currentTimeMillis(),
            todayFactory?.dutyEnd,
        )
        val title: String
        val message: String
        val confirm: String
        when (outcome) {
            PunchOutOutcome.Kind.HALF_DAY -> {
                title = "आधा दिन की हाजिरी"
                message = "शाम 6 बजे से पहले पंच आउट करने पर आधा दिन लगेगा। पक्का करें?"
                confirm = "हाँ, आधा दिन"
            }
            PunchOutOutcome.Kind.ZERO_HOURS -> {
                title = "0 घंटे की हाजिरी"
                message = "अभी 4 घंटे पूरे नहीं हुए। शाम 6 बजे से पहले पंच आउट करने पर आधा दिन भी नहीं लगेगा, हाजिरी 0 घंटे रहेगी। पक्का करें?"
                confirm = "हाँ, पंच आउट"
            }
            PunchOutOutcome.Kind.FULL_DAY -> {
                title = "क्या आप घर जा रहे हैं?"
                message = "पंच आउट लग जाएगा। पक्का करें?"
                confirm = "हाँ, पंच आउट"
            }
        }
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(confirm) { dialog, _ ->
                dialog.dismiss()
                startPunchFlow(isPunchIn = false)
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun startPunchFlow(isPunchIn: Boolean) {
        if (punchInProgress) return
        punchInProgress = true
        hidePunchMessage()
        binding.punchInButton.visibility = View.GONE
        binding.punchOutButton.visibility = View.GONE
        showPunchProgress("आपकी लोकेशन ले रहे हैं...")

        locationHelper.requestCurrentLocation { result ->
            when (result) {
                is LocationHelper.Result.Success -> {
                    showPunchProgress(
                        if (isPunchIn) "आपकी हाजिरी लग रही है..." else "पंच आउट लग रहा है...",
                    )
                    if (isPunchIn) {
                        viewModel.punchIn(result.latitude, result.longitude, result.accuracy)
                    } else {
                        viewModel.punchOut(result.latitude, result.longitude, result.accuracy)
                    }
                }
                is LocationHelper.Result.Error -> onLocationFailed(result)
            }
        }
    }

    private fun onLocationFailed(error: LocationHelper.Result.Error) {
        punchInProgress = false
        hidePunchProgress()
        showPunchMessage(error.message)
        when (error.failure) {
            LocationHelper.Failure.PERMISSION_PERMANENTLY_DENIED -> showSettingsDialog(
                title = "लोकेशन की अनुमति चाहिए",
                message = "हाजिरी लगाने के लिए लोकेशन चाहिए। सेटिंग में Rojgaarwaala की लोकेशन चालू करें।",
                positiveLabel = "सेटिंग खोलें",
                onPositive = { locationHelper.openAppSettings() },
            )
            LocationHelper.Failure.GPS_DISABLED -> showSettingsDialog(
                title = "GPS चालू करें",
                message = "हाजिरी लगाने के लिए लोकेशन चालू करें।",
                positiveLabel = "लोकेशन सेटिंग",
                onPositive = { locationHelper.openLocationSettings() },
            )
            else -> Unit
        }
        // The backend is the source of truth, so nothing is marked locally here.
        viewModel.loadDashboard()
    }

    private fun showSettingsDialog(
        title: String,
        message: String,
        positiveLabel: String,
        onPositive: () -> Unit,
    ) {
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveLabel) { dialog, _ ->
                dialog.dismiss()
                onPositive()
            }
            .setNegativeButton("रद्द करें", null)
            .show()
    }

    private fun observePunchResults() {
        viewModel.punchInLiveData.observe(this) { result ->
            handlePunchResult(result, "पंच इन लग गया")
        }
        viewModel.punchOutLiveData.observe(this) { result ->
            handlePunchResult(result, "पंच आउट लग गया")
        }
    }

    private fun handlePunchResult(result: ApiResult<PunchResponse>, successMessage: String) {
        when (result) {
            is ApiResult.Loading -> Unit
            is ApiResult.Success -> {
                punchInProgress = false
                hidePunchProgress()
                val body = result.data
                if (body?.status == false) {
                    // Defensive: a 2xx that still reports failure must not be treated as marked.
                    showPunchMessage(body.message ?: AttendanceErrorMapper.GENERIC_MESSAGE)
                } else {
                    hidePunchMessage()
                    Toast.makeText(this, body?.message ?: successMessage, Toast.LENGTH_LONG).show()
                }
                viewModel.loadDashboard()
            }
            is ApiResult.Error -> {
                punchInProgress = false
                hidePunchProgress()
                val parsed = AttendanceErrorParser.parse(result.message)
                if (AttendanceErrorMapper.disablesPunchUi(parsed.errorCode)) {
                    punchBlockedByError = true
                }
                // The terms changed while this screen was open, so agree again before retrying.
                if (AttendanceErrorMapper.requiresTermsAcceptance(parsed.errorCode)) {
                    openTermsGate()
                    return
                }
                showPunchMessage(parsed.message)
                Toast.makeText(this, parsed.message, Toast.LENGTH_LONG).show()
                viewModel.loadDashboard()
            }
        }
    }

    private fun showPunchProgress(message: String) {
        binding.punchProgressText.text = message
        binding.punchProgressLayout.visibility = View.VISIBLE
    }

    private fun hidePunchProgress() {
        binding.punchProgressLayout.visibility = View.GONE
    }

    private fun showPunchMessage(message: String) {
        binding.punchMessageText.text = message
        binding.punchMessageText.visibility = View.VISIBLE
    }

    private fun startPunchElapsedTicker() {
        val punchIn = punchInAtMillis
        if (punchIn == null) {
            stopPunchElapsedTicker()
            return
        }
        elapsedHandler.removeCallbacks(elapsedTick)
        renderPunchElapsed()
        if (punchOutAtMillis == null) {
            elapsedHandler.postDelayed(elapsedTick, 1000)
        }
    }

    private fun stopPunchElapsedTicker() {
        elapsedHandler.removeCallbacks(elapsedTick)
        if (punchInAtMillis == null) {
            binding.punchElapsedText.visibility = View.GONE
        }
    }

    private fun renderPunchElapsed() {
        val punchIn = punchInAtMillis ?: run {
            binding.punchElapsedText.visibility = View.GONE
            return
        }
        binding.punchElapsedText.visibility = View.VISIBLE
        binding.punchElapsedText.text = PunchElapsedFormatter.formatHindi(
            PunchElapsedFormatter.elapsedMillis(punchIn, System.currentTimeMillis(), punchOutAtMillis),
            running = punchOutAtMillis == null,
        )
    }

    private fun hidePunchMessage() {
        binding.punchMessageText.visibility = View.GONE
    }
}
