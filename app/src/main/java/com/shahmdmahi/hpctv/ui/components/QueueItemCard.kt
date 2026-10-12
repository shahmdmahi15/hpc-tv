package com.shahmdmahi.hpctv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.shahmdmahi.hpctv.data.model.TvQueueItem
import com.shahmdmahi.hpctv.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Converts numbers/digits to Bengali script (০, ১, ২, ...).
 */
fun formatNumberBn(value: Any?): String {
    if (value == null) return ""
    val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val str = value.toString()
    val sb = StringBuilder(str.length)
    for (ch in str) {
        if (ch in '0'..'9') {
            sb.append(bnDigits[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

/**
 * Formats ISO timestamp to 12-hour format with Bengali numerals (e.g. ১০:১৫ AM).
 */
fun formatTime12hBn(isoString: String?): String {
    if (isoString.isNullOrBlank()) return ""
    return try {
        val clean = isoString.substringBefore(".").replace("Z", "")
        val formatIn = if (clean.contains("T")) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        } else {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        }
        formatIn.timeZone = TimeZone.getTimeZone("UTC")
        val date = formatIn.parse(clean) ?: return formatNumberBn(isoString)
        val formatOut = SimpleDateFormat("hh:mm a", Locale.US)
        formatOut.timeZone = TimeZone.getDefault()
        formatNumberBn(formatOut.format(date))
    } catch (_: Exception) {
        formatNumberBn(isoString)
    }
}

/**
 * Ultra-compact, 2-row high-density patient queue card designed for TV display visibility.
 * 100% matched to Web Waiting Room Live View (`QueueItemCard`).
 *
 * Row 1: Token #, Full Patient Name, Gender tag, Booking tag, and Status Badge.
 * Row 2: Room / In Session / Live Modality Countdown Timer on Left + Told & Check-In timestamps on Right.
 */
@Composable
fun QueueItemCard(
    item: TvQueueItem,
    serialNumber: Int,
    modifier: Modifier = Modifier
) {
    val isCalling = item.status == "CALLING"
    val isServing = item.status == "IN_THERAPY" || item.status == "IN_CONSULTATION"
    val isMale = item.gender?.equals("MALE", ignoreCase = true) == true
    val isFemale = item.gender?.equals("FEMALE", ignoreCase = true) == true
    val timer = item.timer

    // Animated pulsing border for actively called patients
    val infiniteTransition = rememberInfiniteTransition(label = "cardCallingPulse")
    val pulseBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "callingBorderAlpha"
    )

    val cardBorderColor = when {
        isCalling -> HpcEmerald500.copy(alpha = pulseBorderAlpha)
        isServing -> HpcEmerald600.copy(alpha = 0.7f)
        else -> HpcBorderZinc800
    }

    val cardBackground = when {
        isCalling -> HpcEmeraldDark.copy(alpha = 0.35f)
        isServing -> HpcEmeraldDark.copy(alpha = 0.2f)
        else -> HpcCardSurfaceElevated
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackground)
            .border(
                width = if (isCalling) 1.8.dp else 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // ====================================================================
        // ROW 1: Token #, Patient Name, Gender, Booking Tag & Status Badge
        // ====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Token Serial Badge
                Box(
                    modifier = Modifier
                        .background(HpcTvBackground, RoundedCornerShape(6.dp))
                        .border(1.dp, HpcBorderZinc700, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${formatNumberBn(serialNumber)}",
                        color = if (isCalling) HpcEmerald400 else HpcTextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Patient Full Name
                Text(
                    text = item.patientName.ifBlank { "রোগী" },
                    color = HpcTextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Gender Badge
                if (isMale || isFemale) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isMale) HpcTvBackground else HpcPinkDark,
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (isMale) HpcBorderZinc700 else HpcPink400.copy(alpha = 0.4f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isMale) "পু" else "ম",
                            color = if (isMale) HpcTextZinc300 else HpcPink400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Extra Booking Badge (if applicable)
                if (item.bookingType?.equals("EXTRA", ignoreCase = true) == true) {
                    Box(
                        modifier = Modifier
                            .background(HpcAmberDark, RoundedCornerShape(4.dp))
                            .border(1.dp, HpcAmber500.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "অতিরিক্ত",
                            color = HpcAmber400,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Status Badge
            when {
                isCalling -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(HpcEmerald600, RoundedCornerShape(6.dp))
                            .border(1.dp, HpcEmerald400, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Calling",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "ডাকছেন / CALLING",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                isServing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(HpcEmeraldDark, RoundedCornerShape(6.dp))
                            .border(1.dp, HpcEmerald600, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = "In Room",
                            tint = HpcEmerald400,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "চলছে / IN ROOM",
                            color = HpcEmerald400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(HpcTvBackground, RoundedCornerShape(6.dp))
                            .border(1.dp, HpcBorderZinc800, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Waiting",
                            tint = HpcTextZinc400,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "অপেক্ষমান",
                            color = HpcTextZinc400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ====================================================================
        // ROW 2: Room Destination, Active Timer / Modality & Timestamps
        // ====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(HpcTvBackground)
                .border(
                    width = 1.dp,
                    color = if (isCalling) HpcEmerald700.copy(alpha = 0.6f) else HpcBorderZinc800,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Room / Session / Live Countdown Timer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isCalling -> {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = "Door",
                            tint = HpcEmerald400,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "প্রবেশ করুন:",
                            color = HpcEmerald400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(HpcEmerald600, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "রুম ${formatNumberBn(item.roomNumber.ifBlank { "101" })}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    isServing -> {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = "Door",
                            tint = HpcEmerald400,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "রুম ${formatNumberBn(item.roomNumber.ifBlank { "201" })}-এ চলছে",
                            color = HpcEmerald400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // If active physical therapy timer is present, display live countdown!
                        if (timer != null && timer.remainingSeconds > 0) {
                            val mins = timer.remainingSeconds / 60
                            val secs = timer.remainingSeconds % 60
                            val timerStr = String.format("%02d:%02d", mins, secs)

                            Spacer(modifier = Modifier.width(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(HpcCardSurfaceElevated, RoundedCornerShape(5.dp))
                                    .border(1.dp, HpcEmerald600.copy(alpha = 0.6f), RoundedCornerShape(5.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                if (timer.modalityName.isNotBlank()) {
                                    Text(
                                        text = timer.modalityName,
                                        color = HpcBlue400,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "•",
                                        color = HpcTextZinc500,
                                        fontSize = 9.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = "Timer",
                                    tint = HpcEmerald400,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = formatNumberBn(timerStr),
                                    color = HpcEmerald400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    else -> {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Clock",
                            tint = HpcTextZinc400,
                            modifier = Modifier.size(12.dp)
                        )
                        if (item.roomNumber.isNotBlank()) {
                            Text(
                                text = "রুম ${formatNumberBn(item.roomNumber)} • অপেক্ষমান",
                                color = HpcTextZinc300,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "সিরিয়ালে অপেক্ষমান",
                                color = HpcTextZinc400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Right: Timestamps (Told Time & Check-in Time)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (!item.toldTime.isNullOrBlank()) {
                    Text(
                        text = "বলা: ${formatNumberBn(item.toldTime)}",
                        color = HpcTextZinc400,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (!item.checkInTime.isNullOrBlank()) {
                    Text(
                        text = "প্রবেশ: ${formatTime12hBn(item.checkInTime)}",
                        color = HpcTextZinc300,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Clean, high-visibility empty state for a clear queue.
 * Matches web `EmptyQueueCard` styling.
 */
@Composable
fun EmptyQueueCard(
    title: String,
    subtitle: String = "Queue is currently clear",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(HpcTvBackground)
            .border(1.dp, HpcBorderZinc800, RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HpcCardSurfaceElevated)
                    .border(1.dp, HpcBorderZinc700, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Queue Clear",
                    tint = HpcEmerald500,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = HpcTextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "নতুন রোগীর চেক-ইনের জন্য অপেক্ষা করা হচ্ছে • লাইভ মনিটরিং সক্রিয়",
                color = HpcTextZinc400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .background(HpcEmeraldGlow, RoundedCornerShape(14.dp))
                    .border(1.dp, HpcEmerald600, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(HpcEmerald500, CircleShape)
                )
                Text(
                    text = "অপেক্ষমান তালিকা শূন্য • Standby",
                    color = HpcEmerald400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
