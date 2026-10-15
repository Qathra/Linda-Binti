package com.example.lindabinti.sos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SosStatusAggregatorTest {

    @Test
    fun allDelivered() {
        assertEquals("delivered", SosStatusAggregator.overallFromDeliveries(listOf("delivered", "delivered")))
    }

    @Test
    fun allFailed() {
        assertEquals("failed", SosStatusAggregator.overallFromDeliveries(listOf("failed", "failed")))
    }

    @Test
    fun partialWhenMixedSuccessAndFailure() {
        assertEquals(
            "partial",
            SosStatusAggregator.overallFromDeliveries(listOf("delivered", "failed"))
        )
    }

    @Test
    fun submittedWhenSent() {
        assertEquals(
            "submitted",
            SosStatusAggregator.overallFromDeliveries(listOf("sent", "queued"))
        )
    }

    @Test
    fun queuedWhenOnlyQueued() {
        assertEquals("queued", SosStatusAggregator.overallFromDeliveries(listOf("queued", "pending")))
    }

    @Test
    fun submittedWhenSentUnconfirmedStillNotDelivered() {
        assertEquals(
            "submitted",
            SosStatusAggregator.overallFromDeliveries(listOf("sent_unconfirmed"))
        )
        assertEquals(
            "submitted",
            SosStatusAggregator.overallFromDeliveries(listOf("sent"))
        )
    }

    @Test
    fun deliveredOnlyWhenAllDelivered() {
        assertEquals(
            "partial",
            SosStatusAggregator.overallFromDeliveries(listOf("delivered", "sent"))
        )
    }
}
