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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.shahmdmahi.hpctv.data.model.CallAnnouncement
import com.shahmdmahi.hpctv.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DoctorCallAnnouncementDialog(
    announcement: CallAnnouncement,
    countdownSeconds: Int,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "callingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .width(620.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(HpcCardBackground)
                .border(2.5.dp, HpcEmerald500, RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(HpcEmeraldGlow, RoundedCornerShape(20.dp))
                    .border(1.dp, HpcEmerald500, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "Calling",
                    tint = HpcEmerald400,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "সিরিয়াল ডাকা হয়েছে • PATIENT CALL",
                    color = HpcEmerald400,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Token Number in massive typography
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(HpcEmerald500)
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val tokenText = if (announcement.serialNumber != null) {
                    "টোকেন ${announcement.serialNumber} • TOKEN ${announcement.serialNumber}"
                } else {
                    "সিরিয়াল ডাকা হয়েছে"
                }
                Text(
                    text = tokenText,
                    color = HpcTextWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Patient Name
            Text(
                text = announcement.patientName.ifBlank { "রোগীর নাম" },
                color = HpcTextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Assigned Room
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(HpcTvBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, HpcBorderZinc800, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = "Room",
                    tint = HpcEmerald400,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "অনুগ্রহ করে রুম ${announcement.roomNumber} এ প্রবেশ করুন",
                    color = HpcEmerald400,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Countdown dismiss & Remote button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${countdownSeconds} সেকেন্ড পর স্বয়ংক্রিয় বন্ধ হবে",
                    color = HpcTextZinc400,
                    fontSize = 12.sp
                )

                // Dismiss button for TV remote
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(HpcCardSurfaceElevated)
                        .border(1.dp, HpcBorderZinc700, RoundedCornerShape(10.dp))
                        .clickable { onDismiss() }
                        .focusable()
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = HpcTextWhite,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "বন্ধ করুন (OK)",
                            color = HpcTextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
