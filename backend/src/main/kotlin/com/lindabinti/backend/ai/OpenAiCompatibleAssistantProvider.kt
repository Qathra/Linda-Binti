package com.lindabinti.backend.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class OpenAiCompatibleAssistantProvider(
    private val config: AiConfig,
    private val http: AiHttpClient,
    private val gson: Gson = Gson()
) : AssistantChatProvider {

    override val providerId: String = "openai-compatible"

    override fun isConfigured(): Boolean = config.onlineEnabled

    override fun chat(input: AssistantChatInput): AssistantChatResult {
        if (config.enabledFlag == false) {
            throw AiProviderException("ai_disabled", "Online AI is disabled", AiStatus.UNAVAILABLE)
        }
        if (config.apiKey.isBlank()) {
            throw AiProviderException("api_key_missing", "AI API key is not configured", AiStatus.UNAVAILABLE)
        }
        if (config.baseUrl.isBlank()) {
            throw AiProviderException("provider_unconfigured", "AI base URL is not configured", AiStatus.UNAVAILABLE)
        }

        val started = System.currentTimeMillis()
        var lastCode = "ai_failed"
        var lastStatus = AiStatus.FAILED
        var lastMessage = "Assistant unavailable"

        val maxAttempts = config.maxRetries + 1
        for (attempt in 0 until maxAttempts) {
            try {
                val content = callProvider(input)
                val parsed = AssistantChatParser.parse(content)
                val duration = System.currentTimeMillis() - started
                AiAnalysisLogger.info(
                    "assistant_ok",
                    "provider=$providerId attempt=${attempt + 1} durationMs=$duration risk=${parsed.riskLevel}"
                )
                return parsed.copy(
                    providerId = providerId,
                    modelId = config.model,
                    processingDurationMs = duration
                )
            } catch (e: InvalidAiResultException) {
                lastCode = "malformed_response"
                lastStatus = AiStatus.INVALID
                lastMessage = e.message ?: "Invalid assistant response"
                AiAnalysisLogger.failure(lastCode, lastStatus, "attempt=${attempt + 1}")
                break
            } catch (_: AiTransportException.Timeout) {
                lastCode = "timeout"
                lastStatus = AiStatus.TIMEOUT
                lastMessage = "Assistant request timed out"
                AiAnalysisLogger.failure(lastCode, lastStatus, "attempt=${attempt + 1}")
                if (attempt < config.maxRetries) continue
            } catch (_: AiTransportException.Network) {
                lastCode = "provider_unavailable"
                lastStatus = AiStatus.UNAVAILABLE
                lastMessage = "Assistant provider unavailable"
                AiAnalysisLogger.failure(lastCode, lastStatus, "attempt=${attempt + 1}")
                if (attempt < config.maxRetries) continue
            } catch (e: AiProviderException) {
                if (e.code == "rate_limited" && attempt < config.maxRetries) {
                    AiAnalysisLogger.failure(e.code, e.aiStatus, "attempt=${attempt + 1};retrying")
                    Thread.sleep(1_500L * (attempt + 1))
                    continue
                }
                throw e
            } catch (e: Exception) {
                lastCode = "ai_failed"
                lastStatus = AiStatus.FAILED
                lastMessage = "Assistant failed"
                AiAnalysisLogger.failure(lastCode, lastStatus, "attempt=${attempt + 1}")
                break
            }
        }
        throw AiProviderException(lastCode, lastMessage, lastStatus)
    }

    private fun callProvider(input: AssistantChatInput): String {
        val messages = buildList {
            add(mapOf("role" to "system", "content" to ASSISTANT_SYSTEM_PROMPT))
            input.history.forEach { turn ->
                add(mapOf("role" to turn.role, "content" to turn.content))
            }
            add(mapOf("role" to "user", "content" to userMessage(input)))
        }
        val payload = gson.toJson(
            mapOf(
                "model" to config.model,
                "temperature" to 0.45,
                "response_format" to mapOf("type" to "json_object"),
                "messages" to messages
            )
        )
        val url = config.baseUrl.trimEnd('/') + "/v1/chat/completions"
        val response = try {
            http.postJson(
                url = url,
                headers = mapOf(
                    "Authorization" to "Bearer ${config.apiKey}",
                    "Content-Type" to "application/json"
                ),
                body = payload,
                timeoutMs = config.timeoutMs
            )
        } catch (e: java.net.SocketTimeoutException) {
            throw AiTransportException.Timeout(e)
        } catch (e: java.io.IOException) {
            throw AiTransportException.Network(e)
        }
        if (response.statusCode !in 200..299) {
            HttpAiErrorMapper.ensureSuccess(response.statusCode, response.body)
        }
        return extractMessageContent(response.body)
            ?: throw InvalidAiResultException("Missing assistant result")
    }

    private fun userMessage(input: AssistantChatInput): String = buildString {
        append("serviceArea=Kajiado County, Kenya\n")
        append("language=").append(input.language).append('\n')
        if (input.riskHint.isNotBlank() && !input.riskHint.equals("NONE", ignoreCase = true)) {
            append("riskHint=").append(input.riskHint).append('\n')
        }
        if (input.retrievedContext.isNotBlank()) {
            append(input.retrievedContext).append('\n')
        }
        append("message=").append(input.message).append('\n')
        append("Ground answers in CURATED_KNOWLEDGE when present. ")
        append("If riskHint is HIGH or CRITICAL, prioritize immediate safety and relatedAction=emergency. ")
        append("Return JSON only using the required schema.")
    }

    private fun extractMessageContent(body: String): String? {
        return try {
            val root = JsonParser.parseString(body).asJsonObject
            val choices = root.getAsJsonArray("choices") ?: return null
            if (choices.size() == 0) return null
            val message = (choices[0] as JsonObject).getAsJsonObject("message") ?: return null
            val content = message.get("content") ?: return null
            if (!content.isJsonPrimitive) return null
            content.asString
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val ASSISTANT_SYSTEM_PROMPT =
            "You are Linda Binti, a compassionate AI safety companion for survivors and women at risk in Kajiado County, Kenya. " +
                "You specialize in GBV and FGM prevention, response, rights, and wellbeing — but you are NOT limited to canned scripts. " +
                "Answer naturally like a thoughtful chat assistant: follow the user's question, clarify when needed, and give practical next steps. " +
                "You are NOT a therapist, lawyer, doctor, or police officer. Do not diagnose, prescribe medical treatment, give legal verdicts, " +
                "or impersonate authorities. Do not invent services, facilities, or phone numbers. " +
                "Only cite these verified emergency numbers when needed: 999, 112 (police emergency), 1195 (GBV helpline), 116 (Childline). " +
                "Never reveal, repeat, or guess any SOS code, Access PIN, or app unlock secret — explain that the user sets these during setup. " +
                "For high-risk statements (FGM threat, imminent violence, fear of going home, threats with weapons), prioritize immediate safety: " +
                "Emergency Assistance in the app, trusted contacts, and verified hotlines. " +
                "You may discuss general life, school, relationships, stress, and safety planning when relevant to the user's wellbeing in Kajiado context. " +
                "Keep replies clear and supportive (usually 2-6 sentences unless the user asks for more). " +
                "If uncertain, say so conservatively. " +
                "JSON keys: reply (string), relatedAction (emergency|education|support|reporting|contacts|settings|null), " +
                "riskLevel (CRITICAL|HIGH|MEDIUM|LOW|NONE)."
    }
}

object AssistantChatProviderFactory {
    fun create(config: AiConfig, http: AiHttpClient = JdkAiHttpClient()): AssistantChatProvider {
        val cloud = OpenAiCompatibleAssistantProvider(config, http)
        return if (config.onlineEnabled) {
            FallbackAssistantChatProvider(cloud)
        } else {
            RulesAssistantChatProvider()
        }
    }
}
