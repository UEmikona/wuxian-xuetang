package com.example.buildlearn.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 密码哈希工具，使用 PBKDF2-HMAC-SHA256 加盐迭代哈希。
 *
 * 注意：这是**本地账号**的保护方式，只能防止明文落盘。
 * 如果将来接入云端账号，应改用服务端认证（例如 Firebase Auth），不要把此方案用于服务器。
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    private val random = SecureRandom()

    /** 生成随机盐，返回 Base64 字符串。 */
    fun newSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    /** 用给定的盐计算密码哈希，返回 Base64 字符串。 */
    fun hash(password: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        try {
            val key = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
            return Base64.getEncoder().encodeToString(key)
        } finally {
            spec.clearPassword()
        }
    }

    /** 校验密码是否与已保存的哈希一致。使用恒定时间比较，避免时序攻击。 */
    fun verify(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val actual = hash(password, saltBase64).toByteArray()
        val expected = expectedHashBase64.toByteArray()
        return MessageDigest.isEqual(actual, expected)
    }
}
