package com.lindabinti.backend.ai

/**
 * On-server safety knowledge used when the cloud LLM is unavailable (rate limit, timeout).
 * Mirrors the app's offline FAQ patterns — not a substitute for live ChatGPT when quota allows.
 */
class RulesAssistantChatProvider : AssistantChatProvider {
    override val providerId: String = "rules-assistant"

    override fun isConfigured(): Boolean = true

    override fun chat(input: AssistantChatInput): AssistantChatResult {
        val started = System.currentTimeMillis()
        val normalized = normalize(input.message)
        val lang = input.language.trim().lowercase().take(8)
        val matched = matchFaq(normalized, lang)
        return AssistantChatResult(
            reply = matched.reply,
            relatedAction = matched.relatedAction,
            riskLevel = matched.riskLevel,
            providerId = providerId,
            modelId = "rules-v1",
            processingDurationMs = System.currentTimeMillis() - started,
            aiStatus = AiStatus.COMPLETE
        )
    }

    private data class Matched(
        val reply: String,
        val relatedAction: String?,
        val riskLevel: String
    )

    private fun matchFaq(normalized: String, lang: String): Matched {
        val dangerHits = listOf(
            "danger", "help me now", "attack", "unsafe", "threat", "violent",
            "scared to go home", "about to undergo", "forcing my daughter",
            "hatari", "nisaidie sasa", "ogopa kurudi", "karibia kufanyiwa"
        )
        if (dangerHits.any { normalized.contains(it) }) {
            return when {
                normalized.contains("fgm") || normalized.contains("cut") || normalized.contains("keketa") ->
                    fgmSister(lang)
                normalized.contains("husband") || normalized.contains("partner") || normalized.contains("mume") ->
                    husbandThreats(lang)
                normalized.contains("home") || normalized.contains("nyumbani") ->
                    scaredHome(lang)
                else -> immediateDanger(lang)
            }
        }

        fromKnowledgeBase(normalized)?.let { return it }

        val faqs = listOf(
            FaqRule(listOf("fgm", "cut", "circumcision", "mutilation", "illegal", "law", "ukeketaji", "sheria"), ::fgmLaw),
            FaqRule(listOf("sister", "daughter", "girl", "ceremony", "planned", "dada", "binti", "msichana"), ::fgmSister),
            FaqRule(listOf("husband", "partner", "threaten", "threatening", "beat", "mume", "kupiga", "tisho"), ::husbandThreats),
            FaqRule(listOf("report", "gbv", "police", "ripoti", "polisi"), ::reportGbv),
            FaqRule(listOf("right", "law", "haki", "sheria"), ::rights),
            FaqRule(
                listOf(
                    "stigma", "stigmatized", "shame", "ashamed", "blame", "blamed",
                    "judged", "gossip", "aibu", "unyanyapaa"
                ),
                ::stigma
            ),
            FaqRule(listOf("sos", "emergency", "calculator", "dharura"), ::sosHelp),
            FaqRule(listOf("kajiado", "help", "support", "msaada", "where"), ::kajiadoHelp)
        )

        var best: (String) -> Matched = ::genericHelp
        var bestScore = 0
        for (rule in faqs) {
            val score = rule.keywords.count { keyword -> normalized.contains(keyword) }
            if (score > bestScore) {
                bestScore = score
                best = rule.matcher
            }
        }
        return if (bestScore > 0) best(lang) else genericHelp(lang)
    }

    private fun fromKnowledgeBase(message: String): Matched? {
        val hits = AssistantKnowledgeBase.retrieve(message, limit = 3)
        if (hits.isEmpty()) return null
        val reply = hits.joinToString(" ") { it.body }.let { text ->
            if (text.length <= 1100) text else text.take(1097).trimEnd() + "…"
        }
        val action = hits.first().relatedAction
        val risk = when (action) {
            "emergency" -> "HIGH"
            else -> "LOW"
        }
        return Matched(reply = reply, relatedAction = action, riskLevel = risk)
    }

    private data class FaqRule(
        val keywords: List<String>,
        val matcher: (String) -> Matched
    )

    private fun immediateDanger(lang: String) = Matched(
        reply = when {
            lang.startsWith("sw") ->
                "Ukiwa hatarini mara moja, fungua Msaada wa Dharura, piga 999 au 112, au tuma SOS kutoka calculator. Tumia Ficha mtu akiwa karibu na simu."
            else ->
                "If you are in immediate danger, open Emergency Assistance, call 999 or 112, or send SOS from the calculator with your secret code. Use Hide if someone is near the phone."
        },
        relatedAction = "emergency",
        riskLevel = "CRITICAL"
    )

    private fun fgmLaw(lang: String) = Matched(
        reply = when {
            lang.startsWith("sw") ->
                "FGM ni kinyume cha sheria nchini Kenya. Hakuna anayeruhusiwa kufanya FGM. Msichana akiwa hatarini, piga Childline 116 au GBV 1195."
            else ->
                "FGM is illegal in Kenya under the Prohibition of Female Genital Mutilation Act, 2011. If a girl is at risk, call Childline 116 or GBV helpline 1195."
        },
        relatedAction = "education",
        riskLevel = "MEDIUM"
    )

