package com.example.lindabinti.sos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SosLocationRulesTest {

    private val now = 1_720_000_000_000L

    @Test
    fun freshFixWithinNinetySecondsIsCurrent() {
        assertEquals("ok", SosLocationRules.classifyAge(now - 1_000L, now))
        assertEquals("ok", SosLocationRules.classifyAge(now - SosLocationRules.FRESH_MAX_AGE_MS, now))
    }

    @Test
    fun cachedFixKeepsTimestampWindowAndIsNotCurrent() {
        val age = 20 * 60 * 1000L
        assertEquals("cached", SosLocationRules.classifyAge(now - age, now))
        assertEquals(
            "cached",
            SosLocationRules.classifyAge(now - SosLocationRules.CACHE_MAX_AGE_MS, now)
        )
    }

    @Test
    fun tooOldCacheIsDropped() {
        assertNull(
            SosLocationRules.classifyAge(
                now - SosLocationRules.CACHE_MAX_AGE_MS - 1,
                now
            )
        )
    }

    @Test
    fun emergencyStillUsesDayOldLastKnownAsCached() {
        val age = 12 * 60 * 60_000L
        assertNull(SosLocationRules.classifyAge(now - age, now))
        assertEquals("cached", SosLocationRules.classifyAgeForSos(now - age, now))
    }

    @Test
    fun timeoutAllowsColdGpsOnPhone() {
        assertEquals(2_000L, SosLocationRules.SOS_FIRST_SMS_WAIT_MS)
        assertEquals(35_000L, SosLocationRules.SOS_TIMEOUT_MS)
        assertTrue(SosLocationRules.SOS_FIRST_SMS_WAIT_MS < SosLocationRules.SOS_TIMEOUT_MS)
        assertEquals(60_000L, SosLocationRules.SOS_FOLLOWUP_TIMEOUT_MS)
        assertTrue(SosLocationRules.SOS_FOLLOWUP_TIMEOUT_MS > SosLocationRules.SOS_TIMEOUT_MS)
    }

    @Test
    fun gnssLocationDoesNotRequireInternet() {
        assertFalse(SosLocationRules.locationRequiresInternet())
    }

    @Test
    fun offlineGpsScenarioStillClassifiesAFreshFix() {
        // Mobile data/Wi-Fi off does not change GNSS age rules.
        val status = SosLocationRules.classifyAge(now - 5_000L, now)
        assertEquals("ok", status)
        assertFalse(SosLocationRules.locationRequiresInternet())
    }
}

class SosDispatchPolicyTest {

    @Test
    fun smsDoesNotDependOnInternetBackendOrAi() {
        assertFalse(SosDispatchPolicy.smsRequiresInternet())
        assertFalse(SosDispatchPolicy.smsRequiresBackend())
        assertFalse(SosDispatchPolicy.smsRequiresAi())
        assertFalse(SosDispatchPolicy.smsRequiresLocationFix())
    }

    @Test
    fun backendUnavailableDoesNotBlockSmsPolicy() {
        assertFalse(SosDispatchPolicy.smsRequiresBackend())
    }

    @Test
    fun aiUnavailableDoesNotBlockSmsPolicy() {
        assertFalse(SosDispatchPolicy.smsRequiresAi())
    }

    @Test
    fun smsStillSentWithoutInternet() {
        assertFalse(SosDispatchPolicy.smsRequiresInternet())
    }
}
