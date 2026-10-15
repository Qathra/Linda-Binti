package com.lindabinti.backend.services

import com.lindabinti.backend.EnvFileLoader
import com.lindabinti.backend.Role
import com.lindabinti.backend.dev.SecurePasswordGenerator

/**
 * Idempotent anonymized demo cases for NGO dashboard / panel demos.
 * Never stores survivor narratives — category + sub-county only.
 */
class DemoSeedService(
    private val users: UserRepository,
    private val reportService: ReportService,
    private val audit: AuditService
) {
    data class DemoCase(
        val clientUuid: String,
        val categoryId: String,
        val categoryLabel: String,
        val areaCode: String,
        val areaLabel: String,
        val daysAgo: Int
    )

    fun seedIfAppropriate(existingReportCount: Int) {
        if (!shouldSeed(existingReportCount)) return
        val demoUser = users.findByEmail(DEMO_EMAIL) ?: users.create(
            email = DEMO_EMAIL,
            password = SecurePasswordGenerator.generate(),
            displayName = "Panel Demo",
            role = Role.USER
        )
        val now = System.currentTimeMillis()
        DEMO_CASES.forEach { demo ->
            val createdAt = now - demo.daysAgo.toLong() * 86_400_000L
            reportService.upsertOwnedReport(
                userId = demoUser.id,
                clientUuid = demo.clientUuid,
                categoryId = demo.categoryId,
                categoryLabel = demo.categoryLabel,
                body = "",
                createdAt = createdAt,
                includeBody = false,
                areaCode = demo.areaCode,
                areaLabel = demo.areaLabel,
                areaType = ApprovedArea.TYPE_SUB_COUNTY
            )
        }
        audit.log("system", "DEMO_SEED_APPLIED", "cases=${DEMO_CASES.size}")
        println("Demo seed: ${DEMO_CASES.size} anonymized panel cases loaded for NGO dashboard map.")
    }

    private fun shouldSeed(existingReportCount: Int): Boolean {
        val flag = EnvFileLoader.get("LINDA_DEMO_SEED")?.trim().orEmpty()
        return when {
            flag.equals("0", ignoreCase = true) || flag.equals("false", ignoreCase = true) -> false
            flag.equals("1", ignoreCase = true) || flag.equals("true", ignoreCase = true) -> true
            existingReportCount == 0 -> true
            else -> false
        }
    }

    companion object {
        const val DEMO_EMAIL = "panel-demo@linda.local"

        val DEMO_CASES = listOf(
            DemoCase(
                clientUuid = "demo-panel-kajiado-east-fgm",
                categoryId = "fgm_pressure",
                categoryLabel = "FGM threat",
                areaCode = "kajiado_east",
                areaLabel = "Kajiado East",
                daysAgo = 1
            ),
            DemoCase(
                clientUuid = "demo-panel-kajiado-north-dv",
                categoryId = "domestic",
                categoryLabel = "Domestic violence",
                areaCode = "kajiado_north",
                areaLabel = "Kajiado North",
                daysAgo = 2
            ),
            DemoCase(
                clientUuid = "demo-panel-kajiado-central-threat",
                categoryId = "threats",
                categoryLabel = "Threats",
                areaCode = "kajiado_central",
                areaLabel = "Kajiado Central",
                daysAgo = 3
            ),
            DemoCase(
                clientUuid = "demo-panel-kajiado-south-fgm",
                categoryId = "fgm_incident",
                categoryLabel = "FGM incident",
                areaCode = "kajiado_south",
                areaLabel = "Kajiado South",
                daysAgo = 4
            ),
            DemoCase(
                clientUuid = "demo-panel-kajiado-west-harass",
                categoryId = "harassment",
                categoryLabel = "Harassment",
                areaCode = "kajiado_west",
                areaLabel = "Kajiado West",
                daysAgo = 5
            )
        )
    }
}
