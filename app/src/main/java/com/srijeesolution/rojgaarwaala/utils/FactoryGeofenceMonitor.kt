package com.srijeesolution.rojgaarwaala.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeFactory

object FactoryGeofenceMonitor {
    const val ACTION_FACTORY_ARRIVAL = "com.srijeesolution.rojgaarwaala.ACTION_FACTORY_ARRIVAL"
    const val EXTRA_FACTORY_NAME = "factory_name"
    private const val GEOFENCE_ID = "factory_arrival"

    fun register(context: Context, factory: EmployeeFactory?) {
        if (factory == null) return
        val lat = factory.latitude ?: return
        val lng = factory.longitude ?: return
        val radius = (factory.geofenceRadius ?: 50).coerceAtLeast(30).toFloat()

        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            if (!backgroundGranted) return
        }

        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(lat, lng, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(context, FactoryArrivalReceiver::class.java).apply {
            action = ACTION_FACTORY_ARRIVAL
            putExtra(EXTRA_FACTORY_NAME, factory.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            7101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        LocationServices.getGeofencingClient(context)
            .addGeofences(request, pendingIntent)
    }
}
