@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.buildlearn.ui.learning


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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.buildlearn.data.repo.AuthRepository
import com.example.buildlearn.data.repo.CourseRepository
import com.example.buildlearn.data.repo.LearningRepository
import com.example.buildlearn.data.repo.StoreRepository
import com.example.buildlearn.domain.coursePercent
import com.example.buildlearn.ui.common.CourseCover
import com.example.buildlearn.ui.common.EmptyState
import com.example.buildlearn.ui.common.ProgressRow
import com.example.buildlearn.ui.common.formatPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LearningRow(
    val id: String,
    val title: String,
    val instructor: String,
    val coverColor: String,
    val priceCents: Int,
    val percent: Int
)

data class LearningUiState(
    val inProgress: List<LearningRow> = emptyList(),
    val purchased: List<LearningRow> = emptyList(),
    val favorites: List<LearningRow> = emptyList()
)

@HiltViewModel
class LearningViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val courses: CourseRepository,
    private val learning: LearningRepository,
    private val store: StoreRepository
) : ViewModel() {

    val state: StateFlow<LearningUiState> = auth.currentUserId.filterNotNull().flatMapLatest { uid ->
        val catalog = combine(
            courses.observeCourses(),
            courses.observeAllLessons(),
            learning.observeProgress(uid)
        ) { cs, ls, ps -> Triple(cs, ls, ps) }
        val shop = combine(store.observePurchases(uid), store.observeFavorites(uid)) { p, f -> p to f }

        combine(catalog, shop) { (cs, ls, ps), (purchased, favorites) ->
            val completed = ps.filter { it.completed }.map { it.lessonId }.toSet()
            fun rowOf(id: String): LearningRow? {
                val c = cs.firstOrNull { it.id == id } ?: return null
                val lessons = ls.filter { it.courseId == id }
                val percent = coursePercent(lessons.count { it.id in completed }, lessons.size)
                return LearningRow(c.id, c.title, c.instructor, c.coverColor, c.priceCents, percent)
            }
            val purchasedRows = purchased.mapNotNull { rowOf(it) }
            LearningUiState(
                inProgress = purchasedRows.filter { it.percent < 100 },
                purchased = purchasedRows,
                favorites = favorites.mapNotNull { rowOf(it) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearningUiState())
}

@Composable
fun LearningScreen(
    onOpenCourse: (String) -> Unit,
    viewModel: LearningViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("进行中", "已购买", "收藏")

    Scaffold(topBar = { TopAppBar(title = { Text("我的学习", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                titles.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                }
            }
            val rows = when (tab) {
                0 -> state.inProgress
                1 -> state.purchased
                else -> state.favorites
            }
            if (rows.isEmpty()) {
                EmptyState(
                    title = when (tab) {
                        0 -> "还没有进行中的课程"
                        1 -> "还没有购买或领取的课程"
                        else -> "还没有收藏的课程"
                    },
                    subtitle = if (tab == 2) "在课程详情页点击右上角心形即可收藏。" else "去首页或分类页挑选一门课程吧。"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rows, key = { it.id }) { row ->
                        Card(onClick = { onOpenCourse(row.id) }, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CourseCover(row.coverColor, row.title, Modifier.size(width = 96.dp, height = 64.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(row.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(row.instructor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(6.dp))
                                    if (tab == 2 && !rowPurchased(state, row.id)) {
                                        Text(formatPrice(row.priceCents), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        ProgressRow(row.percent)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun rowPurchased(state: LearningUiState, id: String): Boolean =
    state.purchased.any { it.id == id }
