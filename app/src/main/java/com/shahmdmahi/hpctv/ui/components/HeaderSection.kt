package com.shahmdmahi.hpctv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.shahmdmahi.hpctv.R
import com.shahmdmahi.hpctv.data.model.ClinicInfo
import com.shahmdmahi.hpctv.data.network.WaitingRoomSseManager
import com.shahmdmahi.hpctv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeaderSection(
    clinicInfo: ClinicInfo,
    currentTime: String,
    currentDate: String,
    latencyMs: Int,
    connectionState: WaitingRoomSseManager.ConnectionState,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(HpcCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Clinic Brand & Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1.3f)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HpcEmeraldDark)
                        .border(1.5.dp, HpcEmerald500, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hpc_icon),
                        contentDescription = "HPC Clinic Logo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = clinicInfo.name.ifBlank { "HEALTH & PAIN CARE CENTER" },
                        color = HpcTextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = clinicInfo.nameBangla.ifBlank { "হেলথ অ্যান্ড পেইন কেয়ার সেন্টার" },
                        color = HpcEmerald400,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${clinicInfo.branch.ifBlank { "চাঁচড়া ডালমিল, যশোর" }} • Hotline: ${clinicInfo.hotline.ifBlank { "01822-000035" }}",
                        color = HpcTextZinc400,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Center: Live Clock & Date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(0.9f)
            ) {
                Text(
                    text = currentTime,
                    color = HpcTextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = currentDate,
                    color = HpcTextZinc300,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Right: Network Status, Voice Toggle & Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(0.9f),
                // align to end
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // TV Wake Lock Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .background(HpcEmeraldGlow, RoundedCornerShape(20.dp))
                        .border(1.dp, HpcEmerald600, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "TV Awake",
                        tint = HpcEmerald400,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Awake / সজাগ",
                        color = HpcEmerald400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Network Ping Indicator
                val (dotColor, statusText) = when (connectionState) {
                    WaitingRoomSseManager.ConnectionState.CONNECTED -> Pair(
                        HpcEmerald500,
                        if (latencyMs > 0) "Online • ${latencyMs}ms" else "Online • Fast"
                    )
                    WaitingRoomSseManager.ConnectionState.CONNECTING,
                    WaitingRoomSseManager.ConnectionState.RECONNECTING -> Pair(HpcAmber500, "Syncing...")
                    WaitingRoomSseManager.ConnectionState.DISCONNECTED -> Pair(HpcRed500, "Offline")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(HpcCardSurfaceElevated, RoundedCornerShape(20.dp))
                        .border(1.dp, HpcBorderZinc700, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(dotColor, CircleShape)
                    )
                    Text(
                        text = statusText,
                        color = HpcTextZinc200,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Audio Mute Toggle Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMuted) HpcRedDark else HpcCardSurfaceElevated)
                        .border(1.dp, if (isMuted) HpcRed500 else HpcBorderZinc700, RoundedCornerShape(8.dp))
                        .clickable { onToggleMute() }
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Toggle Audio",
                        tint = if (isMuted) HpcRed400 else HpcEmerald400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings Button (Manual IP Dialog)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HpcCardSurfaceElevated)
                        .border(1.dp, HpcBorderZinc700, RoundedCornerShape(8.dp))
                        .clickable { onOpenSettings() }
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Server Settings",
                        tint = HpcTextZinc300,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
