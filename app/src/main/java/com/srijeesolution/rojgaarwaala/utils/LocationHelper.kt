package com.srijeesolution.rojgaarwaala.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Single-shot "give me a fresh, accurate fix" helper for the attendance punch flow.
 *
 * Must be constructed as an activity field (before `onStart`) because it registers
 * activity-result launchers for the permission prompt and the location-settings
 * resolution dialog.
 */
class LocationHelper(private val activity: ComponentActivity) {

    enum class Failure {
        PERMISSION_DENIED,
        PERMISSION_PERMANENTLY_DENIED,
        GPS_DISABLED,
        TIMEOUT,
        UNAVAILABLE,
    }

    sealed class Result {
        data class Success(
            val latitude: Double,
            val longitude: Double,
            val accuracy: Double,
        ) : Result()

        data class Error(val failure: Failure, val message: String) : Result()
    }

    private var pendingCallback: ((Result) -> Unit)? = null
    private var cancellationTokenSource: CancellationTokenSource? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(activity)
    }

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        when {
            granted -> ensureLocationEnabled()
            // Once a denial has been recorded, "no rationale" means the user picked
            // "Don't ask again" (or the OS is blocking the prompt entirely).
            shouldShowRationale() -> deliver(
                Result.Error(Failure.PERMISSION_DENIED, MESSAGE_PERMISSION),
            )
            else -> deliver(
                Result.Error(Failure.PERMISSION_PERMANENTLY_DENIED, MESSAGE_PERMISSION_SETTINGS),
            )
        }
    }

    private val locationSettingsLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchFreshLocation()
        } else {
            deliver(Result.Error(Failure.GPS_DISABLED, MESSAGE_GPS))
        }
    }

    /**
     * Runs permission check -> location-services check -> fresh high accuracy fix.
     * [onResult] is always invoked exactly once, on the main thread.
     */
    fun requestCurrentLocation(onResult: (Result) -> Unit) {
        if (pendingCallback != null) return
        pendingCallback = onResult
        if (hasLocationPermission()) {
            ensureLocationEnabled()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean {
        val manager = activity.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun openAppSettings() {
        try {
            activity.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.packageName, null),
                ),
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to open app settings", e)
        }
    }

    fun openLocationSettings() {
        try {
            activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } catch (e: Exception) {
            Log.w(TAG, "Unable to open location settings", e)
        }
    }

    /** Cancels an in-flight request without invoking the callback (use from `onDestroy`). */
    fun cancel() {
        clearTimeout()
        cancellationTokenSource?.cancel()
        cancellationTokenSource = null
        pendingCallback = null
    }

    private fun shouldShowRationale(): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

    private fun ensureLocationEnabled() {
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_TIMEOUT_MS).build(),
            )
            .setAlwaysShow(true)
            .build()

        LocationServices.getSettingsClient(activity)
            .checkLocationSettings(settingsRequest)
            .addOnSuccessListener { fetchFreshLocation() }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        locationSettingsLauncher.launch(
                            IntentSenderRequest.Builder(exception.resolution).build(),
                        )
                        return@addOnFailureListener
                    } catch (e: Exception) {
                        Log.w(TAG, "Location settings resolution failed", e)
                    }
                }
                // No in-app resolution available: fall back to the raw provider state.
                if (isLocationEnabled()) {
                    fetchFreshLocation()
                } else {
                    deliver(Result.Error(Failure.GPS_DISABLED, MESSAGE_GPS))
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun fetchFreshLocation() {
        if (!hasLocationPermission()) {
            deliver(Result.Error(Failure.PERMISSION_DENIED, MESSAGE_PERMISSION))
            return
        }
        if (!isLocationEnabled()) {
            deliver(Result.Error(Failure.GPS_DISABLED, MESSAGE_GPS))
            return
        }

        val tokenSource = CancellationTokenSource()
        cancellationTokenSource = tokenSource
        startTimeout()

        // maxUpdateAge 0 forces a new fix instead of replaying a stale cached one,
        // which is what makes the punch location trustworthy.
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .setDurationMillis(LOCATION_TIMEOUT_MS)
            .build()

        fusedLocationClient.getCurrentLocation(request, tokenSource.token)
            .addOnSuccessListener { location ->
                if (location == null) {
                    deliver(Result.Error(Failure.UNAVAILABLE, MESSAGE_UNAVAILABLE))
                } else {
                    deliver(
                        Result.Success(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy.toDouble(),
                        ),
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "getCurrentLocation failed", e)
                deliver(Result.Error(Failure.UNAVAILABLE, MESSAGE_UNAVAILABLE))
            }
    }

    private fun startTimeout() {
        clearTimeout()
        val runnable = Runnable {
            cancellationTokenSource?.cancel()
            cancellationTokenSource = null
            deliver(Result.Error(Failure.TIMEOUT, MESSAGE_TIMEOUT))
        }
        timeoutRunnable = runnable
        mainHandler.postDelayed(runnable, LOCATION_TIMEOUT_MS)
    }

    private fun clearTimeout() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun deliver(result: Result) {
        clearTimeout()
        cancellationTokenSource = null
        val callback = pendingCallback ?: return
        pendingCallback = null
        callback(result)
    }

    companion object {
        private const val TAG = "LocationHelper"
        const val LOCATION_TIMEOUT_MS = 15_000L

        const val MESSAGE_PERMISSION = "Location permission required"
        const val MESSAGE_PERMISSION_SETTINGS =
            "Location permission required. Enable it from app settings to mark attendance."
        const val MESSAGE_GPS = "Please enable GPS"
        const val MESSAGE_TIMEOUT =
            "Unable to get accurate location. Move to an open area and try again."
        const val MESSAGE_UNAVAILABLE = "Unable to get accurate location"
    }
}
