package com.lindabinti.backend.api

data class RegisterRequest(val email: String = "", val password: String = "", val displayName: String = "")
data class LoginRequest(val email: String = "", val password: String = "")
data class DeviceSessionRequest(val deviceId: String = "")
data class RefreshRequest(val refreshToken: String = "")
data class LogoutRequest(val refreshToken: String = "")
data class ChangePasswordRequest(val currentPassword: String = "", val newPassword: String = "")
data class AiAnalyzeReportRequest(
    val categoryId: String = "",
    val categoryLabel: String = "",
    val description: String = "",
    val includeDescription: Boolean = true,
    val cloudAiConsent: Boolean = false
)
data class AiAnalyzeReportResponse(
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
    val processingDurationMs: Long? = null,
    val aiStatus: String = "unavailable"
)
data class AiAnalysisErrorResponse(
    val error: String,
    val fallbackRecommended: Boolean = true,
    val reason: String,
    val aiStatus: String = "unavailable"
)
data class AiCapabilitiesResponse(
    val onlineEnabled: Boolean,
    val statusLabel: String = "ONLINE AI: NOT CONFIGURED",
    val configurationReason: String = "",
    val providerId: String? = null,
    val model: String? = null,
    val consentRequired: Boolean = true,
    val timeoutMs: Int = 8000,
    val maxRetries: Int = 1
)
data class AssistantChatTurnDto(val role: String = "", val content: String = "")
data class AssistantChatRequest(
    val message: String = "",
    val history: List<AssistantChatTurnDto> = emptyList(),
    val language: String = "en",
    val cloudAiConsent: Boolean = false
)
data class AssistantChatResponse(
    val reply: String,
    val source: String = "ONLINE",
    val relatedAction: String? = null,
    val riskLevel: String = "NONE",
    val providerId: String? = null,
    val modelId: String? = null,
    val processingDurationMs: Long? = null,
    val aiStatus: String = "complete"
)
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val role: String,
    val userId: String,
    val email: String,
    val expiresAtEpochMs: Long,
    val mustChangePassword: Boolean = false
)

data class ReportSubmitRequest(
    val clientUuid: String = "",
    val categoryId: String = "",
    val categoryLabel: String = "",
    val body: String = "",
    val createdAtEpochMs: Long = 0L,
    val includeBody: Boolean = false,
    val areaCode: String? = null,
    val areaLabel: String? = null,
    val areaType: String? = null
)

data class ReportSubmitResponse(
    val serverReportId: String,
    val clientUuid: String,
    val status: String
)

data class ReportDto(
    val serverReportId: String,
    val clientUuid: String,
    val categoryId: String,
    val categoryLabel: String,
    val body: String,
    val createdAtEpochMs: Long,
    val areaLabel: String = "",
    val areaType: String = ""
)

/** NGO flag payload — no body, userId, email, or phone. */
data class ReportFlagDto(
    val serverReportId: String,
    val categoryId: String,
    val categoryLabel: String,
    val createdAtEpochMs: Long,
    val weekLabel: String,
    val urgency: String,
    val explanation: String,
    val classifierSource: String,
    val disclaimer: String,
    val aiStatus: String = "unavailable",
    val gbvFgmRelevance: String = "",
    val incidentCategory: String = "",
    val urgencyIndicators: String = "",
    val safetyConcerns: String = "",
    val recommendedPriority: String = "",
    val caseStatus: String = "NEW",
    val referenceId: String = "",
    val areaLabel: String = "",
    val areaType: String = "",
    val severity: String = "",
    val confidence: Double = 0.0,
    val confidencePercent: Int = 0,
    val indicators: List<String> = emptyList(),
    val whyFlagged: String = "",
    val sourceLabel: String = "",
    val aiStatusLabel: String = "",
    val scope: String = "",
    val incidentType: String = ""
)

data class AnalyticsEventDto(
    val eventType: String = "",
    val categoryOrBand: String = "",
    val weekLabel: String = "",
    val count: Int = 0
)

data class AnalyticsIngestRequest(val events: List<AnalyticsEventDto> = emptyList())

data class CreateStaffRequest(
    val email: String = "",
    val password: String = "",
    val role: String = "NGO_STAFF"
)

data class StaffDto(val userId: String, val email: String, val role: String, val displayName: String)
data class CaseStatusUpdateRequest(val status: String = "")
data class DashboardSessionDto(
    val role: String,
    val email: String,
    val canManageStaff: Boolean,
    val mustChangePassword: Boolean = false
)
data class ErrorResponse(val error: String)
data class HealthResponse(
    val status: String,
    val version: String,
    val ai: String = "disabled",
    val onlineAiStatus: String = "ONLINE AI: NOT CONFIGURED",
    val ussd: String = "stub"
)

data class UssdInboundRequest(
    val sessionId: String = "",
    val msisdn: String = "",
    val text: String = "",
    val serviceCode: String = ""
)

data class UssdInboundResponse(
    val message: String,
    val endSession: Boolean,
    val note: String,
    val channelStatus: String = "stub",
    val reportAccepted: Boolean = false,
    val referenceId: String = ""
)

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
    val source: String = "backend",
    val services: List<SupportServiceDto> = emptyList()
)

data class EducationContentDto(
    val id: String,
    val contentType: String,
    val title: String,
    val description: String,
    val language: String,
    val category: String,
    val urlOrPath: String,
    val thumbnailUrl: String = "",
    val durationSeconds: Int = 0,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val updatedAt: Long = 0
)

data class EducationContentListResponse(
    val items: List<EducationContentDto>,
    val version: Int = 1
)

data class EducationContentUpsertRequest(
    val contentType: String = "video",
    val title: String = "",
    val description: String = "",
    val language: String = "en",
    val category: String = "general",
    val urlOrPath: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Int = 0,
    val active: Boolean = true,
    val sortOrder: Int = 0
)

data class EducationActiveRequest(
    val active: Boolean = true
)