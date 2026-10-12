package com.shahmdmahi.hpctv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shahmdmahi.hpctv.audio.ClinicTtsManager
import com.shahmdmahi.hpctv.audio.HospitalChimePlayer
import com.shahmdmahi.hpctv.data.discovery.ServerDiscoveryManager
import com.shahmdmahi.hpctv.data.model.*
import com.shahmdmahi.hpctv.data.network.HpcNetworkClient
import com.shahmdmahi.hpctv.data.network.TelemetryHeartbeatManager
import com.shahmdmahi.hpctv.data.network.WaitingRoomApiService
import com.shahmdmahi.hpctv.data.network.WaitingRoomSseManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class WaitingRoomViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val client = HpcNetworkClient.getClient(context)
    private val discoveryManager = ServerDiscoveryManager(context)
    private val apiService = WaitingRoomApiService(client)
    private val sseManager = WaitingRoomSseManager(client, viewModelScope)
    private val telemetryManager = TelemetryHeartbeatManager(context, client, viewModelScope)
    private val ttsManager = ClinicTtsManager(context)

    // UI States
    sealed class ViewState {
        data class Discovering(val message: String) : ViewState()
        data class Connecting(val server: DiscoveredServer) : ViewState()
        data class Ready(val snapshot: WaitingRoomSnapshot) : ViewState()
        data class Offline(val reason: String, val lastServer: DiscoveredServer?) : ViewState()
    }

    private val _viewState = MutableStateFlow<ViewState>(ViewState.Discovering("Searching for clinic server on local Wi-Fi..."))
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    private val _snapshot = MutableStateFlow(WaitingRoomSnapshot())
    val snapshot: StateFlow<WaitingRoomSnapshot> = _snapshot.asStateFlow()

    private val _currentServer = MutableStateFlow<DiscoveredServer?>(null)
    val currentServer: StateFlow<DiscoveredServer?> = _currentServer.asStateFlow()

    private val _activeAnnouncement = MutableStateFlow<CallAnnouncement?>(null)
    val activeAnnouncement: StateFlow<CallAnnouncement?> = _activeAnnouncement.asStateFlow()

    private val _announcementCountdownSeconds = MutableStateFlow(16)
    val announcementCountdownSeconds: StateFlow<Int> = _announcementCountdownSeconds.asStateFlow()

    private val _emergencyAlert = MutableStateFlow<SystemBroadcastAlert?>(null)
    val emergencyAlert: StateFlow<SystemBroadcastAlert?> = _emergencyAlert.asStateFlow()

    private val _isAudioMuted = MutableStateFlow(false)
    val isAudioMuted: StateFlow<Boolean> = _isAudioMuted.asStateFlow()

    private val _isManualIpDialogOpen = MutableStateFlow(false)
    val isManualIpDialogOpen: StateFlow<Boolean> = _isManualIpDialogOpen.asStateFlow()

    private val _currentTimeStr = MutableStateFlow("")
    val currentTimeStr: StateFlow<String> = _currentTimeStr.asStateFlow()

    private val _currentDateStr = MutableStateFlow("")
    val currentDateStr: StateFlow<String> = _currentDateStr.asStateFlow()

    val latencyMs: StateFlow<Int> = telemetryManager.latencyMs
    val sseConnectionState = sseManager.connectionState

    private var clockTickerJob: Job? = null
    private var alertDismissJob: Job? = null

    init {
        startClockAndTicker()
        observeSseEvents()
        startAutoDiscovery()
    }

    private fun startClockAndTicker() {
        clockTickerJob?.cancel()
        clockTickerJob = viewModelScope.launch(Dispatchers.Default) {
            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.US)
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US)

            while (isActive) {
                val now = Date()
                _currentTimeStr.value = timeFormat.format(now)
                _currentDateStr.value = dateFormat.format(now)

                // 1. Decrement announcement dismiss countdown
                if (_activeAnnouncement.value != null) {
                    val currentSec = _announcementCountdownSeconds.value
                    if (currentSec <= 1) {
                        _activeAnnouncement.value = null
                        _announcementCountdownSeconds.value = 16
                    } else {
                        _announcementCountdownSeconds.value = currentSec - 1
                    }
                }

                // 2. Decrement active therapy session timers smoothly
                decrementTherapyTimers()

                delay(1000L)
            }
        }
    }

    private fun decrementTherapyTimers() {
        val currentSnap = _snapshot.value
        var hasChanges = false

        val updatedRooms = currentSnap.therapyQueue.rooms.map { room ->
            val timer = room.timer
            if (timer != null && timer.isRunning && timer.remainingSeconds > 0) {
                hasChanges = true
                room.copy(
                    timer = timer.copy(
                        remainingSeconds = timer.remainingSeconds - 1,
                        isAllCompleted = (timer.remainingSeconds - 1) <= 0
                    )
                )
            } else {
                room
            }
        }

        val updatedTherapyItems = currentSnap.therapyItems.map { item ->
            val timer = item.timer
            if (timer != null && timer.isRunning && timer.remainingSeconds > 0) {
                hasChanges = true
                item.copy(
                    timer = timer.copy(
                        remainingSeconds = timer.remainingSeconds - 1,
                        isAllCompleted = (timer.remainingSeconds - 1) <= 0
                    )
                )
            } else {
                item
            }
        }

        if (hasChanges) {
            _snapshot.value = currentSnap.copy(
                therapyQueue = currentSnap.therapyQueue.copy(rooms = updatedRooms),
                therapyItems = updatedTherapyItems
            )
        }
    }

    private fun normalizeSnapshot(snap: WaitingRoomSnapshot): WaitingRoomSnapshot {
        val consultationItems = if (snap.consultationItems.isNotEmpty()) {
            snap.consultationItems
        } else {
            snap.consultationQueue.items
        }
        val therapyItems = if (snap.therapyItems.isNotEmpty()) {
            snap.therapyItems
        } else {
            snap.therapyQueue.items
        }
        return snap.copy(
            consultationItems = consultationItems,
            therapyItems = therapyItems
        )
    }

    private fun observeSseEvents() {
        viewModelScope.launch {
            sseManager.initialStateFlow.collect { initialSnapshot ->
                val normalized = normalizeSnapshot(initialSnapshot)
                _snapshot.value = normalized
                _viewState.value = ViewState.Ready(normalized)
            }
        }

        viewModelScope.launch {
            sseManager.callAnnouncementFlow.collect { announcement ->
                triggerCallAnnouncement(announcement)
            }
        }

        viewModelScope.launch {
            sseManager.alertFlow.collect { alert ->
                triggerEmergencyAlert(alert)
            }
        }

        viewModelScope.launch {
            sseManager.noticeUpdateFlow.collect { notice ->
                // Refresh full snapshot to ensure notice order
                fetchLatestSnapshot()
            }
        }

        viewModelScope.launch {
            var reconnectingSeconds = 0
            sseManager.connectionState.collect { connState ->
                if (connState == WaitingRoomSseManager.ConnectionState.RECONNECTING) {
                    reconnectingSeconds += 5
                    if (reconnectingSeconds >= 30) {
                        reconnectingSeconds = 0
                        // Server likely rotated DHCP IP address: trigger dynamic re-discovery
                        startAutoDiscovery()
                    }
                } else {
                    reconnectingSeconds = 0
                }
            }
        }
    }

    fun startAutoDiscovery() {
        viewModelScope.launch {
            _viewState.value = ViewState.Discovering("Locating clinic server on local Wi-Fi...")

            val discovered = discoveryManager.autoDiscoverServer { statusMsg ->
                _viewState.value = ViewState.Discovering(statusMsg)
            }

            if (discovered != null) {
                connectToServer(discovered)
            } else {
                val saved = discoveryManager.getSavedServer()
                _viewState.value = ViewState.Offline(
                    reason = "Could not locate clinic server on local network. Ensure TV and Server share Wi-Fi.",
                    lastServer = saved
                )
            }
        }
    }

    fun connectToServer(server: DiscoveredServer) {
        _currentServer.value = server
        _viewState.value = ViewState.Connecting(server)

        viewModelScope.launch {
            val result = apiService.fetchSnapshot(server.baseUrl)
            if (result.isSuccess) {
                val snap = normalizeSnapshot(result.getOrThrow())
                _snapshot.value = snap
                _viewState.value = ViewState.Ready(snap)
                discoveryManager.saveServer(server)

                // Start SSE stream & Telemetry heartbeat
                sseManager.connect(server.baseUrl)
                telemetryManager.start(server.baseUrl)

                // Self-Healing Certificate Sync: Asynchronously cache rotated Root CA
                if (server.ssl) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val req = okhttp3.Request.Builder()
                                .url("${server.baseUrl}/rootCA.crt")
                                .build()
                            val res = client.newCall(req).execute()
                            if (res.isSuccessful) {
                                val bytes = res.body?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) {
                                    HpcNetworkClient.updateActiveRootCa(context, bytes)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } else {
                _viewState.value = ViewState.Offline(
                    reason = "Connection failed: ${result.exceptionOrNull()?.message}",
                    lastServer = server
                )
            }
        }
    }

    fun connectManually(ip: String, port: Int = 3000, ssl: Boolean = true) {
        _isManualIpDialogOpen.value = false
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").split(":")[0]
        val server = DiscoveredServer(
            service = "hpc-clinic-server",
            ip = cleanIp,
            port = port,
            ssl = ssl,
            name = "Health & Pain Care Center"
        )
        connectToServer(server)
    }

    fun fetchLatestSnapshot() {
        val server = _currentServer.value ?: return
        viewModelScope.launch {
            val result = apiService.fetchSnapshot(server.baseUrl)
            if (result.isSuccess) {
                val snap = normalizeSnapshot(result.getOrThrow())
                _snapshot.value = snap
                if (_viewState.value !is ViewState.Ready) {
                    _viewState.value = ViewState.Ready(snap)
                }
            }
        }
    }

    private fun triggerCallAnnouncement(announcement: CallAnnouncement) {
        _activeAnnouncement.value = announcement
        _announcementCountdownSeconds.value = 16

        if (!_isAudioMuted.value) {
            viewModelScope.launch {
                // 1. Play hospital Ding-Dong bell chime
                HospitalChimePlayer.playHospitalChime()

                // 2. Brief natural pause
                delay(250L)

                // 3. Play clear spoken voice
                ttsManager.speakBilingualAnnouncement(announcement.voiceBn, announcement.voiceEn)
            }
        }

        // Also refresh queue to sync caller's currently-serving slot
        fetchLatestSnapshot()
    }

    fun testAnnouncementSound() {
        triggerCallAnnouncement(
            CallAnnouncement(
                appointmentId = "test-call",
                serialNumber = 1,
                patientName = "পরীক্ষামূলক রোগী (Test Patient)",
                roomNumber = "101",
                roomPurpose = "ডাক্তার কনসাল্টেশন",
                station = "CONSULTATION_ROOM",
                calledAt = "",
                sound = "chime",
                voiceBn = "দয়া করে মনোযোগ দিন। টোকেন নম্বর ১। রোগী পরীক্ষামূলক রোগী, রুম নম্বর ১০১-এ আসুন।",
                voiceEn = "Attention please. Token 1. Patient Test Patient. Please proceed to Room 101."
            )
        )
    }

    private fun triggerEmergencyAlert(alert: SystemBroadcastAlert) {
        _emergencyAlert.value = alert

        if (!_isAudioMuted.value) {
            viewModelScope.launch {
                HospitalChimePlayer.playEmergencyAlertTone()
            }
        }

        alertDismissJob?.cancel()
        alertDismissJob = viewModelScope.launch {
            delay(25000L) // Auto dismiss after 25s
            _emergencyAlert.value = null
        }
    }

    fun dismissAnnouncement() {
        _activeAnnouncement.value = null
        ttsManager.stop()
    }

    fun dismissEmergencyAlert() {
        _emergencyAlert.value = null
        alertDismissJob?.cancel()
    }

    fun toggleAudioMute() {
        _isAudioMuted.value = !_isAudioMuted.value
        if (_isAudioMuted.value) {
            ttsManager.stop()
        }
    }

    fun openManualIpDialog() {
        _isManualIpDialogOpen.value = true
    }

    fun closeManualIpDialog() {
        _isManualIpDialogOpen.value = false
    }

    override fun onCleared() {
        super.onCleared()
        clockTickerJob?.cancel()
        alertDismissJob?.cancel()
        sseManager.disconnect()
        telemetryManager.stop()
        ttsManager.release()
    }
}
