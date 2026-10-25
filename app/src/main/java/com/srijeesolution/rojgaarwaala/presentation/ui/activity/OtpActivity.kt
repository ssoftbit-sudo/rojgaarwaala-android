package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import androidx.lifecycle.ViewModelProvider
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.ActivityOtpBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.OtpSms
import com.srijeesolution.rojgaarwaala.utils.ProfileGate
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class OtpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpBinding
    private lateinit var homePageViewModel: HomePageViewModel
    private var mobile: String? = null
    private var resendTimer: CountDownTimer? = null

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    /**
     * SMS User Consent: when the OTP SMS lands, Android asks the seeker once whether the
     * app may read that one message. No SMS permission, no app hash in the SMS, and the
     * sender name (e.g. CP-SBSITS-S) does not matter.
     */
    private val smsConsent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val code = OtpSms.extract(result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE))
            ?: return@registerForActivityResult
        binding.otpEditText.setText(code)
        binding.otpEditText.setSelection(code.length)
        verifyOtp()
    }

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
            val extras = intent.extras ?: return
            val status = IntentCompat.getParcelableExtra(intent, SmsRetriever.EXTRA_STATUS, Status::class.java)
            if (status?.statusCode != CommonStatusCodes.SUCCESS) return
            val consent = IntentCompat.getParcelableExtra(intent, SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                ?: return
            if (extras.isEmpty) return
            runCatching { smsConsent.launch(consent) }
        }
    }

    private var smsReceiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // The manifest hides the keyboard on entry; this screen exists to be typed into.
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
        )
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        mobile = intent.getStringExtra("mobile")
        binding.otpSubtitleText.text = getString(R.string.otp_sent_to, mobile.orEmpty())

        intent.getStringExtra("fallback_otp")?.takeIf { it.isNotBlank() }?.let { otp ->
            binding.fallbackOtpText.visibility = View.VISIBLE
            binding.fallbackOtpText.text = getString(R.string.otp_fallback, otp)
            binding.otpEditText.setText(otp)
        }

        bindOtpBoxes()
        observeVerifyOtp()
        observeResendOtp()
        binding.submitOtpButton.setOnClickListener { verifyOtp() }
        binding.resendOtpButton.setOnClickListener { resendOtp() }
        startResendCooldown(60)
        listenForOtpSms()
    }

    /** Listens for the next SMS with a code in it, for up to five minutes. */
    private fun listenForOtpSms() {
        runCatching { SmsRetriever.getClient(this).startSmsUserConsent(null) }
        if (!smsReceiverRegistered) {
            runCatching {
                ContextCompat.registerReceiver(
                    this,
                    smsReceiver,
                    IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
                    SmsRetriever.SEND_PERMISSION,
                    null,
                    ContextCompat.RECEIVER_EXPORTED,
                )
                smsReceiverRegistered = true
            }
        }
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        if (smsReceiverRegistered) runCatching { unregisterReceiver(smsReceiver) }
        super.onDestroy()
    }

    /**
     * The six boxes only mirror the one real field. Typing, paste and SMS autofill all
     * land in that field, so none of them needs handling per box.
     */
    private fun bindOtpBoxes() {
        val boxes = listOf(
            binding.otpBox1, binding.otpBox2, binding.otpBox3,
            binding.otpBox4, binding.otpBox5, binding.otpBox6,
        )
        fun render(code: String) {
            boxes.forEachIndexed { index, box ->
                box.text = code.getOrNull(index)?.toString().orEmpty()
                // Filled boxes and the one being typed into are lit, so it is clear
                // where the next digit goes even without a blinking cursor.
                box.setBackgroundResource(
                    if (index <= code.length) R.drawable.bg_otp_box_filled else R.drawable.bg_otp_box,
                )
            }
        }
        binding.otpEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                render(s?.toString().orEmpty())
                binding.otpErrorText.visibility = View.GONE
            }
        })
        render(binding.otpEditText.text?.toString().orEmpty())

        // Typing by hand must always be possible: open the keyboard on arrival, and
        // again on any tap on the boxes in case it was closed.
        binding.otpEditText.setOnClickListener { showOtpKeyboard() }
        binding.otpEditText.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showOtpKeyboard() }
        binding.otpEditText.post { showOtpKeyboard() }
    }

    private fun showOtpKeyboard() {
        val field = binding.otpEditText
        field.requestFocus()
        field.setSelection(field.text?.length ?: 0)
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun verifyOtp() {
        val otp = binding.otpEditText.text.toString().trim()
        if (TextUtils.isEmpty(otp) || otp.length < 6) {
            binding.otpErrorText.text = getString(R.string.otp_required)
            binding.otpErrorText.visibility = View.VISIBLE
            return
        }

        hideKeyboard()
        binding.submitOtpButton.isEnabled = false
        binding.submitOtpButton.text = getString(R.string.otp_verifying)
        binding.progressBar.visibility = View.VISIBLE

        val requestBody = HashMap<String, String>()
        requestBody["mobile"] = mobile ?: ""
        requestBody["otp"] = otp
        homePageViewModel.verifyOtp(requestBody)
    }

    private fun resendOtp() {
        val phone = mobile?.takeIf { it.isNotBlank() } ?: return
        binding.resendOtpButton.isEnabled = false
        binding.resendOtpButton.text = getString(R.string.login_sending)
        val requestBody = hashMapOf("mobile" to phone)
        homePageViewModel.sendOtp(requestBody)
        listenForOtpSms()
    }

    private fun startResendCooldown(seconds: Long) {
        resendTimer?.cancel()
        binding.resendOtpButton.isEnabled = false
        resendTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.resendOtpButton.text = getString(R.string.otp_resend_in, (millisUntilFinished / 1000).toInt())
            }

            override fun onFinish() {
                binding.resendOtpButton.isEnabled = true
                binding.resendOtpButton.text = getString(R.string.otp_resend)
            }
        }.start()
    }

    private fun observeVerifyOtp() {
        homePageViewModel.verifyOtpLiveData.observe(this) { apiResponse ->
            when(apiResponse){
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.submitOtpButton.isEnabled = true
                    binding.submitOtpButton.text = getString(R.string.otp_verify)

                    if (apiResponse.data?.status == true) {
                        apiResponse.data.dataObj?.let { dataObj ->
                            dataObj.token?.let { token ->
                                sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.USER_AUTH_TOKEN, token))
                            }
                            ProfileGate.remember(sharedPrefs, dataObj.userDetails)
                        }
                        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.USER_LOGGED_IN_STATUS, true))
                        sharedPrefs.removeSharedPrefs(SharedPrefsConstant.USER_SKIP_STATUS)
                        Toast.makeText(this, "Successfully logged in!", Toast.LENGTH_SHORT).show()
                        val next = if (ProfileGate.needsCompletion(apiResponse.data.dataObj?.userDetails)) {
                            ProfileGate.requiredProfileIntent(this)
                        } else {
                            Intent(this, MainActivity::class.java)
                        }
                        next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(next)
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            apiResponse.data?.message ?: "Invalid OTP",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.submitOtpButton.isEnabled = true
                    binding.submitOtpButton.text = getString(R.string.otp_verify)
                    Toast.makeText(
                        this,
                        parseApiErrorMessage(apiResponse.message),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun observeResendOtp() {
        homePageViewModel.sendOtpLiveData.observe(this) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> Unit
                is ApiResult.Success -> {
                    if (apiResponse.data?.status == true) {
                        apiResponse.data.dataObj?.otp?.takeIf { it.isNotBlank() }?.let { otp ->
                            binding.fallbackOtpText.visibility = View.VISIBLE
                            binding.fallbackOtpText.text = getString(R.string.otp_fallback, otp)
                            binding.otpEditText.setText(otp)
                        }
                        Toast.makeText(this, "OTP sent again", Toast.LENGTH_SHORT).show()
                        startResendCooldown(60)
                    } else {
                        binding.resendOtpButton.isEnabled = true
                        binding.resendOtpButton.text = getString(R.string.otp_resend)
                        Toast.makeText(
                            this,
                            apiResponse.data?.message ?: "Failed to resend OTP",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
                is ApiResult.Error -> {
                    binding.resendOtpButton.isEnabled = true
                    binding.resendOtpButton.text = getString(R.string.otp_resend)
                    Toast.makeText(
                        this,
                        parseApiErrorMessage(apiResponse.message),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    private fun parseApiErrorMessage(error: ApiError?): String {
        val body = error?.errorBody.orEmpty()
        if (body.isNotBlank()) {
            try {
                val message = JSONObject(body).optString("message")
                if (message.isNotBlank()) return message
            } catch (_: Exception) {
                // fall through
            }
        }
        return error?.errorMsg?.takeIf { it.isNotBlank() } ?: "Invalid OTP"
    }
}
