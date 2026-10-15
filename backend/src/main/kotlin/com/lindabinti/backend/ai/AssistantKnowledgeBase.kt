package com.lindabinti.backend.ai

/**
 * Curated Kajiado GBV/FGM knowledge snippets for RAG-style retrieval (keyword ranking, no embeddings).
 * Facts only — verified hotlines, law pointers, safety steps. No graphic detail.
 */
object AssistantKnowledgeBase {

    data class Snippet(
        val id: String,
        val title: String,
        val body: String,
        val keywords: List<String>,
        val relatedAction: String? = null
    )

    val snippets: List<Snippet> = listOf(
        Snippet(
            id = "hotlines",
            title = "Verified emergency numbers (Kenya)",
            body = "Police emergency: 999 or 112. GBV helpline: 1195. Childline: 116. " +
                "In Linda Binti, open Emergency Assistance or send SOS from the calculator using your secret code.",
            keywords = listOf(
                "emergency", "danger", "help", "hotline", "999", "112", "1195", "116",
                "police", "call", "attack", "unsafe", "hatari", "nisaidie"
            ),
            relatedAction = "emergency"
        ),
        Snippet(
            id = "fgm_law",
            title = "FGM is illegal in Kenya",
            body = "FGM is illegal in Kenya under the Prohibition of Female Genital Mutilation Act, 2011. " +
                "Nobody may perform FGM, arrange it, or take a girl for FGM. If a girl is at risk, call Childline 116 or GBV 1195 " +
                "and alert a trusted person. Use Education in the app for more rights information.",
            keywords = listOf(
                "fgm", "cut", "cutting", "circumcision", "mutilation", "illegal", "law",
                "sister", "daughter", "girl", "ceremony", "ukeketaji", "keketa", "sheria"
            ),
            relatedAction = "education"
        ),
        Snippet(
            id = "gbv_report",
            title = "Reporting GBV safely",
            body = "You can document an incident in Linda Binti Reporting (device-first). " +
                "Cloud upload needs your consent and a USER sign-in. For immediate danger, prioritize safety and hotlines first.",
            keywords = listOf(
                "report", "reporting", "gbv", "violence", "abuse", "evidence", "ripoti", "police"
            ),
            relatedAction = "reporting"
        ),
        Snippet(
            id = "safety_home",
            title = "Afraid to go home",
            body = "If returning home feels unsafe, go to a trusted friend/relative or a safe public place if possible. " +
                "Call 1195 or 999. Use Emergency Assistance and Trusted Contacts in the app. Do not stay alone if you can avoid it.",
            keywords = listOf(
                "home", "scared", "afraid", "return", "husband", "partner", "threat",
                "nyumbani", "ogopa", "mume", "tisho"
            ),
            relatedAction = "emergency"
        ),
        Snippet(
            id = "rights",
            title = "Rights and dignity",
            body = "Everyone has the right to safety, dignity, and protection from GBV and FGM. " +
                "Kenyan law protects survivors. Seek support through verified helplines and trusted community services.",
            keywords = listOf("right", "rights", "haki", "dignity", "law", "legal", "protection"),
            relatedAction = "education"
        ),
        Snippet(
            id = "stigma",
            title = "Stigma and shame",
            body = "Stigma after GBV or FGM is common and is not the survivor's fault. " +
                "You have a right to confidential support. Use Survivor Support in Linda Binti, talk to a trusted person when safe, " +
                "and call GBV helpline 1195. If you are in danger, call 999/112 or open Emergency Assistance.",
            keywords = listOf(
                "stigma", "stigmatized", "shame", "ashamed", "blame", "judged",
                "gossip", "aibu", "unyanyapaa"
            ),
            relatedAction = "support"
        ),
        Snippet(
            id = "sos_app",
            title = "How Linda Binti SOS works",
            body = "SOS is sent from the calculator disguise using your own secret code then Equals. " +
                "Trusted contacts get an alert and location when available. Never share your Access PIN or SOS code. " +
                "You set these during setup — the assistant cannot reveal them.",
            keywords = listOf("sos", "calculator", "code", "pin", "secret", "trusted", "contact"),
            relatedAction = "settings"
        ),
        Snippet(
            id = "kajiado_help",
            title = "Getting help in Kajiado",
            body = "In Kajiado County, start with Linda Binti Emergency Assistance, Survivor Support, Education, and Reporting. " +
                "Verified national numbers: 999 or 112 (police emergency), 1195 (GBV helpline), 116 (Childline Kenya). " +
                "You can also go to a police gender desk, a county hospital, or the County Department of Gender when it is safe. " +
                "Services differ by sub-county (Central, North, South, East, West) — ask locally; this app does not invent clinic names.",
            keywords = listOf(
                "kajiado", "help", "support", "where", "msaada", "service", "clinic",
                "hospital", "gender desk", "sub-county"
            ),
            relatedAction = "support"
        ),
        Snippet(
            id = "what_is_fgm",
            title = "What FGM is",
            body = "Female Genital Mutilation (FGM) is the partial or total removal of external female genitalia, or other injury to female genital organs, for non-medical reasons. " +
                "It is a harmful practice and a form of gender-based violence. Kenya law forbids performing FGM, arranging it, or taking a girl to be cut. " +
                "This assistant does not describe procedures. If a girl is at risk, call Childline 116 or GBV 1195.",
            keywords = listOf(
                "what is fgm", "define fgm", "meaning of fgm", "ukeketaji", "fgm ni nini", "mutilation"
            ),
            relatedAction = "education"
        ),
        Snippet(
            id = "kajiado_fgm",
            title = "FGM in Kajiado County",
            body = "Kajiado has historically had among the highest FGM rates in Kenya. KDHS 2014 put county prevalence around 78%. " +
                "Among Maasai women nationally, KDHS 2022 estimated about 53%, down from about 78% in 2014 — progress, but the practice has not ended. " +
                "Kajiado is largely Maasai and pastoralist; FGM has been treated as a rite of passage, often around ages 12–14 in school holidays, and is linked to early/forced marriage and girls leaving school. " +
                "The Tanzania border raises cross-border cutting risk. In 2019 Kajiado was the first Kenyan county to launch a county Anti-FGM Policy, with county, sub-county and ward committees. " +
                "If a girl in Kajiado is about to be cut, treat it as an emergency: 116, 1195, 999/112, trusted adults, and Linda Binti Emergency/Reporting.",
            keywords = listOf(
                "kajiado", "maasai", "pastoralist", "prevalence", "rite of passage",
                "holiday", "cross-border", "tanzania", "anti-fgm policy", "narok",
                "fgm in kajiado", "cutting in kajiado"
            ),
            relatedAction = "education"
        ),
        Snippet(
            id = "kajiado_gbv",
            title = "GBV in Kajiado County",
            body = "GBV in Kajiado includes intimate-partner violence, sexual violence, emotional and economic abuse, FGM, child/early/forced marriage, and stigma that blocks reporting. " +
                "Nationally KDHS 2022 found 43% of women 15–49 had experienced intimate-partner violence, and 13% sexual violence — Kajiado faces these harms plus distance, pastoral mobility, and fear of community judgement. " +
                "The County Department of Gender runs GBV campaigns and anti-FGM coordination. Police gender desks and hospital care exist but coverage varies. " +
                "Linda Binti is for Kajiado GBV/FGM only: discreet SOS, on-phone reports, education, and NGO flags. It complements — it does not replace — police, hospital, courts, or counselling. Call 1195 or 999/112.",
            keywords = listOf(
                "gbv in kajiado", "kajiado gbv", "gbv", "intimate partner", "ipv", "pastoral",
                "underreporting", "gender department", "campaign", "violence in kajiado"
            ),
            relatedAction = "support"
        ),
        Snippet(
            id = "kenya_gbv_laws",
            title = "Kenyan laws that protect against GBV and FGM",
            body = "Useful laws: Constitution of Kenya 2010 (dignity, equality, freedom from violence; extra protection for children). " +
                "Prohibition of Female Genital Mutilation Act, 2011. Protection Against Domestic Violence Act, 2015. Sexual Offences Act, 2006. Children Act (child protection, including from FGM and forced marriage). " +
                "These make FGM, assault, sexual violence, and many forms of domestic abuse crimes. This app is not a lawyer. For a child at risk call 116; for GBV advice 1195; in danger 999/112.",
            keywords = listOf(
                "constitution", "2010", "2011", "2015", "2006", "domestic violence",
                "sexual offences", "children act", "padv", "anti-fgm act", "court"
            ),
            relatedAction = "education"
        ),
        Snippet(
            id = "why_fgm",
            title = "Why FGM has continued in some Kajiado communities",
            body = "FGM has been defended as culture, rite of passage, preparation for marriage, or social respect. Those reasons do not make it legal or safe. " +
                "Girls who refuse can face stigma, loss of marriage prospects, or family pressure. Alternative rites of passage and keeping girls in school are used in Kajiado to mark adulthood without cutting. " +
                "Culture can change; Kenyan law already forbids FGM. If someone is pressuring a girl, call 116 or 1195.",
            keywords = listOf(
                "why fgm", "why is fgm", "why is fgm done", "culture", "tradition", "rite",
                "respect", "marriage prospect", "alternative rite", "arp", "utamaduni"
            ),
            relatedAction = "education"
        ),
        Snippet(
            id = "child_marriage_kajiado",
            title = "Child, early and forced marriage in Kajiado",
            body = "In some Kajiado settings FGM has been tied to readiness for marriage. Child, early, or forced marriage is a protection emergency and a form of GBV. " +
                "A child forced to marry should be reported to Childline 116, a trusted adult, police, or the gender desk. Keep her in school if it is safe. Use Linda Binti Emergency and Reporting. You do not have to confront the family alone.",
            keywords = listOf(
                "child marriage", "early marriage", "forced marriage", "school dropout",
                "ndoa ya utotoni", "bride"
            ),
            relatedAction = "emergency"
        ),
        Snippet(
            id = "kajiado_policy",
            title = "Kajiado Anti-FGM Policy",
            body = "In December 2019 Kajiado became the first Kenyan county to launch a county Anti-FGM Policy. " +
                "It set up County, Sub-County, and Ward Anti-FGM committees to plan and coordinate ending FGM locally, with the national Anti-FGM Board and partners. " +
                "The policy supports prevention and response; it does not replace calling 116, 1195, or 999 when a girl is in danger now.",
            keywords = listOf(
                "policy", "anti-fgm policy", "committee", "ward", "board", "2019",
                "county government"
            ),
            relatedAction = "education"
        ),
        Snippet(
            id = "confidential_care",
            title = "Confidential medical and psychosocial care",
            body = "After GBV or FGM, survivors can seek medical care at a hospital or clinic as soon as it is safe. Ask for confidential treatment. " +
                "Some Kajiado facilities have been strengthening GBV response (gender desks, recovery support) — ask the nurse or gender officer what is available today; do not assume a named centre is always open. " +
                "Counselling is professional care; Linda Binti only guides. Helplines: 1195, 116, 999/112.",
            keywords = listOf(
                "hospital", "clinic", "medical", "nurse", "doctor", "counselling",
                "counseling", "recovery", "gbvrc", "psychosocial"
            ),
            relatedAction = "support"
        ),
        Snippet(
            id = "friend_help",
            title = "Helping a friend",
            body = "Listen without judgment, believe them, and help them reach 1195/999 or a trusted adult if a child is at risk. " +
                "Do not confront an abuser if it increases danger. Encourage using Linda Binti Emergency or Reporting when ready.",
            keywords = listOf("friend", "help a friend", "rafiki", "someone else", "sister", "neighbour"),
            relatedAction = "support"
        ),
        Snippet(
            id = "what_is_gbv",
            title = "What is Gender-Based Violence",
            body = "GBV is harm directed at a person because of their gender. It includes physical, sexual, emotional, and economic abuse. " +
                "It is illegal in Kenya. In danger call 999/112 or GBV helpline 1195. Use Linda Binti Emergency Assistance or Reporting when ready.",
            keywords = listOf(
                "what is gbv", "gbv", "gender based", "violence", "abuse", "unyanyasaji", "kijinsia"
            ),
            relatedAction = "education"
        ),
        Snippet(
            id = "gbv_types",
            title = "Forms of GBV",
            body = "GBV may be physical (hitting), sexual (forced sex/rape), emotional (threats, insults, control), or economic (blocking money, work, or school). " +
                "All can be documented privately in Linda Binti Reporting. Seek 1195/999 if unsafe now.",
            keywords = listOf(
                "types", "forms", "physical", "sexual", "emotional", "economic", "aina", "kimwili"
            ),
            relatedAction = "reporting"
        ),
        Snippet(
            id = "sexual_violence",
            title = "Sexual violence — immediate steps",
            body = "If sexually assaulted: move to safety, call 999/112 or 1195, and seek medical care as soon as possible. " +
                "Document privately in Reporting only when safe. Open Emergency Assistance if danger continues.",
            keywords = listOf(
                "rape", "sexual assault", "forced sex", "ubakaji", "kubakwa", "defilement"
            ),
            relatedAction = "emergency"
        ),
        Snippet(
            id = "forced_marriage",
            title = "Forced marriage risk",
            body = "Forced marriage linked to GBV or FGM pressure is a protection emergency. Call Childline 116 for a child at risk, or 1195. " +
                "Tell a trusted adult if safe and use Linda Binti Emergency or Reporting.",
            keywords = listOf(
                "forced marriage", "marry", "ndoa", "kulazimisha", "bride"
            ),
            relatedAction = "emergency"
        )
    )

    fun retrieve(message: String, limit: Int = 3): List<Snippet> {
        val normalized = normalize(message)
        if (normalized.isBlank()) return emptyList()
        return snippets
            .map { snip -> snip to score(normalized, snip) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    fun formatForPrompt(snippets: List<Snippet>): String {
        if (snippets.isEmpty()) return ""
        return buildString {
            appendLine("CURATED_KNOWLEDGE (use these facts; do not invent phone numbers or services):")
            snippets.forEachIndexed { i, s ->
                append(i + 1).append(") ").append(s.title).append(": ").append(s.body).append('\n')
            }
        }.trim()
    }

    private fun score(normalized: String, snip: Snippet): Int {
        var score = 0
        for (kw in snip.keywords) {
            if (normalized.contains(kw)) score += if (kw.length <= 3) 1 else 2
        }
        return score
    }

    private fun normalize(text: String): String =
        text.trim().lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
