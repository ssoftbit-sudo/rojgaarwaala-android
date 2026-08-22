package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeePaymentItem
import com.srijeesolution.rojgaarwaala.databinding.ItemEmployeePaymentBinding
import com.srijeesolution.rojgaarwaala.utils.WageFormatter

class EmployeePaymentsAdapter(
    private var items: List<EmployeePaymentItem> = emptyList(),
    private val onViewProof: (EmployeePaymentItem) -> Unit,
) : RecyclerView.Adapter<EmployeePaymentsAdapter.PaymentViewHolder>() {

    fun submitList(newItems: List<EmployeePaymentItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class PaymentViewHolder(
        private val binding: ItemEmployeePaymentBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EmployeePaymentItem) {
            binding.paymentTypeText.text = item.paymentTypeLabel
                ?: item.paymentType?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }
                ?: "Payment"
            binding.paymentAmountText.text = WageFormatter.format(item.amount)
            val date = item.dateLabel?.takeIf { it.isNotBlank() }
                ?: item.paymentDate?.takeIf { it.isNotBlank() }
            binding.paymentDateText.text = buildString {
                date?.let { append(it) }
                item.factoryName?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append("  •  ")
                    append(it)
                }
            }

            binding.paymentMethodText.text = buildString {
                item.paymentMethod?.takeIf { it.isNotBlank() }?.let { append(it) }
                item.transactionReference?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append("  •  ")
                    append("Ref $it")
                }
            }
            binding.paymentMethodText.visibility =
                if (binding.paymentMethodText.text.isNullOrBlank()) View.GONE else View.VISIBLE

            val remarks = item.remarks.orEmpty()
            binding.paymentRemarksText.text = remarks
            binding.paymentRemarksText.visibility =
                if (remarks.isBlank()) View.GONE else View.VISIBLE

            val hasProof = !item.proofList.isNullOrEmpty()
            binding.viewProofButton.visibility = if (hasProof) View.VISIBLE else View.GONE
            binding.viewProofButton.setOnClickListener { onViewProof(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemEmployeePaymentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
