package com.example.lindabinti.data.content

data class FaqEntry(
    val id: String,
    val questions: List<String>,
    val keywords: List<String>,
    val answerEn: String,
    val answerSw: String,
    val answerMaa: String? = null,
    val relatedAction: String? = null // emergency | education | support | reporting
) {
    fun answerFor(language: String): String =
        when (SupportedLanguages.catalogCode(language)) {
            SupportedLanguages.SW -> answerSw
            SupportedLanguages.MAA -> answerMaa ?: answerEn
            else -> answerEn
        }
}

object AssistantKnowledge {

    fun quickPrompts(language: String): List<String> =
        when (SupportedLanguages.catalogCode(language)) {
            SupportedLanguages.SW -> quickPromptsSw
            SupportedLanguages.MAA -> quickPromptsMaa
            else -> quickPromptsEn
        }

    private val quickPromptsEn = listOf(
        "What is GBV?",
        "What is FGM?",
        "FGM in Kajiado",
        "GBV in Kajiado",
        "I think my sister is about to undergo FGM",
        "My husband keeps threatening me",
        "Where can I get help in Kajiado?"
    )

    private val quickPromptsSw = listOf(
        "Nadhani dada yangu anakaribia kufanyiwa FGM",
        "Mume wangu anaendelea kunitishia",
        "Ninaripoti GBV wapi?",
        "Ninaogopa kurudi nyumbani",
        "Haki zangu ni zipi?",
        "Nawezaje kumsaidia rafiki?",
        "Mfumo wa dharura unafanya kazi vipi?"
    )

    private val quickPromptsMaa = listOf(
        "Naa enkitok enye ne karibia aitum FGM",
        "Olmurata enye ne aitisho oleng'ata",
        "Ne aripoti GBV enkop naa?",
        "Ne aing'oru olchani aitubul enkaji",
        "Ng'otol enye kejap naa?",
        "Ne aji aitum olaiteng'en?",
        "Enkop oldeke ne aji aji?"
    )

