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
import androidx.core.content.ContextCompat
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
    private lateinit var doneButton: TextView
    private var allDistricts: List<String> = emptyList()
    private val multiSelect = intent.getBooleanExtra(EXTRA_MULTI_SELECT, false)
    private val selectedLocations = linkedSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        intent.getStringArrayListExtra(EXTRA_PRESELECTED_LOCATIONS)
            .orEmpty()
            .filter { it.isNotBlank() }
            .forEach { selectedLocations.add(it) }

        findViewById<View>(R.id.locationBackButton).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        progressBar = findViewById(R.id.locationProgressBar)
        searchInput = findViewById(R.id.locationSearchInput)
        doneButton = findViewById(R.id.locationDoneButton)

        if (multiSelect) {
            findViewById<TextView>(R.id.locationPickerTitle).text =
                getString(R.string.location_picker_multi_title)
            doneButton.visibility = View.VISIBLE
            doneButton.setOnClickListener { finishWithSelectedLocations() }
        }

        val recycler = findViewById<RecyclerView>(R.id.locationsRecyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        locationAdapter = LocationAdapter(
            items = emptyList(),
            selectedItems = selectedLocations,
            multiSelect = multiSelect,
            onPick = { name ->
                if (multiSelect) {
                    if (selectedLocations.contains(name)) {
                        selectedLocations.remove(name)
                    } else {
                        selectedLocations.add(name)
                    }
                    locationAdapter.notifyDataSetChanged()
                } else {
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_SELECTED_LOCATION, name))
                    finish()
                }
            }
        )
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

    private fun finishWithSelectedLocations() {
        if (selectedLocations.isEmpty()) {
            Toast.makeText(this, getString(R.string.field_is_required, "At least one location"), Toast.LENGTH_SHORT).show()
            return
        }
        setResult(
            RESULT_OK,
            Intent().putStringArrayListExtra(EXTRA_SELECTED_LOCATIONS, ArrayList(selectedLocations))
        )
        finish()
    }

    companion object {
        const val EXTRA_SELECTED_LOCATION = "extra_selected_location"
        const val EXTRA_MULTI_SELECT = "extra_multi_select"
        const val EXTRA_PRESELECTED_LOCATIONS = "extra_preselected_locations"
        const val EXTRA_SELECTED_LOCATIONS = "extra_selected_locations"
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
        private val selectedItems: Set<String>,
        private val multiSelect: Boolean,
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
            val selected = selectedItems.contains(name)
            holder.text.text = if (multiSelect && selected) "✓ $name" else name
            holder.text.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    if (multiSelect && selected) R.color.accent else R.color.white
                )
            )
            holder.itemView.setOnClickListener { onPick(name) }
        }

        override fun getItemCount(): Int = items.size

        fun updateItems(newItems: List<String>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
