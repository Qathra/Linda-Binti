package com.lindabinti.backend.ussd

import com.lindabinti.backend.services.KajiadoGeoAreas

/**
 * Feature-phone USSD menu (Safaricom-style):
 * Dial → 1 GBV or 2 FGM → pick Kajiado area → type name → Send.
 * Africa's Talking concatenates replies with `*`.
 */
object UssdMenuService {

    const val GATEWAY_NOTE =
        "USSD channel for feature phones. Gateway must call POST /v1/ussd/inbound or " +
            "/v1/ussd/africas-talking. Complements (does not replace) 999/112/1195/116. " +
            "MSISDN is never stored."

    fun handle(sessionId: String, text: String, serviceCode: String = ""): UssdResult {
        val path = text.trim()
        val steps = path.split("*").map { it.trim() }.filter { it.isNotEmpty() }
        val first = steps.firstOrNull().orEmpty()

        return when {
            path.isBlank() -> welcome()
            first == "0" -> goodbye()
            first == "3" -> emergency()
            first == "1" -> reportFlow(UssdAction.REPORT_GBV, "GBV", steps.drop(1))
            first == "2" -> reportFlow(UssdAction.REPORT_FGM, "FGM", steps.drop(1))
            else -> welcome(prefix = "Unknown choice.\n")
        }
    }

    private fun reportFlow(action: UssdAction, kind: String, rest: List<String>): UssdResult {
        if (rest.isEmpty() || rest.firstOrNull() == "0" && rest.size == 1) {
            if (rest.firstOrNull() == "0") return goodbye()
            return UssdResult(
                message = "$kind report\nType your area, then send.\n" +
                    "1 Kajiado Central\n" +
                    "2 Kajiado East\n" +
                    "3 Kajiado North\n" +
                    "4 Kajiado West\n" +
                    "5 Kajiado South\n" +
                    "Or write the area name.\n" +
                    "0 Exit",
                endSession = false
            )
        }
        val (area, nameSoFar) = resolveAreaAndName(rest)
        if (area == null) {
            return UssdResult(
                message = "Area not recognized.\nReply 1 Central, 2 East, 3 North, 4 West or 5 South.",
                endSession = false
            )
        }
        val name = nameSoFar.trim()
        if (name.isBlank()) {
            return UssdResult(
                message = "Enter your full name, then send.",
                endSession = false
            )
        }
        val safeName = name.take(80)
        return UssdResult(
            message = "Thank you, $safeName. Your $kind report from ${area.label} was sent. " +
                "If in danger call 999 or 1195.",
            endSession = true,
            action = action,
            areaCode = area.code,
            areaLabel = area.label,
            reporterName = safeName
        )
    }

    private fun resolveAreaAndName(rest: List<String>): Pair<com.lindabinti.backend.services.ApprovedArea?, String> {
        fun parseArea(token: String) =
            KajiadoGeoAreas.fromMenuDigit(token) ?: KajiadoGeoAreas.parseFreeText(token)

        parseArea(rest[0])?.let { area ->
            return area to rest.drop(1).joinToString(" ").trim()
        }
        if (rest.size >= 2) {
            parseArea(rest.last())?.let { area ->
                return area to ""
            }
        }
        return null to ""
    }

    private fun welcome(prefix: String = ""): UssdResult = UssdResult(
        message = prefix +
            "Linda Binti (Kajiado GBV/FGM)\n" +
            "1 Report GBV\n" +
            "2 Report FGM\n" +
            "3 Emergency numbers\n" +
            "0 Exit",
        endSession = false
    )

    private fun goodbye(): UssdResult = UssdResult(
        message = "Linda Binti. Stay safe. Call 999 or 1195 if in danger.",
        endSession = true
    )

    private fun emergency(): UssdResult = UssdResult(
        message = "Emergency: Police 999 or 112. GBV helpline 1195. Childline 116. " +
            "A smartphone with Linda Binti can send SOS SMS to trusted contacts.",
        endSession = true
    )
}

enum class UssdAction {
    NONE,
    REPORT_GBV,
    REPORT_FGM,
    SAFETY_REQUEST
}

data class UssdResult(
    val message: String,
    val endSession: Boolean,
    val note: String = UssdMenuService.GATEWAY_NOTE,
    val action: UssdAction = UssdAction.NONE,
    val areaCode: String = "",
    val areaLabel: String = "",
    val reporterName: String = ""
)
