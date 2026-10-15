package com.lindabinti.backend.ussd

/**
 * Africa's Talking USSD callback adapter.
 * Incoming: application/x-www-form-urlencoded (sessionId, phoneNumber, serviceCode, text).
 * Outgoing: plain text with CON / END prefix.
 * MSISDN is accepted for session routing by AT but must never be persisted by Linda Binti.
 */
object AfricasTalkingUssdAdapter {

    fun toAtResponse(result: UssdInboundMenu): String {
        val prefix = if (result.endSession) "END " else "CON "
        return prefix + result.message.trim()
    }

    data class FormFields(
        val sessionId: String,
        val serviceCode: String,
        val text: String,
        /** Present on the wire; never store. */
        val phoneNumberPresent: Boolean
    )

    fun parseForm(params: Map<String, String>): FormFields {
        fun pick(vararg keys: String): String {
            for (k in keys) {
                val v = params[k]?.trim().orEmpty()
                if (v.isNotEmpty()) return v
                val ci = params.entries.firstOrNull { it.key.equals(k, ignoreCase = true) }?.value?.trim()
                if (!ci.isNullOrEmpty()) return ci
            }
            return ""
        }
        val phone = pick("phoneNumber", "msisdn", "phone")
        return FormFields(
            sessionId = pick("sessionId", "session_id").ifBlank { "at-unknown" },
            serviceCode = pick("serviceCode", "service_code"),
            text = pick("text", "ussdServiceCode"),
            phoneNumberPresent = phone.isNotEmpty()
        )
    }

    data class UssdInboundMenu(
        val message: String,
        val endSession: Boolean
    )
}
