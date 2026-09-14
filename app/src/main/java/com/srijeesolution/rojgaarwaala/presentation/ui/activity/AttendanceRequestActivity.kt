package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.srijeesolution.rojgaarwaala.data.remote.model.MissedPunchResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.OtRequestItem
import com.srijeesolution.rojgaarwaala.data.remote.model.OtRequestResponse
import com.srijeesolution.rojgaarwaala.databinding.ActivityAttendanceRequestBinding
import com.srijeesolution.rojgaarwaala.databinding.ItemOtRequestBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import com.srijeesolution.rojgaarwaala.utils.AttendanceHindi
import com.srijeesolution.rojgaarwaala.utils.OtStatusStyle
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class AttendanceRequestActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_OT = "ot"
        const val MODE_MISSED = "missed"
    }

    private lateinit var binding: ActivityAttendanceRequestBinding
    private val viewModel: EmployeeAttendanceViewModel by viewModels()
    private val workDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val missed = intent.getStringExtra(EXTRA_MODE) == MODE_MISSED
        binding.backButton.setOnClickListener { finish() }

        binding.workDateRow.visibility = View.VISIBLE
        renderWorkDate()
        binding.workDateText.setOnClickListener { openWorkDatePicker() }

        if (missed) {
            binding.titleText.text = "मिस्ड पंच"
            binding.helpText.text =
                "अगर आपने पंच लगाना भूल गए हैं तो रिक्वेस्ट भेजें। सुपरवाइज़र अप्रूव करेंगे।"
            binding.hoursLabel.visibility = View.GONE
            binding.hoursInput.visibility = View.GONE
            binding.punchTypeGroup.visibility = View.VISIBLE
            binding.workDateLabel.text = "कौन सी तारीख का पंच मिस हुआ?"
            binding.submitButton.text = "मिस्ड पंच रिक्वेस्ट भेजें"
        } else {
            binding.workDateLabel.text = "किस तारीख का ओवरटाइम है?"
            binding.otHistoryLabel.visibility = View.VISIBLE
            binding.otHistoryList.visibility = View.VISIBLE
            viewModel.loadOtRequests()
        }

        binding.submitButton.setOnClickListener { submit(missed) }

        viewModel.otRequestLiveData.observe(this) { result ->
            if (!missed) bindOt(result)
        }
        viewModel.missedPunchLiveData.observe(this) { result ->
            if (missed) bindMissed(result)
        }
        viewModel.otListLiveData.observe(this) { result ->
            if (!missed) bindOtHistory(result)
        }
    }

    private fun submit(missed: Boolean) {
        val reason = binding.reasonInput.text?.toString()?.trim().orEmpty()
        if (reason.length < 3) {
            Toast.makeText(this, "कारण लिखें", Toast.LENGTH_SHORT).show()
            return
        }
        if (missed) {
            val type = if (binding.punchOutOption.isChecked) "punch_out" else "punch_in"
            viewModel.submitMissedPunch(type, reason, apiWorkDate())
        } else {
            val hours = binding.hoursInput.text?.toString()?.toIntOrNull() ?: 0
            if (hours < 1) {
                Toast.makeText(this, "ओवरटाइम के घंटे लिखें", Toast.LENGTH_SHORT).show()
                return
            }
            viewModel.submitOtRequest(hours, reason, apiWorkDate())
        }
    }

    private fun bindOt(result: ApiResult<OtRequestResponse>) {
        when (result) {
            is ApiResult.Loading -> showSending()
            is ApiResult.Success -> onSent(result.data?.message)
            is ApiResult.Error -> onFailed(AttendanceErrorParser.parse(result.message).message)
        }
    }

    private fun bindMissed(result: ApiResult<MissedPunchResponse>) {
        when (result) {
            is ApiResult.Loading -> showSending()
            is ApiResult.Success -> onSent(result.data?.message)
            is ApiResult.Error -> onFailed(AttendanceErrorParser.parse(result.message).message)
        }
    }

    private fun bindOtHistory(result: ApiResult<OtRequestResponse>) {
        if (result !is ApiResult.Success) {
            return
        }
        val items = result.data?.data?.otRequestList.orEmpty()
        binding.otHistoryLabel.visibility = View.VISIBLE
        binding.otHistoryList.visibility = View.VISIBLE
        binding.otHistoryList.removeAllViews()
        if (items.isEmpty()) {
            val empty = ItemOtRequestBinding.inflate(layoutInflater, binding.otHistoryList, false)
            empty.otDateText.text = "अभी कोई रिक्वेस्ट नहीं"
            empty.otStatusText.visibility = View.GONE
            empty.otHoursText.visibility = View.GONE
            empty.otReasonText.visibility = View.GONE
            binding.otHistoryList.addView(empty.root)
            return
        }
        items.forEach { binding.otHistoryList.addView(otRow(it).root) }
    }

    private fun otRow(item: OtRequestItem): ItemOtRequestBinding {
        val row = ItemOtRequestBinding.inflate(layoutInflater, binding.otHistoryList, false)
        row.otDateText.text = item.workDate ?: "-"
        row.otStatusText.text = AttendanceHindi.status(item.status)
        OtStatusStyle.apply(row.otStatusText, item.status)
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
        return row
    }

    private fun openWorkDatePicker() {
        val picker = DatePickerDialog(
            this,
            { _, year, month, day ->
                workDate.set(year, month, day)
                renderWorkDate()
            },
            workDate.get(Calendar.YEAR),
            workDate.get(Calendar.MONTH),
            workDate.get(Calendar.DAY_OF_MONTH),
        )
        picker.datePicker.maxDate = System.currentTimeMillis()
        val min = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -31) }
        picker.datePicker.minDate = min.timeInMillis
        picker.show()
    }

    private fun renderWorkDate() {
        binding.workDateText.text = hindiDate(workDate)
    }

    private fun apiWorkDate(): String {
        val year = workDate.get(Calendar.YEAR)
        val month = workDate.get(Calendar.MONTH) + 1
        val day = workDate.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(year, month, day)
    }

    private fun hindiDate(calendar: Calendar): String {
        val months = arrayOf(
            "जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून",
            "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर",
        )
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = months[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        return "$day $month $year"
    }

    private fun showSending() {
        binding.submitButton.isEnabled = false
        binding.statusText.visibility = View.VISIBLE
        binding.statusText.text = "भेज रहे हैं..."
    }

    private fun onSent(message: String?) {
        binding.submitButton.isEnabled = true
        Toast.makeText(this, message ?: "रिक्वेस्ट भेज दी गई", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun onFailed(message: String?) {
        binding.submitButton.isEnabled = true
        binding.statusText.visibility = View.VISIBLE
        binding.statusText.text = message ?: "रिक्वेस्ट नहीं गई"
    }
}
