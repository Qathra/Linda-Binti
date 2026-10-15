package com.lindabinti.backend.api

import com.lindabinti.backend.Role
import com.lindabinti.backend.security.AuthRateLimiter
import com.lindabinti.backend.security.RateLimitException
import com.lindabinti.backend.security.AccessClaims
import com.lindabinti.backend.security.ForbiddenException
import com.lindabinti.backend.security.JwtService
import com.lindabinti.backend.security.PasswordChangeRequiredException
import com.lindabinti.backend.security.RoleGuard
import com.lindabinti.backend.services.AnalyticsEventIn
import com.lindabinti.backend.ai.AiConfig
import com.lindabinti.backend.ai.AiProviderException
import com.lindabinti.backend.ai.AssistantChatTurn
import com.lindabinti.backend.services.AiAnalysisService
import com.lindabinti.backend.services.AssistantChatService
import com.lindabinti.backend.services.AnalyticsService
import com.lindabinti.backend.services.AuditService
import com.lindabinti.backend.services.AuthException
import com.lindabinti.backend.services.AuthResult
import com.lindabinti.backend.services.AuthService
import com.lindabinti.backend.services.EducationContentService
import com.lindabinti.backend.services.EducationContentWrite
import com.lindabinti.backend.services.NgoAiDisplayLabels
import com.lindabinti.backend.services.NgoFlagIntelligence
import com.lindabinti.backend.services.NgoIntelligenceService
import com.lindabinti.backend.services.ReportFlag
import com.lindabinti.backend.services.ReportService
import com.lindabinti.backend.services.SupportServicesCatalog
import com.lindabinti.backend.services.UserRepository
import com.lindabinti.backend.ussd.UssdChannelService
import com.lindabinti.backend.ussd.UssdGatewayAuth
import com.lindabinti.backend.ussd.UssdUnauthorizedException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.http.content.staticFiles
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.http.ContentType
import com.lindabinti.backend.ussd.AfricasTalkingUssdAdapter
import java.io.File

