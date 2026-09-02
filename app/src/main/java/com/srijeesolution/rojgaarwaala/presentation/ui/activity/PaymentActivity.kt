package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.ActivityPaymentBinding
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.PaymentViewModel
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.PaymentViewModel.PaymentState
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

/**
 * Hosts the job application fee payment.
 *
 * Checkout happens on the gateway's own page inside a Custom Tab, so no card
 * data passes through this app. When the customer comes back — by deep link or
 * by dismissing the tab — the server is asked what actually happened.
 */
@AndroidEntryPoint
class PaymentActivity : AppCompatActivity() {

  private lateinit var binding: ActivityPaymentBinding
  private val viewModel: PaymentViewModel by viewModels()

  private var applicationId: Int = 0
  private var amountPaise: Int = DEFAULT_AMOUNT_PAISE

  /** Guards against re-opening the tab when the activity is recreated. */
  private var paymentPageOpened = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityPaymentBinding.inflate(layoutInflater)
    setContentView(binding.root)

    applicationId = intent.getStringExtra(EXTRA_APPLICATION_ID)?.toIntOrNull() ?: 0
    amountPaise = intent.getIntExtra(EXTRA_AMOUNT_PAISE, DEFAULT_AMOUNT_PAISE)

    binding.priceText.text = formatAmount(amountPaise)
    binding.backButton.setOnClickListener { finish() }
    binding.payButton.setOnClickListener { startPayment() }

    observeViewModel()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)

    // Arrived back from the gateway's receipt page. The status in the deep link
    // is only a hint — the server is the one that decides.
    if (intent.data?.scheme == RETURN_SCHEME && applicationId > 0) {
      paymentPageOpened = false
      viewModel.verifyPayment(applicationId)
    }
  }

  override fun onResume() {
    super.onResume()

    // The customer swiped the Custom Tab away instead of being redirected.
    // Check anyway: they may well have paid before closing it.
    if (paymentPageOpened && viewModel.awaitingGatewayResult && applicationId > 0) {
      paymentPageOpened = false
      viewModel.verifyPayment(applicationId)
    }
  }

  private fun startPayment() {
    if (applicationId <= 0) {
      Toast.makeText(this, "Application not found. Please apply again.", Toast.LENGTH_LONG).show()
      return
    }

    viewModel.startPayment(applicationId)
  }

  private fun observeViewModel() {
    viewModel.state.observe(this) { state ->
      when (state) {
        is PaymentState.Idle -> {
          setPayButton(enabled = true, label = "Pay ${formatAmount(amountPaise)}")
        }

        is PaymentState.Preparing -> {
          setPayButton(enabled = false, label = "Preparing…")
          showHint(null)
        }

        is PaymentState.OpenPaymentPage -> {
          setPayButton(enabled = false, label = "Opening payment page…")
          openPaymentPage(state.url)
        }

        is PaymentState.AwaitingResult -> {
          setPayButton(enabled = false, label = "Waiting for payment…")
        }

        is PaymentState.Verifying -> {
          setPayButton(enabled = false, label = "Confirming payment…")
          showHint("Confirming your payment. Please do not close the app.")
        }

        is PaymentState.Paid -> {
          setPayButton(enabled = false, label = "Payment successful")
          openStatusScreen()
        }

        is PaymentState.NotPaid -> {
          setPayButton(enabled = true, label = "Try again")
          showHint(state.reason ?: "Payment was not completed. You have not been charged.")
        }

        is PaymentState.Error -> {
          setPayButton(enabled = true, label = "Try again")
          showHint(state.message)
        }
      }
    }
  }

  private fun openPaymentPage(url: String) {
    if (paymentPageOpened) return

    val intent = CustomTabsIntent.Builder()
      .setShowTitle(true)
      .setUrlBarHidingEnabled(false)
      .setDefaultColorSchemeParams(
        CustomTabColorSchemeParams.Builder()
          .setToolbarColor(ContextCompat.getColor(this, R.color.app_background))
          .build(),
      )
      .build()

    try {
      intent.launchUrl(this, Uri.parse(url))
      paymentPageOpened = true
      viewModel.onPaymentPageLaunched()
    } catch (e: ActivityNotFoundException) {
      viewModel.onPaymentPageDismissed()
      Toast.makeText(
        this,
        "No browser found to complete the payment.",
        Toast.LENGTH_LONG,
      ).show()
    }
  }

  private fun setPayButton(enabled: Boolean, label: String) {
    binding.payButton.isEnabled = enabled
    binding.payButton.isClickable = enabled
    binding.payButton.alpha = if (enabled) 1f else 0.6f
    binding.payButton.text = label
  }

  private fun showHint(message: String?) {
    binding.paymentModeHint.text = message.orEmpty()
    binding.paymentModeHint.visibility = if (message.isNullOrBlank()) View.GONE else View.VISIBLE
  }

  private fun openStatusScreen() {
    val intent = Intent(this, ApplicationStatusActivity::class.java)
    intent.putExtra(EXTRA_APPLICATION_ID, applicationId.toString())
    startActivity(intent)
    finish()
  }

  private fun formatAmount(paise: Int): String {
    val rupees = paise / 100.0
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = if (paise % 100 == 0) 0 else 2
    return formatter.format(rupees)
  }

  companion object {
    const val EXTRA_APPLICATION_ID = "application_id"
    const val EXTRA_AMOUNT_PAISE = "amount_paise"

    private const val RETURN_SCHEME = "rojgaarwaala"
    private const val DEFAULT_AMOUNT_PAISE = 10000
  }
}
