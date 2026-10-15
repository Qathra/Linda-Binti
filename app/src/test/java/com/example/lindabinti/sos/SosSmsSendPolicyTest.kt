package com.example.lindabinti.sos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SosSmsSendPolicyTest {

    @Test
    fun fiveContactsAreStaggeredNotBursted() {
        assertTrue(SosSmsSendPolicy.BETWEEN_CONTACTS_MS >= 500L)
        val fiveContactGap = SosSmsSendPolicy.BETWEEN_CONTACTS_MS * 4
        assertTrue(fiveContactGap >= 2_000L)
    }

    @Test
    fun oneCleanSendNoRetryBurst() {
        assertEquals(1, SosSmsSendPolicy.MAX_ATTEMPTS)
        assertFalse(SosSmsSendPolicy.shouldRetry(1, resultCode = 1))
        assertFalse(SosSmsSendPolicy.shouldRetry(1, resultCode = null))
        assertFalse(SosSmsSendPolicy.shouldRetry(1, SosSmsSendPolicy.SENT_NETWORK_ERROR))
        assertFalse(SosSmsSendPolicy.shouldRetry(1, SosSmsSendPolicy.SENT_OK))
    }

    @Test
    fun canceledAndGenericAreNotHardRadioFailures() {
        assertFalse(SosSmsSendPolicy.isHardRadioFailure(0))
        assertFalse(SosSmsSendPolicy.isHardRadioFailure(1))
        assertTrue(SosSmsSendPolicy.isHardRadioFailure(2))
        assertTrue(SosSmsSendPolicy.isHardRadioFailure(4))
        assertTrue(SosSmsSendPolicy.isHardRadioFailure(SosSmsSendPolicy.SENT_NETWORK_ERROR))
    }

    @Test
    fun sendUsesE164AndDefaultSmsc() {
        val dests = listOf("+254712345678", "0712345678", "254712345678")
        assertEquals("+254712345678", SosSmsSendPolicy.pickDestination(dests, 1))
        assertEquals("", SosSmsSendPolicy.pickDestination(emptyList(), 1))
        assertNull(SosSmsSendPolicy.smscForAttempt(1))
        assertNull(SosSmsSendPolicy.smscForAttempt(2))
    }

    @Test
    fun limitExceededWaitsLonger() {
        assertTrue(
            SosSmsSendPolicy.retryDelayMs(SosSmsSendPolicy.SENT_LIMIT_EXCEEDED) >
                SosSmsSendPolicy.retryDelayMs(1)
        )
    }
}
