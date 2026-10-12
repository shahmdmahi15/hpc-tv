package com.shahmdmahi.hpctv.data.network

import com.shahmdmahi.hpctv.data.model.DiscoveredServer
import com.shahmdmahi.hpctv.data.model.WaitingRoomSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class WaitingRoomApiService(private val client: OkHttpClient) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun fetchSnapshot(baseUrl: String): Result<WaitingRoomSnapshot> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/tv/waiting-room")
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val snapshot = json.decodeFromString<WaitingRoomSnapshot>(body)
                Result.success(snapshot)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyDiscovery(baseUrl: String): Result<DiscoveredServer> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/tv/discovery")
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Discovery verification failed: ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val server = json.decodeFromString<DiscoveredServer>(body)
                Result.success(server)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
