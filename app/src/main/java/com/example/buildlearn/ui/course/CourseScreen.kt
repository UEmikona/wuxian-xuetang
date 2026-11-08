@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.buildlearn.ui.course


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buildlearn.data.db.CourseEntity
import com.example.buildlearn.data.db.LessonEntity
import com.example.buildlearn.data.db.ProgressEntity
import com.example.buildlearn.data.db.ReviewEntity
import com.example.buildlearn.data.repo.AuthRepository
import com.example.buildlearn.data.repo.CourseRepository
import com.example.buildlearn.data.repo.StoreRepository
import com.example.buildlearn.domain.coursePercent
import com.example.buildlearn.ui.common.CourseCover
import com.example.buildlearn.ui.common.ProgressRow
import com.example.buildlearn.ui.common.formatMs
import com.example.buildlearn.ui.common.formatPrice
import com.example.buildlearn.ui.common.formatRating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LessonRow(
    val id: String,
    val title: String,
    val durationMs: Long,
    val positionMs: Long,
    val completed: Boolean
)

data class CourseDetailUiState(
    val loaded: Boolean = false,
    val course: CourseEntity? = null,
    val lessons: List<LessonRow> = emptyList(),
    val reviews: List<ReviewEntity> = emptyList(),
    val purchased: Boolean = false,
    val favorite: Boolean = false,
    val percent: Int = 0,
    val resumeLessonId: String? = null
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
    private val courses: CourseRepository,
    private val store: StoreRepository,
    private val learning: com.example.buildlearn.data.repo.LearningRepository
) : ViewModel() {

    val courseId: String = checkNotNull(savedStateHandle["courseId"])

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val userId = auth.currentUserId.filterNotNull()

    val state: StateFlow<CourseDetailUiState> = userId.flatMapLatest { uid ->
        val catalog = combine(
            courses.observeCourses(),
            courses.observeLessons(courseId),
            learning.observeProgress(uid)
        ) { cs, lessons, progress -> Triple(cs, lessons, progress) }
        val shop = combine(
            store.observePurchases(uid),
            store.observeFavorites(uid),
            courses.observeReviews(courseId)
        ) { purchased, favorites, reviews -> Triple(purchased, favorites, reviews) }
        combine(catalog, shop) { (cs, lessons, progress), (purchased, favorites, reviews) ->
            val course = cs.firstOrNull { it.id == courseId }
            buildState(course, lessons, progress, courseId in purchased, courseId in favorites, reviews)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CourseDetailUiState())

    private fun buildState(
        course: CourseEntity?,
        lessons: List<LessonEntity>,
        progress: List<ProgressEntity>,
        purchased: Boolean,
        favorite: Boolean,
        reviews: List<ReviewEntity>
    ): CourseDetailUiState {
        val byLesson = progress.associateBy { it.lessonId }
        val rows = lessons.map {
            val p = byLesson[it.id]
            LessonRow(it.id, it.title, it.durationMs, p?.positionMs ?: 0L, p?.completed == true)
        }
        val resume = rows.firstOrNull { !it.completed && it.positionMs > 0 }
            ?: rows.firstOrNull { !it.completed }
            ?: rows.firstOrNull()
        return CourseDetailUiState(
            loaded = true,
            course = course,
            lessons = rows,
            reviews = reviews,
            purchased = purchased,
            favorite = favorite,
            percent = coursePercent(rows.count { it.completed }, rows.size),
            resumeLessonId = resume?.id
        )
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val uid = userId.first()
            val nowFavorite = store.toggleFavorite(uid, courseId)
            _messages.emit(if (nowFavorite) "已加入收藏" else "已取消收藏")
        }
    }

    /** 占位购买：不接入支付，直接记录为已拥有。免费课程为"免费领取"。 */
    fun purchase() {
        viewModelScope.launch {
            val uid = userId.first()
            val added = store.purchase(uid, courseId)
            val free = state.value.course?.priceCents?.let { it <= 0 } ?: false
            _messages.emit(
                when {
                    !added -> "你已拥有该课程"
                    free -> "已免费领取，开始学习吧"
                    else -> "购买成功（占位流程，未产生真实扣款）"
                }
            )
        }
    }

    fun notifyPlaceholder(text: String) {
        viewModelScope.launch { _messages.emit(text) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    onBack: () -> Unit,
    onOpenLesson: (courseId: String, lessonId: String) -> Unit,
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableIntStateOf(0) }
    var showBuyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    val course = state.course
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(course?.title ?: "课程详情", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (state.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (state.favorite) "取消收藏" else "收藏"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (course != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = viewModel::toggleFavorite) {
                        Text(if (state.favorite) "已收藏" else "收藏")
                    }
                    Button(
                        onClick = {
                            when {
                                state.purchased && state.resumeLessonId != null ->
                                    onOpenLesson(course.id, state.resumeLessonId!!)
                                state.purchased -> viewModel.notifyPlaceholder("该课程暂无课时")
                                course.priceCents <= 0 -> viewModel.purchase()
                                else -> showBuyDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            when {
                                state.purchased -> if (state.percent in 1..99) "继续学习" else if (state.percent == 100) "复习课程" else "开始学习"
                                course.priceCents <= 0 -> "免费领取"
                                else -> "立即购买 ${formatPrice(course.priceCents)}"
                            }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (!state.loaded || course == null) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Text("课程不存在或正在加载", modifier = Modifier.padding(24.dp))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CourseCover(
                    colorHex = course.coverColor,
                    title = course.title,
                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 16.dp),
                    corner = 16.dp
                )
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(course.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(course.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${formatRating(course.rating, course.ratingCount)} · ${course.studentCount} 人学习 · ${course.instructor}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.purchased) "你已拥有本课程" else "价格：${formatPrice(course.priceCents)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.purchased) {
                        Spacer(Modifier.height(8.dp))
                        ProgressRow(state.percent)
                    }
                }
            }
            item {
                TabRow(selectedTabIndex = tab) {
                    listOf("简介", "目录（${state.lessons.size}）", "评论（${state.reviews.size}）").forEachIndexed { i, title ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                    }
                }
            }

            when (tab) {
                0 -> item {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(course.description, style = MaterialTheme.typography.bodyMedium)
                        Text("分类：${course.category}", style = MaterialTheme.typography.bodySmall)
                        Text("讲师：${course.instructor}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                1 -> {
                    if (state.lessons.isEmpty()) {
                        item { Text("暂无课时", modifier = Modifier.padding(16.dp)) }
                    }
                    items(state.lessons, key = { it.id }) { lesson ->
                        Card(
                            onClick = {
                                if (state.purchased) onOpenLesson(course.id, lesson.id)
                                else viewModel.notifyPlaceholder("请先购买或领取本课程")
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when {
                                        !state.purchased -> Icons.Default.Lock
                                        lesson.completed -> Icons.Default.CheckCircle
                                        else -> Icons.Default.PlayCircle
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(lesson.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        when {
                                            !state.purchased -> "未解锁"
                                            lesson.completed -> "已完成"
                                            lesson.positionMs > 0 -> "已看到 ${formatMs(lesson.positionMs)}"
                                            else -> "未开始"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (lesson.durationMs > 0) {
                                    Text(formatMs(lesson.durationMs), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
                else -> {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "平均 ${"%.1f".format(course.rating)} 分（${course.ratingCount} 条评价）",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.notifyPlaceholder("写评论功能为占位，暂未开放") }) {
                                Text("写评论")
                            }
                        }
                    }
                    if (state.reviews.isEmpty()) {
                        item { Text("暂无评论", modifier = Modifier.padding(16.dp)) }
                    }
                    items(state.reviews, key = { it.id }) { review ->
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(review.author, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    Text("★".repeat(review.rating.coerceIn(0, 5)), color = MaterialTheme.colorScheme.primary)
                                }
                                Text(review.content, style = MaterialTheme.typography.bodyMedium)
                                Text(review.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBuyDialog && course != null) {
        AlertDialog(
            onDismissRequest = { showBuyDialog = false },
            title = { Text("确认购买") },
            text = {
                Text("《${course.title}》售价 ${formatPrice(course.priceCents)}。\n\n这是占位支付流程，不会产生真实扣款。")
            },
            confirmButton = {
                Button(onClick = {
                    showBuyDialog = false
                    viewModel.purchase()
                }) { Text("确认支付") }
            },
            dismissButton = {
                TextButton(onClick = { showBuyDialog = false }) { Text("取消") }
            }
        )
    }
}
