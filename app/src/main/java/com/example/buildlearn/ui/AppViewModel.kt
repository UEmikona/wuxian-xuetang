package com.example.buildlearn.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buildlearn.data.prefs.SessionStore
import com.example.buildlearn.data.prefs.ThemeMode
import com.example.buildlearn.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 会话状态。Loading 表示还没从本地读取完成，此时导航层不应做跳转。 */
sealed interface SessionState {
    data object Loading : SessionState
    data object Guest : SessionState
    data class LoggedIn(val userId: Long) : SessionState
}

/** 应用级状态：会话与主题。 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val session: SessionStore
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = auth.currentUserId
        .map { id -> if (id == null) SessionState.Guest else SessionState.LoggedIn(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    val themeMode: StateFlow<ThemeMode> = session.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { session.setThemeMode(mode) }
    }

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }
}
