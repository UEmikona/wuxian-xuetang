package com.example.buildlearn.data.repo

import com.example.buildlearn.data.db.UserDao
import com.example.buildlearn.data.db.UserEntity
import com.example.buildlearn.data.prefs.SessionStore
import com.example.buildlearn.data.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthResult {
    data class Success(val userId: Long) : AuthResult
    data class Error(val message: String) : AuthResult
}

/** 本地账号的注册、登录与退出。 */
@Singleton
class AuthRepository @Inject constructor(
    private val users: UserDao,
    private val session: SessionStore
) {
    val currentUserId: Flow<Long?> = session.currentUserId

    suspend fun register(username: String, password: String): AuthResult {
        val name = username.trim()
        validateUsername(name)?.let { return AuthResult.Error(it) }
        validatePassword(password)?.let { return AuthResult.Error(it) }
        if (users.findByName(name) != null) {
            return AuthResult.Error("该用户名已被注册")
        }
        val salt = PasswordHasher.newSalt()
        val id = users.insert(
            UserEntity(
                username = name,
                passwordHash = PasswordHasher.hash(password, salt),
                salt = salt,
                createdAt = System.currentTimeMillis()
            )
        )
        session.setCurrentUserId(id)
        return AuthResult.Success(id)
    }

    suspend fun login(username: String, password: String): AuthResult {
        val user = users.findByName(username.trim())
            ?: return AuthResult.Error("用户名或密码错误")
        val ok = PasswordHasher.verify(password, user.salt, user.passwordHash)
        if (!ok) return AuthResult.Error("用户名或密码错误")
        session.setCurrentUserId(user.id)
        return AuthResult.Success(user.id)
    }

    suspend fun logout() {
        session.setCurrentUserId(null)
    }

    suspend fun findUsername(userId: Long): String? = users.findById(userId)?.username

    private fun validateUsername(name: String): String? = when {
        name.length !in 2..20 -> "用户名需为 2–20 个字符"
        else -> null
    }

    private fun validatePassword(password: String): String? = when {
        password.length < 6 -> "密码至少 6 位"
        else -> null
    }
}
