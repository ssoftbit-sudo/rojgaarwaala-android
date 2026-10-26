package com.srijeesolution.rojgaarwaala.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome
import com.srijeesolution.rojgaarwaala.data.repository.DayJobRepository
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob.DayJobDetailActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sends the worker's position to the company while they are on the way to a job.
 * Runs only after the worker taps "घर से निकल पड़ा", always with a visible
 * notification, and stops on arrival (the server says so) or when they tap stop.
 * It uses "while using the app" location; it is started from the open screen.
 */
@AndroidEntryPoint
class DayJobTrackingService : Service() {

    companion object {
        private const val CHANNEL = "day_job_tracking"
        private const val NOTIFICATION_ID = 4711
        private const val EXTRA_BOOKING = "booking_id"
        private const val EXTRA_JOB = "job_id"
        private const val ACTION_STOP = "stop"
        private const val PREFS = "day_job"
        private const val KEY_ACTIVE = "tracking_booking_id"
        private const val INTERVAL_MS = 15_000L

        fun start(context: Context, bookingId: Int, jobId: Int) {
            val intent = Intent(context, DayJobTrackingService::class.java)
                .putExtra(EXTRA_BOOKING, bookingId).putExtra(EXTRA_JOB, jobId)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, DayJobTrackingService::class.java).setAction(ACTION_STOP))
        }

        /** The booking being shared now, or 0. */
        fun activeBooking(context: Context): Int =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_ACTIVE, 0)

        private fun setActive(context: Context, bookingId: Int) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_ACTIVE, bookingId).apply()
    }

    @Inject lateinit var repository: DayJobRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var bookingId = 0
    private var jobId = 0

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val fix = result.lastLocation ?: return
            scope.launch {
                when (val sent = repository.sendLocation(bookingId, fix.latitude, fix.longitude)) {
                    is DayJobOutcome.Ok -> if (sent.data?.stop == true) {
                        if (sent.data.arrived) DayJobSpeaker.speak(this@DayJobTrackingService, "आप पहुँच गए।")
                        finish(tellServer = false)
                    }
                    is DayJobOutcome.Fail -> Unit // try again with the next fix
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            finish(tellServer = true)
            return START_NOT_STICKY
        }
        bookingId = intent?.getIntExtra(EXTRA_BOOKING, 0) ?: 0
        jobId = intent?.getIntExtra(EXTRA_JOB, 0) ?: 0
        val allowed = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (bookingId == 0 || !allowed) {
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
        setActive(this, bookingId)
        requestUpdates()
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun requestUpdates() {
        client.removeLocationUpdates(callback)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
            .setMinUpdateDistanceMeters(20f)
            .build()
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun finish(tellServer: Boolean) {
        val id = bookingId.takeIf { it > 0 } ?: activeBooking(this)
        client.removeLocationUpdates(callback)
        setActive(this, 0)
        if (tellServer && id > 0) scope.launch { repository.stopLocation(id) }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        client.removeLocationUpdates(callback)
        scope.cancel()
        super.onDestroy()
    }

    private fun notification() = run {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "काम पर जाते समय लोकेशन", NotificationManager.IMPORTANCE_LOW))
        }
        val open = PendingIntent.getActivity(this, 1, DayJobDetailActivity.intent(this, jobId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 2, Intent(this, DayJobTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("कंपनी आपकी लोकेशन देख रही है")
            .setContentText("काम की जगह पहुँचते ही अपने-आप बंद हो जाएगा।")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "रोकें", stop)
            .build()
    }
}
