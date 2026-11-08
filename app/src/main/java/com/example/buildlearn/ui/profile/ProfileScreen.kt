@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.buildlearn.ui.profile


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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.buildlearn.data.prefs.SessionStore
import com.example.buildlearn.data.prefs.ThemeMode
import com.example.buildlearn.data.repo.AuthRepository
import com.example.buildlearn.data.repo.LearningRepository
import com.example.buildlearn.domain.STUDY_DAY_MIN_SECONDS
import com.example.buildlearn.domain.currentStreak
import com.example.buildlearn.ui.common.formatStudyTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import androidx.compose.material3.ExperimentalMaterial3Api

data class ProfileUiState(
    val username: String = "",
    val streakDays: Int = 0,
    val totalStudySeconds: Long = 0L,
    val achievementCount: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val learning: LearningRepository,
    private val session: SessionStore
) : ViewModel() {

    private val userId = auth.currentUserId.filterNotNull()

    val state: StateFlow<ProfileUiState> = userId.flatMapLatest { uid ->
        combine(
            learning.observeStudyDays(uid),
            learning.observeAchievements(uid),
            session.themeMode
        ) { days, achievements, theme ->
            val today = LocalDate.now(ZoneId.systemDefault())
            val active = days.filterValues { it >= STUDY_DAY_MIN_SECONDS }.keys
            ProfileUiState(
                username = auth.findUsername(uid).orEmpty(),
                streakDays = currentStreak(active, today),
                totalStudySeconds = days.values.sum(),
                achievementCount = achievements.size,
                themeMode = theme
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { session.setThemeMode(mode) }
    }

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }
}

private val themeLabels = listOf(
    ThemeMode.SYSTEM to "跟随系统",
    ThemeMode.LIGHT to "浅色",
    ThemeMode.DARK to "深色"
)

/** 占位功能页的标题，用于"我的"页中尚未开发的入口。 */
object PlaceholderTitles {
    const val MESSAGES = "消息通知"
    const val COUPONS = "优惠券"
    const val HELP = "帮助与反馈"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenNotes: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenPlaceholder: (String) -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("我的", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (state.username.firstOrNull() ?: '学').toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(state.username.ifBlank { "学员" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "连续学习 ${state.streakDays} 天 · 累计 ${formatStudyTime(state.totalStudySeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { MenuCard {
                MenuRow(Icons.Default.Edit, "我的笔记", onClick = onOpenNotes)
                MenuRow(Icons.Default.Star, "我的成就（${state.achievementCount}）", onClick = onOpenAchievements)
            } }

            item { MenuCard {
                MenuRow(Icons.Default.ShoppingCart, "优惠券", onClick = { onOpenPlaceholder(PlaceholderTitles.COUPONS) })
                MenuRow(Icons.Default.Notifications, "消息通知", onClick = { onOpenPlaceholder(PlaceholderTitles.MESSAGES) })
                MenuRow(Icons.Default.Help, "帮助与反馈", onClick = { onOpenPlaceholder(PlaceholderTitles.HELP) })
            } }

            item {
                Text("外观", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    themeLabels.forEach { (mode, label) ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item { MenuCard {
                MenuRow(Icons.Default.Info, "关于无限学堂", onClick = onOpenAbout)
            } }

            item {
                Card(
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("退出登录", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column { content() }
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}
