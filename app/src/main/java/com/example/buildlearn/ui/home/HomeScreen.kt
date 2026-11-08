@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.buildlearn.ui.home


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.buildlearn.data.db.CourseEntity
import com.example.buildlearn.data.db.LessonEntity
import com.example.buildlearn.data.db.ProgressEntity
import com.example.buildlearn.data.repo.AuthRepository
import com.example.buildlearn.data.repo.CourseRepository
import com.example.buildlearn.data.repo.LearningRepository
import com.example.buildlearn.data.repo.StoreRepository
import com.example.buildlearn.ui.common.CourseItem
import com.example.buildlearn.ui.common.CourseListRow
import com.example.buildlearn.ui.common.CourseTile
import com.example.buildlearn.ui.common.formatMs
import com.example.buildlearn.ui.common.toItem
import com.example.buildlearn.domain.coursePercent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContinueItem(
    val courseId: String,
    val lessonId: String,
    val lessonTitle: String,
    val courseTitle: String,
    val positionMs: Long
)

data class HomeUiState(
    val loaded: Boolean = false,
    val username: String = "",
    val banners: List<CourseItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val featured: List<CourseItem> = emptyList(),
    val all: List<CourseItem> = emptyList(),
    val continueItem: ContinueItem? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val courses: CourseRepository,
    private val learning: LearningRepository,
    private val store: StoreRepository
) : ViewModel() {

    private val username = MutableStateFlow("")

    init {
        viewModelScope.launch {
            val id = auth.currentUserId.filterNotNull().first()
            username.value = auth.findUsername(id).orEmpty()
        }
    }

    val state: StateFlow<HomeUiState> = auth.currentUserId.filterNotNull().flatMapLatest { uid ->
        val catalog = combine(
            courses.observeCourses(),
            courses.observeAllLessons(),
            learning.observeProgress(uid)
        ) { cs, ls, ps -> Triple(cs, ls, ps) }

        val shop = combine(store.observePurchases(uid), store.observeFavorites(uid)) { p, f -> p to f }

        combine(catalog, shop, username) { (cs, ls, ps), (purchased, favorites), name ->
            buildState(cs, ls, ps, purchased, favorites, name)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildState(
        cs: List<CourseEntity>,
        ls: List<LessonEntity>,
        ps: List<ProgressEntity>,
        purchased: Set<String>,
        favorites: Set<String>,
        name: String
    ): HomeUiState {
        val items = cs.map { it.toItem(purchased = it.id in purchased, favorite = it.id in favorites) }
        val lastUnfinished = ps
            .filter { !it.completed && it.positionMs > 0 }
            .maxByOrNull { it.updatedAt }
        val continueItem = lastUnfinished?.let { p ->
            val lesson = ls.firstOrNull { it.id == p.lessonId }
            val course = lesson?.let { l -> cs.firstOrNull { it.id == l.courseId } }
            if (lesson != null && course != null && course.id in purchased) {
                ContinueItem(course.id, lesson.id, lesson.title, course.title, p.positionMs)
            } else null
        }
        return HomeUiState(
            loaded = true,
            username = name,
            banners = items.filter { it.featured }.take(4),
            categories = cs.map { it.category }.distinct(),
            featured = items.filter { it.featured },
            all = items,
            continueItem = continueItem
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCourse: (String) -> Unit,
    onContinue: (courseId: String, lessonId: String) -> Unit,
    onSearch: (category: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("无限学堂", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { onSearch("全部") }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                }
            )
        }
    ) { padding ->
        if (!state.loaded) return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.banners.isNotEmpty()) {
                item { BannerCarousel(state.banners, onOpenCourse) }
            }

            item {
                Text(
                    text = if (state.username.isBlank()) "欢迎来到无限学堂" else "欢迎回来，${state.username}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(selected = false, onClick = { onSearch("全部") }, label = { Text("全部") })
                    }
                    items(state.categories) { cat ->
                        FilterChip(selected = false, onClick = { onSearch(cat) }, label = { Text(cat) })
                    }
                }
            }

            state.continueItem?.let { c ->
                item {
                    Card(
                        onClick = { onContinue(c.courseId, c.lessonId) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("继续学习", style = MaterialTheme.typography.labelLarge)
                                Text(c.lessonTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${c.courseTitle} · 上次位置 ${formatMs(c.positionMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (state.featured.isNotEmpty()) {
                item { SectionTitle("精选推荐") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.featured, key = { it.id }) { course ->
                            CourseTile(course, onClick = { onOpenCourse(course.id) })
                        }
                    }
                }
            }

            item { SectionTitle("全部课程") }
            items(state.all, key = { it.id }) { course ->
                CourseListRow(
                    item = course,
                    onClick = { onOpenCourse(course.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/** 自动轮播的横幅：每 4 秒切换一页，手动滑动后继续自动轮播。 */
@Composable
private fun BannerCarousel(banners: List<CourseItem>, onOpen: (String) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(banners.size) {
        if (banners.size > 1) {
            while (true) {
                delay(4_000)
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % banners.size)
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth().height(180.dp)
        ) { page ->
            val course = banners[page]
            val base = com.example.buildlearn.ui.common.parseHexColor(course.coverColor)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(base, base.copy(alpha = 0.55f))))
                    .clickable { onOpen(course.id) }
                    .padding(20.dp)
            ) {
                Column(Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = course.title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(course.subtitle, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            banners.indices.forEach { i ->
                val selected = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (selected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}
