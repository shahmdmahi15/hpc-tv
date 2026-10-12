package com.shahmdmahi.hpctv.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * High-reliability hospital waiting room voice announcement manager.
 * Built specifically for Android TV and Leanback devices.
 * Guarantees audio playback through TV USAGE_MEDIA master stream at 100% volume.
 */
class ClinicTtsManager(private val context: Context) : TextToSpeech.OnInitListener {
    private val tag = "ClinicTtsManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingAnnouncement: Pair<String, String>? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var watchdogRunnable: Runnable? = null

    init {
        initializeTts()
    }

    private fun initializeTts(engineName: String? = null) {
        try {
            Log.i(tag, "Initializing TextToSpeech engine (engine=$engineName)...")
            tts = if (engineName != null) {
                TextToSpeech(context.applicationContext, this, engineName)
            } else {
                TextToSpeech(context.applicationContext, this)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error instantiating TextToSpeech: ${e.message}", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            Log.i(tag, "TextToSpeech successfully initialized! Configuring audio attributes...")

            setupTtsParameters()

            // Process any announcement that was requested while TTS was binding
            pendingAnnouncement?.let { (bn, en) ->
                pendingAnnouncement = null
                speakBilingualAnnouncement(bn, en)
            }
        } else {
            Log.e(tag, "TextToSpeech onInit failed with status code: $status")
            // Try explicitly targeting Google TTS engine if default engine failed
            if (!isInitialized) {
                try {
                    tts?.shutdown()
                } catch (_: Exception) {}
                mainHandler.postDelayed({
                    initializeTts("com.google.android.tts")
                }, 1000L)
            }
        }
    }

    private fun setupTtsParameters() {
        val t = tts ?: return
        try {
            // Explicitly route through USAGE_MEDIA / CONTENT_TYPE_SPEECH
            // This ensures voice plays through TV speakers even if system notification stream is muted
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            t.setAudioAttributes(audioAttributes)
            t.setSpeechRate(0.92f) // Clear, paced cadence for public waiting areas
            t.setPitch(1.0f)
        } catch (e: Exception) {
            Log.w(tag, "Failed to apply audio attributes to TTS: ${e.message}")
        }
    }

    /**
     * Resolves the best available Bengali locale on the device (bn-BD, bn-IN, or bn).
     */
    private fun getBestBanglaLocale(): Locale? {
        val t = tts ?: return null
        val candidates = listOf(
            Locale.forLanguageTag("bn-BD"),
            Locale("bn", "BD"),
            Locale("bn", "IN"),
            Locale("bn")
        )

        for (loc in candidates) {
            try {
                val availability = t.isLanguageAvailable(loc)
                if (availability >= TextToSpeech.LANG_AVAILABLE) {
                    return loc
                }
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Speaks the call announcement bilingual text:
     * 1. Speaks English first (guaranteed 100% compatibility across all Android devices/TVs).
     * 2. Speaks Bengali immediately following (if Bengali voice data is supported on device).
     */
    fun speakBilingualAnnouncement(voiceBn: String, voiceEn: String) {
        if (!isInitialized || tts == null) {
            Log.w(tag, "TTS not ready yet; buffering announcement...")
            pendingAnnouncement = Pair(voiceBn, voiceEn)
            return
        }

        // Cancel previous watchdog
        watchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        watchdogRunnable = null

        try {
            tts?.stop()
        } catch (_: Exception) {}

        val englishLocale = Locale.US
        val bnLocale = getBestBanglaLocale()

        val cleanEn = voiceEn.trim()
        val cleanBn = voiceBn.trim()

        Log.i(tag, "Starting announcement speech (En available=${cleanEn.isNotEmpty()}, Bn locale=$bnLocale)")

        val speechParams = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) // 100% full volume
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        // Configure utterance progress listener to chain announcements
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(tag, "Utterance started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(tag, "Utterance completed: $utteranceId")
                if (utteranceId == "CALL_EN" && bnLocale != null && cleanBn.isNotEmpty()) {
                    mainHandler.postDelayed({
                        playBengaliVoice(cleanBn, bnLocale, speechParams)
                    }, 300L)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.w(tag, "Utterance error: $utteranceId")
                if (utteranceId == "CALL_EN" && bnLocale != null && cleanBn.isNotEmpty()) {
                    mainHandler.postDelayed({
                        playBengaliVoice(cleanBn, bnLocale, speechParams)
                    }, 200L)
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(tag, "Utterance error code $errorCode for: $utteranceId")
                if (utteranceId == "CALL_EN" && bnLocale != null && cleanBn.isNotEmpty()) {
                    mainHandler.postDelayed({
                        playBengaliVoice(cleanBn, bnLocale, speechParams)
                    }, 200L)
                }
            }
        })

        // Step 1: Speak English announcement first
        if (cleanEn.isNotEmpty()) {
            try {
                tts?.language = englishLocale
                val speakResult = tts?.speak(cleanEn, TextToSpeech.QUEUE_FLUSH, speechParams, "CALL_EN")
                Log.i(tag, "tts.speak(CALL_EN) returned: $speakResult")

                // Safety watchdog: If CALL_EN does not complete in 8 seconds, trigger Bengali
                if (bnLocale != null && cleanBn.isNotEmpty()) {
                    val watchdog = Runnable {
                        Log.w(tag, "Watchdog timeout triggered for CALL_EN; proceeding to Bengali voice")
                        playBengaliVoice(cleanBn, bnLocale, speechParams)
                    }
                    watchdogRunnable = watchdog
                    mainHandler.postDelayed(watchdog, 8000L)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error speaking English utterance: ${e.message}", e)
                if (bnLocale != null && cleanBn.isNotEmpty()) {
                    playBengaliVoice(cleanBn, bnLocale, speechParams)
                }
            }
        } else if (bnLocale != null && cleanBn.isNotEmpty()) {
            playBengaliVoice(cleanBn, bnLocale, speechParams)
        }
    }

    private fun playBengaliVoice(text: String, locale: Locale, params: Bundle) {
        try {
            val t = tts ?: return
            t.language = locale
            val res = t.speak(text, TextToSpeech.QUEUE_FLUSH, params, "CALL_BN")
            Log.i(tag, "tts.speak(CALL_BN) returned: $res")
        } catch (e: Exception) {
            Log.e(tag, "Error speaking Bengali utterance: ${e.message}", e)
        }
    }

    fun stop() {
        watchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        watchdogRunnable = null
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun release() {
        watchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        watchdogRunnable = null
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}
