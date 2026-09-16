package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.srijeesolution.rojgaarwaala.BuildConfig
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.databinding.ActivityMapPinBinding
import com.srijeesolution.rojgaarwaala.utils.LocationHelper
import com.srijeesolution.rojgaarwaala.utils.MapAddressMapper
import com.srijeesolution.rojgaarwaala.utils.MapAddressParts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapPinBinding
    private val locationHelper = LocationHelper(this)
    private var googleMap: GoogleMap? = null
    private var pin: Marker? = null
    private var selectedLat = DEFAULT_LAT
    private var selectedLng = DEFAULT_LNG
    private var geocoded: MapAddressParts? = null
    private var locating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra(EXTRA_LAT) && intent.hasExtra(EXTRA_LNG)) {
            selectedLat = intent.getDoubleExtra(EXTRA_LAT, DEFAULT_LAT)
            selectedLng = intent.getDoubleExtra(EXTRA_LNG, DEFAULT_LNG)
        }

        binding.mapPinBackButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.mapPinMyLocationButton.setOnClickListener { moveToCurrentLocation() }
        binding.mapPinConfirmButton.setOnClickListener { confirmPin() }

        setupMap()
        centerOnCurrentLocation()

        if (!BuildConfig.HAS_MAPS_KEY) {
            Toast.makeText(this, getString(R.string.map_pin_unavailable), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        locationHelper.cancel()
        super.onDestroy()
    }

    private fun setupMap() {
        if (!BuildConfig.HAS_MAPS_KEY) {
            binding.mapPinUnavailable.visibility = View.VISIBLE
            return
        }

        val fragment = SupportMapFragment.newInstance(
            GoogleMapOptions()
                .liteMode(false)
                .mapToolbarEnabled(false)
                .zoomControlsEnabled(true)
                .compassEnabled(true),
        )
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapPinContainer, fragment)
            .commit()

        fragment.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isScrollGesturesEnabled = true
            map.uiSettings.isZoomGesturesEnabled = true
            map.uiSettings.isTiltGesturesEnabled = true
            map.uiSettings.isRotateGesturesEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = false
            if (locationHelper.hasLocationPermission()) {
                try {
                    map.isMyLocationEnabled = true
                } catch (_: SecurityException) {
                    // Permission can race with the system prompt.
                }
            }
            val start = LatLng(selectedLat, selectedLng)
            pin = map.addMarker(
                MarkerOptions()
                    .position(start)
                    .draggable(true)
                    .title(getString(R.string.map_pin_marker_title)),
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 16f))
            map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) = Unit
                override fun onMarkerDrag(marker: Marker) = Unit
                override fun onMarkerDragEnd(marker: Marker) {
                    selectedLat = marker.position.latitude
                    selectedLng = marker.position.longitude
                    reverseGeocode(selectedLat, selectedLng)
                }
            })
            map.setOnMapClickListener { latLng ->
                movePin(latLng, animate = true)
                reverseGeocode(latLng.latitude, latLng.longitude)
            }
        }
    }

    private fun moveToCurrentLocation() {
        centerOnCurrentLocation()
    }

    private fun centerOnCurrentLocation() {
        if (locating) return
        locating = true
        binding.mapPinProgress.visibility = View.VISIBLE
        locationHelper.requestCurrentLocation { result ->
            locating = false
            binding.mapPinProgress.visibility = View.GONE
            when (result) {
                is LocationHelper.Result.Success -> {
                    val latLng = LatLng(result.latitude, result.longitude)
                    movePin(latLng, animate = true)
                    reverseGeocode(result.latitude, result.longitude)
                }
                is LocationHelper.Result.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    movePin(LatLng(selectedLat, selectedLng), animate = false)
                    reverseGeocode(selectedLat, selectedLng)
                }
            }
        }
    }

    private fun movePin(latLng: LatLng, animate: Boolean) {
        selectedLat = latLng.latitude
        selectedLng = latLng.longitude
        val map = googleMap
        if (map == null) return
        val marker = pin
        if (marker == null) {
            pin = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .title(getString(R.string.map_pin_marker_title)),
            )
        } else {
            marker.position = latLng
        }
        val update = CameraUpdateFactory.newLatLngZoom(latLng, 17f)
        if (animate) map.animateCamera(update) else map.moveCamera(update)
        if (locationHelper.hasLocationPermission()) {
            try {
                map.isMyLocationEnabled = true
            } catch (_: SecurityException) {
                // Ignore; the pin still moves.
            }
        }
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        binding.mapPinProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val parts = withContext(Dispatchers.IO) { lookupAddress(lat, lng) }
            binding.mapPinProgress.visibility = View.GONE
            geocoded = parts
            binding.mapPinAddressText.text = parts.address.ifBlank {
                getString(R.string.map_pin_coords, lat, lng)
            }
        }
    }

    private fun lookupAddress(lat: Double, lng: Double): MapAddressParts {
        return try {
            val geocoder = Geocoder(this, Locale.forLanguageTag("en-IN"))
            @Suppress("DEPRECATION")
            val match = geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
            MapAddressMapper.fromParts(
                addressLine = match?.getAddressLine(0),
                locality = match?.locality,
                subAdminArea = match?.subAdminArea,
                adminArea = match?.adminArea,
                subLocality = match?.subLocality,
                thoroughfare = match?.thoroughfare,
                featureName = match?.featureName,
                postalCode = match?.postalCode,
            )
        } catch (_: Exception) {
            MapAddressMapper.fromParts(
                addressLine = null,
                locality = null,
                subAdminArea = null,
                adminArea = null,
                subLocality = null,
                thoroughfare = null,
                featureName = null,
                postalCode = null,
            ).copy(address = getString(R.string.map_pin_coords, lat, lng))
        }
    }

    private fun confirmPin() {
        val parts = geocoded
        val address = parts?.address?.ifBlank { null }
            ?: getString(R.string.map_pin_coords, selectedLat, selectedLng)
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(EXTRA_LAT, selectedLat)
                .putExtra(EXTRA_LNG, selectedLng)
                .putExtra(EXTRA_ADDRESS, address),
        )
        finish()
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_ADDRESS = "extra_address"

        const val DEFAULT_LAT = 21.2514
        const val DEFAULT_LNG = 81.6296
    }
}
