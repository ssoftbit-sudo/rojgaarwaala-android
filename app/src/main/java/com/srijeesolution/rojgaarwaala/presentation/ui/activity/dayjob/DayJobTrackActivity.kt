package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTrackData
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTrackWorker
import com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome
import com.srijeesolution.rojgaarwaala.data.repository.DayJobRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The company's live map, like a ride app: the site pin, a dot for each worker on
 * the way, and below it who is how far and how many minutes away. Refreshes itself.
 */
@AndroidEntryPoint
class DayJobTrackActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ID = "day_job_id"
        private const val REFRESH_MS = 10_000L
        fun intent(context: Context, id: Int) = Intent(context, DayJobTrackActivity::class.java).putExtra(EXTRA_ID, id)
    }

    @Inject lateinit var repository: DayJobRepository

    private var map: GoogleMap? = null
    private lateinit var list: LinearLayout
    private var framed = false

    /** Each worker keeps one colour: their person pin on the map and their name in the list. No yellow: that is the status text. */
    private val palette = intArrayOf(
        0xFF4CAF50.toInt(), 0xFF29B6F6.toInt(), 0xFFFF7043.toInt(), 0xFFAB47BC.toInt(), 0xFF5C6BC0.toInt(),
        0xFFEC407A.toInt(), 0xFF26A69A.toInt(), 0xFF8D6E63.toInt(), 0xFF7E57C2.toInt(), 0xFF26C6DA.toInt(),
    )
    private fun colourOf(w: DayJobTrackWorker) = palette[Math.floorMod(w.bookingId, palette.size)]
    private val pins = mutableMapOf<Int, com.google.android.gms.maps.model.BitmapDescriptor>()

    /** A round pin in the worker's colour with a white person inside and a dark ring. */
    private fun personPin(colour: Int) = roundPin(colour, R.drawable.ic_day_job_person, 40)

    /** The work site: a bigger orange pin with a building, so it stands apart from people. */
    private val sitePin by lazy { roundPin(ContextCompat.getColor(this, R.color.brand_color_yellow), R.drawable.ic_day_job_site, 48) }

    private fun roundPin(colour: Int, icon: Int, sizeDp: Int): com.google.android.gms.maps.model.BitmapDescriptor = pins.getOrPut(colour * 31 + icon) {
        val size = dp(sizeDp)
        val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xFF111111.toInt()
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.color = colour
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - dp(3), paint)
        ContextCompat.getDrawable(this, icon)!!.mutate().apply {
            val pad = size / 5
            setBounds(pad, pad, size - pad, size - pad)
            draw(canvas)
        }
        BitmapDescriptorFactory.fromBitmap(bmp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mapId = View.generateViewId()
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.app_background))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), 0, dp(8), 0)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
                addView(ImageButton(context).apply {
                    setImageResource(R.drawable.ic_arrow_back_white)
                    setBackgroundResource(android.R.color.transparent)
                    contentDescription = "पीछे"
                    layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                    setOnClickListener { finish() }
                })
                addView(text("कौन कहाँ पहुँचा", 19f, R.color.white, true).apply { setPadding(dp(4), 0, 0, 0) })
            })
            addView(FrameLayout(context).apply {
                id = mapId
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.2f)
            })
            list = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(16))
            }
            addView(ScrollView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                addView(list)
            })
        })
        val fragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction().replace(mapId, fragment).commitNowAllowingStateLoss()
        fragment.getMapAsync { m ->
            runCatching { m.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark)) }
            m.uiSettings.isMapToolbarEnabled = false
            m.uiSettings.isZoomControlsEnabled = true
            map = m
        }
        lifecycleScope.launch {
            while (isActive) {
                when (val result = repository.tracking(intent.getIntExtra(EXTRA_ID, 0))) {
                    is DayJobOutcome.Ok -> render(result.data)
                    is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobTrackActivity, result.message)
                }
                delay(REFRESH_MS)
            }
        }
    }

    private fun render(data: DayJobTrackData) {
        map?.let { m ->
            m.clear()
            val site = LatLng(data.site.latitude, data.site.longitude)
            m.addMarker(MarkerOptions().position(site).title(data.site.name ?: "काम की जगह")
                .anchor(0.5f, 0.5f)
                .zIndex(1f)
                .icon(sitePin))
            val bounds = LatLngBounds.builder().include(site)
            data.workers.filter { it.latitude != null && it.longitude != null }.forEach { w ->
                val at = LatLng(w.latitude!!, w.longitude!!)
                bounds.include(at)
                // Dashed line in the worker's colour from them to the site.
                m.addPolyline(com.google.android.gms.maps.model.PolylineOptions()
                    .add(at, site)
                    .color(colourOf(w))
                    .width(dp(3).toFloat())
                    .geodesic(true)
                    .pattern(listOf(com.google.android.gms.maps.model.Dash(dp(10).toFloat()), com.google.android.gms.maps.model.Gap(dp(6).toFloat()))))
                m.addMarker(MarkerOptions().position(at)
                    .title(w.name ?: "मज़दूर")
                    .snippet(listOfNotNull(w.distanceKm?.let { "$it km" }, w.etaMinutes?.let { "~$it मिनट" }).joinToString(" · "))
                    .anchor(0.5f, 0.5f)
                    // A worker who stopped sharing is shown faded at their last place.
                    .alpha(if (w.state == "on_way") 1f else 0.55f)
                    .icon(personPin(colourOf(w))))
            }
            // Fit everyone once; after that leave the map where the company moved it.
            if (!framed) {
                framed = true
                val moving = data.workers.any { it.latitude != null }
                runCatching {
                    if (moving) m.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), dp(60)))
                    else m.moveCamera(CameraUpdateFactory.newLatLngZoom(site, 14f))
                }.onFailure { m.moveCamera(CameraUpdateFactory.newLatLngZoom(site, 13f)) }
            }
        }

        list.removeAllViews()
        if (data.workers.isEmpty()) list.addView(text("अभी कोई नहीं आ रहा।", 14f, R.color.search_hint))
        data.workers.sortedBy { order(it) }.forEach { list.addView(row(it)) }
        list.addView(text("मज़दूर जब 'घर से निकल पड़ा' दबाएगा, तभी वह नक्शे पर दिखेगा।", 12f, R.color.search_hint).apply {
            setPadding(0, dp(8), 0, 0)
        })
    }

    private fun order(w: DayJobTrackWorker) = when (w.state) { "on_way" -> 0; "stopped" -> 1; "not_started" -> 2; else -> 3 }

    private fun row(w: DayJobTrackWorker): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bg_day_job_pill)
        setPadding(dp(12), dp(10), dp(8), dp(10))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = dp(8) }
        val (status, color) = when (w.state) {
            "arrived" -> "✓ पहुँच गए" to R.color.day_job_green
            "on_way" -> listOfNotNull("रास्ते में", w.distanceKm?.let { "$it km दूर" }, w.etaMinutes?.let { "लगभग $it मिनट" })
                .joinToString(" · ") to R.color.brand_color_yellow
            "stopped" -> "लोकेशन बंद" + (w.distanceKm?.let { " · आख़िरी बार $it km दूर" } ?: "") to R.color.search_hint
            else -> "अभी निकले नहीं" to R.color.search_hint
        }
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(text(listOfNotNull(w.name ?: "मज़दूर", w.tradeLabel).joinToString(" · "), 15f, R.color.white, true).apply {
                setTextColor(colourOf(w))
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, R.drawable.ic_day_job_person)!!.mutate().apply { setTint(colourOf(w)) }, null, null, null)
                compoundDrawablePadding = dp(6)
            })
            addView(text(status, 13f, color))
            if (w.latitude != null) setOnClickListener {
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(w.latitude, w.longitude!!), 15f))
            }
        })
        addView(text("Call", 14f, R.color.day_job_on_green, true).apply {
            setBackgroundResource(R.drawable.bg_day_job_green_button)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener { DayJobUi.call(context, w.callMobile) }
        })
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(ContextCompat.getColor(context, color))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun dp(v: Int) = DayJobUi.dp(this, v)
}
