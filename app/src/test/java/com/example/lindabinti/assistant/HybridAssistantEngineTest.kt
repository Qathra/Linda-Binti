package com.example.lindabinti.assistant

import com.example.lindabinti.data.content.AssistantKnowledge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridAssistantEngineTest {

    private fun offlineEngine(): AssistantEngine = object : AssistantEngine {
        override suspend fun answer(
            userText: String,
            history: List<AssistantChatTurn>
        ): AssistantMessage {
            val reply = AssistantKnowledge.answer(userText, "en")
            return AssistantMessage(
                text = reply.text,
                relatedAction = reply.relatedAction,
                source = AssistantSource.Local,
                riskLevel = reply.riskLevel
            )
        }
    }

    @Test
    fun fallsBackToOfflineWhenRemoteReturnsNull() = runBlocking {
        val engine = HybridAssistantEngine(
            local = offlineEngine(),
            remote = object : AssistantRemoteClient {
                override suspend fun answer(
                    userText: String,
                    history: List<AssistantChatTurn>
                ): AssistantMessage? = null
            },
            policy = AssistantRoutingPolicy(
                isBackendConfigured = { true },
                isNetworkAvailable = { true },
                hasCloudAiConsent = { true },
                isUserSessionActive = { true },
                isOnlineAssistantConfigured = { true }
            )
        )
        val result = engine.answer("Where can I report GBV?")
        assertEquals(AssistantSource.Local, result.source)
        assertTrue(result.text.contains("1195") || result.text.contains("Reporting"))
    }

    @Test
    fun usesOnlineWhenRemoteReturnsReply() = runBlocking {
        val engine = HybridAssistantEngine(
            local = offlineEngine(),
            remote = object : AssistantRemoteClient {
                override suspend fun answer(
                    userText: String,
                    history: List<AssistantChatTurn>
                ): AssistantMessage? = AssistantMessage(
                    text = "Online safety guidance for your question.",
                    relatedAction = "emergency",
                    source = AssistantSource.Remote,
                    riskLevel = "HIGH"
                )
            },
            policy = AssistantRoutingPolicy(
                isBackendConfigured = { true },
                isNetworkAvailable = { true },
                hasCloudAiConsent = { true },
                isUserSessionActive = { true },
                isOnlineAssistantConfigured = { true }
            )
        )
        val result = engine.answer("I am scared to go home")
        assertEquals(AssistantSource.Remote, result.source)
        assertEquals("ONLINE AI", result.source.modeLabel)
    }

    @Test
    fun staysOfflineWithoutConsent() = runBlocking {
        val engine = HybridAssistantEngine(
            local = offlineEngine(),
            remote = object : AssistantRemoteClient {
                override suspend fun answer(
                    userText: String,
                    history: List<AssistantChatTurn>
                ): AssistantMessage? = AssistantMessage("Should not be used")
            },
            policy = AssistantRoutingPolicy(
                isBackendConfigured = { true },
                isNetworkAvailable = { true },
                hasCloudAiConsent = { false },
                isUserSessionActive = { true },
                isOnlineAssistantConfigured = { true }
            )
        )
        val result = engine.answer("My husband keeps threatening me")
        assertEquals(AssistantSource.Local, result.source)
        assertTrue(result.relatedAction == "emergency" || result.text.contains("1195"))
    }
}
