package com.lindabinti.backend.ai

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException

/**
 * Parses a structured AI JSON object. Rejects diagnoses, guilt findings, and missing fields.
 */
object AiRiskResultParser {
    private val banned = listOf(
        "diagnosed", "ptsd", "schizophrenia", "bipolar", "guilty", "innocent",
        "convicted", "you have depression", "clinical diagnosis"
    )
    private val disclaimer =
        "This is a priority aid for authorized responders, not a medical, legal, police, or counselling diagnosis."

    fun parse(rawJson: String, fallbackCategory: String = ""): AiRiskResult {
        val json = extractObject(rawJson)
        val level = AiRiskLevel.requireValid(json.getString("riskLevel") ?: json.getString("urgency"))
        val relevance = AiRelevance.parse(json.getString("gbvFgmRelevance") ?: json.getString("relevance"))
        val category = json.getString("incidentCategory")
            ?.trim()
            ?.take(128)
            ?.ifBlank { fallbackCategory }
            ?: fallbackCategory.ifBlank { "GBV/FGM-related incident" }
        val indicators = json.stringList("urgencyIndicators")
        val concerns = json.stringList("safetyConcerns")
        val priority = AiRiskLevel.parse(json.getString("recommendedPriority")) ?: level
        var explanation = json.getString("explanation")?.trim().orEmpty()
        if (explanation.isBlank()) {
            throw InvalidAiResultException("Missing explanation")
        }
        explanation = sanitizeExplanation(explanation)
        return AiRiskResult(
            riskLevel = level,
            gbvFgmRelevance = relevance,
            incidentCategory = category,
            urgencyIndicators = indicators.take(6),
            safetyConcerns = concerns.take(6),
            recommendedPriority = priority,
            explanation = explanation,
            source = "ai",
            status = AiStatus.COMPLETE
        )
    }

    fun sanitizeExplanation(text: String): String {
        var out = text.replace('\n', ' ').trim().take(400)
        val lower = out.lowercase()
        if (banned.any { lower.contains(it) }) {
            out = "Priority assigned from GBV/FGM incident signals without a clinical or legal finding. $disclaimer"
        } else if (!lower.contains("not a medical") && !lower.contains("not a diagnosis")) {
            out = "$out $disclaimer"
        }
        return out.trim()
    }

    fun stripQuotedInput(explanation: String, redactedDescription: String): String {
        if (redactedDescription.length < 24) return explanation
        val snippet = redactedDescription.take(24)
        return if (explanation.contains(snippet, ignoreCase = true)) {
            "Priority assigned from category and urgency indicators. Private report text is not repeated. $disclaimer"
        } else explanation
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

    private fun JsonObject.stringList(key: String): List<String> {
        if (!has(key)) return emptyList()
        val el = get(key)
        return when {
            el.isJsonArray -> el.asJsonArray.mapNotNull {
                if (it.isJsonPrimitive) it.asString.trim().take(120).ifBlank { null } else null
            }
            el.isJsonPrimitive -> el.asString.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
    }
}
