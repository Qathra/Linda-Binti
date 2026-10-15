package com.example.lindabinti.ai

import com.example.lindabinti.data.remote.AiCapabilitiesResponseDto
import com.example.lindabinti.data.remote.AuthResponseDto
import com.example.lindabinti.data.remote.BackendSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpOnlineAiClientTest {

    private val session = object : BackendSession {
        override fun saveSession(auth: AuthResponseDto) = Unit
        override fun clear() = Unit
        override fun isLoggedIn(): Boolean = true
        override fun bearer(): String? = "Bearer test-token"
        override fun refreshToken(): String? = "refresh-test"
        override fun role(): String? = "USER"
        override fun isNgoRole(): Boolean = false
        override fun isUserRole(): Boolean = true
    }

    @Test
    fun configuredWhenCapabilitiesStatusLabelSaysConfigured() = runBlocking {
        val client = HttpOnlineAiClient(
            api = FakeLindaBintiApi(
                caps = AiCapabilitiesResponseDto(
                    onlineEnabled = false,
                    statusLabel = "ONLINE AI: CONFIGURED"
                )
            ),
            session = session,
            hasCloudAiConsent = { true }
        )
        assertTrue(client.isOnlineAiConfigured())
    }

    @Test
    fun notConfiguredWhenHealthAndCapabilitiesDisabled() = runBlocking {
        val client = HttpOnlineAiClient(
            api = FakeLindaBintiApi(
                caps = AiCapabilitiesResponseDto(
                    onlineEnabled = false,
                    statusLabel = "ONLINE AI: NOT CONFIGURED"
                ),
                healthMap = mapOf(
                    "onlineAiStatus" to "ONLINE AI: NOT CONFIGURED",
                    "ai" to "disabled"
                )
            ),
            session = session,
            hasCloudAiConsent = { false }
        )
        assertFalse(client.isOnlineAiConfigured())
    }
}
