package com.lindabinti.backend

import com.lindabinti.backend.db.Database
import com.lindabinti.backend.security.JwtService
import com.lindabinti.backend.security.RoleGuard
import com.lindabinti.backend.services.AnalyticsEventIn
import com.lindabinti.backend.services.AnalyticsService
import com.lindabinti.backend.services.AuditService
import com.lindabinti.backend.services.AuthService
import com.lindabinti.backend.services.ReportRiskClassifier
import com.lindabinti.backend.services.ReportService
import com.lindabinti.backend.services.AiOrchestrationService
import com.lindabinti.backend.services.UserRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackendPhase1Test {

    private fun harness(): Harness {
        val db = Database.memory()
        db.migrate()
        val audit = AuditService(db)
        val users = UserRepository(db)
        val jwt = JwtService("test-secret-key-linda-binti-phase1-32b")
        val auth = AuthService(users, jwt, audit, db)
        val reports = ReportService(db, audit, AiOrchestrationService.rulesOnly(audit))
        val analytics = AnalyticsService(db, audit, minThreshold = 3)
        return Harness(auth, reports, analytics, users, audit, jwt)
    }

    @Test
    fun registerAndLoginIssuesJwtWithUserRole() {
        val h = harness()
        val reg = h.auth.registerUser("survivor@example.com", "Password123!", "Amina")
        assertEquals(Role.USER, reg.role)
        val login = h.auth.login("survivor@example.com", "Password123!")
        assertEquals(reg.userId, login.userId)
        val claims = h.jwt.parseAccess(login.tokens.accessToken)
        assertEquals(Role.USER, claims.role)
        try {
            h.jwt.parseAccess(login.tokens.refreshToken)
            fail("refresh token must not parse as access")
        } catch (_: Exception) {
        }
    }

    @Test
    fun reportIdempotencyPreventsDuplicates() {
        val h = harness()
        val user = h.auth.registerUser("u1@example.com", "Password123!", "U")
        val a = h.reports.upsertOwnedReport(user.userId, "uuid-1", "physical", "Physical", "secret", 1L, true)
        val b = h.reports.upsertOwnedReport(user.userId, "uuid-1", "physical", "Physical", "secret-changed", 1L, true)
        assertEquals(a.serverId, b.serverId)
        assertEquals(1, h.reports.listForUser(user.userId).size)
    }

    @Test
    fun userCannotSeeOtherUsersReports() {
        val h = harness()
        val u1 = h.auth.registerUser("a@example.com", "Password123!", "A")
        val u2 = h.auth.registerUser("b@example.com", "Password123!", "B")
        h.reports.upsertOwnedReport(u1.userId, "c1", "threats", "Threats", "private-a", 1L, true)
        h.reports.upsertOwnedReport(u2.userId, "c2", "threats", "Threats", "private-b", 1L, true)
        assertEquals(1, h.reports.listForUser(u1.userId).size)
        assertEquals("private-a", h.reports.listForUser(u1.userId).first().body)
        assertFalse(h.reports.listForUser(u1.userId).any { it.body.contains("private-b") })
    }

    @Test
    fun ngoStaffRoleCannotReadRawReportsGuard() {
        assertTrue(RoleGuard.canReadAggregates(Role.NGO_STAFF))
        assertFalse(RoleGuard.canReadRawReports(Role.NGO_STAFF))
        assertFalse(RoleGuard.canManageStaff(Role.NGO_STAFF))
        assertTrue(RoleGuard.canManageStaff(Role.NGO_ADMIN))
        try {
            RoleGuard.requireRole(Role.USER, Role.NGO_STAFF)
            fail("expected forbidden")
        } catch (_: Exception) {
        }
    }

    @Test
    fun aggregateThresholdSuppressesSmallBuckets() {
        val h = harness()
        val user = h.auth.registerUser("u@example.com", "Password123!", "U")
        repeat(2) { index ->
            h.reports.upsertOwnedReport(
                user.userId,
                "phys-$index",
                "physical",
                "Physical violence",
                "need support",
                System.currentTimeMillis(),
                includeBody = true
            )
        }
        repeat(5) { index ->
            h.reports.upsertOwnedReport(
                user.userId,
                "fgm-$index",
                "fgm_pressure",
                "FGM threat",
                "need support",
                System.currentTimeMillis(),
                includeBody = true
            )
        }
        h.analytics.ingest(
            user.userId,
            listOf(AnalyticsEventIn("education", "module", "2026-W01", 2))
        )
        val admin = h.users.create("admin@x.com", "Password123!", "Admin", Role.NGO_ADMIN)
        val summary = h.analytics.summaryForNgo(admin.id)
        assertTrue(summary.reportsByCategory.any { it.key == "FGM" && it.count == 5 })
        assertTrue(summary.reportsByCategory.any { it.key == "GBV" && it.count == 2 })
        assertEquals(0, summary.educationCompletions)
    }

    @Test
    fun includeBodyFalseStoresEmptyBody() {
        val h = harness()
        val user = h.auth.registerUser("u2@example.com", "Password123!", "U")
        val stored = h.reports.upsertOwnedReport(
            user.userId, "uuid-x", "emotional", "Emotional", "sensitive text", 1L, includeBody = false
        )
        assertEquals("", stored.body)
        assertNotEquals("sensitive text", stored.body)
    }

    @Test
    fun classifierTreatsFgmAndSexualAsHigh() {
        val fgm = ReportRiskClassifier.classify("fgm", null)
        val pressure = ReportRiskClassifier.classify("fgm_pressure", "")
        val sexual = ReportRiskClassifier.classify("sexual", "")
        assertEquals("HIGH", fgm.urgency)
        assertEquals("HIGH", pressure.urgency)
        assertEquals("HIGH", sexual.urgency)
        assertFalse(fgm.explanation.contains("secret", ignoreCase = true))
    }

    @Test
    fun classifierBumpsImminentLanguageToCritical() {
        val flagged = ReportRiskClassifier.classify("fgm", "They plan to cut her tonight")
        assertEquals("CRITICAL", flagged.urgency)
        assertFalse(flagged.explanation.contains("tonight"))
        assertTrue(flagged.explanation.contains("not a medical", ignoreCase = true))
    }

    @Test
    fun ngoFlagsOmitPrivateBodyAndUserId() {
        val h = harness()
        val user = h.auth.registerUser("survivor@example.com", "Password123!", "Amina")
        h.reports.upsertOwnedReport(
            user.userId, "c-secret", "sexual", "Sexual violence", "private survivor text", 1L, true
        )
        val staff = h.users.create("staff@ngo.local", "Password123!", "Staff", Role.NGO_STAFF)
        val flags = h.reports.listFlagsForNgo(staff.id)
        assertEquals(1, flags.size)
        assertEquals("sexual", flags[0].categoryId)
        assertEquals("HIGH", flags[0].urgency)
        val blob = flags.joinToString { "${it.explanation} ${it.categoryLabel} ${it.serverReportId}" }
        assertFalse(blob.contains("private survivor text"))
        assertFalse(blob.contains(user.userId))
        assertFalse(blob.contains("survivor@example.com"))
        assertFalse(RoleGuard.canReadRawReports(Role.NGO_STAFF))
        assertTrue(RoleGuard.canReadReportFlags(Role.NGO_STAFF))
        assertFalse(RoleGuard.canReadReportFlags(Role.USER))
        assertFalse(RoleGuard.canAccessDashboard(Role.USER))
        assertTrue(RoleGuard.canAccessDashboard(Role.NGO_STAFF))
    }
}

private data class Harness(
    val auth: AuthService,
    val reports: ReportService,
    val analytics: AnalyticsService,
    val users: UserRepository,
    val audit: AuditService,
    val jwt: JwtService
)