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

    /** Speaks [text] once, then lets the engine go. Silent if the phone has no Hindi voice. */
    fun speak(context: Context, text: String?) {
        if (text.isNullOrBlank() || !isOn(context)) return
        val app = context.applicationContext
        var tts: TextToSpeech? = null
        tts = TextToSpeech(app) { status ->
            val engine = tts ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                engine.shutdown()
                return@TextToSpeech
            }
            val hindi = Locale("hi", "IN")
            val ok = engine.setLanguage(hindi)
            if (ok == TextToSpeech.LANG_MISSING_DATA || ok == TextToSpeech.LANG_NOT_SUPPORTED) engine.language = Locale("en", "IN")
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = engine.shutdown()
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = engine.shutdown()
            })
            engine.speak(text, TextToSpeech.QUEUE_ADD, null, "day_job_alert")
        }
    }
}
