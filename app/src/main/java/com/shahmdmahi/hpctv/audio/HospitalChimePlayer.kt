package com.shahmdmahi.hpctv.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.sin

/**
 * Resonant dual-tone hospital bell chime (Ding-Dong) generated with pure PCM synthesis.
 * Completely offline, works on all Android TV boxes and soundbars without external assets.
 */
object HospitalChimePlayer {
    private const val SAMPLE_RATE = 44100
    private const val TOTAL_DURATION_SEC = 2.0

    // Pre-synthesized PCM buffer cache for immediate low-latency playback
    private val chimePcmBuffer: ShortArray by lazy {
        synthesizeChimePcm()
    }

    private val alertPcmBuffer: ShortArray by lazy {
        synthesizeAlertPcm()
    }

    suspend fun playHospitalChime() = withContext(Dispatchers.Default) {
        playPcm(chimePcmBuffer)
    }

    suspend fun playEmergencyAlertTone() = withContext(Dispatchers.Default) {
        playPcm(alertPcmBuffer)
    }

    private fun playPcm(buffer: ShortArray) {
        var track: AudioTrack? = null
        try {
            val bufferSize = buffer.size * 2
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()

            // Wait for sound to play through
            val sleepMs = ((buffer.size.toDouble() / SAMPLE_RATE) * 1000).toLong() + 100
            Thread.sleep(sleepMs)
        } catch (_: Exception) {
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Synthesizes a resonant bell chime:
     * Chord 1 (Ding) at t=0.0s: C5 (523Hz), E5 (659Hz), G5 (784Hz)
     * Chord 2 (Dong) at t=0.55s: G4 (392Hz), B4 (494Hz), D5 (587Hz)
     */
    private fun synthesizeChimePcm(): ShortArray {
        val totalSamples = (SAMPLE_RATE * TOTAL_DURATION_SEC).toInt()
        val buffer = ShortArray(totalSamples)

        val dingNotes = listOf(
            Triple(523.25, 0.40, 2.5), // C5
            Triple(659.25, 0.25, 3.2), // E5
            Triple(783.99, 0.30, 3.8)  // G5
        )

        val dongNotes = listOf(
            Triple(392.00, 0.50, 1.8), // G4
            Triple(493.88, 0.28, 2.4), // B4
            Triple(587.33, 0.32, 2.6)  // D5
        )

        val dongStartSample = (0.55 * SAMPLE_RATE).toInt()

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var sampleVal = 0.0

            // 1. Ding chord
            for ((freq, gain, decay) in dingNotes) {
                val envelope = exp(-decay * t)
                sampleVal += gain * sin(2.0 * Math.PI * freq * t) * envelope
            }

            // 2. Dong chord
            if (i >= dongStartSample) {
                val tDong = (i - dongStartSample).toDouble() / SAMPLE_RATE
                for ((freq, gain, decay) in dongNotes) {
                    val envelope = exp(-decay * tDong)
                    sampleVal += gain * sin(2.0 * Math.PI * freq * tDong) * envelope
                }
            }

            // Clamp and convert to 16-bit signed PCM
            val clamped = sampleVal.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * 32767.0).toInt().toShort()
        }

        return buffer
    }

    /**
     * 3-beep emergency alert tone (880Hz triangle wave)
     */
    private fun synthesizeAlertPcm(): ShortArray {
        val alertDuration = 0.7
        val totalSamples = (SAMPLE_RATE * alertDuration).toInt()
        val buffer = ShortArray(totalSamples)

        val beepDuration = 0.16
        val beepInterval = 0.22

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var sample = 0.0

            for (b in 0..2) {
                val bStart = b * beepInterval
                val bEnd = bStart + beepDuration
                if (t in bStart..bEnd) {
                    val tInBeep = t - bStart
                    val env = (1.0 - (tInBeep / beepDuration)).coerceIn(0.0, 1.0)
                    sample += 0.5 * sin(2.0 * Math.PI * 880.0 * t) * env
                }
            }

            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
        }

        return buffer
    }
}
