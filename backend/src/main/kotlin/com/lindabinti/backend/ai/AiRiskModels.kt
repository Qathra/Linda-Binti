package com.lindabinti.backend.ai

/**
 * Canonical responder priority. Not a diagnosis, verdict, or legal decision.
 */
object AiRiskLevel {
    const val CRITICAL = "CRITICAL"
    const val HIGH = "HIGH"
    const val MEDIUM = "MEDIUM"
    const val LOW = "LOW"

    private val allowed = setOf(CRITICAL, HIGH, MEDIUM, LOW)

    fun parse(raw: String?): String? {
        val v = raw?.trim()?.uppercase() ?: return null
        return when (v) {
            CRITICAL, "CRIT" -> CRITICAL
            HIGH -> HIGH
            MEDIUM, "ELEVATED", "MODERATE" -> MEDIUM
            LOW -> LOW
            else -> null
        }
    }

    fun requireValid(raw: String?): String =
        parse(raw) ?: throw InvalidAiResultException("Unknown risk level: $raw")

    fun isValid(raw: String?): Boolean = parse(raw) != null

    fun sortRank(raw: String): Int = when (parse(raw)) {
        CRITICAL -> 0
        HIGH -> 1
        MEDIUM -> 2
        LOW -> 3
        else -> 4
    }
}

object AiRelevance {
    const val GBV = "GBV"
    const val FGM = "FGM"
    const val BOTH = "BOTH"
    const val UNCLEAR = "UNCLEAR"

    fun parse(raw: String?): String {
        val v = raw?.trim()?.uppercase() ?: return UNCLEAR
        return when {
            v == BOTH || (v.contains("GBV") && v.contains("FGM")) -> BOTH
            v.contains("FGM") -> FGM
            v.contains("GBV") -> GBV
            v == UNCLEAR -> UNCLEAR
            else -> UNCLEAR
        }
    }
}

object AiStatus {
    const val PENDING = "pending"
    const val COMPLETE = "complete"
    const val UNAVAILABLE = "unavailable"
    const val FAILED = "failed"
    const val INVALID = "invalid"
    const val TIMEOUT = "timeout"
    const val MISSING = "missing"
}

data class AiRiskInput(
    val categoryId: String,
    val categoryLabel: String,
    val redactedDescription: String
)

data class AiRiskResult(
    val riskLevel: String,
    val gbvFgmRelevance: String,
    val incidentCategory: String,
    val urgencyIndicators: List<String>,
    val safetyConcerns: List<String>,
    val recommendedPriority: String,
    val explanation: String,
    val source: String,
    val status: String
) {
    fun indicatorsText(): String = urgencyIndicators.joinToString("; ").take(500)
    fun concernsText(): String = safetyConcerns.joinToString("; ").take(500)
}

class InvalidAiResultException(message: String) : RuntimeException(message)

sealed class AiTransportException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class Timeout(cause: Throwable? = null) : AiTransportException("AI request timed out", cause)
    class Network(cause: Throwable? = null) : AiTransportException("AI network failure", cause)
}

interface AiHttpClient {
    fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse
}

data class AiHttpResponse(val statusCode: Int, val body: String)

interface AiRiskAnalyzer {
    fun analyze(input: AiRiskInput): AiRiskResult
}

data class AiConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val timeoutMs: Int,
    val maxRetries: Int,
    val enabledFlag: Boolean?
) {
    /** Online AI is available only when explicitly allowed and credentials are present. */
    val onlineEnabled: Boolean
        get() = when (enabledFlag) {
            false -> false
            true -> apiKey.isNotBlank() && baseUrl.isNotBlank()
            null -> apiKey.isNotBlank() && baseUrl.isNotBlank()
        }

    /** Legacy NGO report ingest path. */
    val enabled: Boolean get() = onlineEnabled

    /** Safe startup label — never includes the API key. */
    val onlineStatusLabel: String
        get() = if (onlineEnabled) "ONLINE AI: CONFIGURED" else "ONLINE AI: NOT CONFIGURED"

    /** Human-readable reason when not configured (no secrets). */
    val configurationReason: String
        get() = when {
            enabledFlag == false -> "LINDA_AI_ENABLED is off"
            apiKey.isBlank() -> "LINDA_AI_API_KEY is missing"
            baseUrl.isBlank() -> "LINDA_AI_BASE_URL is missing"
            onlineEnabled -> "OpenAI-compatible provider ready (model=$model)"
            else -> "Online AI not configured"
        }

    companion object {
        fun fromEnv(): AiConfig {
            fun env(key: String): String =
                System.getenv(key)?.takeIf { it.isNotBlank() }
                    ?: System.getProperty(key)?.takeIf { it.isNotBlank() }
                    ?: ""

            val enabledRaw = env("LINDA_AI_ENABLED").trim().lowercase()
            val enabledFlag = when (enabledRaw) {
                "1", "true", "yes", "on" -> true
                "0", "false", "no", "off" -> false
                else -> null
            }
            return AiConfig(
                apiKey = env("LINDA_AI_API_KEY").trim(),
                baseUrl = env("LINDA_AI_BASE_URL").trim().trimEnd('/'),
                model = env("LINDA_AI_MODEL").trim().ifBlank { "gpt-4o-mini" },
                timeoutMs = env("LINDA_AI_TIMEOUT_MS").toIntOrNull()?.coerceIn(2_000, 30_000) ?: 8_000,
                maxRetries = env("LINDA_AI_MAX_RETRIES").toIntOrNull()?.coerceIn(0, 3) ?: 1,
                enabledFlag = enabledFlag
            )
        }
    }
}
