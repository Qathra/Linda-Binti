package com.example.lindabinti.ai

enum class AiSeverity {
    LOWER,
    ELEVATED,
    HIGH,
    CRITICAL;

    companion object {
        fun fromBackendLevel(raw: String?): AiSeverity = when (raw?.trim()?.uppercase()) {
            "CRITICAL", "CRIT" -> CRITICAL
            "HIGH" -> HIGH
            "MEDIUM", "ELEVATED", "MODERATE" -> ELEVATED
            "LOW", "LOWER" -> LOWER
            else -> ELEVATED
        }
    }
}
