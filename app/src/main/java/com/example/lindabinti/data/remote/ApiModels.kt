package com.example.lindabinti.data.remote

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val displayName: String
)

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class DeviceSessionRequestDto(
    val deviceId: String
)

data class RefreshRequestDto(
    val refreshToken: String
)

data class LogoutRequestDto(
    val refreshToken: String = ""
)

data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val role: String,
    val userId: String,
    val email: String,
    val expiresAtEpochMs: Long
)

data class ReportSubmitRequestDto(
    val clientUuid: String,
    val categoryId: String,
    val categoryLabel: String,
    val body: String,
    val createdAtEpochMs: Long,
    val includeBody: Boolean,
    val areaCode: String? = null,
    val areaLabel: String? = null,
    val areaType: String? = null
)

data class ReportSubmitResponseDto(
    val serverReportId: String,
    val clientUuid: String,
    val status: String
)

data class AnalyticsEventDto(
    val eventType: String,
    val categoryOrBand: String,
    val weekLabel: String,
    val count: Int
)

data class AnalyticsIngestRequestDto(
    val events: List<AnalyticsEventDto>
)

data class AggregateBucketDto(
    val key: String,
    val count: Int,
    val suppressed: Boolean = false
)

data class AnalyticsSummaryDto(
    val reportsByCategory: List<AggregateBucketDto> = emptyList(),
    val riskByBand: List<AggregateBucketDto> = emptyList(),
    val educationCompletions: Int = 0,
    val sosEvents: Int = 0,
    val byWeek: List<AggregateBucketDto> = emptyList(),
    val minThreshold: Int = 3,
    val note: String = ""
)

/** NGO flag — never includes report body or user identity. */
data class ReportFlagDto(
    val serverReportId: String = "",
    val categoryId: String = "",
    val categoryLabel: String = "",
    val createdAtEpochMs: Long = 0L,
    val weekLabel: String = "",
    val urgency: String = "",
    val explanation: String = "",
    val classifierSource: String = "rules",
    val disclaimer: String = "",
    val aiStatus: String = "unavailable",
    val gbvFgmRelevance: String = "",
    val incidentCategory: String = "",
    val urgencyIndicators: String = "",
    val safetyConcerns: String = "",
    val recommendedPriority: String = "",
    val caseStatus: String = "NEW",
    val referenceId: String = "",
    val areaLabel: String = "",
    val areaType: String = ""
)

data class ApiErrorDto(val error: String? = null)

data class LocalizedTextDto(
    val en: String = "",
    val sw: String = ""
)

data class SupportServiceDto(
    val id: String = "",
    val kind: String = "",
    val alsoKinds: List<String> = emptyList(),
    val name: LocalizedTextDto = LocalizedTextDto(),
    val helpsWith: LocalizedTextDto = LocalizedTextDto(),
    val phone: String? = null,
    val alternatePhone: String? = null,
    val phoneVerified: Boolean = false,
    val location: LocalizedTextDto = LocalizedTextDto(),
    val locationVerified: Boolean = false,
    val referralGuidance: LocalizedTextDto = LocalizedTextDto(),
    val area: String = "kenya"
)

data class SupportServicesCatalogDto(
    val version: Int = 1,
    val source: String = "bundled",
    val services: List<SupportServiceDto> = emptyList()
)

data class AiAnalyzeReportRequestDto(
    val categoryId: String,
    val categoryLabel: String,
    val description: String = "",
    val includeDescription: Boolean = true,
    val cloudAiConsent: Boolean = false
)

data class AiAnalyzeReportResponseDto(
    val analysisId: String,
    val source: String,
    val category: String,
    val severity: String,
    val confidence: Double,
    val indicators: List<String> = emptyList(),
    val explanation: String,
    val recommendations: List<String> = emptyList(),
    val educationRecommendations: List<String> = emptyList(),
    val timestamp: Long,
    val providerId: String? = null,
    val modelId: String? = null,
    val processingDurationMs: Long? = null
)

data class AiCapabilitiesResponseDto(
    val onlineEnabled: Boolean,
    val statusLabel: String? = null,
    val configurationReason: String? = null,
    val providerId: String? = null,
    val model: String? = null,
    val consentRequired: Boolean = true,
    val timeoutMs: Int = 8000,
    val maxRetries: Int = 1
)

data class AssistantChatTurnDto(
    val role: String,
    val content: String
)

data class AssistantChatRequestDto(
    val message: String,
    val history: List<AssistantChatTurnDto> = emptyList(),
    val language: String = "en",
    val cloudAiConsent: Boolean = false
)

data class AssistantChatResponseDto(
    val reply: String,
    val source: String = "ONLINE",
    val relatedAction: String? = null,
    val riskLevel: String = "NONE",
    val providerId: String? = null,
    val modelId: String? = null,
    val processingDurationMs: Long? = null,
    val aiStatus: String = "complete"
)

data class EducationContentDto(
    val id: String = "",
    val contentType: String = "video",
    val title: String = "",
    val description: String = "",
    val language: String = "en",
    val category: String = "general",
    val urlOrPath: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Int = 0,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val updatedAt: Long = 0
)

data class EducationContentListDto(
    val items: List<EducationContentDto> = emptyList(),
    val version: Int = 1
)