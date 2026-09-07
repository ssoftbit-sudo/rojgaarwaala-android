package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.srijeesolution.rojgaarwaala.data.remote.model.MissedPunchResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.OtRequestResponse
import com.srijeesolution.rojgaarwaala.databinding.ActivityAttendanceRequestBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttendanceRequestActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_OT = "ot"
        const val MODE_MISSED = "missed"
    }

    private lateinit var binding: ActivityAttendanceRequestBinding
    private val viewModel: EmployeeAttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val missed = intent.getStringExtra(EXTRA_MODE) == MODE_MISSED
        binding.backButton.setOnClickListener { finish() }

        if (missed) {
            binding.titleText.text = "Missed Punch"
            binding.helpText.text =
                "अगर आपने पंच लगाना भूल गए हैं तो रिक्वेस्ट भेजें। सुपरवाइज़र अप्रूव करेंगे।"
            binding.hoursLabel.visibility = View.GONE
            binding.hoursInput.visibility = View.GONE
            binding.punchTypeGroup.visibility = View.VISIBLE
            binding.submitButton.text = "Send missed punch request"
        }

        binding.submitButton.setOnClickListener { submit(missed) }

        viewModel.otRequestLiveData.observe(this) { result ->
            if (!missed) bindOt(result)
        }
        viewModel.missedPunchLiveData.observe(this) { result ->
            if (missed) bindMissed(result)
        }
    }

    private fun submit(missed: Boolean) {
        val reason = binding.reasonInput.text?.toString()?.trim().orEmpty()
        if (reason.length < 3) {
            Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show()
            return
        }
        if (missed) {
            val type = if (binding.punchOutOption.isChecked) "punch_out" else "punch_in"
            viewModel.submitMissedPunch(type, reason)
        } else {
            val hours = binding.hoursInput.text?.toString()?.toIntOrNull() ?: 0
            if (hours < 1) {
                Toast.makeText(this, "Enter overtime hours", Toast.LENGTH_SHORT).show()
                return
            }
            viewModel.submitOtRequest(hours, reason)
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

    private fun showSending() {
        binding.submitButton.isEnabled = false
        binding.statusText.visibility = View.VISIBLE
        binding.statusText.text = "Sending..."
    }

    private fun onSent(message: String?) {
        binding.submitButton.isEnabled = true
        Toast.makeText(this, message ?: "Request sent", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun onFailed(message: String?) {
        binding.submitButton.isEnabled = true
        binding.statusText.visibility = View.VISIBLE
        binding.statusText.text = message ?: "Unable to send request"
    }
}
