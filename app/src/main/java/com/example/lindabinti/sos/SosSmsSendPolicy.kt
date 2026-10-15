package com.example.lindabinti.sos

/**
 * One clean SOS text per contact. Extra retries lag the alert and make
 * Safaricom IMS reject later sends.
 */
object SosSmsSendPolicy {
    const val BETWEEN_CONTACTS_MS = 800L
    const val RETRY_DELAY_MS = 8_000L
    const val LIMIT_RETRY_DELAY_MS = 10_000L
    const val SENT_WAIT_MS = 3_000L
    const val MAX_ATTEMPTS = 1

    const val SENT_OK = -1
    const val SENT_LIMIT_EXCEEDED = 5
    const val SENT_NETWORK_ERROR = 17

    fun isSentOk(resultCode: Int?): Boolean = resultCode == SENT_OK

    fun isHardRadioFailure(resultCode: Int): Boolean =
        resultCode == 2 ||
            resultCode == 4 ||
            resultCode == 6 ||
            resultCode == 7 ||
            resultCode == 8 ||
            resultCode == SENT_NETWORK_ERROR

    fun retryDelayMs(resultCode: Int?): Long =
        if (resultCode == SENT_LIMIT_EXCEEDED) LIMIT_RETRY_DELAY_MS else RETRY_DELAY_MS

    fun shouldRetry(attempt: Int, resultCode: Int?): Boolean {
        if (attempt >= MAX_ATTEMPTS) return false
        if (isSentOk(resultCode)) return false
        return true
    }

    fun pickDestination(destinations: List<String>, attempt: Int): String {
        if (destinations.isEmpty()) return ""
        return destinations.first()
    }

    fun smscForAttempt(attempt: Int): String? = null
}