    val faqs = listOf(
        FaqEntry(
            id = "danger",
            questions = listOf(
                "i am in danger", "help me now", "unsafe", "someone is attacking",
                "niko hatarini", "nisaidie sasa", "sina usalama", "ninaoshwa"
            ),
            keywords = listOf(
                "danger", "attack", "hurt", "unsafe", "threat", "violent", "beating",
                "hatari", "shambulio", "umivu", "tisho", "vurugu", "kupigwa", "nisaidie"
            ),
            answerEn = "If you are in immediate danger, open Emergency Assistance, call 999 or 112, or send SOS from the calculator with your secret code. Use Hide if someone is near the phone. Your safety comes first.",
            answerSw = "Ukiwa hatarini mara moja, fungua Msaada wa Dharura, piga 999 au 112, au tuma SOS kutoka calculator kwa msimbo wako wa siri. Tumia Ficha mtu akiwa karibu na simu. Usalama wako ni wa kwanza.",
            answerMaa = "Ne olchani sidai, aing'oru Minto oldeke, aisho 999 kata 112, kata aitum SOS enkop calculator ne kood enye eng'eni. Aitum Olperesh oltungani ne aji enkop esimu. Enkigwanak enye naa oleng'ata.",
            relatedAction = "emergency"
        ),
        FaqEntry(
            id = "fgm_law",
            questions = listOf(
                "what is fgm law", "is fgm illegal", "fgm kenya",
                "sheria ya fgm", "fgm ni kinyume cha sheria"
            ),
            keywords = listOf(
                "fgm", "cut", "circumcision", "mutilation", "illegal", "law",
                "ukeketaji", "sheria", "marufuku", "keketa"
            ),
            answerEn = "FGM is illegal in Kenya under the Prohibition of Female Genital Mutilation Act, 2011. Nobody may perform FGM or take a girl for FGM. If a girl is at risk, call Childline 116 or GBV helpline 1195, and alert a trusted contact.",
            answerSw = "FGM ni kinyume cha sheria nchini Kenya chini ya Sheria ya Kupiga Marufuku Ukeketaji wa Wanawake, 2011. Hakuna anayeruhusiwa kufanya FGM au kumpeleka msichana kwa FGM. Msichana akiwa hatarini, piga Childline 116 au simu ya GBV 1195, na arifu mtu wa kuaminika.",
            answerMaa = "FGM meji sidai Kenya enkop Prohibition of Female Genital Mutilation Act, 2011. Melo oltungani ne aji aitum FGM kata aitum enkitok FGM. Enkitok olchani, aisho Childline 116 kata Esimu ya GBV 1195, kata aitum olaiteng'en.",
            relatedAction = "education"
        ),
        FaqEntry(
            id = "fgm_sister",
            questions = listOf(
                "someone wants to cut my sister", "fgm threat", "planned cutting",
                "about to undergo fgm", "forcing my daughter", "daughter undergo fgm",
                "anataka kumkeketa dadangu", "hatari ya fgm", "binti yangu atafanyiwa fgm"
            ),
            keywords = listOf(
                "sister", "daughter", "girl", "cut", "ceremony", "planned", "undergo", "about to",
                "dada", "binti", "msichana", "keketa", "sherehe", "imepangwa", "karibia"
            ),
            answerEn = "Treat a planned FGM as an emergency for that girl. Call Childline 116, tell a trusted adult, and use Emergency Assistance. You can also save a private report in Reporting if it is safe. Do not confront abusers alone if that puts you at risk.",
            answerSw = "Chukulia FGM iliyopangwa kama dharura kwa huyo msichana. Piga Childline 116, mwambie mtu mzima wa kuaminika, na tumia Msaada wa Dharura. Unaweza pia kuhifadhi ripoti ya faragha katika Kuripoti ikiwa ni salama. Usikabiliane na wanyanyasaji peke yako ikiwa inakuweka hatarini.",
            answerMaa = "Ilo FGM ne aji enkop oleng'ata naa oldeke enkitok eno. Aisho Childline 116, aitum oltungani oleng'ata olaiteng'en, kata aitum Minto oldeke. Ne aji pia aibuk ripoti eng'eni enkop Ripoti ne enkigwanak. Melo aikabiliane olaita olchani eng'eni ne aikuweke olchani.",
            relatedAction = "emergency"
        ),
        FaqEntry(
            id = "husband_threats",
            questions = listOf(
                "husband keeps threatening", "partner threatens me", "domestic threats",
                "mume wangu ananitishia", "mwenzi ananitishia"
            ),
            keywords = listOf(
                "husband", "partner", "boyfriend", "threaten", "threatening", "beat", "hit",
                "mume", "mwenzi", "tisho", "anitishia", "anapiga"
            ),
            answerEn = "Repeated threats from a partner are a serious GBV warning sign. If you feel unsafe now, open Emergency Assistance or call 999/112. GBV helpline 1195 can advise you. Consider updating trusted contacts and reading Safety & Prevention in Education when it is safe.",
            answerSw = "Vitisho vya mara kwa mara kutoka kwa mwenzi ni dalili kubwa ya GBV. Ukihisi hatarini sasa, fungua Msaada wa Dharura au piga 999/112. Simu ya GBV 1195 inaweza kukushauri. Fikiria kusasisha mawasiliano ya kuaminika na somo la Usalama katika Elimu ukiwa salama.",
            answerMaa = "Aitisho oleng'ata enkop olmurata naa olal eng'eni olchani GBV. Ne aing'oru olchani sidai, aing'oru Minto oldeke kata aisho 999/112. Esimu ya GBV 1195 ne aji aing'oru. Nchoo olaiteng'en na eisom Enkigwanak ne enkigwanak.",
            relatedAction = "emergency"
        ),
        FaqEntry(
            id = "scared_home",
            questions = listOf(
                "scared to go home", "afraid to return home", "unsafe at home",
                "ninaogopa kurudi nyumbani", "sina usalama nyumbani"
            ),
            keywords = listOf(
                "scared", "afraid", "go home", "return home", "unsafe at home", "cannot go back",
                "ogopa", "kurudi", "nyumbani", "sina usalama"
            ),
            answerEn = "If home does not feel safe, do not return alone if you can avoid it. Reach a safe place, call 1195 or 999 if in immediate danger, and alert a trusted contact. Emergency Assistance can guide next steps. This is safety guidance, not a guarantee.",
            answerSw = "Nyumbani isipohisi salama, usirudi peke yako ukiweza kuepuka. Fika mahali salama, piga 1195 au 999 ukiwa hatarini mara moja, na arifu mtu wa kuaminika. Msaada wa Dharura unaweza kuongoza hatua zinazofuata. Hii ni mwongozo wa usalama, si dhamana.",
            answerMaa = "Enkaji meji enkigwanak, melo aitubul eng'eni eng'eni ne aji. Aing'oru enkop sidai, aisho 1195 kata 999 ne olchani sidai, kata aitum olaiteng'en. Minto oldeke ne aji aing'oru enkop oleng'ata. Ena naa olaiteng'en enkigwanak, meji ng'otol e ncho.",
            relatedAction = "emergency"
        ),
        FaqEntry(
            id = "emergency_system",
            questions = listOf(
                "how does emergency system work", "how does emergency assistance work",
                "emergency system in the app", "mfumo wa dharura unafanya kazi vipi"
            ),
            keywords = listOf(
                "emergency system", "emergency assistance", "emergency help", "how does emergency",
                "mfumo wa dharura", "msaada wa dharura"
            ),
            answerEn = "Emergency Assistance in Linda Binti shows verified hotlines (999, 112, 1195, 116) and trusted-contact options. The calculator SOS sends SMS to contacts you saved — it is not automatic police dispatch. Your SOS code is private and never shown here.",
            answerSw = "Msaada wa Dharura katika Linda Binti unaonyesha simu zilizothibitishwa (999, 112, 1195, 116) na chaguo za mawasiliano ya kuaminika. SOS ya calculator hutuma SMS kwa mawasiliano uliyohifadhi — si polisi wa moja kwa moja. Msimbo wako wa SOS ni wa faragha na hauonyeshwi hapa.",
            answerMaa = "Minto oldeke Linda Binti ne aonyish Namba oldeke (999, 112, 1195, 116) na olaiteng'en. SOS enkop calculator ne aitum SMS olaiteng'en ne aibuk — meji polisi eng'eni. Kood enye wa SOS naa eng'eni na meji aonyish ene.",
            relatedAction = "emergency"
        ),
        FaqEntry(
            id = "gbv_report",
            questions = listOf(
                "how do i report gbv", "report abuse", "police gender desk",
                "ninaripoti gbv vipi", "ripoti unyanyasaji"
            ),
            keywords = listOf(
                "report", "gbv", "abuse", "violence", "police", "gender",
                "ripoti", "unyanyasaji", "polisi", "jinsia"
            ),
            answerEn = "You can call 1195 (GBV helpline), go to a county gender desk or hospital, or dial 999 if in danger. In this app, use Reporting for a private note on the phone only, and Survivor Support for encouragement and local contacts. Reporting should never put you in more danger — wait until you are safer if needed.",
            answerSw = "Unaweza kupiga 1195 (simu ya GBV), kwenda dawati la jinsia la kaunti au hospitali, au kupiga 999 ukiwa hatarini. Katika programu hii, tumia Kuripoti kwa maelezo ya faragha kwenye simu tu, na Msaada kwa Waathiriwa kwa faraja na mawasiliano ya eneo. Kuripoti hakipaswi kukuweka hatari zaidi — subiri uwe salama zaidi ikiwa inahitajika.",
            answerMaa = "Ne aji aisho 1195 (Esimu ya GBV), aing'oru gender desk enkop Kajiado kata hospital, kata aisho 999 ne olchani. App eno, aitum Ripoti ne olng'obani eng'eni enkop esimu tu, na Minto Olaiteng'en ne enkop eng'eni na olaiteng'en. Ripoti meji aikuweke olchani oleng'ata — eng'eti oleng'ata sidai ne nchoo.",
            relatedAction = "support"
        ),
        FaqEntry(
            id = "kajiado_help",
            questions = listOf(
                "where can i get help in kajiado", "kajiado hospital", "local help",
                "msaada wapi kajiado", "hospitali ya kajiado"
            ),
            keywords = listOf(
                "kajiado", "hospital", "clinic", "local", "county", "help near",
                "hospitali", "kliniki", "kaunti", "msaada"
            ),
            answerEn = "Open Survivor Support for encouragement, next steps, and the bundled Kajiado directory (hospital, gender desk, national hotlines). For urgent help use 999, 112, 1195, or 116. You can also SOS your trusted contacts from the calculator.",
            answerSw = "Fungua Msaada kwa Waathiriwa kwa faraja, hatua zinazofuata, na orodha ya Kajiado (hospitali, dawati la jinsia, simu za kitaifa). Kwa msaada wa haraka tumia 999, 112, 1195, au 116. Unaweza pia kutuma SOS kwa mawasiliano yako kutoka calculator.",
            answerMaa = "Aing'oru Minto Olaiteng'en ne enkop eng'eni, enkop oleng'ata, na olorien Kajiado (hospital, gender desk, Namba oldeke). Minto sidai aitum 999, 112, 1195, kata 116. Ne aji pia aitum SOS olaiteng'en enye enkop calculator.",
            relatedAction = "support"
        ),
        FaqEntry(
            id = "sos_how",
            questions = listOf(
                "how does sos work", "secret code", "calculator alert",
                "sos inafanya kazi vipi", "msimbo wa siri"
            ),
            keywords = listOf(
                "sos", "secret", "code", "calculator", "sms", "alert",
                "msimbo", "siri", "tahadhari", "ujumbe"
            ),
            answerEn = "The app looks like a calculator. Type the SOS code you chose during setup, then press Equals. The phone sends an SMS alert to your trusted contacts, then opens Linda Binti. Your Access Code opens the app without sending an alert. SMS needs a working SIM and credit; this is not a cloud emergency dispatch. The SOS code is never shown in this Q&A.",
            answerSw = "Programu inaonekana kama calculator. Andika msimbo wa SOS uliochagua wakati wa usanidi, kisha bonyeza Equals. Simu hutuma SMS kwa mawasiliano yako, kisha hufungua Linda Binti. Access Code yako inafungua programu bila kutuma tahadhari. SMS inahitaji SIM na salio; hii si mfumo wa wingu wa dharura. Msimbo wa SOS hauonyeshwi katika maswali haya.",
            answerMaa = "App naa kejap sidai calculator. Aitito kood wa SOS ne ichoo enkigwanak e nchoe, oleng'ata aitum Equals. Esimu ne aitum SMS olaiteng'en enye, oleng'ata aing'oru Linda Binti. Access Code enye aing'oru app meji aitum imorijo. SMS ne aji SIM na airitisho; meji backend oldeke. Kood wa SOS meji aonyish enkop ol eno.",
            relatedAction = null
        ),
        FaqEntry(
            id = "rights",
            questions = listOf(
                "what are my rights", "do i have rights",
                "haki zangu ni zipi", "nina haki"
            ),
            keywords = listOf(
                "rights", "law", "legal", "allowed",
                "haki", "sheria", "ruhusa"
            ),
            answerEn = "You have the right to safety, medical care, and to refuse FGM or forced marriage. Violence is never your fault. See Education for short offline lessons on FGM law and GBV rights.",
            answerSw = "Una haki ya usalama, matibabu, na kukataa FGM au ndoa ya kulazimishwa. Ukatili si kosa lako kamwe. Angalia Elimu kwa masomo mafupi nje ya mtandao kuhusu sheria ya FGM na haki za GBV.",
            answerMaa = "Kejap sidai enkigwanak, olchani, na ukata FGM kata enkaiya enkaina. Olchani meji olchani enye. Nchoo Eisom ne eisom enkop enkop meji internet eng'eni FGM na ng'otol GBV.",
            relatedAction = "education"
        ),
        FaqEntry(
            id = "stigma",
            questions = listOf(
                "i am stigmatized", "i feel stigmatized", "people judge me",
                "nina aibu", "wananidharau"
            ),
            keywords = listOf(
                "stigma", "stigmatized", "shame", "ashamed", "blame", "blamed",
                "judged", "gossip", "aibu", "unyanyapaa", "dharau"
            ),
            answerEn = "Stigma and shame after GBV or FGM are common, and they do not mean you did anything wrong. You still have a right to safety, care, and confidentiality. Talk to someone you trust when it is safe, use Survivor Support in this app, and call GBV helpline 1195. Education explains your rights. If you are unsafe now, open Emergency Assistance or call 999/112.",
            answerSw = "Aibu na unyanyapaa baada ya GBV au FGM ni kawaida, na haimaanishi umefanya kosa. Bado una haki ya usalama, huduma, na faragha. Ongea na mtu unayemwamini ikiwa ni salama, tumia Msaada katika programu, na piga 1195. Elimu inaeleza haki zako. Ukiwa hatarini sasa, fungua Msaada wa Dharura au piga 999/112.",
            answerMaa = "Aibu ne GBV kata FGM meji olchani enye. Kejap enkigwanak, olchani, na eng'eni. Aitum olaiteng'en ne sidai, aitum Minto, aisho 1195. Eisom ne aji ng'otol. Ne olchani sidai, aitum Minto oldeke kata aisho 999/112.",
            relatedAction = "support"
        ),
        FaqEntry(
            id = "friend",
            questions = listOf(
                "how to help a friend", "my friend is abused",
                "kumsaidia rafiki", "rafiki yangu ananyanyaswa"
            ),
            keywords = listOf(
                "friend", "help her", "believe", "support her",
                "rafiki", "amini", "msaada"
            ),
            answerEn = "Believe them, listen, and ask what they need. Help them reach 1195, 116, or a trusted adult. Do not share their story without permission unless a child is in immediate danger. See the Education lesson “How to help a friend”.",
            answerSw = "Waamini, wasikilize, na waulize wanachohitaji. Wasaidie kufikia 1195, 116, au mtu mzima wa kuaminika. Usishiriki hadithi yao bila ruhusa isipokuwa mtoto yuko hatarini mara moja. Angalia somo la Elimu “Jinsi ya kumsaidia rafiki”.",
            answerMaa = "Aamini, ting'o, na aom naa ne nchoo. Aitum olaiteng'en aing'oru 1195, 116, kata oltungani oleng'ata olaiteng'en. Melo aisho olng'obani olaita meji enchoo ne nchorro olchani sidai. Nchoo eisom Ne aji aitum olaiteng'en.",
            relatedAction = "education"
        ),
        FaqEntry(
            id = "hide",
            questions = listOf(
                "how do i hide the app", "quick hide",
                "ninaficha programu vipi", "ficha haraka"
            ),
            keywords = listOf(
                "hide", "discreet", "disguise", "catch",
                "ficha", "siri", "kujificha"
            ),
            answerEn = "Tap Hide on any protected screen to return to the calculator instantly. The launcher name is Calculator. Do not leave Linda Binti open if someone may take the phone.",
            answerSw = "Gusa Ficha kwenye skrini yoyote iliyolindwa ili urudi kwa calculator mara moja. Jina la kizinduzi ni Calculator. Usiachie Linda Binti wazi mtu anaweza kuchukua simu.",
            answerMaa = "Aitum Olperesh enkop eng'eni eng'eni ne aitubul calculator sidai. Enkomom enkop launcher naa Calculator. Melo aachie Linda Binti aing'oru oltungani ne aji aitum esimu."
        ),
        FaqEntry(
            id = "privacy",
            questions = listOf(
                "is my data private", "who can see my reports",
                "data yangu ni faragha", "nani anaona ripoti zangu"
            ),
            keywords = listOf(
                "private", "privacy", "data", "cloud", "firebase", "see my",
                "faragha", "data", "ripoti", "wingu"
            ),
            answerEn = "Contacts, PIN, and SOS codes stay on this phone. Reports save locally first. Optional cloud upload needs your consent, a USER account, and a configured backend. NGO staff see aggregates and urgency flags — never another user's private report text. SOS SMS goes only to trusted numbers you saved. Turn sharing off in Settings if you do not want anonymous counts prepared.",
            answerSw = "Mawasiliano, PIN, na misimbo ya SOS yanabaki kwenye simu. Ripoti huhifadhiwa kwanza. Upakiaji wa wingu unahitaji idhini, akaunti ya USER, na backend. Wafanyakazi wa NGO wanaona majumla na bendera za dharura — si maandishi ya faragha ya mtu mwingine. SMS ya SOS huenda tu kwa nambari ulizohifadhi. Zima kushiriki katika Mipangilio usipotaka hesabu zisizojulikana.",
            answerMaa = "Olaiteng'en, PIN, na kood za SOS nejap enkop esimu. Ripoti ne aibuk e nchoe. Aitum backend ne aji enchoo enye, akaunti USER, na backend. Olaiteng'en NGO ne aonyish olorien na alama oldeke — meji olng'obani eng'eni oltungani ang'eni. SMS SOS ne aitum Namba ne aibuk tu. Aar Enkigwan ne meji aji aibuk olorien eng'eni.",
            relatedAction = "settings"
        ),
        FaqEntry(
            id = "safety_plan",
            questions = listOf(
                "how do i make a safety plan", "safety plan",
                "mpango wa usalama", "natengeneza mpango wa usalama"
            ),
            keywords = listOf(
                "safety plan", "plan", "pack", "code word",
                "mpango", "usalama", "neno la siri"
            ),
            answerEn = "A safety plan can include who to call, where to go, what to pack, and a code word. Open Education → Safety & Prevention, and keep 1–5 trusted contacts updated. This is educational guidance, not a guarantee of safety.",
            answerSw = "Mpango wa usalama unaweza kujumuisha nani wa kupigia, wapi kwenda, cha kubeba, na neno la siri. Fungua Elimu → Usalama na Kinga, na weka mawasiliano 1–5. Hii ni mwongozo wa kielimu, si dhamana ya usalama.",
            answerMaa = "Enkop ol enkigwanak ne aji naa aisho, enkop naa aitubul, naa aitum, na olng'obani eng'eni. Aing'oru Eisom → Enkigwanak, na aibuk olaiteng'en 1–5. Ena naa olaiteng'en eisom, meji ng'otol enkigwanak.",
            relatedAction = "education"
        ),
        FaqEntry(
            id = "evidence",
            questions = listOf(
                "how do i keep evidence", "preserve evidence",
                "nahifadhi ushahidi vipi", "ushahidi"
            ),
            keywords = listOf(
                "evidence", "photos", "messages", "clothes",
                "ushahidi", "picha", "ujumbe", "nguo"
            ),
            answerEn = "Only if it is safe: note dates, keep messages/photos, and seek medical documentation early. Do not risk further harm to collect evidence. Use Reporting for a private on-device note. Open Education → Reporting & Evidence for more guidance.",
            answerSw = "Tu ikiwa ni salama: andika tarehe, hifadhi ujumbe/picha, na tafuta nyaraka za matibabu mapema. Usijiweke hatarini kukusanya ushahidi. Tumia Kuripoti kwa maelezo kwenye simu. Fungua Elimu → Kuripoti na Ushahidi.",
            answerMaa = "Ne enkigwanak tu: aitito enkop oleng'ata, aibuk imorijo/picha, na aom olng'obani olchani oleng'ata. Melo aikuweke olchani ne aikusi enkop ol. Aitum Ripoti ne olng'obani enkop esimu. Aing'oru Eisom → Ripoti na Enkop ol ne olaiteng'en oleng'ata.",
            relatedAction = "reporting"
        ),
        FaqEntry(
            id = "what_is_gbv",
            questions = listOf(
                "what is gbv", "define gbv", "meaning of gbv",
                "gbv ni nini", "gender based violence"
            ),
            keywords = listOf(
                "what is gbv", "gbv", "gender based", "violence against women",
                "unyama wa kijinsia", "unyanyasaji"
            ),
            answerEn = "Gender-Based Violence is harm directed at someone because of their gender. It includes physical, sexual, emotional, and economic abuse. In Kenya it is against the law. If you are unsafe now, call 999/112 or 1195, or open Emergency Assistance.",
            answerSw = "Ukatili wa Kijinsia (GBV) ni madhara yanayoelekezwa kwa mtu kwa sababu ya jinsia yake. Inajumuisha unyanyasaji wa kimwili, kingono, kihisia, na kiuchumi. Nchini Kenya ni kinyume cha sheria. Ukiwa hatarini sasa, piga 999/112 au 1195, au fungua Msaada wa Dharura.",
            answerMaa = "GBV naa olchani ne aitum oltungani ne jinsia. Ne aji olchani oleng'ata, sexual, emotional, na economic. Kenya ne aar sheria. Ne olchani, aisho 999/112 kata 1195, kata aing'oru Minto oldeke.",
            relatedAction = "education"
        ),
        FaqEntry(
            id = "gbv_types",
            questions = listOf(
                "types of gbv", "forms of gbv", "kinds of abuse",
                "aina za gbv", "unyanyasaji wa aina gani"
            ),
            keywords = listOf(
                "types", "forms", "physical", "sexual", "emotional", "economic",
                "aina", "kimwili", "kihisia", "kiuchumi"
            ),
            answerEn = "Common forms of GBV include: physical (hitting), sexual (forced sex/rape), emotional (threats, insults, control), and economic (blocking money, work, or school). All can be reported privately in Linda Binti Reporting when you are ready.",
            answerSw = "Aina za GBV: kimwili (kupiga), kingono (kulazimisha ngono/ubakaji), kihisia (vitisho, matusi, kudhibiti), na kiuchumi (kuzuia fedha, kazi, au shule). Zote zinaweza kuripotiwa faraghani katika Kuripoti ya Linda Binti ukiwa tayari.",
            answerMaa = "Aina za GBV: physical, sexual, emotional, na economic. Ne aji aripoti eng'eni enkop Linda Binti Ripoti ne sidai.",
            relatedAction = "reporting"
        ),
        FaqEntry(
            id = "sexual_violence",
            questions = listOf(
                "i was raped", "sexual assault", "forced sex",
                "nilibakwa", "ubakaji", "ngono kwa nguvu"
            ),
            keywords = listOf(
                "rape", "sexual assault", "forced sex", "defilement",
                "ubakaji", "kubakwa", "ngono"
            ),
            answerEn = "If you were sexually assaulted, your safety comes first. Go to a safe place, call 999/112 or 1195, and seek medical care as soon as you can. You may document what happened privately in Reporting when ready. Open Emergency Assistance now if you are still in danger.",
            answerSw = "Ikiwa ulifanyiwa unyanyasaji wa kingono, usalama wako ni wa kwanza. Nenda mahali salama, piga 999/112 au 1195, na tafuta matibabu haraka iwezekanavyo. Unaweza kuandika faraghani katika Kuripoti ukiwa tayari. Fungua Msaada wa Dharura kama bado uko hatarini.",
            answerMaa = "Ne sexual assault, enkigwanak enye oleng'ata. Aitubul enkop sidai, aisho 999/112 kata 1195, na aom olchani. Ne aji aripoti eng'eni. Aing'oru Minto oldeke ne olchani.",
            relatedAction = "emergency"
        ),
        FaqEntry(
            id = "forced_marriage",
            questions = listOf(
                "forced marriage", "being forced to marry",
                "ndoa ya kulazimishwa", "wananilazimisha kuolewa"
            ),
            keywords = listOf(
                "forced marriage", "marry", "bride", "dowry pressure",
                "ndoa", "kulazimisha", "kuolewa"
            ),
            answerEn = "Forced marriage linked to GBV or FGM pressure is a serious protection risk. Tell a trusted adult if safe, call Childline 116 (for a child) or GBV helpline 1195, and use Reporting or Emergency Assistance. You do not have to face this alone.",
            answerSw = "Ndoa ya kulazimishwa inayohusiana na GBV au shinikizo la FGM ni hatari kubwa. Mwambie mtu unayemwamini ikiwa ni salama, piga Childline 116 (kwa mtoto) au 1195, na tumia Kuripoti au Msaada wa Dharura. Hulazimiki kukabiliana na hili peke yako.",
            answerMaa = "Forced marriage ne olchani. Ojok oltungani ne aam, aisho 116 kata 1195, aitum Ripoti kata Minto oldeke.",
            relatedAction = "emergency"
        ),
        FaqEntry(
            id = "economic_abuse",
            questions = listOf(
                "economic abuse", "controls my money", "stops me working",
                "anazuia pesa", "unyanyasaji wa kiuchumi"
            ),
            keywords = listOf(
                "money", "economic", "salary", "work", "school fees",
                "pesa", "kazi", "kiuchumi"
            ),
            answerEn = "Controlling money, blocking work or school, or stealing earnings can be economic GBV. Build a quiet safety plan, keep trusted contacts updated, and use Survivor Support / Reporting when safe. Call 1195 for guidance.",
            answerSw = "Kudhibiti fedha, kuzuia kazi au shule, au kuiba mapato inaweza kuwa GBV wa kiuchumi. Tengeneza mpango wa usalama kwa siri, sasisha mawasiliano, na tumia Msaada / Kuripoti ikiwa ni salama. Piga 1195 kwa mwongozo.",
            answerMaa = "Economic abuse ne GBV. Tito enkop ol enkigwanak, aibuk olaiteng'en, aitum Minto / Ripoti ne sidai. Aisho 1195.",
            relatedAction = "support"
        ),
        FaqEntry(
            id = "what_is_fgm",
            questions = listOf(
                "what is fgm", "define fgm", "meaning of fgm",
                "fgm ni nini", "ukeketaji ni nini"
            ),
            keywords = listOf(
                "what is fgm", "define fgm", "meaning of fgm", "fgm ni nini"
            ),
            answerEn = "FGM is cutting or injuring female genitals for non-medical reasons. It is gender-based violence and is illegal in Kenya (Prohibition of FGM Act, 2011). Nobody may perform it or take a girl to be cut. This app does not describe how it is done. If a girl is at risk, call Childline 116 or GBV 1195, and open Emergency Assistance.",
            answerSw = "FGM ni kukata au kuumiza sehemu za siri za msichana pasipo sababu ya tiba. Ni ukatili wa kijinsia na ni kinyume cha sheria Kenya (Sheria ya 2011). Hakuna anayeruhusiwa kumkeketa au kumpeleka. Programu hii haielezi utaratibu. Msichana akiwa hatarini, piga 116 au 1195, na fungua Msaada wa Dharura.",
            answerMaa = "FGM naa olchani enkitok meji olchani oleng'ata. Kenya ne aar sheria 2011. Melo aitum kata aitum enkitok. Enkitok olchani, aisho 116 kata 1195.",
            relatedAction = "education"
        ),
        FaqEntry(
            id = "fgm_kajiado",
            questions = listOf(
                "fgm in kajiado", "kajiado fgm", "maasai fgm",
                "ukeketaji kajiado", "fgm kaunti ya kajiado"
            ),
            keywords = listOf(
                "kajiado", "maasai", "pastoralist", "prevalence", "cross-border",
                "tanzania", "anti-fgm policy", "rite of passage"
            ),
            answerEn = "Kajiado has had among Kenya’s highest FGM levels (about 78% in KDHS 2014). Among Maasai women, KDHS 2022 was about 53%, down from about 78% in 2014 — still far too high. FGM has been treated as a rite of passage, often in school holidays around ages 12–14, and is linked to early marriage and girls leaving school. The Tanzania border adds cross-border cutting risk. In 2019 Kajiado launched Kenya’s first county Anti-FGM Policy. If a girl is in danger now: 116, 1195, 999/112, and Linda Binti Emergency.",
            answerSw = "Kajiado imekuwa na viwango vya juu vya FGM (karibu 78% KDHS 2014). Miongoni mwa Wamaasai, KDHS 2022 ilikadiria karibu 53%, chini kutoka 78% mwaka 2014. FGM imekuwa ikichukuliwa kama sherehe ya kuingia utu uzima, mara nyingi likizo za shule umri 12–14, na inahusiana na ndoa ya mapema. Mpaka wa Tanzania unaongeza hatari. 2019 Kajiado ilizindua Sera ya kwanza ya Kaunti dhidi ya FGM. Msichana akiwa hatarini: 116, 1195, 999/112.",
            answerMaa = "Kajiado FGM oleng'ata (KDHS 2014 ~78%; Maasai 2022 ~53%). Rite of passage, school holiday, ndoa. Mpaka Tanzania. Sera Anti-FGM 2019. Enkitok olchani: 116, 1195, 999/112.",
            relatedAction = "education"
        ),
        FaqEntry(
            id = "gbv_kajiado",
            questions = listOf(
                "gbv in kajiado", "kajiado gbv", "violence in kajiado",
                "unyanyasaji kajiado"
            ),
            keywords = listOf(
                "gbv in kajiado", "kajiado gbv", "intimate partner", "underreporting"
            ),
            answerEn = "In Kajiado, GBV includes partner violence, sexual violence, emotional and economic abuse, FGM, child/forced marriage, and stigma that stops people reporting. Nationally KDHS 2022 found 43% of women 15–49 had intimate-partner violence and 13% sexual violence. Distance, pastoral life, and fear of gossip make help harder. Use a police gender desk, county hospital, or the County Department of Gender when safe. Linda Binti SOS, Reporting, and Education complement — they do not replace — 1195, 999/112, and 116.",
            answerSw = "Kajiado: GBV inajumuisha ukatili wa mwenzi, kingono, kihisia na kiuchumi, FGM, ndoa ya kulazimishwa, na aibu inayozuia kuripoti. Kitaifa KDHS 2022: 43% ya wanawake 15–49 walipata ukatili wa mwenzi; 13% kingono. Umbali na hofu ya umma zinazuia msaada. Tumia dawati la jinsia, hospitali, au Idara ya Jinsia. Linda Binti inasaidia — haichukui nafasi ya 1195, 999/112, 116.",
            answerMaa = "Kajiado GBV: partner, sexual, FGM, ndoa, aibu. KDHS 2022 Kenya IPV 43%. Gender desk, hospital, 1195, 999/112, 116. Linda Binti ne aji, meji polisi.",
            relatedAction = "support"
        ),
        FaqEntry(
            id = "kenya_laws",
            questions = listOf(
                "kenyan law on gbv", "which laws protect me", "fgm act",
                "sheria za gbv", "sheria ya kenya"
            ),
            keywords = listOf(
                "constitution", "2010", "2011", "2015", "sexual offences",
                "domestic violence", "children act", "padv"
            ),
            answerEn = "Kenya protects you through the Constitution 2010 (dignity, equality, freedom from violence), the Prohibition of FGM Act 2011, the Protection Against Domestic Violence Act 2015, the Sexual Offences Act 2006, and the Children Act. FGM, assault, and sexual violence are crimes. This app is not a lawyer. Child at risk: 116. GBV advice: 1195. Danger: 999/112. Open Education for short lessons.",
            answerSw = "Kenya inakulinda: Katiba 2010, Sheria ya FGM 2011, Sheria ya Ukatili wa Nyumbani 2015, Sheria ya Makosa ya Ngono 2006, na Sheria ya Watoto. FGM na unyanyasaji ni kosa. Programu si wakili. Mtoto hatarini: 116. GBV: 1195. Dharura: 999/112.",
            answerMaa = "Katiba 2010, FGM Act 2011, PADV 2015, Sexual Offences 2006. 116, 1195, 999/112. App meji wakili.",
            relatedAction = "education"
        ),
        FaqEntry(
            id = "why_fgm",
            questions = listOf(
                "why is fgm done", "why fgm in kajiado", "fgm culture",
                "kwa nini fgm"
            ),
            keywords = listOf(
                "why fgm", "tradition", "culture", "rite", "alternative rite", "arp"
            ),
            answerEn = "Some families treat FGM as culture, a rite of passage, or preparation for marriage. That does not make it legal or safe. Girls who refuse can face stigma. Alternative rites of passage and staying in school are used in Kajiado to mark adulthood without cutting. Kenyan law already forbids FGM. If a girl is being pressured, call 116 or 1195.",
            answerSw = "Baadhi ya familia huona FGM kama utamaduni au maandalizi ya ndoa. Hiyo haifanyi iwe halali. Wasichana wanaokataa wanaweza kuaibishwa. Sherehe mbadala na kuendelea shule zinatumika Kajiado. Sheria inakataza FGM. Shinikizo: piga 116 au 1195.",
            answerMaa = "FGM ne utamaduni kata ndoa — meji sidai. Sheria aar. Alternative rite, shule. Shinikizo: 116, 1195.",
            relatedAction = "education"
        )
    )

