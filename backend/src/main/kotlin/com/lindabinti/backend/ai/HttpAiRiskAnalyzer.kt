package com.lindabinti.backend.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * OpenAI-compatible chat completions client. API key is supplied at runtime from backend env — never the Android app.
 */
class HttpAiRiskAnalyzer(
    private val config: AiConfig,
    private val http: AiHttpClient,
    private val gson: Gson = Gson()
) : AiRiskAnalyzer {

    override fun analyze(input: AiRiskInput): AiRiskResult {
        if (!config.onlineEnabled) {
            throw AiTransportException.Network()
        }
        val payload = gson.toJson(
            mapOf(
                "model" to config.model,
                "temperature" to 0,
                "response_format" to mapOf("type" to "json_object"),
                "messages" to listOf(
                    mapOf("role" to "system", "content" to SYSTEM_PROMPT),
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
        } catch (e: AiTransportException) {
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            throw AiTransportException.Timeout(e)
        } catch (e: java.io.IOException) {
            throw AiTransportException.Network(e)
        }
        if (response.statusCode !in 200..299) {
            HttpAiErrorMapper.ensureSuccess(response.statusCode, response.body)
        }
        val content = extractMessageContent(response.body)
            ?: throw InvalidAiResultException("Missing AI result")
        val parsed = AiRiskResultParser.parse(content, input.categoryLabel)
        return parsed.copy(
            explanation = AiRiskResultParser.stripQuotedInput(parsed.explanation, input.redactedDescription)
        )
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
        append("categoryId=").append(input.categoryId).append('\n')
        append("categoryLabel=").append(input.categoryLabel).append('\n')
        append("description=")
        if (input.redactedDescription.isBlank()) {
            append("(not provided)")
        } else {
            append(input.redactedDescription)
        }
        append('\n')
        append("Return JSON only.")
    }

    companion object {
        const val SYSTEM_PROMPT =
            "You help authorized GBV/FGM responders in Kajiado prioritize incident reports. " +
                "You do not diagnose mental health. You do not decide guilt or innocence. " +
                "You do not replace police, doctors, or counsellors. You do not make legal decisions. " +
                "Do not quote the description. Do not use names, phone numbers, or emails. " +
                "JSON keys: riskLevel (CRITICAL|HIGH|MEDIUM|LOW), gbvFgmRelevance (GBV|FGM|BOTH|UNCLEAR), " +
                "incidentCategory, urgencyIndicators (array), safetyConcerns (array), " +
                "recommendedPriority (CRITICAL|HIGH|MEDIUM|LOW), explanation (one or two short sentences)."
    }
}
