package com.restaurant.management.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private val random = SecureRandom()

    fun newSalt(): ByteArray = ByteArray(16).also { random.nextBytes(it) }

    fun hash(
        password: CharArray,
        salt: ByteArray,
    ): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun encodeB64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun decodeB64(s: String): ByteArray =
        Base64.decode(s, Base64.NO_WRAP)

    fun verify(
        password: CharArray,
        saltB64: String,
        hashB64: String,
    ): Boolean {
        val salt = decodeB64(saltB64)
        val expected = decodeB64(hashB64)
        val actual = hash(password, salt)
        return actual.contentEquals(expected)
    }
}
