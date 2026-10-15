package com.lindabinti.backend.ai

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.util.UUID

/**
 * Parses extended online report analysis JSON for the survivor analyze endpoint.
 */
object AiReportAnalysisParser {
    private val allowedCategories = setOf("GBV", "FGM", "FGM_THREAT", "RELATED", "UNKNOWN")
    private val allowedSeverity = setOf("CRITICAL", "HIGH", "ELEVATED", "LOWER")

    fun parse(rawJson: String, fallbackCategoryLabel: String = ""): OnlineReportAnalysisResult {
        val json = extractObject(rawJson)
        val category = normalizeCategory(
            json.getString("category")
                ?: json.getString("incidentCategory")
                ?: mapRelevanceToCategory(json.getString("gbvFgmRelevance"))
        )
        val severity = normalizeSeverity(
            json.getString("severity")
                ?: json.getString("riskLevel")
                ?: json.getString("recommendedPriority")
                ?: json.getString("urgency")
        )
        val confidence = json.getDouble("confidence")?.coerceIn(0.0, 1.0) ?: estimateConfidence(json)
        val indicators = mutableListOf<String>()
        indicators += json.stringList("urgencyIndicators")
        indicators += json.stringList("indicators")
        indicators += json.stringList("safetyConcerns")
        var explanation = json.getString("explanation")?.trim().orEmpty()
        if (explanation.isBlank()) {
            throw InvalidAiResultException("Missing explanation")
        }
        explanation = AiRiskResultParser.sanitizeExplanation(explanation)
        val recommendations = json.stringList("recommendations").ifEmpty {
            defaultRecommendations(category, severity)
        }
        val education = json.stringList("educationRecommendations").ifEmpty {
            defaultEducation(category)
        }
        return OnlineReportAnalysisResult(
            analysisId = UUID.randomUUID().toString(),
            category = category,
            severity = severity,
            confidence = confidence,
            indicators = indicators.distinct().take(8),
            explanation = explanation,
            recommendations = recommendations.take(6),
            educationRecommendations = education.take(6),
            timestamp = System.currentTimeMillis(),
            providerId = "",
            modelId = "",
            processingDurationMs = 0L,
            aiStatus = AiStatus.COMPLETE
        )
    }

    private fun normalizeCategory(raw: String?): String {
        val v = raw?.trim()?.uppercase()?.replace(' ', '_') ?: return "UNKNOWN"
        if (v in allowedCategories) return v
        return when {
            v.contains("FGM") && v.contains("THREAT") -> "FGM_THREAT"
            v.contains("FGM") -> "FGM"
            v.contains("GBV") -> "GBV"
            v.contains("FORCED") || v.contains("MARRIAGE") -> "RELATED"
            else -> "UNKNOWN"
        }
    }

    private fun mapRelevanceToCategory(relevance: String?): String = when (AiRelevance.parse(relevance)) {
        AiRelevance.FGM -> "FGM"
        AiRelevance.BOTH -> "RELATED"
        AiRelevance.GBV -> "GBV"
        else -> "UNKNOWN"
    }

    private fun normalizeSeverity(raw: String?): String {
        val parsed = AiRiskLevel.parse(raw)
        return when (parsed) {
            AiRiskLevel.CRITICAL -> "CRITICAL"
            AiRiskLevel.HIGH -> "HIGH"
            AiRiskLevel.MEDIUM -> "ELEVATED"
            AiRiskLevel.LOW -> "LOWER"
            else -> "ELEVATED"
        }
    }

    private fun estimateConfidence(json: JsonObject): Double {
        var score = 0.65
        if (json.stringList("urgencyIndicators").isNotEmpty()) score += 0.1
        if (json.getString("explanation")?.isNotBlank() == true) score += 0.05
        return score.coerceIn(0.35, 0.95)
    }

    private fun defaultRecommendations(category: String, severity: String): List<String> {
        val out = mutableListOf<String>()
        if (severity == "CRITICAL") {
            out += "If you are in immediate danger, use Emergency Assistance or call 999/112."
        }
        when (category) {
            "FGM", "FGM_THREAT" -> {
                out += "Consider seeking immediate protection for the person at risk."
                out += "Contact a trusted GBV/FGM support service in Kajiado."
            }
            "GBV" -> out += "Reach a safe place if possible and contact a trusted support service."
            "RELATED" -> out += "Forced marriage linked to GBV/FGM may need urgent referral."
            else -> out += "Review Education content for GBV/FGM safety information."
        }
        return out
    }

    private fun defaultEducation(category: String): List<String> = when (category) {
        "FGM", "FGM_THREAT" -> listOf("fgm", "child_protection", "know_rights")
        "GBV" -> listOf("gbv_basics", "safety_planning", "know_rights")
        "RELATED" -> listOf("forced_marriage", "know_rights")
        else -> listOf("gbv_basics")
    }

    private fun extractObject(raw: String): JsonObject {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) throw InvalidAiResultException("Missing AI result")
        val jsonText = if (trimmed.startsWith("```")) {
            trimmed.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
                .substringBeforeLast("```").trim()
        } else trimmed
        return try {
            val parsed = JsonParser.parseString(jsonText)
            if (!parsed.isJsonObject) throw InvalidAiResultException("AI result is not a JSON object")
            parsed.asJsonObject
        } catch (e: JsonSyntaxException) {
            throw InvalidAiResultException("Invalid AI JSON")
        }
    }

    private fun JsonObject.getString(key: String): String? =
        if (has(key) && get(key).isJsonPrimitive) get(key).asString else null

    private fun JsonObject.getDouble(key: String): Double? =
        if (has(key) && get(key).isJsonPrimitive) runCatching { get(key).asDouble }.getOrNull() else null

    private fun JsonObject.stringList(key: String): List<String> {
        if (!has(key)) return emptyList()
        val el = get(key)
        return when {
            el.isJsonArray -> el.asJsonArray.mapNotNull {
                if (it.isJsonPrimitive) it.asString.trim().take(160).ifBlank { null } else null
            }
            el.isJsonPrimitive -> el.asString.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
    }
}
