package com.shahmdmahi.hpctv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.shahmdmahi.hpctv.data.model.BulletinNoticeItem
import com.shahmdmahi.hpctv.ui.theme.*

@Composable
fun BulletinMarqueeBar(
    bulletinNotices: List<BulletinNoticeItem>,
    modifier: Modifier = Modifier
) {
    val hasUrgent = bulletinNotices.any { it.priority == "URGENT" }
    val hasHigh = bulletinNotices.any { it.priority == "HIGH" }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(HpcCardBackground)
            .border(1.dp, HpcBorderZinc800, RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Left Badge
        val badgeColor = when {
            hasUrgent -> HpcRed600
            hasHigh -> HpcAmber500
            else -> HpcEmerald600
        }
        val badgeText = when {
            hasUrgent -> "জরুরি নোটিশ / URGENT"
            hasHigh -> "সতর্কতা / NOTICE"
            else -> "ক্লিনিক নোটিশ / NOTICE"
        }

        Box(
            modifier = Modifier
                .background(badgeColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        // Animated Marquee Text
        val marqueeContent = if (bulletinNotices.isNotEmpty()) {
            bulletinNotices.joinToString("   •   ") { notice ->
                val prefix = if (!notice.title.isNullOrBlank()) "${notice.title}: " else ""
                "$prefix${notice.message}"
            }
        } else {
            "সিরিয়াল ডাকা হলে অনুগ্রহ করে টোকেন টিকিটটি সাথে রাখুন   •   ওয়েটিং রুমে বসার সময় মেরুদণ্ড সোজা রাখুন   •   তীব্র ব্যথা বা মাথা ঘোরা অনুভব করলে অবিলম্বে রিসেপশন কাউন্টারে জানান   •   প্রতিটি সেশনের পর রুম ও থেরাপির যন্ত্রপাতি জীবাণুমুক্ত করা হয়   •   স্বাস্থ্যই সকল সুখের মূল - হেলথ অ্যান্ড পেইন কেয়ার সেন্টার (যশোর)"
        }

        Text(
            text = marqueeContent,
            color = HpcTextZinc200,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    velocity = 45.dp
                )
        )
    }
}
