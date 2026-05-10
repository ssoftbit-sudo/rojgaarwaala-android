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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[PaymentViewModel::class.java]

        // Get intent extras
        videoId = intent.getIntExtra("video_id", 0)
        applicationId = intent.getStringExtra("application_id") ?: ""

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
        val checkout = Checkout()
        // Set your Razorpay key here
        checkout.setKeyID("YOUR_RAZORPAY_KEY_ID")

        try {
            val options = JSONObject()
            options.put("name", "Rojgaarwaala")
            options.put("description", "Job Application Fee")
            options.put("currency", "INR")
            options.put("amount", "10000") // Amount in paise (₹100 = 10000 paise)

            val prefill = JSONObject()
            prefill.put("email", "user@example.com")
            prefill.put("contact", "9876543210")
            options.put("prefill", prefill)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: " + e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Toast.makeText(this, "Payment successful", Toast.LENGTH_SHORT).show()
        viewModel.updateApplicationStatus(applicationId, "paid", razorpayPaymentId)
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show()
        // Handle payment failure
    }

    private fun observeViewModel() {
        viewModel.updateResult.observe(this) { success ->
            if (success) {
                // Navigate to status screen or back to main
                val intent = Intent(this, ApplicationStatusActivity::class.java)
                intent.putExtra("application_id", applicationId)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to update payment status", Toast.LENGTH_SHORT).show()
            }
        }
    }
}