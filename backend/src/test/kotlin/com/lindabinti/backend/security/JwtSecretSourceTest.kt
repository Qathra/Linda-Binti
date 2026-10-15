package com.lindabinti.backend.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class JwtSecretSourceTest {

    @Test
    fun usesEnvWhenLongAndNotAPublishedPlaceholder() {
        val dir = createTempDirectory("jwt-secret").toFile()
        val secret = "a".repeat(32) + "-unique-env-secret"
        assertEquals(secret, JwtSecretSource.resolve(secret, dir))
        assertFalse(File(dir, "jwt.secret").exists())
    }

    @Test
    fun rejectsPublishedPlaceholderAndPersistsGeneratedSecret() {
        val dir = createTempDirectory("jwt-secret").toFile()
        val generated = JwtSecretSource.resolve(
            "dev-only-change-me-linda-binti-phase1-secret-key!!",
            dir
        )
        assertTrue(generated.length >= 32)
        assertNotEquals("dev-only-change-me-linda-binti-phase1-secret-key!!", generated)
        val file = File(dir, "jwt.secret")
        assertTrue(file.isFile)
        assertEquals(generated, file.readText().trim())
        assertEquals(generated, JwtSecretSource.resolve(null, dir))
    }
}
