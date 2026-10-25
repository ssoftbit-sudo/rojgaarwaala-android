package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome
import com.srijeesolution.rojgaarwaala.data.repository.DayJobRepository
import com.srijeesolution.rojgaarwaala.databinding.ActivityDayJobMyJobsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The jobs a worker has accepted, newest first. */
@AndroidEntryPoint
class DayJobMyJobsActivity : AppCompatActivity() {

    @Inject lateinit var repository: DayJobRepository

    private lateinit var binding: ActivityDayJobMyJobsBinding
    private val adapter = DayJobAdapter(forCompany = false) { job ->
        reopen.launch(DayJobDetailActivity.intent(this, job.id))
    }
    private val reopen = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { load() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDayJobMyJobsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.myJobsBack.setOnClickListener { finish() }
        binding.myJobsList.layoutManager = LinearLayoutManager(this)
        binding.myJobsList.adapter = adapter
        load()
    }

    private fun load() {
        binding.myJobsLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.myBookings()
            binding.myJobsLoading.visibility = View.GONE
            when (result) {
                is DayJobOutcome.Ok -> {
                    adapter.submit(result.data)
                    binding.myJobsEmpty.visibility = if (result.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobMyJobsActivity, result.message)
            }
        }
    }
}
