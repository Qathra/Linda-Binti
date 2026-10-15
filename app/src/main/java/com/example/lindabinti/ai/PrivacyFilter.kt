package com.example.lindabinti.ai

/**
 * Strips identifiers and secrets before any cloud AI call.
 * Never sends Access PIN, SOS code, trusted contacts, or authentication material.
 */
object PrivacyFilter {

    private val email = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    private val phone = Regex("(?:\\+?\\d[\\s-]?){7,15}")
    private val longId = Regex("\\b[0-9]{8,}\\b")
    private val labeledSecret = Regex(
        "(?i)\\b(?:access\\s*pin|access\\s*code|sos\\s*code|sos\\s*pin|password|passwd|pin|trusted\\s*contact)\\b[:\\s-]*\\d{0,12}"
    )
    private val nameLike = Regex("(?i)\\b(?:my name is|call me|i am|jina langu ni)\\s+[A-Za-z]{2,}(?:\\s+[A-Za-z]{2,})?")

    fun redactForCloudAnalysis(raw: String?, maxChars: Int = 400): String {
        if (raw.isNullOrBlank()) return ""
        var text = raw.trim()
        text = email.replace(text, "[redacted]")
        text = phone.replace(text, "[redacted]")
        text = longId.replace(text, "[redacted]")
        text = labeledSecret.replace(text, "[redacted]")
        text = nameLike.replace(text, "[redacted]")
        if (text.length > maxChars) text = text.take(maxChars)
        return text.trim()
    }

    fun prepareOnlineInput(input: AiAnalysisInput): AiAnalysisInput {
        val description = if (input.includeDescription) {
            redactForCloudAnalysis(input.description)
        } else {
            ""
        }
        return input.copy(description = description)
    }

    /** Ensures redacted payload never carries forbidden keys when serialized. */
    fun assertSafeForCloud(fields: Map<String, String>) {
        val forbidden = setOf(
            "accessPin", "sosCode", "pin", "password", "refreshToken",
            "accessToken", "trustedContact", "trustedPhone"
        )
        fields.forEach { (key, value) ->
            require(key !in forbidden) { "Forbidden cloud AI field: $key" }
            require(!phone.containsMatchIn(value)) { "Phone-like value rejected for cloud AI" }
        }
    }
}
