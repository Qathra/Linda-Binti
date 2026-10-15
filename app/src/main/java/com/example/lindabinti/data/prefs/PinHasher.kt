package com.example.lindabinti.data.prefs

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted hashing for Access Code and SOS code.
 * New hashes use PBKDF2-HMAC-SHA256. Legacy `salt:sha256` hashes still verify
 * so existing installs keep working after upgrade.
 *
 * New hashes use 12_000 iterations so Equals on the calculator stays responsive.
 * Stored hashes still verify with the iteration count embedded in the string
 * (including older 120_000 hashes).
 */
object PinHasher {

    private const val PBKDF2_PREFIX = "pbkdf2"
    private const val PBKDF2_ITERATIONS = 12_000
    private const val KEY_LENGTH_BITS = 256

    fun hash(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val derived = pbkdf2(pin, salt, PBKDF2_ITERATIONS)
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hashB64 = Base64.getEncoder().encodeToString(derived)
        return "$PBKDF2_PREFIX:$PBKDF2_ITERATIONS:$saltB64:$hashB64"
    }

    fun verify(pin: String, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return false
        return runCatching {
            when {
                stored.startsWith("$PBKDF2_PREFIX:") -> verifyPbkdf2(pin, stored)
                else -> verifyLegacySha256(pin, stored)
            }
        }.getOrDefault(false)
    }

    fun isPbkdf2Format(stored: String?): Boolean =
        stored?.startsWith("$PBKDF2_PREFIX:") == true

    private fun verifyPbkdf2(pin: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 4) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(parts[3]) }.getOrNull() ?: return false
        val actual = pbkdf2(pin, salt, iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun verifyLegacySha256(pin: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = runCatching { Base64.getDecoder().decode(parts[0]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(parts[1]) }.getOrNull() ?: return false
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val actual = md.digest(pin.toByteArray(Charsets.UTF_8))
        return MessageDigest.isEqual(expected, actual)
    }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations.coerceAtLeast(1), KEY_LENGTH_BITS)
        val factory = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (_: Exception) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }
        return factory.generateSecret(spec).encoded
    }
}
