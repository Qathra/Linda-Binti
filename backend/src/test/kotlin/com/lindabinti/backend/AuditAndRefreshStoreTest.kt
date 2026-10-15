package com.lindabinti.backend

import com.lindabinti.backend.db.Database
import com.lindabinti.backend.services.AuditService
import com.lindabinti.backend.services.RefreshTokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditAndRefreshStoreTest {

    @Test
    fun auditListRecentReturnsNewestFirstWithoutBodies() {
        val db = Database.memory()
        db.migrate()
        val audit = AuditService(db)
        audit.log("u1", "LOGIN", "email=a@x.com")
        audit.log("u1", "CASE_STATUS", "reportId=r1;status=OPEN")
        val rows = audit.listRecent(10)
        assertEquals(2, rows.size)
        assertEquals("CASE_STATUS", rows[0].action)
        assertEquals("LOGIN", rows[1].action)
        assertFalse(rows.any { it.detail.contains("survivor narrative") })
    }

    @Test
    fun refreshTokenStoreRevokeAndRevokeAll() {
        val db = Database.memory()
        db.migrate()
        val store = RefreshTokenStore(db)
        store.store("t1", "user-a")
        store.store("t2", "user-a")
        store.store("t3", "user-b")
        assertTrue(store.isActive("t1"))
        store.revoke("t1")
        assertFalse(store.isActive("t1"))
        assertTrue(store.isActive("t2"))
        store.revokeAllForUser("user-a")
        assertFalse(store.isActive("t2"))
        assertTrue(store.isActive("t3"))
        assertFalse(store.isActive("missing"))
    }
}