    private fun fgmSister(lang: String) = Matched(
        reply = when {
            lang.startsWith("sw") ->
                "Hatari ya FGM ni ya dharura. Usimwache dada/binti peke yake. Piga 116 au 1195, arifu mtu wa kuaminika, na tumia Msaada wa Dharura ikiwa ni hatari sasa."
            else ->
                "FGM risk is urgent. Do not leave the girl alone. Call Childline 116 or GBV 1195, alert a trusted contact, and use Emergency Assistance if danger is immediate."
        },
        relatedAction = "emergency",
        riskLevel = "CRITICAL"
    )

    private fun husbandThreats(lang: String) = Matched(
        reply = when {
            lang.startsWith("sw") ->
                "Tishio la mume/mpenzi si sawa. Andika tukio, usikae peke yako ikiwa hatari iko, na piga 1195 au 999. Unaweza kuripoti GBV kupitia programu."
            else ->
                "Threats from a partner are not acceptable. Note what happened, avoid being alone if you feel unsafe, and call 1195 or 999. You can also report GBV through the app."
        },
        relatedAction = "reporting",
        riskLevel = "HIGH"
    )

    private fun scaredHome(lang: String) = Matched(
        reply = when {
            lang.startsWith("sw") ->
                "Usirudi nyumbani ikiwa unaogopa. Nenda kwa rafiki au kituo salama, piga 1195, na tumia Msaada wa Dharura ikiwa unahitaji msaada wa haraka."
            else ->
                "Do not return home if you feel unsafe. Go to a trusted friend or safe place, call 1195, and use Emergency Assistance if you need urgent help."
        },
        relatedAction = "emergency",
        riskLevel = "CRITICAL"
    )

    private fun reportGbv(lang: String) = Matched(
        reply = when {
            lang.startsWith("sw") ->
                "Fungua Ripoti katika programu, chagua aina ya tukio, na hifadhi ripoti. Unaweza kuongeza maelezo na kuchambua kwa AI ikiwa unataka."
            else ->
                "Open Report in the app, choose the incident type, and save your report. You can add details and run AI analysis if you choose."
        },
        relatedAction = "reporting",
        riskLevel = "LOW"
    )

    private fun rights(lang: String) = Matched(
        reply = when {
            lang.startsWith("sw") ->
                "Una haki ya usalama, heshima, na kulindwa dhidi ya FGM na GBV. Sheria ya Kenya inakupa ulinzi — tumia msaada wa dharura na ripoti ikiwa unahitaji."
            else ->
                "You have the right to safety, dignity, and protection from FGM and GBV. Kenyan law protects you — use emergency help and reporting when needed."
        },
        relatedAction = "education",
        riskLevel = "NONE"
    )

    private fun stigma(lang: String) = Matched(
        reply = when {
            lang.startsWith("sw") ->
                "Aibu na unyanyapaa baada ya GBV au FGM ni kawaida, na haimaanishi umefanya kosa. Bado una haki ya usalama, huduma, na faragha. Ongea na mtu unayemwamini ikiwa ni salama, tumia Msaada katika programu, na piga 1195. Ukiwa hatarini sasa, fungua Msaada wa Dharura au piga 999/112."
            else ->
                "Stigma and shame after GBV or FGM are common, and they do not mean you did anything wrong. You still have a right to safety, care, and confidentiality. Talk to someone you trust when it is safe, use Survivor Support in this app, and call GBV helpline 1195. If you are unsafe now, open Emergency Assistance or call 999/112."
        },
        relatedAction = "support",
        riskLevel = "LOW"
    )

    private fun sosHelp(lang: String) = Matched(
        reply = when {
            lang.startsWith("sw") ->
                "SOS inatumwa kutoka calculator kwa msimbo wako wa siri ulioweka wakati wa kusakinisha, kisha =. Waarifu waaminifu wako na eneo linalopatikana. Usishiriki msimbo na mtu yeyote."
            else ->
                "SOS is sent from the calculator using the secret code you set during setup, then Equals. Trusted contacts receive your alert and location when available. Never share your code."
        },
        relatedAction = "settings",
        riskLevel = "NONE"
    )

    private fun kajiadoHelp(lang: String) = Matched(
        reply = when {
            lang.startsWith("sw") ->
                "Kajiado: tumia Msaada wa Dharura, Wasiliana (waaminifu), Elimu, na Ripoti. Simu: 999, 112, 1195 (GBV), 116 (Childline)."
            else ->
                "In Kajiado County: use Emergency Assistance, Contacts, Education, and Report. Verified hotlines: 999, 112, 1195 (GBV), 116 (Childline)."
        },
        relatedAction = "support",
        riskLevel = "NONE"
    )

    private fun genericHelp(lang: String) = Matched(
        reply = when {
            lang.startsWith("sw") ->
                "Niko hapa kukusaidia kuhusu FGM, GBV, haki, na usalama Kajiado. Jaribu swali lingine au chagua ombi la haraka. Kwa hatari mara moja tumia Msaada wa Dharura."
            else ->
                "I hear you. I can help with GBV, FGM, rights, and safety in Kajiado. Feeling judged or alone does not mean you did anything wrong. Use Survivor Support or Education in this app, and call 1195 when you want to talk. If you are in danger now, open Emergency Assistance or call 999/112."
        },
        relatedAction = null,
        riskLevel = "NONE"
    )

    private fun normalize(text: String): String =
        text.trim().lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
