package com.example.buildlearn.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.buildlearn.data.db.CourseEntity

/** 列表与卡片使用的课程展示模型，已合并购买与收藏状态。 */
data class CourseItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val instructor: String,
    val category: String,
    val priceCents: Int,
    val coverColor: String,
    val rating: Double,
    val ratingCount: Int,
    val studentCount: Int,
    val featured: Boolean,
    val purchased: Boolean,
    val favorite: Boolean
)

fun CourseEntity.toItem(purchased: Boolean, favorite: Boolean) = CourseItem(
    id = id,
    title = title,
    subtitle = subtitle,
    instructor = instructor,
    category = category,
    priceCents = priceCents,
    coverColor = coverColor,
    rating = rating,
    ratingCount = ratingCount,
    studentCount = studentCount,
    featured = featured,
    purchased = purchased,
    favorite = favorite
)

/** 列表中的一行：左侧封面，右侧标题、讲师、评分与价格。 */
@Composable
fun CourseListRow(item: CourseItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CourseCover(
                colorHex = item.coverColor,
                title = item.title,
                modifier = Modifier.size(width = 104.dp, height = 70.dp)
            )
            Spacer(Modifier.width(12.dp))
            CourseSummaryText(
                title = item.title,
                instructor = item.instructor,
                rating = formatRating(item.rating, item.ratingCount),
                price = if (item.purchased) "已拥有" else formatPrice(item.priceCents),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** 纵向卡片，用于横向滚动的"精选推荐"。 */
@Composable
fun CourseTile(item: CourseItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.width(160.dp)) {
        Column(Modifier.padding(8.dp)) {
            CourseCover(
                colorHex = item.coverColor,
                title = item.title,
                modifier = Modifier.fillMaxWidth().height(96.dp)
            )
            Spacer(Modifier.height(8.dp))
            CourseSummaryText(
                title = item.title,
                instructor = item.instructor,
                rating = formatRating(item.rating, item.ratingCount),
                price = if (item.purchased) "已拥有" else formatPrice(item.priceCents)
            )
        }
    }
}
