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
import androidx.lifecycle.lifecycleScope
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.ActivityPaymentBinding
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.PaymentViewModel
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.PaymentViewModel.PaymentState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Hosts the job application fee payment.
 *
 * Checkout happens on the gateway's own page inside a Custom Tab, so no card
 * data passes through this app. The tab is closed for the customer as soon as
 * the server sees the payment; otherwise they come back by the receipt page's
 * deep link or by dismissing the tab, and the server is asked what happened.
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

    applicationId = intent.getStringExtra(EXTRA_APPLICATION_ID)?.toIntOrNull()
      ?: intent.data?.getQueryParameter("application_id")?.toIntOrNull()
      ?: savedInstanceState?.getInt(STATE_APPLICATION_ID, 0)
      ?: 0
    amountPaise = intent.getIntExtra(EXTRA_AMOUNT_PAISE, DEFAULT_AMOUNT_PAISE)
    paymentPageOpened = savedInstanceState?.getBoolean(STATE_PAGE_OPENED) ?: false

    binding.priceText.text = formatAmount(amountPaise)
    binding.backButton.setOnClickListener { finish() }
    binding.payButton.setOnClickListener { startPayment() }

    observeViewModel()
    handleReturn(intent)

    // Collected while stopped too: that is exactly when the tab is on top.
    lifecycleScope.launch {
      viewModel.paidWhileAway.collect { closePaymentPage() }
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel.stopAwayPolling()
  }

  override fun onStop() {
    super.onStop()
    if (paymentPageOpened && applicationId > 0) {
      viewModel.startAwayPolling(applicationId)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(STATE_APPLICATION_ID, applicationId)
    outState.putBoolean(STATE_PAGE_OPENED, paymentPageOpened)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)

    // singleTask: a fresh launch from Apply arrives here when the screen exists.
    (intent.getStringExtra(EXTRA_APPLICATION_ID)?.toIntOrNull()
      ?: intent.data?.getQueryParameter("application_id")?.toIntOrNull())
      ?.let { applicationId = it }

    // Arrived back from the gateway's receipt page. The status in the deep link
    // is only a hint — the server is the one that decides.
    handleReturn(intent)
  }

  private fun handleReturn(intent: Intent?) {
    if (intent?.data?.scheme == RETURN_SCHEME && applicationId > 0) {
      paymentPageOpened = false
      viewModel.rememberOrderId(intent.data?.getQueryParameter("order_id"))
      viewModel.verifyPayment(applicationId)
    }
  }

  /**
   * Brings this screen back over the Custom Tab. The tab sits above us in the
   * same task, so CLEAR_TOP finishes it — the customer does not have to tap ✕.
   */
  private fun closePaymentPage() {
    paymentPageOpened = false
    try {
      startActivity(
        Intent(this, PaymentActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
      )
    } catch (e: RuntimeException) {
      // Some Android versions refuse to launch from behind another app's
      // screen. The state is already Paid, so closing the tab still lands on
      // the success path.
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
          showHint(
            state.reason
              ?: "Payment was not confirmed. If money was debited, it will be confirmed automatically or refunded. Check My Applications before paying again.",
          )
        }

        is PaymentState.Confirming -> {
          setPayButton(enabled = false, label = "Confirming…")
          showHint(state.message)
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
    private const val STATE_APPLICATION_ID = "application_id"
    private const val STATE_PAGE_OPENED = "payment_page_opened"
  }
}
