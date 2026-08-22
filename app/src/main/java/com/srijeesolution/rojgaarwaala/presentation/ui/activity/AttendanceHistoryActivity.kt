package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.databinding.ActivityAttendanceHistoryBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.AttendanceHistoryAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import com.srijeesolution.rojgaarwaala.utils.MonthSelector
import com.srijeesolution.rojgaarwaala.utils.WageFormatter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttendanceHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceHistoryBinding
    private val viewModel: EmployeeAttendanceViewModel by viewModels()
    private val adapter = AttendanceHistoryAdapter()

    private val currentMonth = MonthSelector.currentMonth()
    private var selectedMonth = currentMonth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.attendanceRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.attendanceRecyclerView.adapter = adapter

        binding.backButton.setOnClickListener { finish() }
        binding.errorRetryButton.setOnClickListener { loadSelectedMonth() }
        binding.monthPrevButton.setOnClickListener { shiftMonth(-1) }
        binding.monthNextButton.setOnClickListener { shiftMonth(1) }

        observeAttendance()
        loadSelectedMonth()
    }

    private fun shiftMonth(delta: Int) {
        val next = MonthSelector.shift(selectedMonth, delta)
        // The API only has history up to the current month.
        if (next.year * 12 + next.month > currentMonth.year * 12 + currentMonth.month) return
        selectedMonth = next
        loadSelectedMonth()
    }

    private fun loadSelectedMonth() {
        binding.monthLabelText.text = selectedMonth.label
        binding.monthNextButton.visibility =
            if (selectedMonth.value == currentMonth.value) View.INVISIBLE else View.VISIBLE
        viewModel.loadAttendance(selectedMonth.value)
    }

    private fun observeAttendance() {
        viewModel.attendanceLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Loading -> {
                    binding.loadingOverlay.visibility = View.VISIBLE
                    binding.errorStateLayout.visibility = View.GONE
                    binding.mainContent.visibility = View.GONE
                }
                is ApiResult.Success -> {
                    binding.loadingOverlay.visibility = View.GONE
                    binding.errorStateLayout.visibility = View.GONE
                    binding.mainContent.visibility = View.VISIBLE

                    val data = result.data?.data
                    binding.presentDaysText.text = (data?.presentDays ?: 0).toString()
                    binding.absentDaysText.text = (data?.absentDays ?: 0).toString()
                    binding.halfDaysText.text = (data?.halfDays ?: 0).toString()
                    binding.totalEarnedText.text = WageFormatter.format(data?.totalEarned)
                    data?.monthLabel?.takeIf { it.isNotBlank() }?.let {
                        binding.monthLabelText.text = it
                    }

                    val items = data?.attendanceList ?: emptyList()
                    adapter.submitList(items)
                    binding.emptyStateText.visibility =
                        if (items.isEmpty()) View.VISIBLE else View.GONE
                    binding.attendanceRecyclerView.visibility =
                        if (items.isEmpty()) View.GONE else View.VISIBLE
                }
                is ApiResult.Error -> {
                    binding.loadingOverlay.visibility = View.GONE
                    binding.mainContent.visibility = View.GONE
                    binding.errorStateLayout.visibility = View.VISIBLE
                    binding.errorStateText.text = AttendanceErrorParser.parse(result.message).message
                }
            }
        }
    }
}
