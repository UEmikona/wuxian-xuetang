@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.buildlearn.ui.search


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.buildlearn.data.db.CourseEntity
import com.example.buildlearn.data.repo.AuthRepository
import com.example.buildlearn.data.repo.CourseRepository
import com.example.buildlearn.data.repo.StoreRepository
import com.example.buildlearn.ui.common.CourseItem
import com.example.buildlearn.ui.common.CourseListRow
import com.example.buildlearn.ui.common.EmptyState
import com.example.buildlearn.ui.common.toItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

const val ALL_CATEGORIES = "全部"

data class SearchUiState(
    val query: String = "",
    val category: String = ALL_CATEGORIES,
    val categories: List<String> = emptyList(),
    val results: List<CourseItem> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
    private val courses: CourseRepository,
    private val store: StoreRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow(
        (savedStateHandle.get<String>("category") ?: ALL_CATEGORIES).ifBlank { ALL_CATEGORIES }
    )

    val state: StateFlow<SearchUiState> = auth.currentUserId.filterNotNull().flatMapLatest { uid ->
        combine(
            courses.observeCourses(),
            store.observePurchases(uid),
            store.observeFavorites(uid),
            query,
            category
        ) { cs: List<CourseEntity>, purchased, favorites, q, cat ->
            val matched = cs.filter { c ->
                (cat == ALL_CATEGORIES || c.category == cat) &&
                    (q.isBlank() || c.title.contains(q.trim(), ignoreCase = true) ||
                        c.instructor.contains(q.trim(), ignoreCase = true))
            }
            SearchUiState(
                query = q,
                category = cat,
                categories = listOf(ALL_CATEGORIES) + cs.map { it.category }.distinct(),
                results = matched.map { it.toItem(it.id in purchased, it.id in favorites) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) = query.update { value }
    fun onCategoryChange(value: String) = category.update { value }
}

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenCourse: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索课程") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("搜索课程名或讲师") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.categories) { cat ->
                    FilterChip(
                        selected = state.category == cat,
                        onClick = { viewModel.onCategoryChange(cat) },
                        label = { Text(cat) }
                    )
                }
            }
            if (state.results.isEmpty()) {
                EmptyState(title = "没有找到课程", subtitle = "换个关键词或分类试试。")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.results, key = { it.id }) { course ->
                        CourseListRow(item = course, onClick = { onOpenCourse(course.id) })
                    }
                }
            }
        }
    }
}
