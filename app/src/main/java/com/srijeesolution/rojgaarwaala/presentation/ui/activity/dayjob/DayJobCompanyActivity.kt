package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobCompanyRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobMeta
import com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome
import com.srijeesolution.rojgaarwaala.data.repository.DayJobRepository
import com.srijeesolution.rojgaarwaala.databinding.ActivityDayJobCompanyBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.MapPinActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Register (or edit) the company that posts 1 Day Jobs, after accepting the rules. */
@AndroidEntryPoint
class DayJobCompanyActivity : AppCompatActivity() {

    @Inject lateinit var repository: DayJobRepository

    private lateinit var binding: ActivityDayJobCompanyBinding
    private var meta: DayJobMeta? = null
    private var lat: Double? = null
    private var lng: Double? = null

    private val pickAddress = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        lat = data.getDoubleExtra(MapPinActivity.EXTRA_LAT, MapPinActivity.DEFAULT_LAT)
        lng = data.getDoubleExtra(MapPinActivity.EXTRA_LNG, MapPinActivity.DEFAULT_LNG)
        binding.companyAddress.text = data.getStringExtra(MapPinActivity.EXTRA_ADDRESS).orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDayJobCompanyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.companyBack.setOnClickListener { finish() }
        binding.companyAddress.setOnClickListener {
            val intent = Intent(this, MapPinActivity::class.java)
            lat?.let { intent.putExtra(MapPinActivity.EXTRA_LAT, it) }
            lng?.let { intent.putExtra(MapPinActivity.EXTRA_LNG, it) }
            pickAddress.launch(intent)
        }
        binding.companySave.setOnClickListener { save() }
        lifecycleScope.launch {
            when (val result = repository.meta()) {
                is DayJobOutcome.Ok -> fill(result.data)
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobCompanyActivity, result.message)
            }
        }
    }

    private fun fill(meta: DayJobMeta) {
        this.meta = meta
        binding.companyTerms.text = meta.terms.joinToString("\n") { "• $it" }
        val districts = listOf("Choose district") + meta.districts
        binding.companyDistrict.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, districts)
        val company = meta.company ?: return
        binding.companyName.setText(company.name)
        binding.companyOwner.setText(company.ownerName.orEmpty())
        binding.companyPhone.setText(company.phone.orEmpty())
        binding.companyAddress.text = company.address.orEmpty()
        lat = company.latitude
        lng = company.longitude
        binding.companyDistrict.setSelection(districts.indexOf(company.district).coerceAtLeast(0))
        binding.companyAccept.isChecked = true
        binding.companySave.text = "Save"
    }

    private fun save() {
        val name = binding.companyName.text.toString().trim()
        val phone = binding.companyPhone.text.toString().filter { it.isDigit() }
        when {
            name.isEmpty() -> return DayJobUi.toast(this, "Write the company name")
            phone.length != 10 -> return DayJobUi.toast(this, "Enter a 10 digit mobile number")
            !binding.companyAccept.isChecked -> return DayJobUi.toast(this, "Please accept the rules to continue")
        }
        val district = binding.companyDistrict.selectedItemPosition.takeIf { it > 0 }
            ?.let { meta?.districts?.getOrNull(it - 1) }
        val request = DayJobCompanyRequest(
            name = name,
            ownerName = binding.companyOwner.text.toString().trim().ifBlank { null },
            phone = phone,
            address = binding.companyAddress.text.toString().ifBlank { null },
            district = district,
            latitude = lat,
            longitude = lng,
            acceptTerms = true,
        )
        binding.companySave.isEnabled = false
        lifecycleScope.launch {
            val result = repository.saveCompany(request)
            binding.companySave.isEnabled = true
            when (result) {
                is DayJobOutcome.Ok -> {
                    DayJobUi.toast(this@DayJobCompanyActivity, result.message)
                    setResult(RESULT_OK)
                    finish()
                }
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobCompanyActivity, result.message)
            }
        }
    }
}
