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

    /**
     * Speaks [text] once, then lets the engine go. Silent if the phone has no Hindi voice.
     * [always] is for a "सुनें" button the worker pressed, which plays even with alerts muted.
     */
    fun speak(context: Context, text: String?, always: Boolean = false) {
        if (text.isNullOrBlank() || (!always && !isOn(context))) return
        val app = context.applicationContext
        var tts: TextToSpeech? = null
        // Many Samsung phones default to Samsung's engine, which has no Hindi; Google's does.
        val engineName = GOOGLE_TTS.takeIf { name ->
            runCatching { app.packageManager.getPackageInfo(name, 0) }.isSuccess
        }
        tts = TextToSpeech(app, { status ->
            val engine = tts ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                engine.shutdown()
                return@TextToSpeech
            }
            val hindi = Locale("hi", "IN")
            val ok = engine.setLanguage(hindi)
            if (ok == TextToSpeech.LANG_MISSING_DATA || ok == TextToSpeech.LANG_NOT_SUPPORTED) {
                // The phone has no Hindi voice on it: use a Hindi voice that speaks over the internet if
                // there is one, and ask once to download the Hindi voice. Never fall back to an
                // English voice, which reads Hindi words wrongly.
                val online = runCatching { engine.voices.orEmpty().firstOrNull { it.locale.language == "hi" } }.getOrNull()
                if (online != null) {
                    engine.voice = online
                } else {
                    askForHindiVoice(app)
                    engine.shutdown()
                    return@TextToSpeech
                }
            }
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = engine.shutdown()
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = engine.shutdown()
            })
            engine.speak(hindiWords(text), TextToSpeech.QUEUE_ADD, null, "day_job_alert")
        }, engineName)
    }

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
