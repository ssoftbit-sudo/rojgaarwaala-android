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

    private val pickSite = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        lat = data.getDoubleExtra(MapPinActivity.EXTRA_LAT, MapPinActivity.DEFAULT_LAT)
        lng = data.getDoubleExtra(MapPinActivity.EXTRA_LNG, MapPinActivity.DEFAULT_LNG)
        siteName = data.getStringExtra(MapPinActivity.EXTRA_ADDRESS).orEmpty()
        binding.postSite.text = siteName.ifBlank { "Pinned on map" }
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
        binding.postHours.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (1..12).map { "$it hours" })
        binding.postHours.setSelection(7)
        setupDates()
        renderPhotos()

        lifecycleScope.launch {
            when (val result = repository.meta()) {
                is DayJobOutcome.Ok -> {
                    trades = result.data.trades
                    result.data.company?.let { c ->
                        if (lat == null && c.latitude != null && c.longitude != null) {
                            lat = c.latitude; lng = c.longitude; siteName = c.address.orEmpty()
                            binding.postSite.text = siteName.ifBlank { "Pinned on map" }
                        }
                    }
                    renderSlots()
                }
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobPostActivity, result.message)
            }
        }
    }

    private fun setupDates() {
        val group = binding.postDateChips
        val today = LocalDate.now()
        listOf("Today" to today, "Tomorrow" to today.plusDays(1)).forEach { (label, date) ->
            group.addView(DayJobUi.chip(this, label, false).apply {
                setOnClickListener { workDate = date }
            })
        }
        group.addView(DayJobUi.chip(this, "Pick date", false).apply {
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
                text = "+\nPhoto"
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
                .setTitle("Fill these first")
                .setMessage(errors.joinToString("\n") { "• $it" })
                .setPositiveButton("OK", null)
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
            "Workers: $who",
            "Wage: ${DayJobFormat.wageText(draft.wagePerDay)}",
            "When: ${DayJobFormat.whenText(draft.workDate, draft.startTime)} · ${draft.hours} hours",
            "Where: ${draft.areaName.ifBlank { "Pinned on map" }}",
            "Photos: ${draft.photos.size}",
        ).joinToString("\n")
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle("Publish this job?")
            .setMessage(summary + "\n\nWorkers within 5 km will get a notification.")
            .setPositiveButton("Publish Job") { _, _ -> publish(draft) }
            .setNegativeButton("Edit", null)
            .show()
    }

    private fun publish(draft: DayJobDraft) {
        binding.postSubmit.isEnabled = false
        binding.postSubmit.text = "Publishing…"
        lifecycleScope.launch {
            val result = repository.create(draft)
            binding.postSubmit.isEnabled = true
            binding.postSubmit.text = "Preview"
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
