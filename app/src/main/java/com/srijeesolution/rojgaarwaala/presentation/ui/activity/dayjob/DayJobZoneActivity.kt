package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobZoneAskRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobZonesData
import com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome
import com.srijeesolution.rojgaarwaala.data.repository.DayJobRepository
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.MapPinActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * The worker's labour zones: the chowks they come to. Work near a chosen zone reaches
 * them even when they live far away. Only admin makes zones; a worker whose chowk is
 * missing asks for it with its place on the map.
 */
@AndroidEntryPoint
class DayJobZoneActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_LAT = "lat"
        private const val EXTRA_LNG = "lng"
        private const val EXTRA_MINE = "mine"

        fun intent(context: Context, lat: Double?, lng: Double?, mine: List<Int>) =
            Intent(context, DayJobZoneActivity::class.java).apply {
                lat?.let { putExtra(EXTRA_LAT, it) }
                lng?.let { putExtra(EXTRA_LNG, it) }
                putExtra(EXTRA_MINE, mine.toIntArray())
            }
    }

    @Inject lateinit var repository: DayJobRepository

    private lateinit var content: LinearLayout
    private lateinit var loading: ProgressBar
    private val chosen = linkedSetOf<Int>()
    private var maxZones = 2

    /** The worker put the pin on their chowk; now ask for its name. */
    private val pickPlace = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        askDetails(
            data.getDoubleExtra(MapPinActivity.EXTRA_LAT, MapPinActivity.DEFAULT_LAT),
            data.getDoubleExtra(MapPinActivity.EXTRA_LNG, MapPinActivity.DEFAULT_LNG),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chosen.addAll(intent.getIntArrayExtra(EXTRA_MINE)?.toList().orEmpty())
        setContentView(buildScreen())
        load()
    }

    private fun lat() = if (intent.hasExtra(EXTRA_LAT)) intent.getDoubleExtra(EXTRA_LAT, 0.0) else null
    private fun lng() = if (intent.hasExtra(EXTRA_LNG)) intent.getDoubleExtra(EXTRA_LNG, 0.0) else null

    private fun buildScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.app_background))
        }
        root.addView(LinearLayout(this).apply {
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
            addView(text("मेरा ज़ोन (चौक)", 19f, R.color.white, bold = true).apply { setPadding(dp(4), 0, 0, 0) })
        })
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(24))
        }
        frame.addView(ScrollView(this).apply { addView(content) })
        loading = ProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER)
            visibility = View.GONE
        }
        frame.addView(loading)
        root.addView(frame)
        return root
    }

    private fun load() {
        loading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.zones(lat(), lng())
            loading.visibility = View.GONE
            when (result) {
                is DayJobOutcome.Ok -> render(result.data)
                is DayJobOutcome.Fail -> { DayJobUi.toast(this@DayJobZoneActivity, result.message); finish() }
            }
        }
    }

    private fun render(data: DayJobZonesData) {
        maxZones = data.maxZones
        content.removeAllViews()
        content.addView(note("आप जिस चौक पर काम के लिए आते हैं, वह चुनें (ज़्यादा से ज़्यादा $maxZones)। घर दूर हो तब भी उस चौक के पास के काम की ख़बर आपको मिलेगी।"))
        if (data.zones.isEmpty()) content.addView(note("अभी कोई ज़ोन नहीं बना है। नीचे से अपना चौक माँगें।"))
        data.zones.forEach { zone ->
            val on = zone.id in chosen
            content.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(if (on) R.drawable.bg_day_job_highlight else R.drawable.bg_day_job_card)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = dp(10) }
                addView(text(if (on) "✅" else "⬜", 20f, R.color.white).apply { setPadding(0, 0, dp(12), 0) })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(text(zone.name, 17f, R.color.white, bold = true))
                    val where = listOfNotNull(zone.district, zone.distanceKm?.let { String.format(Locale.ENGLISH, "आपसे %.1f km", it) })
                    if (where.isNotEmpty()) addView(text(where.joinToString(" · "), 13f, R.color.search_hint))
                })
                setOnClickListener {
                    when {
                        on -> chosen.remove(zone.id)
                        chosen.size >= maxZones -> { DayJobUi.toast(context, "ज़्यादा से ज़्यादा $maxZones ज़ोन चुन सकते हैं। पहले एक हटाएँ।"); return@setOnClickListener }
                        else -> chosen.add(zone.id)
                    }
                    render(data)
                }
            })
        }
        if (data.zones.isNotEmpty()) content.addView(button("✅  सेव करें", 2) { save() })

        content.addView(text("आपका चौक लिस्ट में नहीं है?", 17f, R.color.white, bold = true).apply { setPadding(0, dp(18), 0, dp(4)) })
        content.addView(note("नक्शे पर चौक की जगह लगाइए और नाम लिखिए। जाँच के बाद ज़ोन बनेगा और आपको ख़बर मिलेगी।"))
        content.addView(button("📍  नया ज़ोन माँगें", 1) {
            val intent = Intent(this, MapPinActivity::class.java)
            lat()?.let { intent.putExtra(MapPinActivity.EXTRA_LAT, it) }
            lng()?.let { intent.putExtra(MapPinActivity.EXTRA_LNG, it) }
            pickPlace.launch(intent)
        })
        data.requests.forEach { ask ->
            val state = when (ask.status) {
                "approved" -> "✅ बन गया"
                "rejected" -> "❌ नहीं माना गया" + (ask.adminNote?.let { ": $it" } ?: "")
                else -> "⏳ जाँच में"
            }
            content.addView(text("${ask.name} — $state", 14f, R.color.search_hint).apply { setPadding(0, dp(4), 0, dp(4)) })
        }
    }

    private fun save() {
        loading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repository.saveZones(chosen.toList())
            loading.visibility = View.GONE
            DayJobUi.toast(this@DayJobZoneActivity, result.messageText())
            if (result is DayJobOutcome.Ok) { setResult(RESULT_OK); finish() }
        }
    }

    private fun askDetails(lat: Double, lng: Double) {
        fun field(hint: String) = EditText(this).apply {
            this.hint = hint
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setHintTextColor(ContextCompat.getColor(context, R.color.search_hint))
        }
        val name = field("चौक / जगह का नाम (ज़रूरी)")
        val landmark = field("पास की पहचान, जैसे: बस स्टैंड")
        val note = field("और कुछ बताना हो (जैसे: रोज़ कितने लोग आते हैं)")
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(name); addView(landmark); addView(note)
        }
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle("नया ज़ोन माँगें")
            .setView(box)
            .setPositiveButton("भेजें") { _, _ ->
                val n = name.text.toString().trim()
                if (n.isEmpty()) { DayJobUi.toast(this, "चौक या जगह का नाम लिखें।"); return@setPositiveButton }
                loading.visibility = View.VISIBLE
                lifecycleScope.launch {
                    val result = repository.askZone(DayJobZoneAskRequest(
                        name = n,
                        landmark = landmark.text.toString().trim().ifEmpty { null },
                        district = null,
                        latitude = lat,
                        longitude = lng,
                        note = note.text.toString().trim().ifEmpty { null },
                    ))
                    loading.visibility = View.GONE
                    DayJobUi.toast(this@DayJobZoneActivity, result.messageText())
                    if (result is DayJobOutcome.Ok) load()
                }
            }
            .setNegativeButton("रहने दें", null)
            .show()
    }

    private fun DayJobOutcome<*>.messageText(): String = when (this) {
        is DayJobOutcome.Ok -> message ?: "ठीक है"
        is DayJobOutcome.Fail -> message
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(ContextCompat.getColor(context, color))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun note(value: String) = text(value, 14f, R.color.search_hint).apply { setPadding(0, 0, 0, dp(12)) }

    private fun button(label: String, style: Int, onClick: () -> Unit) = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 16f
        setTypeface(typeface, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)).apply { bottomMargin = dp(10) }
        when (style) {
            1 -> { setBackgroundResource(R.drawable.bg_free_job_map_button); setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow)) }
            else -> { setBackgroundResource(R.drawable.bg_day_job_green_button); setTextColor(ContextCompat.getColor(context, R.color.day_job_on_green)) }
        }
        setOnClickListener { onClick() }
    }

    private fun dp(v: Int) = DayJobUi.dp(this, v)
}
