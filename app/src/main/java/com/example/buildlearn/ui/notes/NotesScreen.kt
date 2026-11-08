@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.buildlearn.ui.notes


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.buildlearn.data.repo.AuthRepository
import com.example.buildlearn.data.repo.CourseRepository
import com.example.buildlearn.data.repo.LearningRepository
import com.example.buildlearn.ui.common.EmptyState
import com.example.buildlearn.ui.common.formatMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteRow(
    val id: Long,
    val courseId: String,
    val lessonId: String,
    val lessonTitle: String,
    val positionMs: Long,
    val text: String
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val courses: CourseRepository,
    private val learning: LearningRepository
) : ViewModel() {

    private val userId = auth.currentUserId.filterNotNull()

    val rows: StateFlow<List<NoteRow>> = userId.flatMapLatest { uid ->
        combine(
            learning.observeNotes(uid),
            courses.observeAllLessons()
        ) { notes, lessons ->
            val byId = lessons.associateBy { it.id }
            notes.map { n ->
                val l = byId[n.lessonId]
                NoteRow(
                    id = n.id,
                    courseId = l?.courseId.orEmpty(),
                    lessonId = n.lessonId,
                    lessonTitle = l?.title ?: "（已移除的课时）",
                    positionMs = n.positionMs,
                    text = n.text
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(noteId: Long) {
        viewModelScope.launch {
            learning.deleteNote(userId.first(), noteId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBack: () -> Unit,
    onOpenLesson: (courseId: String, lessonId: String) -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的笔记") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        val list = rows
        if (list.isEmpty()) {
            EmptyState(
                title = "还没有笔记",
                subtitle = "在播放页点击“添加笔记”，会记录当时的视频位置。",
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items(list, key = { it.id }) { row ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenLesson(row.courseId, row.lessonId) }
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${row.lessonTitle} · ${formatMs(row.positionMs)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(row.text, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { viewModel.delete(row.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除笔记")
                        }
                    }
                }
            }
        }
    }
}
