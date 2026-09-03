package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.databinding.ActivityMonthlyWageBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.FactoryBreakdownAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import com.srijeesolution.rojgaarwaala.utils.MonthSelector
import com.srijeesolution.rojgaarwaala.utils.WageFormatter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MonthlyWageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonthlyWageBinding
    private val viewModel: EmployeeAttendanceViewModel by viewModels()
    private val adapter = FactoryBreakdownAdapter()

    private val currentMonth = MonthSelector.currentMonth()
    private var selectedMonth = currentMonth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthlyWageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.breakdownRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.breakdownRecyclerView.adapter = adapter

        binding.backButton.setOnClickListener { finish() }
        binding.errorRetryButton.setOnClickListener { loadSelectedMonth() }
        binding.monthPrevButton.setOnClickListener { shiftMonth(-1) }
        binding.monthNextButton.setOnClickListener { shiftMonth(1) }

        observeSummary()
        loadSelectedMonth()
    }

    private fun shiftMonth(delta: Int) {
        val next = MonthSelector.shift(selectedMonth, delta)
        if (next.year * 12 + next.month > currentMonth.year * 12 + currentMonth.month) return
        selectedMonth = next
        loadSelectedMonth()
    }

    private fun loadSelectedMonth() {
        binding.monthLabelText.text = selectedMonth.label
        binding.monthNextButton.visibility =
            if (selectedMonth.value == currentMonth.value) View.INVISIBLE else View.VISIBLE
        viewModel.loadMonthlySummary(selectedMonth.value)
    }

    private fun observeSummary() {
        viewModel.monthlySummaryLiveData.observe(this) { result ->
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
                    data?.monthLabel?.takeIf { it.isNotBlank() }?.let {
                        binding.monthLabelText.text = it
                    }
                    binding.totalEarnedText.text = WageFormatter.format(data?.totalEarned)
                    binding.advanceTakenText.text = WageFormatter.format(data?.advanceTaken)
                    binding.salaryPaidText.text = WageFormatter.format(data?.salaryPaid)
                    binding.remainingBalanceText.text =
                        WageFormatter.format(data?.remainingBalance)
                    binding.cumulativeBalanceText.text =
                        WageFormatter.format(data?.cumulativeBalance)

                    val breakdown = data?.factoryBreakdown ?: emptyList()
                    adapter.submitList(breakdown)
                    binding.emptyStateText.visibility =
                        if (breakdown.isEmpty()) View.VISIBLE else View.GONE
                    binding.breakdownRecyclerView.visibility =
                        if (breakdown.isEmpty()) View.GONE else View.VISIBLE
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
