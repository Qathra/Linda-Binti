package com.lindabinti.backend.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * OpenAI-compatible chat completions provider for survivor report analysis.
 */
class OpenAiCompatibleProvider(
    private val config: AiConfig,
    private val http: AiHttpClient,
    private val gson: Gson = Gson()
) : OnlineAiProvider {

    override val providerId: String = "openai-compatible"

    override fun isConfigured(): Boolean = config.onlineEnabled

    override fun analyzeReport(input: AiRiskInput): OnlineReportAnalysisResult {
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
        var lastMessage = "Online AI failed"

        val maxAttempts = config.maxRetries + 1
        for (attempt in 0 until maxAttempts) {
            try {
                val content = callProvider(input)
                val parsed = AiReportAnalysisParser.parse(content, input.categoryLabel)
                val duration = System.currentTimeMillis() - started
                AiAnalysisLogger.info(
                    "analyze_ok",
                    "provider=$providerId model=${config.model} attempt=${attempt + 1} durationMs=$duration category=${parsed.category}"
                )
                return parsed.copy(
                    providerId = providerId,
                    modelId = config.model,
                    processingDurationMs = duration
                )
            } catch (e: InvalidAiResultException) {
                lastCode = "malformed_response"
                lastStatus = AiStatus.INVALID
                lastMessage = e.message ?: "Invalid AI response"
                AiAnalysisLogger.failure(lastCode, lastStatus, "attempt=${attempt + 1}")
                break
            } catch (_: AiTransportException.Timeout) {
                lastCode = "timeout"
                lastStatus = AiStatus.TIMEOUT
                lastMessage = "AI request timed out"
                AiAnalysisLogger.failure(lastCode, lastStatus, "attempt=${attempt + 1}")
                if (attempt < config.maxRetries) continue
            } catch (_: AiTransportException.Network) {
                lastCode = "provider_unavailable"
                lastStatus = AiStatus.UNAVAILABLE
                lastMessage = "AI provider unavailable"
                AiAnalysisLogger.failure(lastCode, lastStatus, "attempt=${attempt + 1}")
                if (attempt < config.maxRetries) continue
            } catch (e: AiProviderException) {
                throw e
            } catch (e: Exception) {
                lastCode = "ai_failed"
                lastStatus = AiStatus.FAILED
                lastMessage = "Online AI failed"
                AiAnalysisLogger.failure(lastCode, lastStatus, "attempt=${attempt + 1}")
                break
            }
        }
        throw AiProviderException(lastCode, lastMessage, lastStatus)
    }

    private fun callProvider(input: AiRiskInput): String {
        val payload = gson.toJson(
            mapOf(
                "model" to config.model,
                "temperature" to 0,
                "response_format" to mapOf("type" to "json_object"),
                "messages" to listOf(
                    mapOf("role" to "system", "content" to REPORT_SYSTEM_PROMPT),
                    mapOf("role" to "user", "content" to userMessage(input))
                )
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
            ?: throw InvalidAiResultException("Missing AI result")
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

    private fun userMessage(input: AiRiskInput): String = buildString {
        append("serviceArea=Kajiado County, Kenya\n")
        append("categoryId=").append(input.categoryId).append('\n')
        append("categoryLabel=").append(input.categoryLabel).append('\n')
        append("description=")
        append(if (input.redactedDescription.isBlank()) "(not provided)" else input.redactedDescription)
        append('\n')
        append("Return JSON only using the required schema.")
    }

    companion object {
        const val REPORT_SYSTEM_PROMPT =
            "You are the Linda Binti online safety analysis assistant for GBV and FGM prevention and response in Kajiado County, Kenya. " +
                "You help survivors and authorized systems prioritize safety — you do NOT diagnose medical conditions, " +
                "provide unsafe medical instructions, impersonate police/courts/counsellors, invent facts, claim certainty when uncertain, " +
                "or automatically contact authorities. " +
                "Do not quote or repeat private description text. Do not use names, phone numbers, or emails. " +
                "If information is insufficient, say so conservatively in the explanation. " +
                "JSON keys: category (GBV|FGM|FGM_THREAT|RELATED|UNKNOWN), severity (CRITICAL|HIGH|ELEVATED|LOWER), " +
                "confidence (0.0-1.0), urgencyIndicators (array of short strings), safetyConcerns (array), " +
                "explanation (1-2 sentences, safety-oriented, no diagnosis), recommendations (array of safe actions), " +
                "educationRecommendations (array of topic ids such as fgm, gbv_basics, safety_planning, know_rights, child_protection, forced_marriage)."
    }
}

object AiProviderFactory {
    fun createOnlineProvider(config: AiConfig, http: AiHttpClient = JdkAiHttpClient()): OnlineAiProvider =
        OpenAiCompatibleProvider(config, http)
}
