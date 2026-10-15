package com.example.lindabinti.data.content

import com.example.lindabinti.data.sync.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportingFormRulesTest {

    @Test
    fun requiresCategoryBeforeSave() {
        assertFalse(ReportingFormRules.canSave(null))
        assertFalse(ReportingFormRules.canSave(""))
        assertFalse(ReportingFormRules.canSave("theft"))
        assertTrue(ReportingFormRules.canSave("physical"))
        assertTrue(ReportingFormRules.canSave("fgm_pressure"))
    }

    @Test
    fun descriptionIsOptional() {
        assertEquals("", ReportingFormRules.storedDescription("   "))
        assertEquals("unsafe to detail", ReportingFormRules.storedDescription("  unsafe to detail "))
    }

    @Test
    fun localReportsStayOnDeviceUntilQueued() {
        assertTrue(ReportingFormRules.isDeviceOnly(SyncStatus.LOCAL))
        assertFalse(ReportingFormRules.isDeviceOnly(SyncStatus.PENDING))
        assertFalse(ReportingFormRules.shouldAttemptSyncAfterSave(SyncStatus.LOCAL))
        assertTrue(ReportingFormRules.shouldAttemptSyncAfterSave(SyncStatus.PENDING))
        assertTrue(ReportingFormRules.shouldAttemptSyncAfterSave(SyncStatus.FAILED))
    }
}
