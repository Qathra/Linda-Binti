package com.lindabinti.backend

import com.lindabinti.backend.services.AiOrchestrationService
import com.lindabinti.backend.ai.AiConfig
import com.lindabinti.backend.ai.AiHttpClient
import com.lindabinti.backend.ai.JdkAiHttpClient
import com.lindabinti.backend.api.ErrorResponse
import com.lindabinti.backend.api.configureRoutes
import com.lindabinti.backend.db.Database
import com.lindabinti.backend.dev.SecurePasswordGenerator
import com.lindabinti.backend.security.ForbiddenException
import com.lindabinti.backend.security.JwtSecretSource
import com.lindabinti.backend.security.JwtService
import com.lindabinti.backend.services.AiAnalysisService
import com.lindabinti.backend.services.AssistantChatService
import com.lindabinti.backend.services.AnalyticsService
import com.lindabinti.backend.services.AuditService
import com.lindabinti.backend.services.AuthService
import com.lindabinti.backend.services.DemoSeedService
import com.lindabinti.backend.services.EducationContentService
import com.lindabinti.backend.services.NgoIntelligenceService
import com.lindabinti.backend.services.ReportService
import com.lindabinti.backend.services.UserRepository
import com.lindabinti.backend.ussd.UssdChannelService
import com.lindabinti.backend.ussd.UssdGatewayAuth
import com.lindabinti.backend.ussd.UssdUnauthorizedException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import java.net.BindException

fun main() {
    EnvFileLoader.loadIfPresent()
    val port = EnvFileLoader.get("LINDA_PORT")?.toIntOrNull()
        ?: System.getenv("PORT")?.toIntOrNull()
        ?: 8080
    // Bind all interfaces so LAN devices (e.g. tablet) can reach the API.
    // 127.0.0.1 would accept only local loopback and break physical-device DEBUG.
    val host = EnvFileLoader.get("LINDA_HOST") ?: "0.0.0.0"
    val publicUrl = (EnvFileLoader.get("LINDA_PUBLIC_URL") ?: System.getenv("LINDA_PUBLIC_URL"))
        ?.trimEnd('/')
    println("Linda Binti backend starting — bind host=$host port=$port")
    if (!publicUrl.isNullOrBlank()) {
        println("Public site: $publicUrl/")
        println("NGO dashboard: $publicUrl/dashboard/")
    } else {
        println("Stakeholder dashboard: http://127.0.0.1:$port/dashboard/")
        println("Education MP4 folder: ./data/education_videos/ → http://127.0.0.1:$port/media/education/")
        println("Set LINDA_PUBLIC_URL=https://lindabinti.com after DNS is configured.")
    }
    println("Note: Ktor may still log 'Responding at http://127.0.0.1:$port' even when bound to 0.0.0.0.")
    try {
        embeddedServer(
            factory = Netty,
            port = port,
            host = host,
            module = Application::module
        ).start(wait = true)
    } catch (e: BindException) {
        System.err.println(
            "FATAL: cannot bind $host:$port — Address already in use. " +
                "Stop the previous :backend:run (or other process on port $port), then retry."
        )
        throw e
    }
}

