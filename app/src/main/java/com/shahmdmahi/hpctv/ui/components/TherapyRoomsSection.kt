package com.shahmdmahi.hpctv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.shahmdmahi.hpctv.data.model.TvQueueItem
import com.shahmdmahi.hpctv.ui.theme.*

/**
 * Physical Therapy Queue Section.
 * 100% matched to Web Waiting Room Live View layout (`waiting-room-live-queue-view.tsx`).
 * Renders high-density queue item cards for all physical therapy patients with live modality countdown timers.
 */
@Composable
fun TherapyRoomsSection(
    therapyItems: List<TvQueueItem>,
    activeCount: Int = 0,
    waitingCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(HpcCardBackground, RoundedCornerShape(16.dp))
            .border(1.dp, HpcBorderZinc800, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(HpcBlueDark, RoundedCornerShape(10.dp))
                        .border(1.dp, HpcBlue400.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Physical Therapy",
                        tint = HpcBlue400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ফিজিওথেরাপি সিরিয়াল",
                            color = HpcTextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(HpcBlueDark, RoundedCornerShape(10.dp))
                                .border(1.dp, HpcBlue400.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 7.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${formatNumberBn(therapyItems.size)}",
                                color = HpcBlue400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "PHYSICAL THERAPY QUEUE",
                        color = HpcBlue400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Right Badges & Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val runningCount = activeCount.coerceAtLeast(
                    therapyItems.count { it.status == "IN_THERAPY" || it.status == "CALLING" }
                )
                if (runningCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(HpcEmeraldDark, RoundedCornerShape(10.dp))
                            .border(1.dp, HpcEmerald600.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "চলমান:",
                            color = HpcEmerald400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${formatNumberBn(runningCount)}",
                            color = HpcTextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "থেরাপি রুমসমূহ",
                    color = HpcTextZinc400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content Area: List of patient cards or Empty State
        if (therapyItems.isEmpty()) {
            EmptyQueueCard(
                title = "থেরাপি সিরিয়াল সম্পূর্ণ খালি",
                subtitle = "Therapy Queue is Clear"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = therapyItems,
                    key = { idx, item -> item.id.ifBlank { "therapy_${item.serialNumber}_$idx" } }
                ) { idx, item ->
                    QueueItemCard(
                        item = item,
                        serialNumber = item.serialNumber ?: (idx + 1)
                    )
                }
            }
        }
    }
}
