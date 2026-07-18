package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.ChhattisgarhDistricts
import com.srijeesolution.rojgaarwaala.utils.HomeLocationDefaults
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationPickerActivity : AppCompatActivity() {

    private val homePageViewModel: HomePageViewModel by viewModels()
    private lateinit var locationAdapter: LocationAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var searchInput: EditText
    private var allDistricts: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        findViewById<View>(R.id.locationBackButton).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        progressBar = findViewById(R.id.locationProgressBar)
        searchInput = findViewById(R.id.locationSearchInput)

        val recycler = findViewById<RecyclerView>(R.id.locationsRecyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        locationAdapter = LocationAdapter(emptyList()) { name ->
            setResult(RESULT_OK, Intent().putExtra(EXTRA_SELECTED_LOCATION, name))
            finish()
        }
        recycler.adapter = locationAdapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                filterDistricts(s?.toString().orEmpty())
            }
        })

        observeCities()
        homePageViewModel.getCityList()
    }

    companion object {
        const val EXTRA_SELECTED_LOCATION = "extra_selected_location"
    }

    private fun withDefaultOption(cities: List<String>): List<String> {
        val rest = cities
            .filter { !HomeLocationDefaults.skipsDistrictFilter(it) }
            .distinct()
        return listOf(HomeLocationDefaults.ALL_CHHATTISGARH) + rest
    }

    private fun filterDistricts(query: String) {
        val filtered = if (query.isBlank()) {
            allDistricts
        } else {
            allDistricts.filter { it.contains(query, ignoreCase = true) }
        }
        locationAdapter.updateItems(filtered)
    }

    private fun observeCities() {
        homePageViewModel.cityListLiveData.observe(this) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    progressBar.visibility = View.GONE
                    val cities = apiResponse.data?.dataObj?.cityList
                        .orEmpty()
                        .mapNotNull { it.name?.trim()?.takeIf { name -> name.isNotEmpty() } }
                        .distinct()
                    allDistricts = withDefaultOption(
                        if (cities.isNotEmpty()) cities else ChhattisgarhDistricts.list
                    )
                    filterDistricts(searchInput.text?.toString().orEmpty())
                }
                is ApiResult.Error -> {
                    progressBar.visibility = View.GONE
                    allDistricts = withDefaultOption(ChhattisgarhDistricts.list)
                    filterDistricts(searchInput.text?.toString().orEmpty())
                    val errorMessage = apiResponse.message?.toString() ?: "Failed to load districts"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private class LocationAdapter(
        private var items: List<String>,
        private val onPick: (String) -> Unit
    ) : RecyclerView.Adapter<LocationAdapter.Holder>() {

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view.findViewById(R.id.locationName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_location_row, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val name = items[position]
            holder.text.text = name
            holder.itemView.setOnClickListener { onPick(name) }
        }

        override fun getItemCount(): Int = items.size

        fun updateItems(newItems: List<String>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
