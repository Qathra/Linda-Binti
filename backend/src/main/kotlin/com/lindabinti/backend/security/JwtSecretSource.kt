package com.lindabinti.backend.security

import java.io.File
import java.security.SecureRandom

/**
 * Resolves the JWT HMAC secret from the environment or a local file.
 * Never uses the well-known source-code placeholder (that would let anyone forge tokens).
 */
object JwtSecretSource {

    private val forbidden = setOf(
        "dev-only-change-me-linda-binti-phase1-secret-key!!",
        "test-secret-key-linda-binti-phase1-32b"
    )

    fun resolve(envValue: String?, dataDir: File): String {
        val fromEnv = envValue?.trim().orEmpty()
        if (fromEnv.length >= 32 && fromEnv !in forbidden) {
            return fromEnv
        }
        if (fromEnv.isNotEmpty()) {
            System.err.println(
                "LINDA_JWT_SECRET is missing, too short, or matches a published placeholder. " +
                    "Generating a local file secret instead."
            )
        }
        dataDir.mkdirs()
        val file = File(dataDir, "jwt.secret")
        if (file.isFile) {
            val existing = file.readText().trim()
            if (existing.length >= 32 && existing !in forbidden) {
                return existing
            }
        }
        val generated = generate()
        file.writeText(generated)
        System.err.println(
            "JWT secret stored at ${file.absolutePath}. Set LINDA_JWT_SECRET for production. " +
                "The secret value is not printed."
        )
        return generated
    }

    fun generate(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