fun Application.configureRoutes(
    authService: AuthService,
    reportService: ReportService,
    analyticsService: AnalyticsService,
    userRepository: UserRepository,
    auditService: AuditService,
    jwtService: JwtService,
    ussdChannel: UssdChannelService,
    aiAnalysisService: AiAnalysisService,
    assistantChatService: AssistantChatService,
    ngoIntelligenceService: NgoIntelligenceService,
    educationContentService: EducationContentService,
    aiConfig: AiConfig = AiConfig("", "", "gpt-4o-mini", 8000, 0, false),
    authRateLimiter: AuthRateLimiter = AuthRateLimiter()
) {
    val educationMediaDir = File("./data/education_videos").apply { mkdirs() }

    routing {
        get("/") {
            call.respondRedirect("/dashboard/", permanent = false)
        }
        staticResources("/dashboard", "dashboard")
        get("/ussd") {
            call.respondRedirect("/ussd/", permanent = false)
        }
        staticResources("/ussd", "ussd")
        // Drop .mp4 files into backend/data/education_videos/ then use:
        // http://YOUR_PC_IP:8080/media/education/filename.mp4
        staticFiles("/media/education", educationMediaDir)

        get("/v1/health") {
            call.respond(
                HealthResponse(
                    status = "ok",
                    version = "1.0",
                    ai = if (aiConfig.onlineEnabled) "configured" else "disabled",
                    onlineAiStatus = aiConfig.onlineStatusLabel,
                    ussd = if (ussdChannel.gatewayConfigured) "gateway_key_configured" else "stub"
                )
            )
        }

        get("/v1/ai/capabilities") {
            call.respond(aiAnalysisService.capabilities())
        }

        get("/v1/support-services") {
            call.respond(SupportServicesCatalog.dto())
        }

        get("/v1/education/content") {
            val language = call.request.queryParameters["language"]
            val category = call.request.queryParameters["category"]
            val type = call.request.queryParameters["type"]
            val items = educationContentService.list(
                language = language,
                category = category,
                type = type,
                includeInactive = false
            ).map { it.toDto() }
            call.respond(EducationContentListResponse(items = items))
        }

        get("/v1/education/content/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("id_required")
            )
            val item = educationContentService.getById(id)
            if (item == null || !item.active) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found"))
            } else {
                call.respond(item.toDto())
            }
        }

        post("/v1/ussd/inbound") {
            val req = runCatching { call.receive<UssdInboundRequest>() }.getOrElse { UssdInboundRequest() }
            try {
                val result = ussdChannel.handleInbound(
                    req,
                    call.request.header(UssdGatewayAuth.HEADER)
                        ?: call.request.queryParameters[UssdGatewayAuth.QUERY_PARAM]
                )
                call.respond(result)
            } catch (_: UssdUnauthorizedException) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("ussd_gateway_unauthorized"))
            }
        }

        post("/v1/ussd/demo") {
            val req = runCatching { call.receive<UssdInboundRequest>() }.getOrElse { UssdInboundRequest() }
            call.respond(ussdChannel.handleDemoInbound(req))
        }

        /**
         * Africa's Talking USSD callback.
         * Configure AT console → Callback URL:
         * https://lindabinti.com/v1/ussd/africas-talking?key=<LINDA_USSD_GATEWAY_SECRET>
         */
        post("/v1/ussd/africas-talking") {
            val key = call.request.header(UssdGatewayAuth.HEADER)
                ?: call.request.queryParameters[UssdGatewayAuth.QUERY_PARAM]
            val params = runCatching {
                call.receiveParameters().entries().associate { it.key to (it.value.firstOrNull() ?: "") }
            }.getOrElse { emptyMap() }
            val form = AfricasTalkingUssdAdapter.parseForm(params)
            try {
                val result = ussdChannel.handleInbound(
                    UssdInboundRequest(
                        sessionId = form.sessionId,
                        msisdn = "",
                        text = form.text,
                        serviceCode = form.serviceCode
                    ),
                    key
                )
                val body = AfricasTalkingUssdAdapter.toAtResponse(
                    AfricasTalkingUssdAdapter.UssdInboundMenu(
                        message = result.message,
                        endSession = result.endSession
                    )
                )
                call.respondText(body, ContentType.Text.Plain)
            } catch (_: UssdUnauthorizedException) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("ussd_gateway_unauthorized"))
            }
        }

        route("/v1/auth") {
            post("/register") {
                try {
                    val req = call.receive<RegisterRequest>()
                    authRateLimiter.checkOrThrow(call.authRateKey("register"))
                    val result = authService.registerUser(req.email, req.password, req.displayName)
                    call.respond(result.toAuthResponse())
                } catch (e: RateLimitException) {
                    call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("rate_limited"))
                } catch (e: AuthException) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
                } catch (e: IllegalArgumentException) {
                    val code = when {
                        e.message?.contains("already", ignoreCase = true) == true -> "email_taken"
                        e.message?.contains("Password", ignoreCase = true) == true -> "password_too_short"
                        e.message?.contains("email", ignoreCase = true) == true -> "invalid_email"
                        else -> "register_failed"
                    }
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(code))
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("register_failed"))
                }
            }
            post("/login") {
                try {
                    val req = call.receive<LoginRequest>()
                    authRateLimiter.checkOrThrow(call.authRateKey("login"))
                    val result = authService.login(req.email, req.password)
                    call.respond(result.toAuthResponse())
                } catch (e: RateLimitException) {
                    call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("rate_limited"))
                } catch (e: AuthException) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("login_failed"))
                }
            }
            post("/device") {
                try {
                    val req = call.receive<DeviceSessionRequest>()
                    authRateLimiter.checkOrThrow(call.authRateKey("device"))
                    val result = authService.ensureDeviceUser(req.deviceId)
                    call.respond(result.toAuthResponse())
                } catch (e: RateLimitException) {
                    call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("rate_limited"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_device_id"))
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("device_session_failed"))
                }
            }
            post("/refresh") {
                try {
                    authRateLimiter.checkOrThrow(call.authRateKey("refresh"))
                    val req = call.receive<RefreshRequest>()
                    if (req.refreshToken.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("missing_refresh_token"))
                        return@post
                    }
                    val result = authService.refresh(req.refreshToken)
                    call.respond(result.toAuthResponse())
                } catch (e: RateLimitException) {
                    call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("rate_limited"))
                } catch (e: AuthException) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("invalid_refresh_token"))
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("refresh_failed"))
                }
            }
            post("/logout") {
                val bearer = call.request.header("Authorization")?.removePrefix("Bearer ")?.trim()
                val body = runCatching { call.receive<LogoutRequest>() }.getOrNull()
                if (!bearer.isNullOrBlank()) {
                    runCatching {
                        val claims = jwtService.parseAccess(bearer)
                        authService.logout(claims.userId, body?.refreshToken)
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }

        authenticate("auth-jwt") {
            post("/v1/ai/analyze-report") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.USER)
                    val req = call.receive<AiAnalyzeReportRequest>()
                    val result = aiAnalysisService.analyzeReportOnline(
                        categoryId = req.categoryId,
                        categoryLabel = req.categoryLabel,
                        description = req.description,
                        includeDescription = req.includeDescription,
                        cloudAiConsent = req.cloudAiConsent,
                        actorUserId = claims.userId
                    )
                    auditService.log(
                        claims.userId,
                        "AI_ANALYZE_REPORT",
                        "category=${req.categoryId};status=${result.aiStatus};consent=${req.cloudAiConsent}"
                    )
                    call.respond(result)
                } catch (e: AiProviderException) {
                    auditService.log(
                        claims.userId,
                        "AI_ANALYZE_FAILED",
                        "code=${e.code};status=${e.aiStatus}"
                    )
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        AiAnalysisErrorResponse(
                            error = e.code,
                            fallbackRecommended = true,
                            reason = e.message ?: "Online AI unavailable",
                            aiStatus = e.aiStatus
                        )
                    )
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "invalid_request"))
                }
            }
            post("/v1/ai/assistant-chat") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.USER)
                    val req = call.receive<AssistantChatRequest>()
                    val result = assistantChatService.chat(
                        message = req.message,
                        history = req.history.map { AssistantChatTurn(it.role, it.content) },
                        language = req.language,
                        cloudAiConsent = req.cloudAiConsent,
                        actorUserId = claims.userId
                    )
                    auditService.log(
                        claims.userId,
                        "AI_ASSISTANT_CHAT",
                        "risk=${result.riskLevel};status=${result.aiStatus};consent=${req.cloudAiConsent}"
                    )
                    call.respond(result)
                } catch (e: AiProviderException) {
                    auditService.log(
                        claims.userId,
                        "AI_ASSISTANT_FAILED",
                        "code=${e.code};status=${e.aiStatus}"
                    )
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        AiAnalysisErrorResponse(
                            error = e.code,
                            fallbackRecommended = true,
                            reason = e.message ?: "Online assistant unavailable",
                            aiStatus = e.aiStatus
                        )
                    )
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "invalid_request"))
                }
            }

            post("/v1/auth/change-password") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_ADMIN)
                    val req = call.receive<ChangePasswordRequest>()
                    val result = authService.changePassword(claims.userId, req.currentPassword, req.newPassword)
                    call.respond(result.toAuthResponse())
                } catch (e: AuthException) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("invalid_current_password"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "invalid_password"))
                }
            }

            post("/v1/reports") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.USER)
                    val idem = call.request.header("Idempotency-Key")
                    val req = call.receive<ReportSubmitRequest>()
                    val clientUuid = idem?.takeIf { it.isNotBlank() } ?: req.clientUuid
                    val stored = reportService.upsertOwnedReport(
                        userId = claims.userId,
                        clientUuid = clientUuid,
                        categoryId = req.categoryId,
                        categoryLabel = req.categoryLabel.ifBlank { req.categoryId },
                        body = req.body,
                        areaCode = req.areaCode,
                        areaLabel = req.areaLabel,
                        areaType = req.areaType,
                        createdAt = if (req.createdAtEpochMs > 0) req.createdAtEpochMs else System.currentTimeMillis(),
                        includeBody = req.includeBody
                    )
                    call.respond(
                        ReportSubmitResponse(
                            serverReportId = stored.serverId,
                            clientUuid = stored.clientUuid,
                            status = "stored"
                        )
                    )
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "report_failed"))
                }
            }

            get("/v1/reports/me") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.USER)
                    val list = reportService.listForUser(claims.userId).map {
                        ReportDto(
                            serverReportId = it.serverId,
                            clientUuid = it.clientUuid,
                            categoryId = it.categoryId,
                            categoryLabel = it.categoryLabel,
                            body = it.body,
                            createdAtEpochMs = it.createdAt,
                            areaLabel = it.areaLabel,
                            areaType = it.areaType
                        )
                    }
                    call.respond(list)
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                }
            }

            get("/v1/reports/flags") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_STAFF, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    val flags = reportService.listFlagsForNgo(claims.userId).map { it.toDto(aiConfig.onlineEnabled) }
                    call.respond(flags)
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                }
            }

            post("/v1/reports/flags/{id}/analyze") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_STAFF, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    val id = call.parameters["id"].orEmpty()
                    if (id.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("missing_id"))
                        return@post
                    }
                    val flag = reportService.retryAiAnalysis(claims.userId, id)
                    call.respond(flag.toDto(aiConfig.onlineEnabled))
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse(e.message ?: "not_found"))
                }
            }

            post("/v1/analytics/events") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.USER)
                    val req = call.receive<AnalyticsIngestRequest>()
                    analyticsService.ingest(
                        claims.userId,
                        req.events.map {
                            AnalyticsEventIn(it.eventType, it.categoryOrBand, it.weekLabel, it.count)
                        }
                    )
                    call.respond(mapOf("status" to "accepted"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "analytics_failed"))
                }
            }

            get("/v1/analytics/summary") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_STAFF, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    call.respond(analyticsService.summaryForNgo(claims.userId))
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                }
            }

            get("/v1/dashboard/session") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_STAFF, Role.NGO_ADMIN)
                    val user = userRepository.findById(claims.userId)
                        ?: throw ForbiddenException("Missing authentication")
                    call.respond(
                        DashboardSessionDto(
                            role = claims.role.name,
                            email = claims.email,
                            canManageStaff = RoleGuard.canManageStaff(claims.role),
                            mustChangePassword = user.mustChangePassword
                        )
                    )
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                }
            }

            get("/v1/dashboard/overview") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_STAFF, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    call.respond(reportService.overviewForNgo(claims.userId))
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                }
            }

            get("/v1/dashboard/cases") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_STAFF, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    call.respond(reportService.listFlagsForNgo(claims.userId).map { it.toDto(aiConfig.onlineEnabled) })
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                }
            }

            get("/v1/dashboard/trends") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_STAFF, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    val analytics = analyticsService.summaryForNgo(claims.userId)
                    call.respond(reportService.trendsForNgo(claims.userId, analytics))
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                }
            }

            get("/v1/dashboard/intelligence") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_STAFF, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    val periodDays = call.request.queryParameters["periodDays"]?.toIntOrNull() ?: 30
                    call.respond(ngoIntelligenceService.intelligenceForNgo(claims.userId, periodDays))
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                }
            }

            post("/v1/dashboard/cases/{id}/status") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_STAFF, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    val id = call.parameters["id"].orEmpty()
                    if (id.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("missing_id"))
                        return@post
                    }
                    val req = call.receive<CaseStatusUpdateRequest>()
                    val flag = reportService.updateCaseStatus(claims.userId, id, req.status)
                    call.respond(flag.toDto(aiConfig.onlineEnabled))
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "invalid_status"))
                }
            }

            get("/v1/admin/staff") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    val staff = userRepository.listStaff().map {
                        StaffDto(it.id, it.email, it.role.name, it.displayName)
                    }
                    auditService.log(claims.userId, "STAFF_LIST", "count=${staff.size}")
                    call.respond(staff)
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                }
            }

            post("/v1/admin/staff") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    val req = call.receive<CreateStaffRequest>()
                    val role = Role.parse(req.role)
                    val created = authService.createStaff(claims.userId, req.email, req.password, role)
                    call.respond(StaffDto(created.id, created.email, created.role.name, created.displayName))
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "staff_create_failed"))
                }
            }

            get("/v1/admin/audit") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    call.respond(auditService.listRecent())
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                }
            }

            get("/v1/admin/education/content") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_ADMIN, Role.NGO_STAFF)
                    call.requireNgoDashboardData(userRepository, claims)
                    val items = educationContentService.list(includeInactive = true).map { it.toDto() }
                    call.respond(EducationContentListResponse(items = items))
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                }
            }

            post("/v1/admin/education/content") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    val req = call.receive<EducationContentUpsertRequest>()
                    val created = educationContentService.create(req.toWrite(), claims.userId)
                    call.respond(HttpStatusCode.Created, created.toDto())
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "invalid_request"))
                }
            }

            put("/v1/admin/education/content/{id}") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    val id = call.parameters["id"] ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("id_required")
                    )
                    val req = call.receive<EducationContentUpsertRequest>()
                    val updated = educationContentService.update(id, req.toWrite(), claims.userId)
                    call.respond(updated.toDto())
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                } catch (e: IllegalArgumentException) {
                    val code = e.message ?: "invalid_request"
                    val status = if (code == "not_found") HttpStatusCode.NotFound else HttpStatusCode.BadRequest
                    call.respond(status, ErrorResponse(code))
                }
            }

            post("/v1/admin/education/content/{id}/active") {
                val claims = call.requireClaims()
                try {
                    RoleGuard.requireRole(claims.role, Role.NGO_ADMIN)
                    call.requireNgoDashboardData(userRepository, claims)
                    val id = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("id_required")
                    )
                    val req = call.receive<EducationActiveRequest>()
                    val updated = educationContentService.setActive(id, req.active, claims.userId)
                    call.respond(updated.toDto())
                } catch (e: PasswordChangeRequiredException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("password_change_required"))
                } catch (e: ForbiddenException) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "forbidden"))
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse(e.message ?: "not_found"))
                }
            }
        }
    }
}

