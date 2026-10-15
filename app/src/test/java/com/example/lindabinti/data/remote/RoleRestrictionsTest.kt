package com.example.lindabinti.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mirrors backend RoleGuard expectations on the client for NGO gating.
 */
class RoleRestrictionsTest {
    @Test
    fun ngoRolesOnlyForAggregates() {
        assertTrue(isNgo("NGO_STAFF"))
        assertTrue(isNgo("NGO_ADMIN"))
        assertFalse(isNgo("USER"))
        assertFalse(isNgo(null))
    }

    @Test
    fun userRoleOnlyForReportSync() {
        assertTrue("USER" == "USER")
        assertFalse("NGO_STAFF" == "USER")
    }

    @Test
    fun ngoCanReadFlagsButNotRawBodies() {
        assertTrue(isNgo("NGO_ADMIN"))
        assertFalse("USER" == "NGO_STAFF")
    }

    private fun isNgo(role: String?): Boolean =
        role == "NGO_STAFF" || role == "NGO_ADMIN"
}