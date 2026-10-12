package com.shahmdmahi.hpctv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.shahmdmahi.hpctv.data.model.ConsultationQueueData
import com.shahmdmahi.hpctv.data.model.DoctorChamberQueue
import com.shahmdmahi.hpctv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DoctorChambersSection(
    consultationQueue: ConsultationQueueData,
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
                        .background(HpcEmeraldGlow, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "Doctor",
                        tint = HpcEmerald400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "ডাক্তার কনসাল্টেশন সিরিয়াল",
                        color = HpcTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DOCTOR CONSULTATION CHAMBERS",
                        color = HpcEmerald400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Total Waiting badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(HpcEmeraldDark, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "অপেক্ষমান:",
                    color = HpcEmerald400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${consultationQueue.totalWaiting} জন",
                    color = HpcTextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Doctors List
        if (consultationQueue.doctors.isEmpty() && consultationQueue.waitingQueue.isEmpty()) {
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
                        text = "কনসাল্টেশন সিরিয়াল এই মুহূর্তে খালি রয়েছে",
                        color = HpcTextZinc300,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Consultation Queue is currently clear",
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
                items(consultationQueue.doctors) { doctorChamber ->
                    DoctorChamberCard(chamber = doctorChamber)
                }
            }
        }
    }
}

@Composable
fun DoctorChamberCard(chamber: DoctorChamberQueue) {
    val serving = chamber.currentlyServing
    val isServingActive = serving != null

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
        // Doctor info header
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
                        .background(HpcEmeraldDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "রুম ${chamber.roomNumber.ifBlank { "101" }}",
                        color = HpcEmerald400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = chamber.doctorName.ifBlank { "ডাক্তার কনসাল্টেশন" },
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
                        text = "চেম্বারে আছেন / CALLING",
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

        // Currently Serving Box
        if (serving != null) {
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Token Serial Pill
                    Box(
                        modifier = Modifier
                            .background(HpcEmerald500, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (serving.serialNumber != null) "টোকেন ${serving.serialNumber}" else "চলমান",
                            color = HpcTextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column {
                        Text(
                            text = serving.patientName.ifBlank { "রোগী" },
                            color = HpcTextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "বর্তমান রোগী • Now Consulting",
                            color = HpcTextZinc400,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Upcoming Serials Ticker
        if (chamber.waitingSerials.isNotEmpty()) {
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
                    chamber.waitingSerials.take(4).forEach { item ->
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
