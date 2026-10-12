package com.shahmdmahi.hpctv

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.shahmdmahi.hpctv.ui.components.*
import com.shahmdmahi.hpctv.ui.theme.HPCTVTheme
import com.shahmdmahi.hpctv.ui.theme.HpcTvBackground
import com.shahmdmahi.hpctv.ui.viewmodel.WaitingRoomViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: WaitingRoomViewModel by viewModels()

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Keep TV screen active indefinitely (prevents ambient sleep/screensaver)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Hide system bars for seamless edge-to-edge television kiosk experience
        hideSystemBars()

        setContent {
            HPCTVTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HpcTvBackground),
                    shape = androidx.compose.ui.graphics.RectangleShape
                ) {
                    WaitingRoomScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // TV Remote Back key handling (Samsung/LG/Sony/Android TV)
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (viewModel.activeAnnouncement.value != null) {
                viewModel.dismissAnnouncement()
                return true
            }
            if (viewModel.emergencyAlert.value != null) {
                viewModel.dismissEmergencyAlert()
                return true
            }
            if (viewModel.isManualIpDialogOpen.value) {
                viewModel.closeManualIpDialog()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun WaitingRoomScreen(viewModel: WaitingRoomViewModel) {
    val viewState by viewModel.viewState.collectAsState()
    val snapshot by viewModel.snapshot.collectAsState()
    val currentTime by viewModel.currentTimeStr.collectAsState()
    val currentDate by viewModel.currentDateStr.collectAsState()
    val latencyMs by viewModel.latencyMs.collectAsState()
    val sseState by viewModel.sseConnectionState.collectAsState()
    val isMuted by viewModel.isAudioMuted.collectAsState()
    val activeAnnouncement by viewModel.activeAnnouncement.collectAsState()
    val countdownSec by viewModel.announcementCountdownSeconds.collectAsState()
    val emergencyAlert by viewModel.emergencyAlert.collectAsState()
    val isManualIpOpen by viewModel.isManualIpDialogOpen.collectAsState()
    val currentServer by viewModel.currentServer.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HpcTvBackground)
    ) {
        when (val state = viewState) {
            is WaitingRoomViewModel.ViewState.Discovering -> {
                ServerDiscoveryOverlay(
                    statusMessage = state.message,
                    onRetry = { viewModel.startAutoDiscovery() },
                    onManualIpClick = { viewModel.openManualIpDialog() }
                )
            }

            is WaitingRoomViewModel.ViewState.Offline -> {
                ServerDiscoveryOverlay(
                    statusMessage = state.reason,
                    onRetry = { viewModel.startAutoDiscovery() },
                    onManualIpClick = { viewModel.openManualIpDialog() }
                )
            }

            is WaitingRoomViewModel.ViewState.Connecting -> {
                ServerDiscoveryOverlay(
                    statusMessage = "Connecting to ${state.server.baseUrl}...",
                    onRetry = { viewModel.connectToServer(state.server) },
                    onManualIpClick = { viewModel.openManualIpDialog() }
                )
            }

            is WaitingRoomViewModel.ViewState.Ready -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Emergency Alert Top Banner (if active)
                    if (emergencyAlert != null) {
                        EmergencyAlertBanner(
                            alert = emergencyAlert!!,
                            onDismiss = { viewModel.dismissEmergencyAlert() }
                        )
                    }

                    // 2. Main Header Toolbar
                    HeaderSection(
                        clinicInfo = snapshot.clinic,
                        currentTime = currentTime,
                        currentDate = currentDate,
                        latencyMs = latencyMs,
                        connectionState = sseState,
                        isMuted = isMuted,
                        onToggleMute = { viewModel.toggleAudioMute() },
                        onOpenSettings = { viewModel.openManualIpDialog() }
                    )

                    // 3. Main Split-Screen Workspace (Consultation Chambers on Left, Therapy Rooms on Right)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Doctor Consultation Queue
                        DoctorChambersSection(
                            consultationQueue = snapshot.consultationQueue,
                            modifier = Modifier.weight(1.15f)
                        )

                        // Right Column: Physical Therapy Rooms
                        TherapyRoomsSection(
                            therapyQueue = snapshot.therapyQueue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 4. Bottom Continuous Marquee Ticker
                    BulletinMarqueeBar(
                        bulletinNotices = snapshot.bulletinNotices
                    )
                }

                // 5. Doctor Call Announcement Fullscreen Modal
                if (activeAnnouncement != null) {
                    DoctorCallAnnouncementDialog(
                        announcement = activeAnnouncement!!,
                        countdownSeconds = countdownSec,
                        onDismiss = { viewModel.dismissAnnouncement() }
                    )
                }
            }
        }

        // 6. Manual Server IP Entry Modal
        if (isManualIpOpen) {
            ManualIpDialog(
                initialIp = currentServer?.ip ?: "192.168.1.100",
                onConnect = { ip, port, ssl ->
                    viewModel.connectManually(ip, port, ssl)
                },
                onDismiss = { viewModel.closeManualIpDialog() }
            )
        }
    }
}