package com.example.buildlearn.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 把毫秒格式化为 mm:ss 或 h:mm:ss。 */
fun formatMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** 把秒数格式化为"X 小时 Y 分钟"或"Y 分钟"。 */
fun formatStudyTime(seconds: Long): String {
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0) "$hours 小时 ${minutes % 60} 分钟" else "$minutes 分钟"
}

/** 价格显示：0 显示"免费"，否则显示"¥xx"。 */
fun formatPrice(priceCents: Int): String =
    if (priceCents <= 0) "免费" else "¥" + "%.2f".format(priceCents / 100.0).removeSuffix(".00")

/** 解析 "#RRGGBB" 颜色，失败时使用默认蓝色。 */
fun parseHexColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF5B6CFF))

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProgressRow(percent: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(text = "$percent%", style = MaterialTheme.typography.labelLarge)
    }
}

/** 占位封面：渐变色块加课程首字。没有真实图片时使用。 */
@Composable
fun CourseCover(
    colorHex: String,
    title: String,
    modifier: Modifier = Modifier,
    corner: androidx.compose.ui.unit.Dp = 12.dp
) {
    val base = parseHexColor(colorHex)
    val light = lerp(base, Color.White, 0.35f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(base, light))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.take(1),
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** 评分文字，例如 "★ 4.8（236）"。 */
fun formatRating(rating: Double, count: Int): String =
    "★ %.1f（%d）".format(rating, count)

/** 骨架屏式占位：一条带流动光带的灰色条，用于尚未开发的页面。 */
@Composable
fun ShimmerBar(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = lerp(base, Color.White, 0.5f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = androidx.compose.ui.geometry.Offset(x * 600f, 0f),
                    end = androidx.compose.ui.geometry.Offset(x * 600f + 300f, 0f)
                )
            )
    )
}

/** 课程卡片的紧凑信息行（标题、讲师、评分、价格）。 */
@Composable
fun CourseSummaryText(
    title: String,
    instructor: String,
    rating: String,
    price: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = instructor,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rating, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(price, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
