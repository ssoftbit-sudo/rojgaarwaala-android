package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeDashboardData
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchResponse
import com.srijeesolution.rojgaarwaala.databinding.ActivityAttendanceDashboardBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorMapper
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
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

        observeDashboard()
        observePunchResults()

        skipNextResumeRefresh = true
        viewModel.loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false
            return
        }
        if (!punchInProgress) viewModel.loadDashboard()
    }

    override fun onDestroy() {
        locationHelper.cancel()
        super.onDestroy()
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
                    binding.errorStateLayout.visibility = View.GONE
                    bindDashboard(result.data?.data)
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

        val inactive = employee?.isActive == false
        if (inactive) {
            showPunchMessage("Your employee account is inactive.")
        } else if (today?.hasActiveAssignment == false) {
            showPunchMessage(AttendanceErrorMapper.message(AttendanceErrorMapper.NO_ACTIVE_ASSIGNMENT))
        } else {
            hidePunchMessage()
        }

        updatePunchButtons(
            canPunchIn = today?.canPunchIn == true,
            canPunchOut = today?.canPunchOut == true,
            attendanceMarked = today?.attendanceMarked == true,
            disabled = inactive || punchBlockedByError,
        )
    }

    private fun updatePunchButtons(
        canPunchIn: Boolean,
        canPunchOut: Boolean,
        attendanceMarked: Boolean,
        disabled: Boolean,
    ) {
        if (disabled || punchInProgress) {
            binding.punchInButton.visibility = View.GONE
            binding.punchOutButton.visibility = View.GONE
            binding.dayCompleteText.visibility = View.GONE
            return
        }
        binding.punchInButton.visibility = if (canPunchIn) View.VISIBLE else View.GONE
        binding.punchOutButton.visibility = if (canPunchOut) View.VISIBLE else View.GONE
        binding.dayCompleteText.visibility =
            if (!canPunchIn && !canPunchOut && attendanceMarked) View.VISIBLE else View.GONE
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
}
