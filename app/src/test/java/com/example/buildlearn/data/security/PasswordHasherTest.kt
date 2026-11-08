package com.example.buildlearn.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun correctPassword_verifies() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("secret123", salt)
        assertTrue(PasswordHasher.verify("secret123", salt, hash))
    }

    @Test
    fun wrongPassword_fails() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("secret123", salt)
        assertFalse(PasswordHasher.verify("secret124", salt, hash))
    }

    @Test
    fun samepassword_withDifferentSalt_producesDifferentHash() {
        val hashA = PasswordHasher.hash("secret123", PasswordHasher.newSalt())
        val hashB = PasswordHasher.hash("secret123", PasswordHasher.newSalt())
        assertNotEquals(hashA, hashB)
    }
}
