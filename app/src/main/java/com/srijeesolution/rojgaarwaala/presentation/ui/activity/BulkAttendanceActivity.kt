package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.srijeesolution.rojgaarwaala.data.remote.model.TeamMember
import com.srijeesolution.rojgaarwaala.databinding.ActivityBulkAttendanceBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import com.srijeesolution.rojgaarwaala.utils.LocationHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BulkAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBulkAttendanceBinding
    private val viewModel: EmployeeAttendanceViewModel by viewModels()
    private val locationHelper = LocationHelper(this)
    private val checkBoxes = mutableListOf<CheckBox>()

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
                    text = "Loading team..."
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
                    binding.statusText.text = "Marking attendance..."
                }
                is ApiResult.Success -> {
                    binding.submitButton.isEnabled = true
                    val punched = result.data?.data?.punched?.size ?: 0
                    val skipped = result.data?.data?.skipped?.size ?: 0
                    Toast.makeText(this, "Marked $punched, skipped $skipped", Toast.LENGTH_LONG).show()
                    viewModel.loadTeam()
                }
                is ApiResult.Error -> {
                    binding.submitButton.isEnabled = true
                    binding.statusText.visibility = View.VISIBLE
                    binding.statusText.text = AttendanceErrorParser.parse(result.message).message
                }
            }
        }

        viewModel.loadTeam()
    }

    private fun renderTeam(members: List<TeamMember>) {
        binding.teamListContainer.removeAllViews()
        checkBoxes.clear()
        members.forEach { member ->
            val box = CheckBox(this).apply {
                text = "${member.employeeCode} — ${member.name} (${member.statusLabel ?: "Not Marked"})"
                setTextColor(0xFFFFFFFF.toInt())
                isEnabled = member.attendanceMarked != true
                isChecked = member.attendanceMarked != true
                tag = member.id
            }
            checkBoxes.add(box)
            binding.teamListContainer.addView(box)
        }
        if (members.isEmpty()) {
            binding.statusText.visibility = View.VISIBLE
            binding.statusText.text = "No workers assigned to your factory today."
        }
    }

    private fun submit() {
        val ids = checkBoxes.mapNotNull { box ->
            if (box.isChecked && box.isEnabled) box.tag as? Int else null
        }
        if (ids.isEmpty()) {
            Toast.makeText(this, "Select at least one worker", Toast.LENGTH_SHORT).show()
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