    /**
     * Simple on-device matcher: exact-ish question match, then keyword score.
     * Supports English and Kiswahili keywords. No network. No PII leaves the phone.
     */
    fun answer(userText: String, language: String = "en"): AssistantReply {
        val lang = SupportedLanguages.catalogCode(language)
        val normalized = normalize(userText)

        if (normalized.isBlank()) {
            return AssistantReply(
                when (lang) {
                    SupportedLanguages.SW ->
                        "Uliza kuhusu FGM, GBV, SOS, haki, au chagua swali la haraka hapa chini. Kila kitu kinabaki kwenye simu hii."
                    SupportedLanguages.MAA ->
                        "Bor FGM, GBV, SOS, ng'otol, kata ichoo swali sidai ene ncho. Eng'eni nejap enkop esimu eno."
                    else ->
                        "Ask about FGM, GBV, SOS, rights, or type a quick prompt below. Everything stays on this phone."
                },
                null
            )
        }

        val dangerHits = listOf(
            "danger", "help me now", "attack", "killing", "rape", "bleeding",
            "scared to go home", "about to undergo", "forcing my daughter",
            "hatari", "nisaidie sasa", "shambulio", "kubakwa", "kutokwa damu",
            "ogopa kurudi", "karibia kufanyiwa"
        )
        if (dangerHits.any { normalized.contains(it) }) {
            val faq = when {
                normalized.contains("fgm") || normalized.contains("cut") || normalized.contains("keketa") ->
                    faqs.first { it.id == "fgm_sister" }
                normalized.contains("husband") || normalized.contains("partner") || normalized.contains("mume") ->
                    faqs.first { it.id == "husband_threats" }
                normalized.contains("home") || normalized.contains("nyumbani") ->
                    faqs.first { it.id == "scared_home" }
                else -> faqs.first { it.id == "danger" }
            }
            return AssistantReply(faq.answerFor(lang), faq.relatedAction, riskFor(faq.id))
        }

        var best: FaqEntry? = null
        var bestScore = 0
        for (faq in faqs) {
            var score = 0
            if (faq.questions.any { normalized.contains(it) || it.contains(normalized) }) {
                score += 10
            }
            for (kw in faq.keywords) {
                if (normalized.contains(kw)) score += 2
            }
            if (score > bestScore) {
                bestScore = score
                best = faq
            } else if (score == bestScore && best != null) {
                if (faq.id == "fgm_sister" && best.id == "fgm_law") {
                    best = faq
                }
                if (faq.id == "scared_home" && best.id == "rights") {
                    best = faq
                }
            }
        }

        if (best != null && bestScore >= 2) {
            return AssistantReply(best.answerFor(lang), best.relatedAction, riskFor(best.id))
        }

        if (best != null && bestScore >= 1) {
            return AssistantReply(best.answerFor(lang), best.relatedAction, riskFor(best.id))
        }

        return AssistantReply(
            when (lang) {
                SupportedLanguages.SW ->
                    "Niko nje ya mtandao kwa sasa, lakini bado naweza kusaidia kuhusu FGM, GBV, haki, na usalama Kajiado. Jaribu swali lingine, chagua ombi la haraka, au fungua Elimu. Kwa hatari mara moja tumia Msaada wa Dharura."
                SupportedLanguages.MAA ->
                    "Ne offline, kainyorr aji FGM, GBV, ng'otol, enkigwanak Kajiado. Bor enkop ol lingine, ichoo swali, kata aing'oru Eisom. Ne olchani, aitum Minto oldeke."
                else ->
                    "I hear you. Linda Binti can help with GBV, FGM, rights, and safety in Kajiado even without a cloud reply. Open Education, Survivor Support, or Reporting on this phone. If you feel judged, that is not your fault — call 1195 when you can, or 999/112 in danger."
            },
            null
        )
    }

    private fun riskFor(faqId: String): String? = when (faqId) {
        "danger", "fgm_sister", "scared_home", "sexual_violence", "forced_marriage" -> "CRITICAL"
        "husband_threats" -> "HIGH"
        "stigma" -> "LOW"
        else -> null
    }

    private fun normalize(userText: String): String =
        userText.trim().lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}

data class AssistantReply(
    val text: String,
    val relatedAction: String?,
    val riskLevel: String? = null
)