private fun com.lindabinti.backend.services.EducationContentItem.toDto() = EducationContentDto(
    id = id,
    contentType = contentType,
    title = title,
    description = description,
    language = language,
    category = category,
    urlOrPath = urlOrPath,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    active = active,
    sortOrder = sortOrder,
    updatedAt = updatedAt
)

private fun EducationContentUpsertRequest.toWrite() = EducationContentWrite(
    contentType = contentType,
    title = title,
    description = description,
    language = language,
    category = category,
    urlOrPath = urlOrPath,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    active = active,
    sortOrder = sortOrder
)

private fun AuthResult.toAuthResponse() = AuthResponse(
    accessToken = tokens.accessToken,
    refreshToken = tokens.refreshToken,
    role = role.name,
    userId = userId,
    email = email,
    expiresAtEpochMs = tokens.expiresAtEpochMs,
    mustChangePassword = mustChangePassword
)

private fun ApplicationCall.requireNgoDashboardData(userRepository: UserRepository, claims: AccessClaims) {
    val user = userRepository.findById(claims.userId) ?: throw ForbiddenException("Missing authentication")
    RoleGuard.requireDashboardDataAccess(user.mustChangePassword)
}

private fun ReportFlag.toDto(onlineConfigured: Boolean = false): ReportFlagDto {
    val confidence = NgoFlagIntelligence.estimateConfidence(this)
    val indicators = NgoFlagIntelligence.parseIndicators(this)
    val severity = NgoFlagIntelligence.severityLabel(urgency)
    val scope = when {
        gbvFgmRelevance.equals("FGM", ignoreCase = true) -> "FGM"
        gbvFgmRelevance.equals("BOTH", ignoreCase = true) -> "FGM"
        categoryId.lowercase().startsWith("fgm") -> "FGM"
        else -> "GBV"
    }
    val incident = NgoAiDisplayLabels.incidentType(categoryId, incidentCategory)
    return ReportFlagDto(
        serverReportId = serverReportId,
        categoryId = categoryId,
        categoryLabel = categoryLabel,
        createdAtEpochMs = createdAtEpochMs,
        weekLabel = weekLabel,
        urgency = urgency,
        explanation = explanation,
        classifierSource = classifierSource,
        disclaimer = disclaimer,
        aiStatus = aiStatus,
        gbvFgmRelevance = gbvFgmRelevance,
        incidentCategory = incidentCategory,
        urgencyIndicators = urgencyIndicators,
        safetyConcerns = safetyConcerns,
        recommendedPriority = recommendedPriority.ifBlank { severity },
        caseStatus = caseStatus,
        referenceId = referenceId,
        areaLabel = areaLabel,
        areaType = areaType,
        severity = severity,
        confidence = confidence,
        confidencePercent = (confidence * 100).toInt(),
        indicators = indicators,
        whyFlagged = NgoAiDisplayLabels.buildWhyFlaggedExplanation(
            severity, scope, incident, indicators, explanation
        ),
        sourceLabel = NgoAiDisplayLabels.sourceLabel(classifierSource),
        aiStatusLabel = NgoAiDisplayLabels.caseStatusLabel(aiStatus, classifierSource, onlineConfigured),
        scope = scope,
        incidentType = incident
    )
}

private fun ApplicationCall.requireClaims(): AccessClaims =
    principal<AccessClaims>() ?: throw ForbiddenException("Missing authentication")

private fun ApplicationCall.authRateKey(action: String): String {
    val ip = request.local.remoteHost.ifBlank { "unknown" }
    return "$action:$ip"
}