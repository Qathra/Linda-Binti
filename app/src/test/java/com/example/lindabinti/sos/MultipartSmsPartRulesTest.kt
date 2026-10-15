package com.example.lindabinti.sos

import org.junit.Assert.assertEquals
import org.junit.Test

class MultipartSmsPartRulesTest {

    @Test
    fun sent_anyPartFailureMarksFailed() {
        assertEquals(
            MultipartSmsPartRules.Outcome.MarkSendFailed,
            MultipartSmsPartRules.onSentResult(0, 3, resultOk = false)
        )
        assertEquals(
            MultipartSmsPartRules.Outcome.MarkSendFailed,
            MultipartSmsPartRules.onSentResult(2, 3, resultOk = false)
        )
    }

    @Test
    fun sent_onlyLastPartSuccessMarksSent() {
        assertEquals(
            MultipartSmsPartRules.Outcome.Ignore,
            MultipartSmsPartRules.onSentResult(0, 3, resultOk = true)
        )
        assertEquals(
            MultipartSmsPartRules.Outcome.MarkSent,
            MultipartSmsPartRules.onSentResult(2, 3, resultOk = true)
        )
    }

    @Test
    fun delivered_neverConfirmedWithoutSuccessCallback() {
        assertEquals(
            MultipartSmsPartRules.Outcome.MarkDeliveryFailed,
            MultipartSmsPartRules.onDeliveredResult(1, 2, resultOk = false)
        )
        assertEquals(
            MultipartSmsPartRules.Outcome.Ignore,
            MultipartSmsPartRules.onDeliveredResult(0, 2, resultOk = true)
        )
        assertEquals(
            MultipartSmsPartRules.Outcome.MarkDelivered,
            MultipartSmsPartRules.onDeliveredResult(1, 2, resultOk = true)
        )
    }

    @Test
    fun sent_resultCanceledIsNotHardFailure() {
        assertEquals(
            MultipartSmsPartRules.Outcome.Ignore,
            MultipartSmsPartRules.onSentResult(0, 1, resultCode = 0)
        )
    }

    @Test
    fun pendingRequestCodesAreDistinctPerPart() {
        val a = SmsSender.pendingRequestCode(42, 0, delivered = false)
        val b = SmsSender.pendingRequestCode(42, 1, delivered = false)
        val c = SmsSender.pendingRequestCode(42, 0, delivered = true)
        assertEquals(false, a == b)
        assertEquals(false, a == c)
    }

    @Test
    fun pendingRequestCodesAreDistinctAcrossFiveContactsAndRetries() {
        val codes = mutableSetOf<Int>()
        for (deliveryId in 1L..5L) {
            for (attempt in 1..2) {
                for (part in 0..2) {
                    for (delivered in listOf(false, true)) {
                        val code = SmsSender.pendingRequestCode(deliveryId, part, delivered, attempt)
                        assertEquals("collision at $deliveryId/$part/$delivered/$attempt", true, codes.add(code))
                    }
                }
            }
        }
    }
}
