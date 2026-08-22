package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import com.srijeesolution.rojgaarwaala.utils.GeofenceEvaluator
import com.srijeesolution.rojgaarwaala.utils.LocationHelper
import com.srijeesolution.rojgaarwaala.utils.WageFormatter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttendanceDashboardActivity : AppCompatActivity() {

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

        binding.backButton.setOnClickListener { finish() }
        binding.refreshButton.setOnClickListener { viewModel.loadDashboard() }
        binding.errorRetryButton.setOnClickListener { viewModel.loadDashboard() }
        binding.punchInButton.setOnClickListener { startPunchFlow(isPunchIn = true) }
        binding.punchOutButton.setOnClickListener { startPunchFlow(isPunchIn = false) }

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
    }

    override fun onPause() {
        locationHelper.stopLocationUpdates()
        super.onPause()
    }

    override fun onDestroy() {
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
        binding.historyRow.navRowTitle.text = "Attendance History"
        binding.historyRow.navRowSubtitle.text = "Day by day record for any month"
        binding.historyRow.root.setOnClickListener {
            startActivity(Intent(this, AttendanceHistoryActivity::class.java))
        }

        binding.wageRow.navRowTitle.text = "Monthly Wage"
        binding.wageRow.navRowSubtitle.text = "Earnings, advance and balance"
        binding.wageRow.root.setOnClickListener {
            startActivity(Intent(this, MonthlyWageActivity::class.java))
        }

        binding.paymentsRow.navRowTitle.text = "Payment History"
        binding.paymentsRow.navRowSubtitle.text = "Advances, salary and bonus receipts"
        binding.paymentsRow.root.setOnClickListener {
            startActivity(Intent(this, PaymentHistoryActivity::class.java))
        }

        binding.termsRow.navRowTitle.text = "Factory Terms"
        binding.termsRow.navRowSubtitle.text = "Rules and conditions of your factory"
        binding.termsRow.root.setOnClickListener {
            startActivity(Intent(this, FactoryTermsActivity::class.java))
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

        binding.greetingText.text = data?.greeting ?: "Welcome"
        binding.employeeNameText.text = employee?.name ?: "-"
        val code = employee?.employeeCode.orEmpty()
        binding.employeeCodeText.text = if (code.isBlank()) "" else "Employee code: $code"
        binding.employeeCodeText.visibility = if (code.isBlank()) View.GONE else View.VISIBLE

        binding.todayDateText.text = today?.dateLabel ?: today?.date ?: "Today"
        binding.todayStatusChip.text = when {
            today?.attendanceMarked == true -> today.statusLabel ?: "Marked"
            !today?.statusLabel.isNullOrBlank() -> today?.statusLabel.orEmpty()
            else -> "Not Marked"
        }

        binding.factoryNameText.text = today?.factory?.name ?: "No factory assigned today"
        val dailyWage = today?.dailyWage
        binding.dailyWageText.text =
            if (dailyWage == null) "" else "Daily wage ${WageFormatter.format(dailyWage)}"
        binding.dailyWageText.visibility = if (dailyWage == null) View.GONE else View.VISIBLE

        binding.punchInTimeText.text = today?.punchInAt?.takeIf { it.isNotBlank() } ?: "--"
        binding.punchOutTimeText.text = today?.punchOutAt?.takeIf { it.isNotBlank() } ?: "--"
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

        if (employeeInactive) {
            showPunchMessage("Your employee account is inactive.")
        } else if (today?.hasActiveAssignment == false) {
            showPunchMessage(AttendanceErrorMapper.message(AttendanceErrorMapper.NO_ACTIVE_ASSIGNMENT))
        } else {
            hidePunchMessage()
        }

        renderGeofence()
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
            binding.dayCompleteText.visibility = View.GONE
            return
        }

        // When tracking never started, the punch flow itself raises the permission and GPS
        // dialogs, so the button has to stay tappable for the employee to recover.
        val locationUndetermined = trackingError != null ||
            evaluation.status == GeofenceEvaluator.Status.LOCATING
        val enabled = evaluation.allowed || locationUndetermined

        applyPunchButtonState(binding.punchInButton, visible = serverCanPunchIn, enabled = enabled)
        applyPunchButtonState(binding.punchOutButton, visible = serverCanPunchOut, enabled = enabled)

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
                .title(factory.name ?: "Factory"),
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
                .title("You")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
        )

        val bounds = LatLngBounds.Builder().include(factoryPoint).include(here).build()
        // Lite mode has no laid-out map view to measure yet, so padding is passed explicitly.
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, MAP_BOUNDS_PADDING_PX))
    }

    private fun startPunchFlow(isPunchIn: Boolean) {
        if (punchInProgress) return
        punchInProgress = true
        hidePunchMessage()
        binding.punchInButton.visibility = View.GONE
        binding.punchOutButton.visibility = View.GONE
        showPunchProgress("Getting your location...")

        locationHelper.requestCurrentLocation { result ->
            when (result) {
                is LocationHelper.Result.Success -> {
                    showPunchProgress(
                        if (isPunchIn) "Marking your attendance..." else "Recording punch out...",
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
                title = "Location permission required",
                message = "Attendance can only be marked with your location. " +
                    "Enable the location permission for Rojgaarwaala in app settings.",
                positiveLabel = "Open Settings",
                onPositive = { locationHelper.openAppSettings() },
            )
            LocationHelper.Failure.GPS_DISABLED -> showSettingsDialog(
                title = "Please enable GPS",
                message = "Turn on location services to mark your attendance.",
                positiveLabel = "Location Settings",
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
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observePunchResults() {
        viewModel.punchInLiveData.observe(this) { result ->
            handlePunchResult(result, "Punched in successfully")
        }
        viewModel.punchOutLiveData.observe(this) { result ->
            handlePunchResult(result, "Punched out successfully")
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

    private fun hidePunchMessage() {
        binding.punchMessageText.visibility = View.GONE
    }

    private companion object {
        val COLOR_INSIDE_STROKE = Color.argb(255, 76, 175, 80)
        val COLOR_INSIDE_FILL = Color.argb(60, 76, 175, 80)
        val COLOR_OUTSIDE_STROKE = Color.argb(255, 229, 57, 53)
        val COLOR_OUTSIDE_FILL = Color.argb(50, 229, 57, 53)

        const val FACTORY_ONLY_ZOOM = 16f
        const val MAP_BOUNDS_PADDING_PX = 80
    }
}
