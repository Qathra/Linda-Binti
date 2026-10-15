package com.example.lindabinti.data.repository

import com.example.lindabinti.data.prefs.SecurePrefs
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryLockoutTest {

    @Test
    fun lockoutConstantsAreReasonable() {
        assertTrue(SecurePrefs.MAX_PIN_FAILURES >= 3)
        assertTrue(SecurePrefs.PIN_LOCK_DURATION_MS >= 30_000L)
    }
}
