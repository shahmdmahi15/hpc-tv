package com.shahmdmahi.hpctv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.shahmdmahi.hpctv.data.model.WaitingSerialItem
import com.shahmdmahi.hpctv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TherapyRoomsSection(
    therapyQueue: TherapyQueueData,
    modifier: Modifier = Modifier
) {
    val activeRooms = therapyQueue.rooms.filter {
        it.status == "OCCUPIED" && it.currentPatient != null
    }
    val waitingQueue = therapyQueue.waitingQueue
    val isQueueClear = activeRooms.isEmpty() && waitingQueue.isEmpty()

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
                        text = "ফিজিওথেরাপি সিরিয়াল",
                        color = HpcTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PHYSICAL THERAPY QUEUE",
                        color = HpcBlue400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Summary Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (therapyQueue.activeTherapyCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(HpcEmeraldDark, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "চলমান:",
                            color = HpcEmerald400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${therapyQueue.activeTherapyCount} জন",
                            color = HpcTextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val totalWaiting = therapyQueue.waitingTherapyCount.coerceAtLeast(waitingQueue.size)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(HpcBlueDark, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "অপেক্ষমান:",
                        color = HpcBlue400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$totalWaiting জন",
                        color = HpcTextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content Area
        if (isQueueClear) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HpcTvBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, HpcBorderZinc800, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Queue Clear",
                        tint = HpcEmerald500,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ফিজিওথেরাপি সিরিয়াল এই মুহূর্তে খালি রয়েছে",
                        color = HpcTextZinc300,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Therapy Queue is currently clear",
                        color = HpcTextZinc500,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. If there are active rooms, display each active therapy session card
                if (activeRooms.isNotEmpty()) {
                    items(activeRooms) { room ->
                        TherapyChamberCard(
                            room = room,
                            waitingQueue = waitingQueue
                        )
                    }
                } else if (waitingQueue.isNotEmpty()) {
                    // 2. If NO active room yet, but patients are waiting in queue
                    item {
                        TherapyStandbyQueueCard(
                            waitingQueue = waitingQueue
                        )
                    }
                }

                // 3. If there are multiple waiting patients, show the upcoming queue list card
                if (activeRooms.isNotEmpty() && waitingQueue.isNotEmpty()) {
                    item {
                        TherapyWaitingListCard(
                            waitingQueue = waitingQueue
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card representing an active physical therapy chamber / bed session,
 * designed in exact symmetry with DoctorChamberCard.
 */
@Composable
fun TherapyChamberCard(
    room: TherapyRoomState,
    waitingQueue: List<WaitingSerialItem>
) {
    val patient = room.currentPatient
    val isServingActive = patient != null
    val timer = room.timer
    val isTimerActive = timer != null && timer.isRunning && timer.remainingSeconds > 0

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HpcCardSurfaceElevated)
            .border(
                width = if (isServingActive) 1.5.dp else 1.dp,
                color = if (isServingActive) HpcEmerald500.copy(alpha = pulseBorderAlpha) else HpcBorderZinc800,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        // Chamber Info Header
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
                        .background(HpcBlueDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "রুম ${room.roomNumber.ifBlank { "201" }}",
                        color = HpcBlue400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = room.purpose.ifBlank { "ফিজিওথেরাপি ও রিহ্যাব" },
                    color = HpcTextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isServingActive) {
                Box(
                    modifier = Modifier
                        .background(HpcEmerald600, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "সেশন চলমান / IN THERAPY",
                        color = HpcTextWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(HpcTvBackground, RoundedCornerShape(6.dp))
                        .border(1.dp, HpcBorderZinc800, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "অপেক্ষমান",
                        color = HpcTextZinc400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Currently Serving Patient Box
        if (patient != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HpcTvBackground, RoundedCornerShape(10.dp))
                    .border(1.dp, HpcEmerald700, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Token Serial Pill
                    Box(
                        modifier = Modifier
                            .background(HpcEmerald500, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (patient.serialNumber != null) "টোকেন ${patient.serialNumber}" else "চলমান",
                            color = HpcTextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column {
                        Text(
                            text = patient.patientName.ifBlank { "রোগী" },
                            color = HpcTextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "বর্তমান রোগী • Undergoing Therapy",
                            color = HpcTextZinc400,
                            fontSize = 10.sp
                        )
                    }
                }

                // Live Timer Badge (if available)
                if (timer != null && timer.remainingSeconds > 0) {
                    val mins = timer.remainingSeconds / 60
                    val secs = timer.remainingSeconds % 60
                    val formattedTime = String.format("%02d:%02d", mins, secs)

                    Row(
                        modifier = Modifier
                            .background(HpcCardBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, HpcEmerald700, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "Timer",
                            tint = HpcEmerald400,
                            modifier = Modifier.size(14.dp)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            if (timer.modalityName.isNotBlank()) {
                                Text(
                                    text = timer.modalityName,
                                    color = HpcBlue400,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                }
            }
        }

        // Upcoming Serials Ticker
        if (waitingQueue.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "পরবর্তী সিরিয়াল: ",
                    color = HpcTextZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    waitingQueue.take(5).forEach { item ->
                        Box(
                            modifier = Modifier
                                .background(HpcTvBackground, RoundedCornerShape(6.dp))
                                .border(1.dp, HpcBorderZinc800, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "#${item.serialNumber ?: "?"}",
                                color = HpcAmber400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card displayed when no therapy session is currently active,
 * but patients are checked in and waiting in the therapy queue.
 */
@Composable
fun TherapyStandbyQueueCard(
    waitingQueue: List<WaitingSerialItem>
) {
    val nextPatient = waitingQueue.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HpcCardSurfaceElevated)
            .border(1.dp, HpcBorderZinc800, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        // Header
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
                        .background(HpcBlueDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "থেরাপি স্টেশন",
                        color = HpcBlue400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "ফিজিওথেরাপি ও রিহ্যাব ইউনিট",
                    color = HpcTextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .background(HpcTvBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, HpcBorderZinc800, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "প্রস্তুত / READY",
                    color = HpcTextZinc300,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Next Patient Callout
        if (nextPatient != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HpcTvBackground, RoundedCornerShape(10.dp))
                    .border(1.dp, HpcBorderZinc700, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(HpcAmberDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (nextPatient.serialNumber != null) "টোকেন ${nextPatient.serialNumber}" else "পরবর্তী",
                        color = HpcAmber400,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Column {
                    Text(
                        text = nextPatient.patientName.ifBlank { "রোগী" },
                        color = HpcTextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "পরবর্তী সিরিয়াল • Next Patient in Line",
                        color = HpcAmber400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Remaining Serials
        if (waitingQueue.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "পরবর্তী সিরিয়াল: ",
                    color = HpcTextZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    waitingQueue.drop(1).take(5).forEach { item ->
                        Box(
                            modifier = Modifier
                                .background(HpcTvBackground, RoundedCornerShape(6.dp))
                                .border(1.dp, HpcBorderZinc800, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "#${item.serialNumber ?: "?"}",
                                color = HpcAmber400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Waiting List card showing detailed list of queued therapy patients.
 */
@Composable
fun TherapyWaitingListCard(
    waitingQueue: List<WaitingSerialItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HpcCardSurfaceElevated)
            .border(1.dp, HpcBorderZinc800, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "অপেক্ষমান সিরিয়াল তালিকা",
                color = HpcTextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${waitingQueue.size} জন রোগী",
                color = HpcBlue400,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            waitingQueue.take(6).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HpcTvBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, HpcBorderZinc800, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(HpcBlueDark, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "#${item.serialNumber ?: "?"}",
                                color = HpcBlue400,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = item.patientName.ifBlank { "রোগী" },
                            color = HpcTextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = item.toldTime?.ifBlank { "অপেক্ষমান" } ?: "অপেক্ষমান",
                        color = HpcTextZinc400,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
