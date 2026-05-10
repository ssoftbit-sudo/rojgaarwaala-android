package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.srijeesolution.rojgaarwaala.databinding.ActivityPaymentBinding
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject

@AndroidEntryPoint
class PaymentActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var viewModel: PaymentViewModel
    private var videoId: Int = 0
    private var applicationId: String = ""
    private var candidateName: String = ""
    private var candidatePhone: String = ""
    private var candidateEmail: String = ""

    companion object {
        // Demo Razorpay test key (public key). Replace with your project-specific test key anytime.
        private const val DEMO_RAZORPAY_TEST_KEY = "rzp_test_1DP5mmOlF5G5ag"
        private const val APPLICATION_FEE_PAISE = 10000
    }

    private val isLocalBypassFlow: Boolean
        get() = applicationId.startsWith("local_")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[PaymentViewModel::class.java]

        // Get intent extras
        videoId = intent.getIntExtra("video_id", 0)
        applicationId = intent.getStringExtra("application_id") ?: ""
        candidateName = intent.getStringExtra("candidate_name") ?: ""
        candidatePhone = intent.getStringExtra("candidate_phone") ?: ""
        candidateEmail = intent.getStringExtra("candidate_email") ?: ""

        Checkout.preload(applicationContext)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.payButton.setOnClickListener {
            startPayment()
        }
    }

    private fun startPayment() {
        if (applicationId.isBlank()) {
            Toast.makeText(this, "Application not found. Please re-apply.", Toast.LENGTH_LONG).show()
            return
        }

        val checkout = Checkout()
        checkout.setKeyID(DEMO_RAZORPAY_TEST_KEY)

        try {
            val options = JSONObject()
            options.put("name", "Rojgaarwaala")
            options.put("description", "Job Application Fee - Resume Forwarding")
            options.put("currency", "INR")
            options.put("amount", APPLICATION_FEE_PAISE)
            options.put("notes", JSONObject().apply {
                put("application_id", applicationId)
                put("video_id", videoId)
            })

            val prefill = JSONObject()
            prefill.put("name", candidateName)
            prefill.put("email", candidateEmail)
            prefill.put("contact", candidatePhone)
            options.put("prefill", prefill)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: " + e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Toast.makeText(this, "Payment successful. Resume forwarded to HR.", Toast.LENGTH_SHORT).show()
        if (isLocalBypassFlow) {
            openStatusScreen("pending")
            return
        }
        viewModel.updateApplicationAfterPayment(applicationId, razorpayPaymentId)
    }

    override fun onPaymentError(code: Int, response: String?) {
        if (!isLocalBypassFlow) {
            viewModel.updatePaymentFailure(applicationId)
        }
        Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_SHORT).show()
    }

    private fun observeViewModel() {
        viewModel.updateResult.observe(this) { success ->
            if (success) {
                openStatusScreen()
            } else {
                Toast.makeText(this, "Failed to update payment status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openStatusScreen(statusOverride: String? = null) {
        val intent = Intent(this, ApplicationStatusActivity::class.java)
        intent.putExtra("application_id", applicationId)
        statusOverride?.let { intent.putExtra("status_override", it) }
        startActivity(intent)
        finish()
    }
}