@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.buildlearn.ui.achievements


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
import com.example.buildlearn.data.repo.LearningRepository
import com.example.buildlearn.domain.AchievementCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AchievementRow(val code: AchievementCode, val unlocked: Boolean, val unlockedAt: Long?)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    auth: AuthRepository,
    learning: LearningRepository
) : ViewModel() {

    val rows: StateFlow<List<AchievementRow>> = auth.currentUserId.filterNotNull().flatMapLatest { uid ->
        learning.observeAchievements(uid).map { earned ->
            val byCode = earned.associateBy { it.code }
            AchievementCode.entries.map { code ->
                val e = byCode[code.name]
                AchievementRow(code, unlocked = e != null, unlockedAt = e?.unlockedAt)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val unlockedCount = rows.count { it.unlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成就") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "已解锁 $unlockedCount / ${rows.size}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(rows, key = { it.code.name }) { row ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (row.unlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(row.code.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                row.code.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
