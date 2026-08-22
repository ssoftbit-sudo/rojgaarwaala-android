package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeePaymentItem
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeePaymentProof
import com.srijeesolution.rojgaarwaala.databinding.ActivityPaymentHistoryBinding
import com.srijeesolution.rojgaarwaala.databinding.DialogPaymentProofBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.EmployeePaymentsAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import com.srijeesolution.rojgaarwaala.utils.AttendanceErrorParser
import com.srijeesolution.rojgaarwaala.utils.AuthenticatedGlide
import com.srijeesolution.rojgaarwaala.utils.WageFormatter
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PaymentHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentHistoryBinding
    private val viewModel: EmployeeAttendanceViewModel by viewModels()

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private val adapter = EmployeePaymentsAdapter(onViewProof = { payment -> openProof(payment) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.paymentsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.paymentsRecyclerView.adapter = adapter

        binding.backButton.setOnClickListener { finish() }
        binding.errorRetryButton.setOnClickListener { viewModel.loadPayments() }

        observePayments()
        viewModel.loadPayments()
    }

    private fun observePayments() {
        viewModel.paymentsLiveData.observe(this) { result ->
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
                    val totals = data?.totals
                    binding.totalAdvanceText.text = WageFormatter.format(totals?.advance)
                    binding.totalSalaryText.text = WageFormatter.format(totals?.salaryPayment)
                    binding.totalBonusText.text = WageFormatter.format(totals?.bonus)
                    binding.totalDeductionText.text = WageFormatter.format(totals?.deduction)

                    val payments = data?.paymentList ?: emptyList()
                    adapter.submitList(payments)
                    binding.emptyStateText.visibility =
                        if (payments.isEmpty()) View.VISIBLE else View.GONE
                    binding.paymentsRecyclerView.visibility =
                        if (payments.isEmpty()) View.GONE else View.VISIBLE
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

    private fun openProof(payment: EmployeePaymentItem) {
        val proofs = payment.proofList.orEmpty()
        when {
            proofs.isEmpty() -> return
            proofs.size == 1 -> showProofDialog(proofs.first())
            else -> {
                val labels = proofs.mapIndexed { index, proof ->
                    proof.fileName ?: "Proof ${index + 1}"
                }.toTypedArray()
                AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
                    .setTitle("Select proof")
                    .setItems(labels) { dialog, which ->
                        dialog.dismiss()
                        showProofDialog(proofs[which])
                    }
                    .show()
            }
        }
    }

    private fun showProofDialog(proof: EmployeePaymentProof) {
        val dialogBinding = DialogPaymentProofBinding.inflate(layoutInflater)
        dialogBinding.proofFileNameText.text = proof.fileName.orEmpty()
        dialogBinding.proofFileNameText.visibility =
            if (proof.fileName.isNullOrBlank()) View.GONE else View.VISIBLE

        val dialog = AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setView(dialogBinding.root)
            .create()
        dialogBinding.proofCloseButton.setOnClickListener { dialog.dismiss() }

        val url = proof.url.orEmpty()
        if (proof.isImage != true || url.isBlank()) {
            // PDFs and other documents cannot be rendered inline; the file has to be
            // opened from a browser session that carries the same login.
            dialogBinding.proofFallbackText.visibility = View.VISIBLE
            dialogBinding.proofFallbackText.text = buildString {
                append(proof.fileName ?: "This proof")
                append(" cannot be previewed in the app.\n\n")
                append("Please ask your supervisor for a copy, or open it from the website.")
            }
            dialog.show()
            return
        }

        dialogBinding.proofProgressBar.visibility = View.VISIBLE
        dialogBinding.proofImageView.visibility = View.VISIBLE

        // Proof files are behind the API auth guard: Glide has to send the bearer token.
        val authToken = sharedPrefs.getPrefs(SharedPrefsConstant.USER_AUTH_TOKEN, "").orEmpty()
        Glide.with(this)
            .load(AuthenticatedGlide.url(url, authToken))
            .fitCenter()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    dialogBinding.proofProgressBar.visibility = View.GONE
                    dialogBinding.proofImageView.visibility = View.GONE
                    dialogBinding.proofFallbackText.visibility = View.VISIBLE
                    dialogBinding.proofFallbackText.text =
                        "Unable to load this proof. Please try again."
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    dialogBinding.proofProgressBar.visibility = View.GONE
                    return false
                }
            })
            .into(dialogBinding.proofImageView)

        dialog.show()
    }
}
