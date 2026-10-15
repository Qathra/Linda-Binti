package com.example.lindabinti.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface LindaBintiApi {
    @POST("v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): AuthResponseDto

    @POST("v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthResponseDto

    @POST("v1/auth/device")
    suspend fun deviceSession(@Body body: DeviceSessionRequestDto): AuthResponseDto

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): AuthResponseDto

    @POST("v1/auth/logout")
    suspend fun logout(
        @Header("Authorization") bearer: String,
        @Body body: LogoutRequestDto = LogoutRequestDto()
    ): Response<Unit>

    @POST("v1/reports")
    suspend fun submitReport(
        @Header("Authorization") bearer: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: ReportSubmitRequestDto
    ): ReportSubmitResponseDto

    @POST("v1/analytics/events")
    suspend fun ingestAnalytics(
        @Header("Authorization") bearer: String,
        @Body body: AnalyticsIngestRequestDto
    ): Response<Unit>

    @GET("v1/analytics/summary")
    suspend fun analyticsSummary(
        @Header("Authorization") bearer: String
    ): AnalyticsSummaryDto

    @GET("v1/reports/flags")
    suspend fun reportFlags(
        @Header("Authorization") bearer: String
    ): List<ReportFlagDto>

    @POST("v1/reports/flags/{id}/analyze")
    suspend fun retryReportFlagAnalysis(
        @Header("Authorization") bearer: String,
        @Path("id") serverReportId: String
    ): ReportFlagDto

    @GET("v1/health")
    suspend fun health(): Map<String, String>

    @GET("v1/ai/capabilities")
    suspend fun aiCapabilities(): AiCapabilitiesResponseDto

    @POST("v1/ai/analyze-report")
    suspend fun analyzeReport(
        @Header("Authorization") bearer: String,
        @Body body: AiAnalyzeReportRequestDto
    ): retrofit2.Response<AiAnalyzeReportResponseDto>

    @POST("v1/ai/assistant-chat")
    suspend fun assistantChat(
        @Header("Authorization") bearer: String,
        @Body body: AssistantChatRequestDto
    ): retrofit2.Response<AssistantChatResponseDto>

    @GET("v1/support-services")
    suspend fun supportServices(): SupportServicesCatalogDto

    @GET("v1/education/content")
    suspend fun educationContent(
        @retrofit2.http.Query("language") language: String? = null,
        @retrofit2.http.Query("category") category: String? = null,
        @retrofit2.http.Query("type") type: String? = null
    ): EducationContentListDto

    @GET("v1/education/content/{id}")
    suspend fun educationContentById(@retrofit2.http.Path("id") id: String): EducationContentDto
}