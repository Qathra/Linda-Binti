package com.lindabinti.backend.services

import com.lindabinti.backend.ai.AiConfig
import com.lindabinti.backend.db.Database
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoSeedServiceTest {

    @Test
    fun seedsFiveAnonymizedCasesOnEmptyDatabase() {
        val db = Database.memory()
        db.migrate()
        val users = UserRepository(db)
        val audit = AuditService(db)
        val ai = AiOrchestrationService.create(
            AiConfig("", "", "gpt-4o-mini", 8000, 0, false),
            audit
        )
        val reports = ReportService(db, audit, ai)
        DemoSeedService(users, reports, audit).seedIfAppropriate(0)
        assertEquals(5, reports.countReports())
        val flags = reports.listFlagsForNgo("test-admin", limit = 20)
        assertEquals(5, flags.size)
        assertTrue(flags.all { it.areaLabel.isNotBlank() })
    }

    @Test
    fun doesNotDuplicateOnSecondRun() {
        val db = Database.memory()
        db.migrate()
        val users = UserRepository(db)
        val audit = AuditService(db)
        val ai = AiOrchestrationService.create(
            AiConfig("", "", "gpt-4o-mini", 8000, 0, false),
            audit
        )
        val reports = ReportService(db, audit, ai)
        val seed = DemoSeedService(users, reports, audit)
        seed.seedIfAppropriate(0)
        seed.seedIfAppropriate(reports.countReports())
        assertEquals(5, reports.countReports())
    }
}
