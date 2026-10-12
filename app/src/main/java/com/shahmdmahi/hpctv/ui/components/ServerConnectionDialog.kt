package com.shahmdmahi.hpctv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.shahmdmahi.hpctv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ServerDiscoveryOverlay(
    statusMessage: String,
    onRetry: () -> Unit,
    onManualIpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarScale"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HpcTvBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(HpcCardBackground)
                .border(1.dp, HpcBorderZinc800, RoundedCornerShape(20.dp))
                .padding(32.dp)
        ) {
            // Radar Icon Box
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                // Expanding wave
                Box(
                    modifier = Modifier
                        .size(90.dp * radarScale)
                        .background(HpcEmerald500.copy(alpha = radarAlpha), CircleShape)
                )

                // Core icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(HpcEmeraldDark, CircleShape)
                        .border(2.dp, HpcEmerald500, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Searching",
                        tint = HpcEmerald400,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ক্লিনিক সার্ভার অনুসন্ধান করা হচ্ছে...",
                color = HpcTextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusMessage,
                color = HpcTextZinc400,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Retry button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(HpcEmerald600)
                        .clickable { onRetry() }
                        .focusable()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "পুনরায় চেষ্টা / Retry",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Manual IP button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(HpcCardSurfaceElevated)
                        .border(1.dp, HpcBorderZinc700, RoundedCornerShape(10.dp))
                        .clickable { onManualIpClick() }
                        .focusable()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ম্যানুয়াল IP প্রবেশ / Manual IP",
                        color = HpcTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ManualIpDialog(
    initialIp: String,
    onConnect: (ip: String, port: Int, ssl: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var ipInput by remember { mutableStateOf(initialIp.ifBlank { "192.168.1.100" }) }
    var portInput by remember { mutableStateOf("3000") }
    var isSsl by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(HpcCardBackground)
                .border(1.5.dp, HpcBorderZinc800, RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(26.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "সার্ভার আইপি কনফিগারেশন",
                    color = HpcTextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(HpcTvBackground)
                        .clickable { onDismiss() }
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = HpcTextZinc300,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "সার্ভার লোকাল IP অ্যাড্রেস (LAN):",
                color = HpcTextZinc300,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Preset IP Chips for fast TV Remote click
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("192.168.1.100", "192.168.0.100", "192.168.1.105", "10.0.2.2").forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (ipInput == preset) HpcEmeraldDark else HpcTvBackground)
                            .border(1.dp, if (ipInput == preset) HpcEmerald500 else HpcBorderZinc800, RoundedCornerShape(6.dp))
                            .clickable { ipInput = preset }
                            .focusable()
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = preset,
                            color = if (ipInput == preset) HpcEmerald400 else HpcTextZinc300,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // IP Input Box (displayed prominently)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HpcTvBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, HpcEmerald600, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "$ipInput : $portInput (${if (isSsl) "HTTPS" else "HTTP"})",
                    color = HpcEmerald400,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SSL Toggle Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(HpcTvBackground)
                    .clickable { isSsl = !isSsl }
                    .focusable()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SSL / HTTPS এনক্রিপশন সক্রিয়",
                    color = HpcTextWhite,
                    fontSize = 12.sp
                )
                Text(
                    text = if (isSsl) "চালু (ON)" else "বন্ধ (OFF)",
                    color = if (isSsl) HpcEmerald400 else HpcTextZinc500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Connect button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HpcEmerald500)
                    .clickable {
                        val portNum = portInput.toIntOrNull() ?: 3000
                        onConnect(ipInput, portNum, isSsl)
                    }
                    .focusable()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "সংযুক্ত করুন / CONNECT",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
