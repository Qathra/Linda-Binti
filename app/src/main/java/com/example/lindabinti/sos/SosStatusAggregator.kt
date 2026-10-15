package com.example.lindabinti.sos

/**
 * Pure rollup of per-contact delivery statuses into an event-level status.
 * Kept free of Android APIs so unit tests can cover the rules.
 */
object SosStatusAggregator {

    fun overallFromDeliveries(statuses: List<String>): String {
        if (statuses.isEmpty()) return "sending"
        return when {
            statuses.all { it == "delivered" } -> "delivered"
            statuses.all { it == "failed" } -> "failed"
            statuses.any { it == "failed" } &&
                statuses.any {
                    it == "delivered" || it == "sent" || it == "sent_unconfirmed" || it == "queued"
                } -> "partial"
            statuses.any { it == "delivered" } -> "partial"
            statuses.any { it == "sent" || it == "sent_unconfirmed" } -> "submitted"
            statuses.any { it == "queued" } -> "queued"
            else -> "sending"
        }
    }
}
