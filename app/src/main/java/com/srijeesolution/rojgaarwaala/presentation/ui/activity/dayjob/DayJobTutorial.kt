package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.utils.DayJobSpeaker

/**
 * "कैसे काम करता है": a few swipe slides that walk a worker or a company through
 * 1 Day Job from start to finish, each read aloud. Shows once per side, and again
 * from the ❓ button.
 */
object DayJobTutorial {

    private const val PREFS = "day_job"

    private class Slide(val icon: String, val title: String, val body: String)

    private val workerSlides = listOf(
        Slide("🧰", "अपना काम चुनें", "हेल्पर, मिस्त्री जैसा अपना काम चुनें। गाँव दूर है तो 'अपना चौक' चुनें, वहाँ के पास का काम भी आपको मिलेगा।"),
        Slide("🔔", "नया काम आएगा", "पास में काम आते ही फ़ोन बोलकर बताएगा। बड़ा कार्ड खुलेगा, स्वाइप करके सारे काम देखें। ✅ हाँ या ❌ ना दबाएँ।"),
        Slide("✅", "4 मिनट में Book करें", "हाँ दबाने पर 4 मिनट तक जगह आपके लिए रुकती है। काम, पैसा, समय देखकर 'अभी Book करें' दबाएँ।"),
        Slide("🛵", "घर से निकल पड़ा", "काम के दिन निकलते समय 'घर से निकल पड़ा' दबाएँ। कंपनी देख पाएगी कि आप आ रहे हैं।"),
        Slide("🔢", "कोड बताकर हाज़िरी", "काम की जगह पहुँचकर अपने फ़ोन पर दिख रहा 4 अंक का कोड सुपरवाइज़र को बताएँ। वह डालेगा तो हाज़िरी लगेगी।"),
        Slide("⭐", "Job Done और स्टार", "काम ख़त्म होने पर Job Done दबाएँ, पैसा मिला या नहीं बताएँ और कंपनी को स्टार दें। अच्छी रेटिंग से काम पहले मिलता है।"),
    )

    private val companySlides = listOf(
        Slide("📝", "काम डालें", "'मज़दूर चाहिए' में काम, कितने लोग, मज़दूरी, समय और जगह डालें। माइक दबाकर बोलकर भी भर सकते हैं।"),
        Slide("📣", "मज़दूरों को ख़बर", "पास के मज़दूरों और टीमों को अपने-आप ख़बर जाती है। जो हाँ करके Book करते हैं, उनके नाम आपको दिखते हैं।"),
        Slide("🗺️", "कौन कहाँ पहुँचा", "काम के दिन 'कौन कहाँ पहुँचा' में नक्शे पर देखें कि कौन रास्ते में है। ज़रूरत हो तो सीधे Call करें।"),
        Slide("🔢", "कोड से काम शुरू", "मज़दूर से उसके फ़ोन का 4 अंक का कोड पूछें और उसके नाम पर 'काम शुरू' दबाकर डालें। हाज़िरी लग जाएगी।"),
        Slide("⏱️", "ज़्यादा देर का काम (OT)", "काम बचा है तो OT माँगें। ऐप पैसे गिनकर बताता है, आप बदल भी सकते हैं।"),
        Slide("⭐", "Job Done और स्टार", "काम के बाद हर मज़दूर पर Job Done दबाएँ और स्टार दें। अच्छे मज़दूर को अगली बार 'फिर बुलाएँ'।"),
    )

    /** Shows the tutorial for [forCompany] the first time only. */
    fun showOnce(context: Context, forCompany: Boolean): Dialog? {
        val key = if (forCompany) "tutorial_company" else "tutorial_worker"
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(key, false)) return null
        prefs.edit().putBoolean(key, true).apply()
        return show(context, forCompany)
    }

    fun show(context: Context, forCompany: Boolean): Dialog {
        val slides = if (forCompany) companySlides else workerSlides
        val dp = { v: Int -> DayJobUi.dp(context, v) }
        val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xCC000000.toInt())
            setPadding(0, dp(32), 0, dp(32))
        }
        root.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(16), dp(10))
            addView(text(context, if (forCompany) "❓ मज़दूर कैसे बुलाएँ" else "❓ काम कैसे मिलेगा", 20f, R.color.white).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(text(context, "छोड़ें ✕", 16f, R.color.white).apply {
                setBackgroundResource(R.drawable.bg_day_job_flash_close)
                setPadding(dp(16), dp(10), dp(16), dp(10))
                setOnClickListener { dialog.dismiss() }
            })
        })

        val pager = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val dots = text(context, "", 18f, R.color.brand_color_yellow).apply { gravity = Gravity.CENTER; setPadding(0, dp(12), 0, 0) }
        val next = text(context, "", 19f, R.color.day_job_on_green).apply {
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_day_job_green_button)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply { setMargins(dp(32), dp(14), dp(32), 0) }
        }
        var at = 0
        fun go(to: Int) {
            at = to
            dots.text = slides.indices.joinToString("  ") { if (it == to) "●" else "○" }
            next.text = if (to == slides.lastIndex) "✅  समझ गया, शुरू करें" else "आगे  ›"
            DayJobSpeaker.speak(context, "${slides[to].title}। ${slides[to].body}", always = true)
        }
        pager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = slides.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object : RecyclerView.ViewHolder(LinearLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(parent.width.takeIf { it > 0 } ?: ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(dp(16), 0, dp(16), 0)
            }) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val frame = holder.itemView as LinearLayout
                frame.removeAllViews()
                val slide = slides[position]
                frame.addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setBackgroundResource(R.drawable.bg_day_job_flash_card)
                    setPadding(dp(22), dp(26), dp(22), dp(26))
                    addView(text(context, "${position + 1} / ${slides.size}", 14f, R.color.search_hint))
                    addView(TextView(context).apply { text = slide.icon; textSize = 64f; gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(6)) })
                    addView(text(context, slide.title, 24f, R.color.brand_color_yellow).apply { gravity = Gravity.CENTER })
                    addView(text(context, slide.body, 18f, R.color.white, bold = false).apply {
                        gravity = Gravity.CENTER
                        setLineSpacing(dp(4).toFloat(), 1f)
                        setPadding(0, dp(12), 0, dp(14))
                    })
                    addView(text(context, "🔊  फिर सुनें", 16f, R.color.brand_color_yellow).apply {
                        setBackgroundResource(R.drawable.bg_free_job_map_button)
                        setPadding(dp(18), dp(10), dp(18), dp(10))
                        setOnClickListener { DayJobSpeaker.speak(context, "${slide.title}। ${slide.body}", always = true) }
                    })
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }
        PagerSnapHelper().attachToRecyclerView(pager)
        pager.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, state: Int) {
                if (state != RecyclerView.SCROLL_STATE_IDLE) return
                val shown = (rv.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                if (shown >= 0 && shown != at) go(shown)
            }
        })
        next.setOnClickListener { if (at == slides.lastIndex) dialog.dismiss() else pager.smoothScrollToPosition(at + 1) }
        root.addView(pager)
        root.addView(dots)
        root.addView(next)
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        go(0)
        return dialog
    }

    private fun text(context: Context, value: String, size: Float, color: Int, bold: Boolean = true) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(ContextCompat.getColor(context, color))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
}
