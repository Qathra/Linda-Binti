package com.example.lindabinti.compass

import com.example.lindabinti.ai.AiSeverity

enum class SafetyCompassStatus {
    LOWER,
    ELEVATED,
    HIGH,
    CRITICAL;

    companion object {
        fun fromRiskBand(band: String?): SafetyCompassStatus? = when (band?.lowercase()) {
            "critical" -> CRITICAL
            "high" -> HIGH
            "elevated" -> ELEVATED
            "lower" -> LOWER
            else -> null
        }

        fun max(a: SafetyCompassStatus, b: SafetyCompassStatus): SafetyCompassStatus =
            if (a.ordinal >= b.ordinal) a else b
    }
}

data class SafetyCompassEducationItem(
    val topicId: String,
    val title: String
)

data class SafetyCompassSnapshot(
    val status: SafetyCompassStatus,
    val statusConfidenceNote: String,
    val aiSourceLabel: String,
    val generatedAt: Long,
    val reassurance: String,
    val disclaimer: String,
    val riskIndicators: List<String>,
    val gbvFgmConcerns: List<String>,
    val safetyFactors: List<String>,
    val nextSteps: List<String>,
    val educationItems: List<SafetyCompassEducationItem>,
    val trustedContactCount: Int,
    val showTrustedContacts: Boolean,
    val showEmergency: Boolean,
    val showSupportServices: Boolean,
    val hasAnyData: Boolean
)

fun AiSeverity.toCompassStatus(): SafetyCompassStatus = when (this) {
    AiSeverity.CRITICAL -> SafetyCompassStatus.CRITICAL
    AiSeverity.HIGH -> SafetyCompassStatus.HIGH
    AiSeverity.ELEVATED -> SafetyCompassStatus.ELEVATED
    AiSeverity.LOWER -> SafetyCompassStatus.LOWER
}
