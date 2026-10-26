package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamData
import com.srijeesolution.rojgaarwaala.data.remote.model.DayJobTeamMember
import com.srijeesolution.rojgaarwaala.data.repository.DayJobOutcome
import com.srijeesolution.rojgaarwaala.data.repository.DayJobRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The gang leader's team. A leader adds people by mobile number (they say yes in
 * their own app) and sees who is free today; a member sees their team and can
 * leave; anyone else can start a team or answer an invite.
 */
@AndroidEntryPoint
class DayJobTeamActivity : AppCompatActivity() {

    @Inject lateinit var repository: DayJobRepository

    private lateinit var content: LinearLayout
    private lateinit var loading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildScreen())
        load()
    }

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
            addView(text("मेरी टीम", 19f, R.color.white, bold = true).apply { setPadding(dp(4), 0, 0, 0) })
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
            val result = repository.team()
            loading.visibility = View.GONE
            when (result) {
                is DayJobOutcome.Ok -> render(result.data)
                is DayJobOutcome.Fail -> { DayJobUi.toast(this@DayJobTeamActivity, result.message); finish() }
            }
        }
    }

    private fun render(data: DayJobTeamData) {
        content.removeAllViews()
        data.invites.forEach { invite ->
            content.addView(card().apply {
                addView(text("${invite.leaderName ?: "एक लीडर"} आपको अपनी टीम \"${invite.teamName.orEmpty()}\" में बुला रहे हैं।", 15f, R.color.white))
                addView(text("टीम में रहने पर लीडर आपके लिए काम पक्का कर सकते हैं। न जा सकें तो आप मना कर सकते हैं।", 13f, R.color.search_hint).apply { setPadding(0, dp(6), 0, dp(10)) })
                addView(button("हाँ, टीम में जुड़ें", 2) { act { repository.respondInvite(invite.id, true) } })
                addView(button("नहीं", 1) { act { repository.respondInvite(invite.id, false) } })
            })
        }

        val team = data.team
        when (data.role) {
            "leader" -> renderLeader(data)
            "member" -> {
                content.addView(text(team!!.name, 20f, R.color.white, bold = true))
                content.addView(text("लीडर: ${team.leader.name.orEmpty()} · ${team.people} लोग", 14f, R.color.search_hint).apply { setPadding(0, dp(4), 0, dp(12)) })
                content.addView(note("लीडर आपके लिए काम पक्का करेंगे तो आपको ख़बर आएगी। न जा सकें तो उस काम में 'नहीं जा सकता' दबाएँ।"))
                content.addView(button("लीडर को Call करें", 2) { DayJobUi.call(this, team.leader.mobile) })
                team.members.filter { it.status == "active" }.forEach { content.addView(memberRow(it, forLeader = false)) }
                content.addView(button("टीम छोड़ें", 1) {
                    ask("टीम छोड़ें?", "लीडर को ख़बर जाएगी।", "हाँ, छोड़ें") { act { repository.leaveTeam() } }
                })
            }
            else -> {
                content.addView(text("टीम बनाएँ", 20f, R.color.white, bold = true).apply { setPadding(0, dp(8), 0, dp(6)) })
                content.addView(note("आप ठेकेदार या मुखिया हैं? अपने लोगों की टीम बनाइए (आप समेत ${data.maxPeople} लोग तक)। फिर एक बार में सबके लिए काम पक्का कर सकेंगे, और कंपनी सिर्फ़ आपको Call करेगी।"))
                content.addView(button("नई टीम बनाएँ", 0) {
                    askText("टीम का नाम", "जैसे: रमेश की टीम", InputType.TYPE_CLASS_TEXT) { act { repository.createTeam(it) } }
                })
            }
        }
    }

    private fun renderLeader(data: DayJobTeamData) {
        val team = data.team!!
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(text(team.name, 20f, R.color.white, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(text("नाम बदलें", 14f, R.color.brand_color_yellow, bold = true).apply {
                setPadding(dp(8), dp(8), 0, dp(8))
                setOnClickListener {
                    askText("टीम का नया नाम", team.name, InputType.TYPE_CLASS_TEXT) { act { repository.renameTeam(it) } }
                }
            })
        })
        val free = team.members.count { it.free }
        content.addView(text("${team.people}/${data.maxPeople} लोग · आज $free खाली", 14f, R.color.search_hint).apply { setPadding(0, dp(2), 0, dp(12)) })
        if (team.people + team.members.count { it.status == "invited" } < data.maxPeople) {
            content.addView(button("+ आदमी जोड़ें (मोबाइल नंबर से)", 0) {
                askText("मोबाइल नंबर", "10 अंकों का नंबर", InputType.TYPE_CLASS_PHONE) { act { repository.invite(it) } }
            })
            content.addView(note("जिसे जोड़ना है उसके फ़ोन में यह app होना चाहिए। उसके हाँ करते ही वह टीम में आ जाएगा।"))
        } else {
            content.addView(note("टीम में ${data.maxPeople} लोग हो गए, और नहीं जुड़ सकते।"))
        }
        if (team.members.isEmpty()) content.addView(note("अभी टीम में कोई नहीं।"))
        team.members.sortedWith(compareBy({ it.status != "active" }, { !it.free })).forEach { content.addView(memberRow(it, forLeader = true)) }
        content.addView(button("टीम बंद करें", 1) {
            ask("टीम बंद करें?", "सबको ख़बर जाएगी। पहले से पक्के काम वैसे ही रहेंगे।", "हाँ, बंद करें") { act { repository.disbandTeam() } }
        })
    }

    private fun memberRow(m: DayJobTeamMember, forLeader: Boolean): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bg_day_job_pill)
        setPadding(dp(12), dp(10), dp(8), dp(10))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = dp(8) }
        // Green dot: free for work today.
        addView(View(context).apply {
            setBackgroundResource(R.drawable.bg_day_job_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context,
                if (m.free) R.color.day_job_green else R.color.search_hint))
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(10) }
        })
        val state = when {
            m.status == "invited" -> "हाँ का इंतज़ार"
            m.bookedToday -> "आज काम पर"
            m.free -> "आज खाली"
            else -> "आज काम नहीं"
        }
        addView(text("${m.name ?: "मज़दूर"}\n${(m.trades.joinToString(", ").ifBlank { "—" })} · $state", 14f, R.color.white).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (forLeader) {
            addView(text("हटाएँ", 14f, R.color.new_tag_red).apply {
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setOnClickListener {
                    ask("${m.name ?: "इन्हें"} टीम से हटाएँ?", "उन्हें ख़बर जाएगी।", "हाँ, हटाएँ") { act { repository.removeMember(m.id) } }
                }
            })
            if (m.status == "active") addView(text("Call", 14f, R.color.day_job_on_green, bold = true).apply {
                setBackgroundResource(R.drawable.bg_day_job_green_button)
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setOnClickListener { DayJobUi.call(context, m.mobile) }
            })
        }
    }

    // ------------------------------------------------------------ helpers

    private fun act(block: suspend () -> DayJobOutcome<DayJobTeamData>) {
        loading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = block()
            loading.visibility = View.GONE
            when (result) {
                is DayJobOutcome.Ok -> { DayJobUi.toast(this@DayJobTeamActivity, result.message); load() }
                is DayJobOutcome.Fail -> DayJobUi.toast(this@DayJobTeamActivity, result.message)
            }
        }
    }

    private fun ask(title: String, message: String, yes: String, onYes: () -> Unit) {
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle(title).setMessage(message)
            .setPositiveButton(yes) { _, _ -> onYes() }
            .setNegativeButton("नहीं", null)
            .show()
    }

    private fun askText(title: String, hint: String, type: Int, onOk: (String) -> Unit) {
        val input = EditText(this).apply {
            this.hint = hint
            inputType = type
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setHintTextColor(ContextCompat.getColor(context, R.color.search_hint))
        }
        val box = FrameLayout(this).apply { setPadding(dp(20), dp(8), dp(20), 0); addView(input) }
        AlertDialog.Builder(this, R.style.Theme_Rojgaarwaala_AlertDialog)
            .setTitle(title).setView(box)
            .setPositiveButton("OK") { _, _ -> input.text.toString().trim().takeIf { it.isNotEmpty() }?.let(onOk) }
            .setNegativeButton("रहने दें", null)
            .show()
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_day_job_card)
        setPadding(dp(14), dp(14), dp(14), dp(6))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = dp(14) }
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
            0 -> { setBackgroundResource(R.drawable.bg_brand_button); setTextColor(ContextCompat.getColor(context, R.color.on_brand)) }
            1 -> { setBackgroundResource(R.drawable.bg_free_job_map_button); setTextColor(ContextCompat.getColor(context, R.color.brand_color_yellow)) }
            else -> { setBackgroundResource(R.drawable.bg_day_job_green_button); setTextColor(ContextCompat.getColor(context, R.color.day_job_on_green)) }
        }
        setOnClickListener { onClick() }
    }

    private fun dp(v: Int) = DayJobUi.dp(this, v)
}
