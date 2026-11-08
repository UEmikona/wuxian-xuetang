package com.example.buildlearn.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 保存"当前登录用户""主题偏好"和"播放偏好"（倍速、自动播放下一课），重启 App 后仍然有效。 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val store = context.sessionDataStore

    private val userIdKey = longPreferencesKey("current_user_id")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val speedKey = floatPreferencesKey("playback_speed")
    private val autoplayKey = booleanPreferencesKey("autoplay_next")

    val currentUserId: Flow<Long?> = store.data.map { it[userIdKey] }

    val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    /** 默认 1.0 倍速。 */
    val playbackSpeed: Flow<Float> = store.data.map { it[speedKey] ?: 1f }

    /** 默认开启自动播放下一课。 */
    val autoplayNext: Flow<Boolean> = store.data.map { it[autoplayKey] ?: true }

    suspend fun setCurrentUserId(userId: Long?) {
        store.edit { prefs ->
            if (userId == null) prefs.remove(userIdKey) else prefs[userIdKey] = userId
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[themeKey] = mode.name }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        store.edit { it[speedKey] = speed }
    }

    suspend fun setAutoplayNext(enabled: Boolean) {
        store.edit { it[autoplayKey] = enabled }
    }
}
