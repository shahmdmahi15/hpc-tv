package com.shahmdmahi.hpctv.data.network

import com.shahmdmahi.hpctv.data.model.BulletinNoticeItem
import com.shahmdmahi.hpctv.data.model.CallAnnouncement
import com.shahmdmahi.hpctv.data.model.SystemBroadcastAlert
import com.shahmdmahi.hpctv.data.model.WaitingRoomSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class WaitingRoomSseManager(
    private val client: OkHttpClient,
    private val scope: CoroutineScope
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private var eventSource: EventSource? = null
    private var currentBaseUrl: String = ""
    private var isIntentionalDisconnect = false
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _initialStateFlow = MutableSharedFlow<WaitingRoomSnapshot>(extraBufferCapacity = 1)
    val initialStateFlow: SharedFlow<WaitingRoomSnapshot> = _initialStateFlow

    private val _callAnnouncementFlow = MutableSharedFlow<CallAnnouncement>(extraBufferCapacity = 10)
    val callAnnouncementFlow: SharedFlow<CallAnnouncement> = _callAnnouncementFlow

    private val _alertFlow = MutableSharedFlow<SystemBroadcastAlert>(extraBufferCapacity = 5)
    val alertFlow: SharedFlow<SystemBroadcastAlert> = _alertFlow

    private val _noticeUpdateFlow = MutableSharedFlow<BulletinNoticeItem>(extraBufferCapacity = 10)
    val noticeUpdateFlow: SharedFlow<BulletinNoticeItem> = _noticeUpdateFlow

    private val _queueRefreshTriggerFlow = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val queueRefreshTriggerFlow: SharedFlow<Unit> = _queueRefreshTriggerFlow

    private val _lastPingTimestamp = MutableStateFlow(0L)
    val lastPingTimestamp: StateFlow<Long> = _lastPingTimestamp

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    }

    fun connect(baseUrl: String) {
        currentBaseUrl = baseUrl.trimEnd('/')
        isIntentionalDisconnect = false
        reconnectAttempts = 0
        reconnectJob?.cancel()
        startSseConnection()
    }

    fun disconnect() {
        isIntentionalDisconnect = true
        reconnectJob?.cancel()
        eventSource?.cancel()
        eventSource = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun startSseConnection() {
        eventSource?.cancel()
        _connectionState.value = if (reconnectAttempts == 0) ConnectionState.CONNECTING else ConnectionState.RECONNECTING

        val request = Request.Builder()
            .url("$currentBaseUrl/api/v1/tv/waiting-room/stream")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build()

        val factory = EventSources.createFactory(client)
        eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                reconnectAttempts = 0
                _connectionState.value = ConnectionState.CONNECTED
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data.isBlank()) return
                val eventType = (type ?: "").uppercase()

                when (eventType) {
                    "INITIAL_STATE" -> {
                        try {
                            val snapshot = json.decodeFromString<WaitingRoomSnapshot>(data)
                            _initialStateFlow.tryEmit(snapshot)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    "DOCTOR_CALLED" -> {
                        try {
                            val announcement = json.decodeFromString<CallAnnouncement>(data)
                            _callAnnouncementFlow.tryEmit(announcement)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        _queueRefreshTriggerFlow.tryEmit(Unit)
                    }

                    "SYSTEM_BROADCAST_ALERT" -> {
                        try {
                            val alert = json.decodeFromString<SystemBroadcastAlert>(data)
                            _alertFlow.tryEmit(alert)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    "BULLETIN_NOTICE_CREATED", "BULLETIN_NOTICE_UPDATED", "BULLETIN_NOTICE_DELETED" -> {
                        try {
                            val notice = json.decodeFromString<BulletinNoticeItem>(data)
                            _noticeUpdateFlow.tryEmit(notice)
                        } catch (_: Exception) {
                            _queueRefreshTriggerFlow.tryEmit(Unit)
                        }
                    }

                    "APPOINTMENT_UPDATED", "QUEUE_UPDATED", "PATIENT_CHECKED_IN", "PATIENT_QUEUED_FOR_THERAPY", "PATIENT_QUEUED_FOR_CONSULTATION", "PATIENT_FORWARDED", "PATIENT_CHECKED_OUT" -> {
                        _queueRefreshTriggerFlow.tryEmit(Unit)
                    }

                    "PING" -> {
                        _lastPingTimestamp.value = System.currentTimeMillis()
                    }

                    else -> {
                        // Any other event (custom or future events) immediately triggers queue snapshot refresh
                        _queueRefreshTriggerFlow.tryEmit(Unit)
                    }
                }
            }

            override fun onClosed(eventSource: EventSource) {
                if (!isIntentionalDisconnect) {
                    scheduleReconnect()
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (!isIntentionalDisconnect) {
                    scheduleReconnect()
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        })
    }

    private fun scheduleReconnect() {
        _connectionState.value = ConnectionState.RECONNECTING
        reconnectAttempts++
        val delayMs = (2000L * reconnectAttempts).coerceAtMost(15000L)

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delayMs)
            if (!isIntentionalDisconnect) {
                startSseConnection()
            }
        }
    }
}
