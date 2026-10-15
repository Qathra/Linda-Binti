package com.lindabinti.backend.ai

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException

object AssistantChatParser {
    private val allowedActions = setOf("emergency", "education", "support", "reporting", "contacts", "settings")
    private val allowedRisk = setOf("CRITICAL", "HIGH", "MEDIUM", "LOW", "NONE")

    fun parse(rawJson: String): AssistantChatResult {
        val json = extractObject(rawJson)
        var reply = json.getString("reply")?.trim().orEmpty()
        if (reply.isBlank()) {
            reply = json.getString("explanation")?.trim().orEmpty()
        }
        if (reply.isBlank()) throw InvalidAiResultException("Missing assistant reply")
        reply = sanitizeReply(reply)
        val action = json.getString("relatedAction")?.trim()?.lowercase()?.takeIf { it in allowedActions }
        val risk = normalizeRisk(
            json.getString("riskLevel")
                ?: json.getString("severity")
                ?: json.getString("urgency")
        )
        return AssistantChatResult(
            reply = reply,
            relatedAction = action,
            riskLevel = risk,
            providerId = "",
            modelId = "",
            processingDurationMs = 0L
        )
    }

    private fun sanitizeReply(text: String): String {
        var out = AiRiskResultParser.sanitizeExplanation(text)
        // Never echo the default SOS demonstration code.
        out = out.replace("1102", "[redacted]")
        if (out.length > 1200) out = out.take(1200).trim() + "…"
        return out
    }

    private fun normalizeRisk(raw: String?): String {
        val v = raw?.trim()?.uppercase() ?: return "NONE"
        return when {
            v in allowedRisk -> v
            v.contains("CRIT") -> "CRITICAL"
            v.contains("HIGH") -> "HIGH"
            v.contains("MED") || v.contains("ELEV") -> "MEDIUM"
            v.contains("LOW") -> "LOW"
            else -> "NONE"
        }
    }

    private fun extractObject(raw: String): JsonObject {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) throw InvalidAiResultException("Missing assistant result")
        val jsonText = if (trimmed.startsWith("```")) {
            trimmed.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
                .substringBeforeLast("```").trim()
        } else trimmed
        return try {
            val parsed = JsonParser.parseString(jsonText)
            if (!parsed.isJsonObject) {
                return proseResult(trimmed)
            }
            parsed.asJsonObject
        } catch (_: JsonSyntaxException) {
            return proseResult(trimmed)
        }
    }

    private fun proseResult(raw: String): JsonObject {
        val reply = raw.trim()
        if (reply.isBlank()) throw InvalidAiResultException("Missing assistant result")
        val obj = JsonObject()
        obj.addProperty("reply", reply)
        obj.addProperty("riskLevel", "NONE")
        return obj
    }

    private fun JsonObject.getString(key: String): String? =
        if (has(key) && get(key).isJsonPrimitive) get(key).asString else null
}
