package com.example.lindabinti.data.offline

import com.example.lindabinti.data.content.EducationCatalog
import com.example.lindabinti.data.content.LocalContent
import com.example.lindabinti.data.content.SurvivorSupportCatalog
import com.example.lindabinti.data.sync.SyncStatus
import com.example.lindabinti.sos.SmsSender
import com.example.lindabinti.sos.SosDispatchPolicy
import com.example.lindabinti.sos.SosLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstDesignTest {

    @Test
    fun coreSafetyToolsAreDeclaredToWorkWithoutInternet() {
        val coreIds = OfflineCapabilities.offlineCore().map { it.id }.toSet()
        assertTrue(coreIds.containsAll(
            listOf(
                "education",
                "safety_guidance",
                "survivor_support",
                "risk_questions",
                "local_reports",
                "trusted_contacts",
                "sos_sms",
                "emergency_guidance",
                "support_services"
            )
        ))
        OfflineCapabilities.offlineCore().forEach { cap ->
            assertTrue(cap.id, cap.worksWithoutInternet)
            assertFalse(cap.id, cap.needsUssdProvider)
        }
    }

    @Test
    fun educationSupportRiskAndDirectoryAreBundledOnDevice() {
        assertTrue(EducationCatalog.topicIds.size >= 8)
        assertTrue(EducationCatalog.topics("en").isNotEmpty())
        assertTrue(EducationCatalog.topics("sw").isNotEmpty())
        assertTrue(LocalContent.educationModules("en").isNotEmpty())
        assertTrue(SurvivorSupportCatalog.truths("en").isNotEmpty())
        assertTrue(SurvivorSupportCatalog.messages("en").isNotEmpty())
        assertTrue(SurvivorSupportCatalog.nextSteps("en").isNotEmpty())
        assertEquals(5, LocalContent.riskQuestions("en").size)
        assertTrue(LocalContent.hotlines("en").any { it.number == "999" })
        assertTrue(LocalContent.hotlines("en").any { it.number == "1195" })
        assertTrue(LocalContent.supportServices("en").any { it.phone == "116" })
    }

    @Test
    fun reportsStayLocalWithoutConsentOrBackendSession() {
        assertEquals(SyncStatus.PENDING, SyncStatus.afterEnqueueFromLocal(false, false))
        assertEquals(SyncStatus.PENDING, SyncStatus.afterEnqueueFromLocal(false, true))
        assertEquals(SyncStatus.PENDING, SyncStatus.afterEnqueueFromLocal(true, false))
        assertEquals(SyncStatus.PENDING, SyncStatus.afterEnqueueFromLocal(true, true))
    }

    @Test
    fun sosSmsMessageIsBuiltOnDeviceWithoutBackend() {
        val unavailable = SmsSender.buildAlertMessage(
            SosLocation(0.0, 0.0, null, "unavailable"),
            1_710_000_000_000L
        )
        assertTrue(unavailable.startsWith("LINDA BINTI EMERGENCY ALERT"))
        assertTrue(unavailable.contains("Location pending."))
        assertFalse(unavailable.contains("http://"))
        assertFalse(SosDispatchPolicy.smsRequiresBackend())
        assertFalse(SosDispatchPolicy.smsRequiresAi())
        assertFalse(SosDispatchPolicy.smsRequiresInternet())
    }

    @Test
    fun sosNeedsSmsPermissionButNotBackendOrUssdProvider() {
        val sos = OfflineCapabilities.sosSms
        assertTrue(sos.worksWithoutInternet)
        assertEquals(ConnectivityNeed.CELLULAR_SMS, sos.need)
        assertTrue(sos.androidPermissions.contains("SEND_SMS"))
        assertFalse(sos.needsBackend)
        assertFalse(sos.needsUssdProvider)
    }

    @Test
    fun ussdChannelIsFutureProviderWorkAndDoesNotInventAShortcode() {
        val ussd = OfflineCapabilities.ussdFeaturePhone
        assertFalse(ussd.worksWithoutInternet)
        assertTrue(ussd.needsBackend)
        assertTrue(ussd.needsUssdProvider)
        assertEquals(ConnectivityNeed.USSD_PROVIDER, ussd.need)
        assertTrue(OfflineCapabilities.NO_LIVE_SHORTCODE.contains("not a USSD service"))
        assertFalse(OfflineCapabilities.NO_LIVE_SHORTCODE.contains("*123#"))
        assertTrue(OfflineCapabilities.NO_SAFARICOM_CLAIM.contains("No Safaricom"))
    }

    @Test
    fun cloudSyncAndDashboardRequireInternetAndBackend() {
        assertTrue(OfflineCapabilities.reportSync.needsBackend)
        assertFalse(OfflineCapabilities.reportSync.worksWithoutInternet)
        assertTrue(OfflineCapabilities.stakeholderDashboard.needsBackend)
        assertEquals(ConnectivityNeed.INTERNET, OfflineCapabilities.stakeholderDashboard.need)
    }
}
