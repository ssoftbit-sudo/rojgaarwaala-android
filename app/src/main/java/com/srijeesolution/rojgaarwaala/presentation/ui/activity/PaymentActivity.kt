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
  private var applicationId: Int = 0
  private var fallbackRazorpayKey: String = ""
  private var fallbackAmountPaise: Int = 10000
  private var candidateName: String = ""
  private var candidatePhone: String = ""
  private var candidateEmail: String = ""
  private var pendingOrderId: String? = null
  private var pendingPaymentId: String? = null
  private var orderRequestStarted = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityPaymentBinding.inflate(layoutInflater)
    setContentView(binding.root)

    viewModel = ViewModelProvider(this)[PaymentViewModel::class.java]

    applicationId = intent.getStringExtra("application_id")?.toIntOrNull() ?: 0
    fallbackRazorpayKey = intent.getStringExtra("razorpay_key_id").orEmpty()
    fallbackAmountPaise = intent.getIntExtra("amount_paise", 10000)
    candidateName = intent.getStringExtra("candidate_name") ?: ""
    candidatePhone = intent.getStringExtra("candidate_phone") ?: ""
    candidateEmail = intent.getStringExtra("candidate_email") ?: ""

    Checkout.preload(applicationContext)

    setupClickListeners()
    observeViewModel()
  }

  private fun setupClickListeners() {
    binding.backButton.setOnClickListener { finish() }
    binding.payButton.setOnClickListener { prepareAndPay() }
  }

  private fun prepareAndPay() {
    if (applicationId <= 0) {
      Toast.makeText(this, "Application not found. Please re-apply.", Toast.LENGTH_LONG).show()
      return
    }
    orderRequestStarted = true
    binding.payButton.isEnabled = false
    binding.payButton.text = "Preparing..."
    viewModel.createRazorpayOrder(applicationId)
  }

  private fun openCheckout(orderId: String, keyId: String, amountPaise: Int) {
    val checkout = Checkout()
    checkout.setKeyID(keyId.ifBlank { fallbackRazorpayKey })

    try {
      val options = JSONObject()
      options.put("name", "Rojgaarwaala")
      options.put("description", "Job Application Fee")
      options.put("currency", "INR")
      options.put("order_id", orderId)
      options.put("amount", amountPaise)

      val prefill = JSONObject()
      prefill.put("name", candidateName)
      prefill.put("email", candidateEmail)
      prefill.put("contact", candidatePhone)
      options.put("prefill", prefill)

      checkout.open(this, options)
    } catch (e: Exception) {
      Toast.makeText(this, "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
      binding.payButton.isEnabled = true
      binding.payButton.text = "Pay ₹100"
    }
  }

  override fun onPaymentSuccess(razorpayPaymentId: String?) {
    pendingPaymentId = razorpayPaymentId
    Toast.makeText(this, "Verifying payment...", Toast.LENGTH_SHORT).show()
    viewModel.updateApplicationAfterPayment(
      applicationId,
      razorpayPaymentId,
      pendingOrderId,
    )
  }

  override fun onPaymentError(code: Int, response: String?) {
    if (applicationId > 0) {
      viewModel.updatePaymentFailure(applicationId)
    }
    binding.payButton.isEnabled = true
    binding.payButton.text = "Pay ₹100"
    Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_SHORT).show()
  }

  private fun observeViewModel() {
    viewModel.orderReady.observe(this) { state ->
      if (!orderRequestStarted) return@observe
      binding.payButton.isEnabled = true
      binding.payButton.text = "Pay ₹100"
      if (state != null && state.orderId.isNotBlank()) {
        pendingOrderId = state.orderId
        openCheckout(
          orderId = state.orderId,
          keyId = state.razorpayKeyId.ifBlank { fallbackRazorpayKey },
          amountPaise = state.amountPaise,
        )
      } else if (viewModel.isLoading.value != true) {
        Toast.makeText(
          this,
          viewModel.errorMessage.value ?: "Could not start payment",
          Toast.LENGTH_LONG,
        ).show()
      }
    }

    viewModel.updateResult.observe(this) { success ->
      if (success) {
        openStatusScreen()
      } else {
        binding.payButton.isEnabled = true
        binding.payButton.text = "Pay ₹100"
        Toast.makeText(this, "Failed to confirm payment on server", Toast.LENGTH_SHORT).show()
      }
    }

    viewModel.isLoading.observe(this) { loading ->
      if (loading && pendingPaymentId == null) {
        binding.payButton.isEnabled = false
        binding.payButton.text = "Please wait..."
      }
    }
  }

  private fun openStatusScreen() {
    val intent = Intent(this, ApplicationStatusActivity::class.java)
    intent.putExtra("application_id", applicationId.toString())
    startActivity(intent)
    finish()
  }
}
