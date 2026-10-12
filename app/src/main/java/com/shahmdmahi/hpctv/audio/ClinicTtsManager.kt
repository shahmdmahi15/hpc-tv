package com.shahmdmahi.hpctv.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class ClinicTtsManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val banglaLocale = Locale.forLanguageTag("bn-BD")
    private val englishLocale = Locale.US

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setSpeechRate(0.95f) // Slightly relaxed pace for TV public announcement clarity
        }
    }

    /**
     * Speaks the call announcement: Bengali text first, then English text upon completion.
     */
    fun speakBilingualAnnouncement(voiceBn: String, voiceEn: String) {
        if (!isInitialized || tts == null) return

        tts?.stop()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == "CALL_BN" && voiceEn.isNotBlank()) {
                    // Chain English voice after Bengali voice completes
                    try {
                        tts?.language = englishLocale
                        tts?.speak(voiceEn, TextToSpeech.QUEUE_ADD, null, "CALL_EN")
                    } catch (_: Exception) {}
                }
            }

            override fun onError(utteranceId: String?) {
                if (utteranceId == "CALL_BN" && voiceEn.isNotBlank()) {
                    try {
                        tts?.language = englishLocale
                        tts?.speak(voiceEn, TextToSpeech.QUEUE_FLUSH, null, "CALL_EN")
                    } catch (_: Exception) {}
                }
            }
        })

        if (voiceBn.isNotBlank()) {
            val bnAvailable = tts?.isLanguageAvailable(banglaLocale)
            if (bnAvailable != null && bnAvailable >= TextToSpeech.LANG_AVAILABLE) {
                tts?.language = banglaLocale
            }
            tts?.speak(voiceBn, TextToSpeech.QUEUE_FLUSH, null, "CALL_BN")
        } else if (voiceEn.isNotBlank()) {
            tts?.language = englishLocale
            tts?.speak(voiceEn, TextToSpeech.QUEUE_FLUSH, null, "CALL_EN")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
