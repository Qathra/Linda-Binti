package com.lindabinti.backend.ai

/**
 * Strips emails, phone-like numbers, and extra length before any AI call.
 * Never used for Access PIN / SOS — those never leave the phone and are not on this payload.
 */
object PiiMinimizer {
    private val email = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    private val phone = Regex("(?:\\+?\\d[\\s-]?){7,15}")
    private val longId = Regex("\\b[0-9]{8,}\\b")
    private val labeledSecret = Regex(
        "(?i)\\b(?:access\\s*pin|access\\s*code|sos\\s*code|sos\\s*pin|password|passwd|pin)\\b[:\\s-]*\\d{4,8}"
    )

    fun minimizeDescription(raw: String?, maxChars: Int = 400): String {
        if (raw.isNullOrBlank()) return ""
        var text = raw.trim()
        text = email.replace(text, "[redacted-email]")
        text = phone.replace(text, "[redacted-phone]")
        text = longId.replace(text, "[redacted-id]")
        text = labeledSecret.replace(text, "[redacted-code]")
        if (text.length > maxChars) text = text.take(maxChars)
        return text.trim()
    }

    fun toInput(categoryId: String, categoryLabel: String, body: String?, includeBody: Boolean): AiRiskInput {
        val description = if (includeBody) minimizeDescription(body) else ""
        return AiRiskInput(
            categoryId = categoryId.trim().take(64),
            categoryLabel = categoryLabel.trim().take(128),
            redactedDescription = description
        )
    }
}
