package com.shahmdmahi.hpctv.data.network

import android.content.Context
import android.os.Build
import com.shahmdmahi.hpctv.data.model.HeartbeatPayload
import com.shahmdmahi.hpctv.data.model.HeartbeatResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class TelemetryHeartbeatManager(
    private val context: Context,
    private val client: OkHttpClient,
    private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("hpc_tv_prefs", Context.MODE_PRIVATE)

    private val deviceId: String by lazy {
        val saved = prefs.getString("hpc_device_id", null)
        if (saved != null) saved else {
            val newId = "hpc-tv-" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString("hpc_device_id", newId).apply()
            newId
        }
    }

    private var heartbeatJob: Job? = null
    private var currentBaseUrl: String = ""

    private val _latencyMs = MutableStateFlow(0)
    val latencyMs: StateFlow<Int> = _latencyMs

    private val _isHealthy = MutableStateFlow(true)
    val isHealthy: StateFlow<Boolean> = _isHealthy

    fun start(baseUrl: String) {
        currentBaseUrl = baseUrl.trimEnd('/')
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                sendHeartbeat()
                delay(12000L) // 12-second interval
            }
        }
    }

    fun stop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private suspend fun sendHeartbeat() = withContext(Dispatchers.IO) {
        if (currentBaseUrl.isBlank()) return@withContext
        val startMs = System.currentTimeMillis()

        try {
            val payload = HeartbeatPayload(
                deviceId = deviceId,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL} (Android TV)",
                deviceType = "ANDROID_TV",
                role = "WAITING_ROOM",
                station = "Waiting Room TV",
                latencyMs = _latencyMs.value,
                clientTimestamp = startMs,
                browser = "AndroidTV/Native",
                userAgent = "HPC-AndroidTV/${Build.VERSION.RELEASE}"
            )

            val jsonBody = json.encodeToString(HeartbeatPayload.serializer(), payload)
            val request = Request.Builder()
                .url("$currentBaseUrl/api/network/heartbeat")
                .header("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val roundTrip = (System.currentTimeMillis() - startMs).toInt()
                if (response.isSuccessful) {
                    _latencyMs.value = roundTrip
                    _isHealthy.value = true
                } else {
                    _isHealthy.value = false
                }
            }
        } catch (_: Exception) {
            _isHealthy.value = false
        }
    }
}
