package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.srijeesolution.rojgaarwaala.data.remote.model.OtRequestItem
import com.srijeesolution.rojgaarwaala.data.remote.model.OtRequestResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.OtReviewResponse
import com.srijeesolution.rojgaarwaala.databinding.ActivityOtReviewBinding
import com.srijeesolution.rojgaarwaala.databinding.ItemOtReviewBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import com.srijeesolution.rojgaarwaala.utils.AttendanceHindi
import com.srijeesolution.rojgaarwaala.utils.OtStatusStyle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OtReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtReviewBinding
    private val viewModel: EmployeeAttendanceViewModel by viewModels()
    private var reviewing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        viewModel.otReviewsLiveData.observe(this) { bindQueue(it) }
        viewModel.otReviewActionLiveData.observe(this) { bindAction(it) }
        viewModel.loadOtReviews()
    }

    private fun bindQueue(result: ApiResult<OtReviewResponse>) {
        when (result) {
            is ApiResult.Loading -> {
                binding.statusText.visibility = View.VISIBLE
                binding.statusText.text = "ओवरटाइम लोड हो रहा है..."
            }
            is ApiResult.Success -> {
                binding.statusText.visibility = View.GONE
                val data = result.data?.data
                binding.factoryNameText.text = data?.factoryName.orEmpty()
                renderPending(data?.pendingList.orEmpty())
                renderHistory(data?.historyList.orEmpty())
            }
            is ApiResult.Error -> {
                binding.statusText.visibility = View.VISIBLE
                binding.statusText.text = AttendanceErrorParser.parse(result.message).message
            }
        }
    }

    private fun bindAction(result: ApiResult<OtRequestResponse>) {
        when (result) {
            is ApiResult.Loading -> {
                reviewing = true
                binding.statusText.visibility = View.VISIBLE
                binding.statusText.text = "सेव हो रहा है..."
            }
            is ApiResult.Success -> {
                reviewing = false
                Toast.makeText(this, result.data?.message ?: "सेव हो गया", Toast.LENGTH_SHORT).show()
                viewModel.loadOtReviews()
            }
            is ApiResult.Error -> {
                reviewing = false
                binding.statusText.visibility = View.VISIBLE
                binding.statusText.text = AttendanceErrorParser.parse(result.message).message
            }
        }
    }

    private fun renderPending(items: List<OtRequestItem>) {
        binding.pendingList.removeAllViews()
        if (items.isEmpty()) {
            binding.pendingList.addView(emptyRow("अभी कोई पेंडिंग ओवरटाइम नहीं").root)
            return
        }
        items.forEach { binding.pendingList.addView(reviewRow(it, showActions = true).root) }
    }

    private fun renderHistory(items: List<OtRequestItem>) {
        binding.historyList.removeAllViews()
        if (items.isEmpty()) {
            binding.historyList.addView(emptyRow("अभी कोई हिस्ट्री नहीं").root)
            return
        }
        items.forEach { binding.historyList.addView(reviewRow(it, showActions = false).root) }
    }

    private fun emptyRow(message: String): ItemOtReviewBinding {
        val row = ItemOtReviewBinding.inflate(layoutInflater, binding.pendingList, false)
        row.otEmployeeText.text = message
        row.otStatusText.visibility = View.GONE
        row.otDateText.visibility = View.GONE
        row.otHoursText.visibility = View.GONE
        row.otReasonText.visibility = View.GONE
        row.otActionRow.visibility = View.GONE
        return row
    }

    private fun reviewRow(item: OtRequestItem, showActions: Boolean): ItemOtReviewBinding {
        val row = ItemOtReviewBinding.inflate(layoutInflater, binding.pendingList, false)
        val name = item.employeeName.orEmpty().ifBlank { "वर्कर" }
        val code = item.employeeCode.orEmpty()
        row.otEmployeeText.text = if (code.isBlank()) name else "$name  •  $code"
        row.otStatusText.text = AttendanceHindi.status(item.status)
        OtStatusStyle.apply(row.otStatusText, item.status)
        row.otDateText.text = item.workDate ?: "-"
        val hours = item.hours ?: 0
        val amount = item.approvedAmount
        row.otHoursText.text = buildString {
            append("$hours घंटे")
            if (amount != null && amount > 0) {
                append("  •  ₹${amount.toInt()}")
            }
        }
        val reason = item.reason.orEmpty()
        row.otReasonText.text = reason
        row.otReasonText.visibility = if (reason.isBlank()) View.GONE else View.VISIBLE
        row.otActionRow.visibility = if (showActions) View.VISIBLE else View.GONE
        if (showActions) {
            row.approveButton.setOnClickListener { decide(item.id, approve = true) }
            row.rejectButton.setOnClickListener { decide(item.id, approve = false) }
        }
        return row
    }

    private fun decide(id: Int?, approve: Boolean) {
        if (id == null || reviewing) return
        if (approve) viewModel.approveOtRequest(id) else viewModel.rejectOtRequest(id)
    }
}
