package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.srijeesolution.rojgaarwaala.data.remote.model.MissedPunchItem
import com.srijeesolution.rojgaarwaala.data.remote.model.MissedPunchResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.MissedPunchReviewResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.TeamMember
import com.srijeesolution.rojgaarwaala.databinding.ActivityBulkAttendanceBinding
import com.srijeesolution.rojgaarwaala.databinding.ItemOtReviewBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import com.srijeesolution.rojgaarwaala.utils.AttendanceHindi
import com.srijeesolution.rojgaarwaala.utils.LocationHelper
import com.srijeesolution.rojgaarwaala.utils.OtStatusStyle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BulkAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBulkAttendanceBinding
    private val viewModel: EmployeeAttendanceViewModel by viewModels()
    private val locationHelper = LocationHelper(this)
    private val checkBoxes = mutableListOf<CheckBox>()
    private var reviewing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBulkAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.submitButton.setOnClickListener { submit() }

        viewModel.teamLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> binding.statusText.apply {
                    visibility = View.VISIBLE
                    text = "टीम लोड हो रही है..."
                }
                is ApiResult.Success -> {
                    binding.statusText.visibility = View.GONE
                    binding.factoryNameText.text = result.data?.data?.factoryName ?: ""
                    renderTeam(result.data?.data?.teamList.orEmpty())
                }
                is ApiResult.Error -> {
                    binding.statusText.visibility = View.VISIBLE
                    binding.statusText.text = AttendanceErrorParser.parse(result.message).message
                }
            }
        }

        viewModel.bulkPunchLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.submitButton.isEnabled = false
                    binding.statusText.visibility = View.VISIBLE
                    binding.statusText.text = "हाजिरी लग रही है..."
                }
                is ApiResult.Success -> {
                    binding.submitButton.isEnabled = true
                    val punched = result.data?.data?.punched?.size ?: 0
                    val skipped = result.data?.data?.skipped?.size ?: 0
                    Toast.makeText(this, "लग गई $punched, छोड़ी $skipped", Toast.LENGTH_LONG).show()
                    viewModel.loadTeam()
                }
                is ApiResult.Error -> {
                    binding.submitButton.isEnabled = true
                    binding.statusText.visibility = View.VISIBLE
                    binding.statusText.text = AttendanceErrorParser.parse(result.message).message
                }
            }
        }

        viewModel.missedReviewsLiveData.observe(this) { bindMissedQueue(it) }
        viewModel.missedReviewActionLiveData.observe(this) { bindMissedAction(it) }

        viewModel.loadTeam()
        viewModel.loadMissedPunchReviews()
    }

    private fun bindMissedQueue(result: ApiResult<MissedPunchReviewResponse>) {
        when (result) {
            is ApiResult.Loading -> Unit
            is ApiResult.Success -> {
                val data = result.data?.data
                renderMissedRequests(
                    data?.pendingList.orEmpty(),
                    data?.historyList.orEmpty(),
                )
            }
            is ApiResult.Error -> {
                binding.missedRequestList.removeAllViews()
                binding.missedRequestList.addView(
                    emptyMissedRow(AttendanceErrorParser.parse(result.message).message).root,
                )
            }
        }
    }

    private fun bindMissedAction(result: ApiResult<MissedPunchResponse>) {
        when (result) {
            is ApiResult.Loading -> {
                reviewing = true
                binding.statusText.visibility = View.VISIBLE
                binding.statusText.text = "सेव हो रहा है..."
            }
            is ApiResult.Success -> {
                reviewing = false
                binding.statusText.visibility = View.GONE
                Toast.makeText(this, result.data?.message ?: "सेव हो गया", Toast.LENGTH_SHORT).show()
                viewModel.loadMissedPunchReviews()
                viewModel.loadTeam()
            }
            is ApiResult.Error -> {
                reviewing = false
                binding.statusText.visibility = View.VISIBLE
                binding.statusText.text = AttendanceErrorParser.parse(result.message).message
            }
        }
    }

    private fun renderMissedRequests(pending: List<MissedPunchItem>, history: List<MissedPunchItem>) {
        binding.missedRequestList.removeAllViews()
        if (pending.isEmpty() && history.isEmpty()) {
            binding.missedRequestList.addView(emptyMissedRow("अभी कोई मिस्ड पंच रिक्वेस्ट नहीं").root)
            return
        }
        pending.forEach { item ->
            binding.missedRequestList.addView(missedRow(item, showActions = true).root)
        }
        history.forEach { item ->
            binding.missedRequestList.addView(missedRow(item, showActions = false).root)
        }
    }

    private fun emptyMissedRow(message: String): ItemOtReviewBinding {
        val row = ItemOtReviewBinding.inflate(layoutInflater, binding.missedRequestList, false)
        row.otEmployeeText.text = message
        row.otStatusText.visibility = View.GONE
        row.otDateText.visibility = View.GONE
        row.otHoursText.visibility = View.GONE
        row.otReasonText.visibility = View.GONE
        row.otActionRow.visibility = View.GONE
        return row
    }

    private fun missedRow(item: MissedPunchItem, showActions: Boolean): ItemOtReviewBinding {
        val row = ItemOtReviewBinding.inflate(layoutInflater, binding.missedRequestList, false)
        val name = item.employeeName.orEmpty().ifBlank { "वर्कर" }
        val code = item.employeeCode.orEmpty()
        row.otEmployeeText.text = if (code.isBlank()) name else "$name  •  $code"
        row.otStatusText.text = AttendanceHindi.status(item.status)
        OtStatusStyle.apply(row.otStatusText, item.status)
        row.otDateText.text = item.workDate ?: "-"
        row.otHoursText.text = AttendanceHindi.punchType(item.punchType)
        val reason = item.reason.orEmpty()
        row.otReasonText.text = reason
        row.otReasonText.visibility = if (reason.isBlank()) View.GONE else View.VISIBLE
        row.otActionRow.visibility = if (showActions) View.VISIBLE else View.GONE
        row.approveButton.setOnClickListener { decideMissed(item.id, approve = true) }
        row.rejectButton.setOnClickListener { decideMissed(item.id, approve = false) }
        return row
    }

    private fun decideMissed(id: Int?, approve: Boolean) {
        if (id == null || reviewing) return
        if (approve) viewModel.approveMissedPunch(id) else viewModel.rejectMissedPunch(id)
    }

    private fun renderTeam(members: List<TeamMember>) {
        binding.teamListContainer.removeAllViews()
        checkBoxes.clear()
        members.forEach { member ->
            val box = CheckBox(this).apply {
                text = "${member.employeeCode} — ${member.name} (${AttendanceHindi.status(member.statusLabel)})"
                setTextColor(0xFFFFFFFF.toInt())
                isEnabled = member.attendanceMarked != true
                isChecked = member.attendanceMarked != true
                tag = member.id
            }
            checkBoxes.add(box)
            binding.teamListContainer.addView(box)
        }
        if (members.isEmpty()) {
            binding.teamListContainer.addView(
                CheckBox(this).apply {
                    text = "आज आपकी फैक्ट्री में कोई वर्कर नहीं है।"
                    setTextColor(0xFFCCCCCC.toInt())
                    isEnabled = false
                    isClickable = false
                },
            )
        }
    }

    private fun submit() {
        val ids = checkBoxes.mapNotNull { box ->
            if (box.isChecked && box.isEnabled) box.tag as? Int else null
        }
        if (ids.isEmpty()) {
            Toast.makeText(this, "कम से कम एक वर्कर चुनें", Toast.LENGTH_SHORT).show()
            return
        }
        locationHelper.requestCurrentLocation { result ->
            when (result) {
                is LocationHelper.Result.Success -> viewModel.bulkPunchIn(
                    ids,
                    result.latitude,
                    result.longitude,
                    result.accuracy,
                )
                is LocationHelper.Result.Error -> Toast.makeText(
                    this,
                    result.message,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
}
