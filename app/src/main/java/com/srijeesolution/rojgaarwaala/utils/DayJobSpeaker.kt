package com.srijeesolution.rojgaarwaala.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Reads a new 1 Day Job alert aloud in Hindi, for workers who find reading hard.
 * On by default; the worker can turn it off on Find Work.
 */
object DayJobSpeaker {

    private const val PREFS = "day_job"
    private const val KEY = "speak_alerts"

    fun isOn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)

    fun setOn(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()
    }

    /** The app's own English words, as a Hindi voice should say them. */
    private val words = listOf(
        "Job Done" to "जॉब डन", "Booking Summary" to "बुकिंग समरी", "Book" to "बुक", "Call" to "कॉल",
        "km" to "किलोमीटर", "OT" to "ओवरटाइम", "rating" to "रेटिंग", "OTP" to "ओ टी पी",
    )

    /** [text] with known English words in Devanagari; whole words only, so "Booking" stays whole. */
    fun hindiWords(text: String): String = words.fold(text) { acc, (en, hi) ->
        acc.replace(Regex("(?<![A-Za-z])" + Regex.escape(en) + "(?![A-Za-z])"), hi)
    }

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    private var playing: android.widget.TextView? = null
    private var playingLabel: CharSequence = ""

    /** Starts the voice early, so the first words are not late. */
    fun warmUp(context: Context) {
        if (engine != null) return
        val app = context.applicationContext
        // Many Samsung phones default to Samsung's engine, which has no Hindi; Google's does.
        val engineName = GOOGLE_TTS.takeIf { name ->
            runCatching { app.packageManager.getPackageInfo(name, 0) }.isSuccess
        }
        engine = TextToSpeech(app, { status ->
            val tts = engine
            if (tts == null || status != TextToSpeech.SUCCESS || !useHindi(app, tts)) {
                tts?.shutdown()
                engine = null
                pending = null
                return@TextToSpeech
            }
            // Fixed here, because some phones have a fast system speed.
            tts.setSpeechRate(SPEECH_RATE)
            tts.setPitch(1f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = finished()
                override fun onStop(utteranceId: String?, interrupted: Boolean) = Unit
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finished()
            })
            ready = true
            pending?.let { say(tts, it) }
            pending = null
        }, engineName)
    }

    private fun useHindi(context: Context, tts: TextToSpeech): Boolean {
        val ok = tts.setLanguage(Locale("hi", "IN"))
        if (ok != TextToSpeech.LANG_MISSING_DATA && ok != TextToSpeech.LANG_NOT_SUPPORTED) return true
        // The phone has no Hindi voice on it: use a Hindi voice that speaks over the internet if
        // there is one, and ask once to download the Hindi voice. Never fall back to an
        // English voice, which reads Hindi words wrongly.
        val online = runCatching { tts.voices.orEmpty().firstOrNull { it.locale.language == "hi" } }.getOrNull()
        if (online == null) {
            askForHindiVoice(context)
            return false
        }
        tts.voice = online
        return true
    }

    private fun say(tts: TextToSpeech, text: String) {
        tts.speak(hindiWords(text), TextToSpeech.QUEUE_FLUSH, null, "day_job_alert")
    }

    /** Puts a pressed "सुनें" button back once the voice has ended. */
    private fun finished() {
        main.post {
            playing?.text = playingLabel
            playing = null
        }
    }

    /**
     * Speaks [text], cutting off whatever was being said. Silent if the phone has no Hindi voice.
     * [always] is for a "सुनें" button the worker pressed, which plays even with alerts muted.
     */
    fun speak(context: Context, text: String?, always: Boolean = false) {
        if (text.isNullOrBlank() || (!always && !isOn(context))) return
        finished()
        val tts = engine
        if (tts != null && ready) say(tts, text) else {
            pending = text
            warmUp(context)
        }
    }

    /** Stops the voice now: a closed card must go quiet. */
    fun stop() {
        pending = null
        engine?.takeIf { ready }?.stop()
        finished()
    }

    /** Makes [button] play [text], and pause it when pressed again. */
    fun bindPlayPause(context: Context, button: android.widget.TextView, text: () -> String?) {
        button.setOnClickListener {
            if (playing === button) {
                stop()
            } else {
                val label = button.text
                speak(context, text(), always = true)
                playing = button
                playingLabel = label
                button.text = "⏸  रोकें"
            }
        }
    }

    private const val SPEECH_RATE = 0.9f
    private const val GOOGLE_TTS = "com.google.android.tts"
    private const val KEY_ASKED_VOICE = "asked_hindi_voice"

    /** Opens the phone's "download voice" screen, once, so the app can speak Hindi. */
    private fun askForHindiVoice(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED_VOICE, false)) return
        prefs.edit().putBoolean(KEY_ASKED_VOICE, true).apply()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, "हिंदी में सुनने के लिए फ़ोन में हिंदी आवाज़ डाउनलोड करें", android.widget.Toast.LENGTH_LONG).show()
            runCatching {
                context.startActivity(android.content.Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
