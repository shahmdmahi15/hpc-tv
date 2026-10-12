package com.shahmdmahi.hpctv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.shahmdmahi.hpctv.data.model.CallAnnouncement
import com.shahmdmahi.hpctv.ui.theme.*

/**
 * Large-Format Fullscreen Doctor/Therapy Call Announcement Modal.
 * Designed specifically for high-distance visibility on large TV screens (43"-75"+).
 *
 * Patient Name: 38sp ExtraBold
 * Room Number Callout: 56sp Font-Black inside high-contrast illuminated gradient banner.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DoctorCallAnnouncementDialog(
    announcement: CallAnnouncement,
    countdownSeconds: Int,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "callingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val cleanRoom = announcement.roomNumber.ifBlank { "101" }
    val isTherapy = announcement.station.contains("THERAPY", ignoreCase = true) ||
                    announcement.roomPurpose.contains("Therapy", ignoreCase = true)

    val purposeTitleBn = if (isTherapy) {
        "ফিজিওথেরাপি সেশন • রুমে প্রবেশ করুন"
    } else {
        "ডাক্তার কনসাল্টেশন • চেম্বারে প্রবেশ করুন"
    }

    val purposeSubtitleEn = if (isTherapy) {
        "PHYSICAL THERAPY • PLEASE PROCEED TO ROOM $cleanRoom"
    } else {
        "DOCTOR CONSULTATION • PLEASE PROCEED TO ROOM $cleanRoom"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .width(760.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(HpcCardBackground)
                .border(3.dp, HpcEmerald500, RoundedCornerShape(28.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Calling Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(HpcEmeraldGlow, RoundedCornerShape(20.dp))
                    .border(1.5.dp, HpcEmerald400, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "Calling",
                    tint = HpcEmerald400,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "রোগীকে ডাকা হচ্ছে • NOW CALLING PATIENT",
                    color = HpcEmerald400,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Token Serial Number Badge
            if (announcement.serialNumber != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(HpcEmeraldGlow)
                        .border(1.5.dp, HpcEmerald500.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "টোকেন #${formatNumberBn(announcement.serialNumber)} • TOKEN #${announcement.serialNumber}",
                        color = HpcEmerald300,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. Patient Full Name (Large Typography)
            Text(
                text = announcement.patientName.ifBlank { "রোগীর নাম" },
                color = HpcTextWhite,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 44.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Large-Format Chamber / Room Callout Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF047857), // Emerald 700
                                Color(0xFF065F46), // Emerald 800
                                Color(0xFF064E3B)  // Emerald Dark
                            )
                        )
                    )
                    .border(2.5.dp, HpcEmerald400, RoundedCornerShape(22.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = purposeTitleBn,
                    color = Color(0xFFA7F3D0), // Emerald 200
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Massive Room Icon Box
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = "Room",
                            tint = Color.White,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    // Enormous Room Number Text
                    Column {
                        Text(
                            text = "রুম নং ${formatNumberBn(cleanRoom)}",
                            color = Color.White,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = purposeSubtitleEn,
                            color = HpcEmerald300,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 5. Visual Countdown Progress Bar
            val progress = (countdownSeconds / 16f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(HpcBorderZinc800)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(HpcEmerald500)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Dismiss Info & Remote Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatNumberBn(countdownSeconds)} সেকেন্ড পর স্বয়ংক্রিয় বন্ধ হবে",
                    color = HpcTextZinc400,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                // Dismiss button for TV remote
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(HpcCardSurfaceElevated)
                        .border(1.dp, HpcBorderZinc700, RoundedCornerShape(12.dp))
                        .clickable { onDismiss() }
                        .focusable()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "বন্ধ করুন (OK)",
                            color = HpcTextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
