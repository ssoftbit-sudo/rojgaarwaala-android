package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTrade
import com.srijeesolution.rojgaarwaala.data.repository.DayJobDraft
import com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome
import com.srijeesolution.rojgaarwaala.data.repository.DayJobRepository
import com.srijeesolution.rojgaarwaala.databinding.ActivityDayJobPostBinding
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.MapPinActivity
import com.srijeesolution.rojgaarwaala.utils.DayJobFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

/** The company's job form: who, how much, when, where, photos. Then a preview and Publish. */
@AndroidEntryPoint
class DayJobPostActivity : AppCompatActivity() {

    @Inject lateinit var repository: DayJobRepository

    private lateinit var binding: ActivityDayJobPostBinding
    private val counts = linkedMapOf<String, Int>()
    private var trades: List<DayJobTrade> = emptyList()
    private var workDate: LocalDate? = null
    private var startTime: String? = null
    private var lat: Double? = null
    private var lng: Double? = null
    private var siteName = ""
    private val photos = mutableListOf<File>()
    private var district: String? = null
    private var hintFor: String? = null

    private val pickSite = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        lat = data.getDoubleExtra(MapPinActivity.EXTRA_LAT, MapPinActivity.DEFAULT_LAT)
        lng = data.getDoubleExtra(MapPinActivity.EXTRA_LNG, MapPinActivity.DEFAULT_LNG)
        siteName = data.getStringExtra(MapPinActivity.EXTRA_ADDRESS).orEmpty()
        binding.postSite.text = siteName.ifBlank { "नक्शे पर लगा दी" }
    }

    /** The phone's own speech screen in Hindi; no microphone permission needed here. */
    private val listen = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode != RESULT_OK || spoken.isNullOrBlank()) return@registerForActivityResult
        fillFromVoice(spoken)
    }

    private val pickPhotos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        lifecycleScope.launch {
            val added = withContext(Dispatchers.IO) {
                uris.take(4 - photos.size).mapNotNull { shrink(it) }
            }
            photos.addAll(added)
            renderPhotos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDayJobPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.postBack.setOnClickListener { finish() }
        binding.postSite.setOnClickListener {
            val intent = Intent(this, MapPinActivity::class.java)
            lat?.let { intent.putExtra(MapPinActivity.EXTRA_LAT, it) }
            lng?.let { intent.putExtra(MapPinActivity.EXTRA_LNG, it) }
            pickSite.launch(intent)
        }
        binding.postTime.setOnClickListener { pickTime() }
        binding.postSubmit.setOnClickListener { preview() }
        binding.postHours.adapter = ArrayAdapter(this, R.layout.item_day_job_spinner, (1..12).map { "$it घंटे" })
        binding.postHours.setSelection(7)
        setupDates()
        renderPhotos()

        lifecycleScope.launch {
            when (val result = repository.meta()) {
                is DayJobOutcome.Ok -> {
                    if (result.data.voiceAvailable) addMicButton()
                    trades = result.data.trades
                    district = result.data.company?.district
                    result.data.company?.let { c ->
                        if (lat == null && c.latitude != null && c.longitude != null) {
                            lat = c.latitude; lng = c.longitude; siteName = c.address.orEmpty()
                            binding.postSite.text = siteName.ifBlank { "नक्शे पर लगा दी" }
                        }
                    }
                    renderSlots()
                }
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobPostActivity, result.message)
            }
        }
    }

    private fun addMicButton() {
        val form = binding.postTitle.parent as? LinearLayout ?: return
        form.addView(TextView(this).apply {
            text = "🎤  बोलकर काम डालें"
            textSize = 17f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.day_job_on_green))
            setBackgroundResource(R.drawable.bg_day_job_green_button)
            setPadding(0, DayJobUi.dp(context, 14), 0, DayJobUi.dp(context, 14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = DayJobUi.dp(context, 8); bottomMargin = DayJobUi.dp(context, 4) }
            contentDescription = "बोलकर काम डालें"
            setOnClickListener { startListening() }
        }, 0)
        form.addView(TextView(this).apply {
            text = "जैसे: \"कल सुबह 8 बजे 5 हेल्पर चाहिए, 600 रुपये, गोल चौक\""
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.search_hint))
            setPadding(0, 0, 0, DayJobUi.dp(context, 8))
        }, 1)
    }

    private fun startListening() {
        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "काम, कितने लोग, मज़दूरी, कब और कहाँ — बोलें")
        runCatching { listen.launch(intent) }.onFailure { DayJobUi.toast(this, "इस फ़ोन में बोलकर लिखने की सुविधा नहीं है।") }
    }

    /** Sends what was said to the server and fills the form; the company checks and posts. */
    private fun fillFromVoice(spoken: String) {
        DayJobUi.toast(this, "सुना: $spoken")
        lifecycleScope.launch {
            when (val result = repository.voice(spoken)) {
                is DayJobOutcome.Ok -> result.data?.let { d ->
                    d.title?.let { binding.postTitle.setText(it) }
                    if (d.slots.isNotEmpty()) {
                        counts.clear()
                        d.slots.forEach { counts[it.trade] = it.count }
                        renderSlots()
                    }
                    d.wagePerDay?.let { binding.postWage.setText(it.toString()) }
                    d.hours?.let { binding.postHours.setSelection((it - 1).coerceIn(0, 11)) }
                    d.startTime?.let { t ->
                        startTime = t
                        binding.postTime.text = DayJobFormat.whenText(LocalDate.now().toString(), t).substringAfter(", ")
                    }
                    d.workDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.let { date ->
                        workDate = date
                        val group = binding.postDateChips
                        val index = when (date) { LocalDate.now() -> 0; LocalDate.now().plusDays(1) -> 1; else -> 2 }
                        (group.getChildAt(index) as? com.google.android.material.chip.Chip)?.let { chip ->
                            if (index == 2) chip.text = DayJobFormat.whenText(date.toString(), "", LocalDate.now()).substringBefore(",")
                            chip.isChecked = true
                        }
                    }
                    d.description?.let { binding.postDescription.setText(it) }
                    d.areaName?.let { if (siteName.isBlank()) binding.postSite.text = "$it — नक्शे पर जगह लगाएँ" }
                    DayJobUi.toast(this@DayJobPostActivity, result.message)
                }
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobPostActivity, result.message)
            }
        }
    }

    private fun setupDates() {
        val group = binding.postDateChips
        val today = LocalDate.now()
        listOf("आज" to today, "कल" to today.plusDays(1)).forEach { (label, date) ->
            group.addView(DayJobUi.chip(this, label, false).apply {
                setOnClickListener { workDate = date }
            })
        }
        group.addView(DayJobUi.chip(this, "तारीख़ चुनें", false).apply {
            setOnClickListener {
                val base = workDate ?: today.plusDays(2)
                DatePickerDialog(this@DayJobPostActivity, { _, y, m, d ->
                    workDate = LocalDate.of(y, m + 1, d)
                    text = DayJobFormat.whenText(workDate.toString(), "", LocalDate.now()).substringBefore(",")
                    isChecked = true
                }, base.year, base.monthValue - 1, base.dayOfMonth).apply {
                    datePicker.minDate = System.currentTimeMillis() - 1000
                    datePicker.maxDate = System.currentTimeMillis() + 14L * 24 * 3600 * 1000
                }.show()
            }
        })
    }

    private fun pickTime() {
        val parts = startTime?.split(":")?.mapNotNull { it.toIntOrNull() }
        TimePickerDialog(this, { _, h, m ->
            startTime = "%02d:%02d".format(h, m)
            binding.postTime.text = DayJobFormat.whenText(LocalDate.now().toString(), startTime!!).substringAfter(", ")
        }, parts?.getOrNull(0) ?: 8, parts?.getOrNull(1) ?: 0, false).show()
    }

    /** One row per trade with − count +, so a company can ask for 5 masons and 10 helpers. */
    private fun renderSlots() {
        val box = binding.postSlots
        box.removeAllViews()
        trades.forEach { trade ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(DayJobUi.dp(context, 10), DayJobUi.dp(context, 4), DayJobUi.dp(context, 4), DayJobUi.dp(context, 4))
            }
            val label = TextView(this).apply {
                text = trade.label
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val count = TextView(this).apply {
                textSize = 18f
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                minWidth = DayJobUi.dp(context, 36)
            }
            fun show() {
                val n = counts[trade.key] ?: 0
                count.text = n.toString()
                val on = n > 0
                val color = ContextCompat.getColor(this, if (on) R.color.brand_color_yellow else R.color.search_hint)
                count.setTextColor(color)
                label.setTextColor(ContextCompat.getColor(this, if (on) R.color.white else R.color.search_hint))
            }
            fun stepper(sign: String, delta: Int) = TextView(this).apply {
                text = sign
                textSize = 22f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow))
                setBackgroundResource(R.drawable.bg_day_job_pill)
                layoutParams = LinearLayout.LayoutParams(DayJobUi.dp(context, 42), DayJobUi.dp(context, 38))
                setOnClickListener {
                    counts[trade.key] = ((counts[trade.key] ?: 0) + delta).coerceIn(0, 200)
                    show()
                    loadWageHint()
                }
            }
            row.addView(label)
            row.addView(stepper("−", -1))
            row.addView(count)
            row.addView(stepper("+", 1))
            show()
            box.addView(row)
        }
    }

    /** "Helper usually gets ₹600–₹650 a day in Raipur" for the first trade asked for. */
    private fun loadWageHint() {
        val trade = counts.entries.firstOrNull { it.value > 0 }?.key
        if (trade == hintFor) return
        hintFor = trade
        if (trade == null) {
            binding.postWageHint.visibility = android.view.View.GONE
            return
        }
        lifecycleScope.launch {
            val hint = (repository.wageHint(trade, district) as? DayJobOutcome.Ok)?.data
            if (hintFor != trade) return@launch
            binding.postWageHint.visibility = if (hint == null) android.view.View.GONE else android.view.View.VISIBLE
            binding.postWageHint.text = hint?.let { DayJobFormat.wageHintText(it.label, it.low, it.high, it.district) }
        }
    }

    private fun renderPhotos() {
        val box = binding.postPhotos
        box.removeAllViews()
        val size = DayJobUi.dp(this, 68)
        photos.forEachIndexed { index, file ->
            box.addView(FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = DayJobUi.dp(context, 8) }
                addView(ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                    clipToOutline = true
                    setBackgroundResource(R.drawable.bg_day_job_card)
                })
                setOnClickListener {
                    photos.removeAt(index)
                    renderPhotos()
                }
            })
        }
        if (photos.size < 4) {
            box.addView(TextView(this).apply {
                text = "+\nफ़ोटो"
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow))
                setBackgroundResource(R.drawable.bg_free_job_map_button)
                layoutParams = LinearLayout.LayoutParams(size, size)
                setOnClickListener { pickPhotos.launch("image/*") }
            })
        }
    }

    /** Large phone photos are cut down to about 1280 px so the upload stays small. */
    private fun shrink(uri: Uri): File? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1280) sample *= 2
        val bitmap = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        File(cacheDir, "day_job_${System.nanoTime()}.jpg").also { out ->
            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        }
    }.getOrNull()

    private fun draft(): DayJobDraft? {
        val title = binding.postTitle.text.toString().trim()
        val wage = binding.postWage.text.toString().toIntOrNull()
        val slots = counts.filterValues { it > 0 }
        val errors = DayJobFormat.postErrors(title, slots, wage, workDate != null, startTime != null, lat != null)
        if (errors.isNotEmpty()) {
            AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
                .setTitle("पहले यह भरें")
                .setMessage(errors.joinToString("\n") { "• $it" })
                .setPositiveButton("ठीक है", null)
                .show()
            return null
        }
        return DayJobDraft(
            title = title,
            slots = slots,
            wagePerDay = wage!!,
            workDate = workDate.toString(),
            startTime = startTime!!,
            hours = binding.postHours.selectedItemPosition + 1,
            areaName = siteName,
            latitude = lat!!,
            longitude = lng!!,
            description = binding.postDescription.text.toString().trim(),
            photos = photos.toList(),
        )
    }

    private fun preview() {
        val draft = draft() ?: return
        val who = draft.slots.entries.joinToString(", ") { (key, n) -> "$n ${trades.firstOrNull { it.key == key }?.label ?: key}" }
        val summary = listOf(
            draft.title,
            "मज़दूर: $who",
            "मज़दूरी: ${DayJobFormat.wageText(draft.wagePerDay)}",
            "कब: ${DayJobFormat.whenText(draft.workDate, draft.startTime)} · ${draft.hours} घंटे",
            "कहाँ: ${draft.areaName.ifBlank { "नक्शे पर लगा दी" }}",
            "फ़ोटो: ${draft.photos.size}",
        ).joinToString("\n")
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle("यह काम डालें?")
            .setMessage(summary + "\n\n5 km के मज़दूरों को ख़बर जाएगी।")
            .setPositiveButton("काम डालें") { _, _ -> publish(draft) }
            .setNegativeButton("बदलें", null)
            .show()
    }

    private fun publish(draft: DayJobDraft) {
        binding.postSubmit.isEnabled = false
        binding.postSubmit.text = "डाल रहे हैं…"
        lifecycleScope.launch {
            val result = repository.create(draft)
            binding.postSubmit.isEnabled = true
            binding.postSubmit.text = "देखें और डालें"
            when (result) {
                is DayJobOutcome.Ok -> {
                    DayJobUi.toast(this@DayJobPostActivity, result.message)
                    result.data?.let { startActivity(DayJobDetailActivity.intent(this@DayJobPostActivity, it.id)) }
                    setResult(RESULT_OK)
                    finish()
                }
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobPostActivity, result.message)
            }
        }
    }
}