fun Application.module() {
    val jdbc = EnvFileLoader.get("LINDA_DB_URL") ?: "jdbc:sqlite:./data/linda_binti_backend.db"
    val jwtSecret = JwtSecretSource.resolve(
        EnvFileLoader.get("LINDA_JWT_SECRET"),
        java.io.File("./data")
    )
    val minThreshold = EnvFileLoader.get("LINDA_MIN_THRESHOLD")?.toIntOrNull() ?: 3
    val seedAdminEmail = EnvFileLoader.get("LINDA_SEED_ADMIN_EMAIL") ?: "admin@linda.local"
    val seedAdminPassword = EnvFileLoader.get("LINDA_SEED_ADMIN_PASSWORD")?.trim().orEmpty()

    val db = when {
        jdbc == "jdbc:sqlite::memory:" || jdbc.endsWith(":memory:") -> Database.memory()
        jdbc.startsWith("jdbc:sqlite:") -> Database.fileDb(jdbc.removePrefix("jdbc:sqlite:"))
        else -> Database(jdbc)
    }
    db.migrate()

    val jwtService = JwtService(jwtSecret)
    val audit = AuditService(db)
    val users = UserRepository(db)
    val authService = AuthService(users, jwtService, audit, db)
    val aiConfig = AiConfig.fromEnv()
    val aiOrchestration = AiOrchestrationService.create(aiConfig, audit)
    val reportService = ReportService(db, audit, aiOrchestration)
    val aiAnalysisService = AiAnalysisService(aiOrchestration)
    val assistantChatService = AssistantChatService(aiOrchestration)
    val analyticsService = AnalyticsService(db, audit, minThreshold)
    val ngoIntelligenceService = NgoIntelligenceService(reportService, analyticsService, aiOrchestration, audit)
    val educationContentService = EducationContentService(db, audit)
    educationContentService.seedDemoIfEmpty()
    val ussdAuth = UssdGatewayAuth.fromEnv()
    val ussdChannel = UssdChannelService(users, reportService, audit, ussdAuth)
    ussdChannel.ensureChannelUser()

    DemoSeedService(users, reportService, audit).seedIfAppropriate(reportService.countReports())

    if (users.findByEmail(seedAdminEmail) == null) {
        if (seedAdminPassword.isNotEmpty()) {
            users.create(seedAdminEmail, seedAdminPassword, "NGO Admin", Role.NGO_ADMIN)
            audit.log("system", "SEED_ADMIN", "email=$seedAdminEmail")
        } else {
            val placeholder = SecurePasswordGenerator.generate()
            users.create(seedAdminEmail, placeholder, "NGO Admin", Role.NGO_ADMIN, mustChangePassword = true)
            audit.log("system", "SEED_ADMIN_PLACEHOLDER", "email=$seedAdminEmail;run_dev_reset=true")
            System.err.println(
                "NGO admin placeholder created for $seedAdminEmail. " +
                    "Run: gradlew :backend:devResetAdmin to generate a one-time development password."
            )
        }
    }

    val demoUserEmail = EnvFileLoader.get("LINDA_DEMO_USER_EMAIL") ?: "survivor@linda.local"
    val demoUserPassword = EnvFileLoader.get("LINDA_DEMO_USER_PASSWORD") ?: "LindaBinti1!"
    if (users.findByEmail(demoUserEmail) == null) {
        users.create(demoUserEmail, demoUserPassword, "Demo Survivor", Role.USER)
        audit.log("system", "SEED_DEMO_USER", "email=$demoUserEmail")
        println("Demo USER account: $demoUserEmail / $demoUserPassword (for phone sign-in)")
    }

    println(aiConfig.onlineStatusLabel)
    println("  ${aiConfig.configurationReason}")
    println(
        "USSD: ${if (ussdAuth.configured) "gateway key configured (Africa's Talking / JSON inbound)" else "stub — set LINDA_USSD_GATEWAY_SECRET for live USSD"}"
    )

    installLindaPlugins(jwtService)
    configureRoutes(
        authService,
        reportService,
        analyticsService,
        users,
        audit,
        jwtService,
        ussdChannel,
        aiAnalysisService,
        assistantChatService,
        ngoIntelligenceService,
        educationContentService,
        aiConfig = aiConfig
    )
}

fun Application.moduleForTests(
    db: Database = Database.memory(),
    jwtSecret: String = "test-secret-key-linda-binti-phase1-32b",
    minThreshold: Int = 3,
    seedAdmin: Boolean = false,
    adminMustChangePassword: Boolean = false,
    ussdGatewayAuth: UssdGatewayAuth = UssdGatewayAuth.disabled(),
    aiConfig: AiConfig = AiConfig("", "", "gpt-4o-mini", 8000, 0, false),
    aiHttpClient: AiHttpClient = JdkAiHttpClient()
) {
    db.migrate()
    val jwtService = JwtService(jwtSecret)
    val audit = AuditService(db)
    val users = UserRepository(db)
    val authService = AuthService(users, jwtService, audit, db)
    val aiOrchestration = AiOrchestrationService.create(aiConfig, audit, aiHttpClient)
    val reportService = ReportService(db, audit, aiOrchestration)
    val aiAnalysisService = AiAnalysisService(aiOrchestration)
    val assistantChatService = AssistantChatService(aiOrchestration)
    val analyticsService = AnalyticsService(db, audit, minThreshold)
    val ngoIntelligenceService = NgoIntelligenceService(reportService, analyticsService, aiOrchestration, audit)
    val educationContentService = EducationContentService(db, audit)
    educationContentService.seedDemoIfEmpty()
    val ussdChannel = UssdChannelService(users, reportService, audit, ussdGatewayAuth)
    if (seedAdmin) {
        users.create(
            "admin@linda.local",
            "ChangeMeAdmin1!",
            "NGO Admin",
            Role.NGO_ADMIN,
            mustChangePassword = adminMustChangePassword
        )
    }
    installLindaPlugins(jwtService)
    configureRoutes(
        authService,
        reportService,
        analyticsService,
        users,
        audit,
        jwtService,
        ussdChannel,
        aiAnalysisService,
        assistantChatService,
        ngoIntelligenceService,
        educationContentService,
        aiConfig = aiConfig
    )
}

private fun Application.installLindaPlugins(jwtService: JwtService) {
    // No CORS plugin: the Android app does not use browsers, and the dashboard is same-origin.
    // anyHost() CORS would let other websites read API responses if a token leaked.
    install(ContentNegotiation) { gson() }
    install(StatusPages) {
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(cause.message ?: "forbidden"))
        }
        exception<UssdUnauthorizedException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("ussd_gateway_unauthorized"))
        }
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error"))
        }
    }
    install(Authentication) {
        bearer("auth-jwt") {
            authenticate { cred ->
                runCatching { jwtService.parseAccess(cred.token) }.getOrNull()
            }
        }
    }
}