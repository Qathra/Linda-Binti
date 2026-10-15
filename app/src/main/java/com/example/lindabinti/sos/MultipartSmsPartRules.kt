package com.example.lindabinti.sos

/**
 * Pure rules for multipart SMS SENT/DELIVERED parts.
 * Any part failure fails the delivery; success only finalizes on the last part.
 */
object MultipartSmsPartRules {

    enum class Outcome {
        Ignore,
        MarkSent,
        MarkSendFailed,
        MarkDelivered,
        MarkDeliveryFailed
    }

    fun onSentResult(partIndex: Int, partCount: Int, resultOk: Boolean): Outcome =
        onSentResult(partIndex, partCount, if (resultOk) -1 else 1)

    /**
     * SENT pending-intent result codes.
     * -1 = RESULT_OK. 0 = RESULT_CANCELED (Samsung/IMS often fires this; not a hard fail).
     */
    fun onSentResult(partIndex: Int, partCount: Int, resultCode: Int): Outcome {
        if (resultCode == -1) {
            return if (partIndex == partCount - 1) Outcome.MarkSent else Outcome.Ignore
        }
        if (resultCode == 0) return Outcome.Ignore
        return Outcome.MarkSendFailed
    }

    fun onDeliveredResult(partIndex: Int, partCount: Int, resultOk: Boolean): Outcome {
        if (!resultOk) return Outcome.MarkDeliveryFailed
        return if (partIndex == partCount - 1) Outcome.MarkDelivered else Outcome.Ignore
    }
}
