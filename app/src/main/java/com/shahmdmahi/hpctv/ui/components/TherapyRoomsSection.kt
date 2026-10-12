package com.shahmdmahi.hpctv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Spa
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.shahmdmahi.hpctv.data.model.TherapyQueueData
import com.shahmdmahi.hpctv.data.model.TherapyRoomState
import com.shahmdmahi.hpctv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TherapyRoomsSection(
    therapyQueue: TherapyQueueData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(HpcCardBackground, RoundedCornerShape(16.dp))
            .border(1.dp, HpcBorderZinc800, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(HpcBlueDark, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Therapy",
                        tint = HpcBlue400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "ফিজিওথেরাপি কক্ষ ও সেশন টাইমার",
                        color = HpcTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PHYSICAL THERAPY ROOMS (201-215)",
                        color = HpcBlue400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Summary Pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Active therapies count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(HpcEmeraldDark, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "চলমান সেশন:",
                        color = HpcEmerald400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${therapyQueue.activeTherapyCount} টি",
                        color = HpcTextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Estimated wait time
                if (therapyQueue.estimatedWaitMinutes > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(HpcAmberDark, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "গড় অপেক্ষা:",
                            color = HpcAmber400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "~${therapyQueue.estimatedWaitMinutes} মি.",
                            color = HpcTextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grid of therapy rooms
        if (therapyQueue.rooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HpcTvBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, HpcBorderZinc800, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ফিজিওথেরাপি কক্ষ লোড হচ্ছে...",
                    color = HpcTextZinc400,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(therapyQueue.rooms) { room ->
                    TherapyRoomCard(room = room)
                }
            }
        }
    }
}

@Composable
fun TherapyRoomCard(room: TherapyRoomState) {
    val patient = room.currentPatient
    val isOccupied = room.status == "OCCUPIED" && patient != null
    val timer = room.timer
    val isTimerActive = timer != null && timer.isRunning && timer.remainingSeconds > 0

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOccupied) HpcCardSurfaceElevated else HpcTvBackground)
            .border(
                width = if (isOccupied) 1.5.dp else 1.dp,
                color = when {
                    isOccupied -> HpcEmerald500.copy(alpha = if (isTimerActive) pulseAlpha else 0.8f)
                    else -> HpcBorderZinc800
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        // Room Number & Status Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(if (isOccupied) HpcEmeraldDark else HpcBorderZinc800, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "রুম ${room.roomNumber}",
                    color = if (isOccupied) HpcEmerald400 else HpcTextZinc300,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isOccupied) {
                Box(
                    modifier = Modifier
                        .background(HpcEmerald600, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "চলমান",
                        color = HpcTextWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "প্রস্তুত / READY",
                    color = HpcTextZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Patient & Modality Info
        if (patient != null) {
            Text(
                text = patient.patientName.ifBlank { "রোগী" },
                color = HpcTextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (timer != null && timer.modalityName.isNotBlank()) {
                Text(
                    text = timer.modalityName,
                    color = HpcBlue400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Timer display MM:SS
            if (timer != null && timer.remainingSeconds > 0) {
                val mins = timer.remainingSeconds / 60
                val secs = timer.remainingSeconds % 60
                val formattedTime = String.format("%02d:%02d", mins, secs)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HpcTvBackground, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "Timer",
                            tint = HpcEmerald400,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "অবশিষ্ট সময়",
                            color = HpcTextZinc400,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = formattedTime,
                        color = HpcEmerald400,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        } else {
            // Idle Room state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "পরবর্তী রোগীর জন্য উন্মুক্ত",
                    color = HpcTextZinc500,
                    fontSize = 11.sp
                )
            }
        }
    }
}
