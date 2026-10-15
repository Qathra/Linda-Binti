package com.lindabinti.backend.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AiConfigTest {

    @Test
    fun configuredWhenEnabledFlagAndCredentialsPresent() {
        val config = AiConfig(
            apiKey = "secret",
            baseUrl = "https://api.openai.com",
            model = "gpt-4o-mini",
            timeoutMs = 8000,
            maxRetries = 1,
            enabledFlag = true
        )
        assertEquals("ONLINE AI: CONFIGURED", config.onlineStatusLabel)
        assertFalse(config.configurationReason.contains("secret"))
    }

    @Test
    fun notConfiguredWhenDisabled() {
        val config = AiConfig("", "", "gpt-4o-mini", 8000, 0, false)
        assertEquals("ONLINE AI: NOT CONFIGURED", config.onlineStatusLabel)
        assertEquals("LINDA_AI_ENABLED is off", config.configurationReason)
    }

    @Test
    fun notConfiguredWhenKeyMissing() {
        val config = AiConfig("", "https://api.openai.com", "gpt-4o-mini", 8000, 0, true)
        assertEquals("ONLINE AI: NOT CONFIGURED", config.onlineStatusLabel)
        assertEquals("LINDA_AI_API_KEY is missing", config.configurationReason)
    }
}
