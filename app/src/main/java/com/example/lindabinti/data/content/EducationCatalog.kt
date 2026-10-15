package com.example.lindabinti.data.content

/**
 * Offline Education & Safety � eight GBV/FGM topics for Kajiado.
 * UI receives already-translated [EducationTopicView] objects.
 * Adding Maa later: fill optional Copy.maa fields. Do not rewrite screens.
 */
data class EducationCategory(
    val id: String,
    val title: String,
    val summary: String,
    val iconResName: String,
    val lessons: List<EducationModule>
)

data class EducationQuizOption(
    val id: String,
    val text: String,
    val correct: Boolean
)

data class EducationQuizView(
    val question: String,
    val options: List<EducationQuizOption>,
    val explain: String
)

data class EducationSlideView(
    val title: String,
    val body: String
)

data class EducationTopicView(
    val id: String,
    val title: String,
    val summary: String,
    val iconResName: String,
    val illustrationResName: String,
    val means: List<String>,
    val warningSigns: List<String>,
    val whatToDo: List<String>,
    val whereHelp: List<String>,
    val learnMore: List<String>,
    val slides: List<EducationSlideView>,
    val videoRawName: String?,
    val audioKey: String,
    val quiz: EducationQuizView
)

private data class QuizSrc(
    val question: Copy,
    val options: List<Triple<String, Copy, Boolean>>,
    val explain: Copy
)

private data class SlideSrc(
    val title: Copy,
    val body: Copy
)

private data class TopicSrc(
    val id: String,
    val iconResName: String,
    val illustrationResName: String,
    val title: Copy,
    val summary: Copy,
    val means: List<Copy>,
    val warningSigns: List<Copy>,
    val whatToDo: List<Copy>,
    val whereHelp: List<Copy>,
    val learnMore: List<Copy>,
    val slides: List<SlideSrc> = emptyList(),
    val videoRawName: String? = null,
    val quiz: QuizSrc
) {
    fun resolve(language: String): EducationTopicView = EducationTopicView(
        id = id,
        title = title.t(language),
        summary = summary.t(language),
        iconResName = iconResName,
        illustrationResName = illustrationResName,
        means = means.map { it.t(language) },
        warningSigns = warningSigns.map { it.t(language) },
        whatToDo = whatToDo.map { it.t(language) },
        whereHelp = whereHelp.map { it.t(language) },
        learnMore = learnMore.map { it.t(language) },
        slides = slides.map { EducationSlideView(it.title.t(language), it.body.t(language)) },
        videoRawName = videoRawName,
        audioKey = EducationMediaLookup.audioRawName(id, language),
        quiz = EducationQuizView(
            question = quiz.question.t(language),
            options = quiz.options.map { (oid, text, correct) ->
                EducationQuizOption(oid, text.t(language), correct)
            },
            explain = quiz.explain.t(language)
        )
    )
}

object EducationCatalog {

    val topicIds = listOf(
        "gbv",
        "fgm",
        "warning_signs",
        "prevention",
        "immediate_safety",
        "reporting_evidence",
        "seeking_help",
        "dignity"
    )

    fun topics(language: String): List<EducationTopicView> =
        sources.map { it.resolve(language) }

    fun topicById(language: String, id: String): EducationTopicView? =
        topics(language).find { it.id == id }

    fun categories(language: String): List<EducationCategory> =
        topics(language).map { view ->
            EducationCategory(
                id = view.id,
                title = view.title,
                summary = view.summary,
                iconResName = view.iconResName,
                lessons = listOf(view.asModule())
            )
        }

    fun allLessons(language: String): List<EducationModule> =
        categories(language).flatMap { it.lessons }

    fun categoryById(language: String, id: String): EducationCategory? =
        categories(language).find { it.id == id }

    fun lessonById(language: String, id: String): EducationModule? =
        allLessons(language).find { it.id == id }

    fun iconResNameFor(categoryId: String): String =
        sources.find { it.id == categoryId }?.iconResName ?: "ic_edu_gbv"

    private fun EducationTopicView.asModule(): EducationModule = EducationModule(
        id = id,
        title = title,
        summary = summary,
        body = (learnMore + whereHelp).joinToString("\n"),
        videoRawName = videoRawName
    )

    private val sources = listOf(
        TopicSrc(
            id = "gbv",
            iconResName = "ic_edu_gbv",
            illustrationResName = "ic_edu_gbv",
            title = copy("Understanding GBV", "Eisom GBV", "Eisom GBV"),
            summary = copy("GBV is harm because of gender. It is never your fault.", "GBV ni madhara kwa sababu ya jinsia. Si kosa lako.", "GBV naa olchani olaita enkop jinsia. Meji olchani enye."),
            means = listOf(
                copy("Hitting, kicking, or burning.", "Kupiga, teke, au kuchoma.", "Olmurata, teke, kata olchani o ncho."),
                copy("Forced sex or sexual touching.", "Ngono ya kulazimishwa au kugusa kingono.", "Olmurata olmurata oleng'ata kata kugusa eng'eni."),
                copy("Threats, insults, or locking you in.", "Aitisho, matusi, au kukufunga ndani.", "Aitisho, olng'obani olchani, kata aikure enkop."),
                copy("Stopping school, work, or money.", "Kukuzuia shule, kazi, au pesa.", "Melo aitum enkolong, olchani, kata airitisho.")
            ),
            warningSigns = listOf(
                copy("Fear at home or with a partner.", "Hofu nyumbani au na mwenzi.", "Olchani enkaji kata na olmurata."),
                copy("Someone controls your phone.", "Mtu anadhibiti simu yako.", "Oltungani ne aitum esimu enye."),
                copy("You are stopped from seeing friends.", "Unazuiliwa kuona marafiki.", "Kejap aitum olaiteng'en."),
                copy("Threats if you say no.", "Aitisho ne ukataa.", "Aitisho ne aukata.")
            ),
            whatToDo = listOf(
                copy("Tell a person you trust.", "Jok mtu unayemwamini.", "Jok olaiteng'en oleng'ata."),
                copy("Go to a safer place if you can.", "Nenda mahali sidai ikiwa unaweza.", "Aing'oru enkop sidai ne aji."),
                copy("Call 1195 for GBV help.", "Piga 1195 kwa Minto wa GBV.", "Aisho 1195 ne Minto GBV."),
                copy("In danger, call 999 or 112.", "Ukiwa olchani, piga 999 au 112.", "Ne olchani sidai, aisho 999 kata 112.")
            ),
            whereHelp = listOf(
                copy("GBV helpline 1195.", "esimu ya GBV 1195.", "esimu ya GBV 1195."),
                copy("Police 999 or 112.", "Polisi 999 au 112.", "Polisi 999 kata 112."),
                copy("Hospital in Kajiado Town.", "Hospitali mjini Kajiado.", "Hospital enkop Kajiado."),
                copy("Survivor Support in this app.", "Minto kwa Waathiriwa katika programu hii.", "Minto Olaiteng'en app eno.")
            ),
            learnMore = listOf(
                copy("GBV can come from someone you know.", "GBV inaweza kutoka kwa mtu unayemjua.", "GBV ne aji olaiteng'en olaita."),
                copy("Culture never requires violence.", "Utamaduni hauhitaji vurugu.", "Olchani melo nchoo olchani."),
                copy("This is teaching, not a diagnosis.", "Hii ni elimu, si uchunguzi.", "Ena naa eisom, meji enkop ol.")
            ),
            slides = listOf(
                SlideSrc(copy("GBV", "GBV", "GBV"), copy("Harm because of gender. Never the survivor�s fault.", "Olchani olaita enkop jinsia. Meji olchani olaita.", "Olchani olaita enkop jinsia. Meji olchani olaita.")),
                SlideSrc(copy("Forms", "Aina", "Enkop"), copy("Physical, sexual, emotional, and money control.", "Olmurata, olmurata, enkop enye, na enkiteng airitisho.", "Olmurata, olmurata, enkop enye, na enkiteng airitisho.")),
                SlideSrc(copy("Help", "Minto", "Minto"), copy("Call 999, 112, or GBV helpline 1195.", "Piga 999, 112, au esimu ya GBV 1195.", "Aisho 999, 112, kata esimu ya GBV 1195."))
            ),
            videoRawName = "edu_gbv_what",
            quiz = QuizSrc(
                question = copy("Is hitting a woman or girl GBV?", "kumpiga mwanamke au enkitok ni GBV?", "Olmurata entito kata enkitok naa GBV?"),
                options = listOf(
                    Triple("yes", copy("Yes", "Ndiyo", "Ee"), true),
                    Triple("family", copy("Only if a stranger does it", "Tu mgeni akifanya", "Tu oltungani ang'eni"), false),
                    Triple("no", copy("No, it is a private matter", "Hapana, ni siri ya nyumbani", "Maya, naa olng'obani eng'eni enkaji"), false)
                ),
                explain = copy("Yes. Violence because of gender is GBV. Help is available.", "Ndiyo. Vurugu kwa sababu ya jinsia ni GBV. Minto upo.", "Ee. Olchani olaita enkop jinsia naa GBV. Minto ne aji.")
            )
        ),
        TopicSrc(
            id = "fgm",
            iconResName = "ic_edu_fgm",
            illustrationResName = "ic_edu_fgm",
            title = copy("Understanding FGM", "Eisom FGM", "Eisom FGM"),
            summary = copy("FGM is cutting a girl�s genitals. It is illegal in Kenya.", "FGM ni Akekete sehemu za siri za enkitok. Ni kinyume cha sheria Kenya.", "FGM naa Akekete eng'eni enkitok. Meji sidai Kenya."),
            means = listOf(
                copy("Cutting or injuring a girl�s genitals.", "Akekete au aipungu sehemu za siri za enkitok.", "Akekete kata aipungu eng'eni enkitok."),
                copy("Taking a girl away to be cut.", "Aitum enkitok akeeketwe.", "Aitum enkitok akeeketwe."),
                copy("Pressuring a family to cut a girl.", "Aitum enkop familia akekete enkitok.", "Aitum enkop familia akekete enkitok.")
            ),
            warningSigns = listOf(
                copy("Talk of �becoming a woman� by cutting.", "Olng'obani eng'eni ne aitum enkitok.", "Olng'obani eng'eni ne aitum enkitok."),
                copy("Secret travel or a ceremony soon.", "Safari ya siri au sherehe karibuni.", "Aitubul eng'eni kata enkop ol oleng'ata."),
                copy("A girl is kept home from school.", "Enkitok kejap aitum enkolong.", "Enkitok kejap aitum enkolong."),
                copy("Relatives pressure the family.", "Olaiteng'en ne enkiteng olfamilia.", "Olaiteng'en ne enkiteng olfamilia.")
            ),
            whatToDo = listOf(
                copy("Believe the girl.", "Aamini enkitok.", "Aamini enkitok."),
                copy("Tell a trusted adult.", "Jok oltungani oleng'ata olaiteng'en.", "Jok oltungani oleng'ata olaiteng'en."),
                copy("Call Childline 116.", "Piga Childline 116.", "Aisho Childline 116."),
                copy("Do not face cutters alone if unsafe.", "Melo aikabiliane na wanaokeketa peke yako ikiwa si sidai.", "Melo aikabiliane olaita FGM eng'eni ne meji enkigwanak.")
            ),
            whereHelp = listOf(
                copy("Childline 116.", "Childline 116.", "Childline 116."),
                copy("GBV helpline 1195.", "esimu ya GBV 1195.", "esimu ya GBV 1195."),
                copy("Police 999 or 112.", "Polisi 999 au 112.", "Polisi 999 kata 112."),
                copy("County gender desk in Kajiado.", "Gender desk enkop Kajiado.", "Gender desk enkop Kajiado.")
            ),
            learnMore = listOf(
                copy("Kenya�s 2011 FGM law forbids FGM.", "Sheria ya FGM ya 2011 inakataza FGM.", "Ng'otol FGM 2011 melo aji FGM Kenya."),
                copy("No religion requires FGM.", "Meji dini ne nchoo FGM.", "Meji dini ne nchoo FGM."),
                copy("Marriage never needs cutting.", "Ndoa haihitaji ukeketaji.", "Enkaiya melo nchoo FGM.")
            ),
            slides = listOf(
                SlideSrc(copy("FGM law", "Sheria ya FGM", "Ng'otol FGM"), copy("FGM is illegal in Kenya (2011 Act).", "FGM ni kinyume cha sheria Kenya (sheria ya 2011).", "FGM meji sidai Kenya (ng'otol 2011).")),
                SlideSrc(copy("Rights", "Haki", "Ng'otol"), copy("Nobody may cut a girl or take her for FGM.", "Hakuna anayeruhusiwa kumkeketa au kumpeleka FGM.", "Melo oltungani ne aji akekete enkitok kata aitum FGM.")),
                SlideSrc(copy("Help", "Minto", "Minto"), copy("Call Childline 116 or GBV helpline 1195.", "Piga Childline 116 au esimu ya GBV 1195.", "Aisho Childline 116 kata esimu ya GBV 1195."))
            ),
            videoRawName = "edu_fgm_law",
            quiz = QuizSrc(
                question = copy("Is FGM allowed by Kenyan law?", "FGM inaruhusiwa na sheria ya Kenya?", "FGM ne sidai enkop ng'otol Kenya?"),
                options = listOf(
                    Triple("no", copy("No. FGM is illegal.", "Hapana. FGM ni kinyume cha sheria.", "Maya. FGM meji sidai."), true),
                    Triple("culture", copy("Yes, if the family agrees", "Ndiyo, familia ikikubali", "Ee, olfamilia ne aji"), false),
                    Triple("age", copy("Only for older girls", "Tu enkitok oleng'ata", "Tu enkitok oleng'ata"), false)
                ),
                explain = copy("FGM is illegal. A girl has the right to be protected.", "FGM ni kinyume cha sheria. enkitok ana haki ya kulindwa.", "FGM meji sidai. Enkitok kejap ng'otol enkigwanak.")
            )
        ),
        TopicSrc(
            id = "warning_signs",
            iconResName = "ic_edu_warning",
            illustrationResName = "ic_edu_safety",
            title = copy("Warning signs", "Ishara za olchani", "Ishara za olchani"),
            summary = copy("See danger early. You do not wait for a �big� injury.", "Ona olchani oleng'ata. Melo eng'eti olchani nene.", "Ona olchani oleng'ata. Melo eng'eti olchani nene."),
            means = listOf(
                copy("Warning signs are clues of GBV or FGM risk.", "Ishara ni dalili za hatari ya GBV au FGM.", "Ishara za olchani GBV kata FGM."),
                copy("They can be words, rules, or fear.", "Zinaweza kuwa maneno, sheria, au hofu.", "Ne aji olng'obani, ng'otol, kata olchani."),
                copy("Trust your feeling if you are unsafe.", "Amini hisia yako ukihisi hatari.", "Aamini enkop enye ne meji enkigwanak.")
            ),
            warningSigns = listOf(
                copy("Jealousy that controls your life.", "Olchani ne aitum oltau lino.", "Olchani ne aitum oltau lino."),
                copy("Checking your messages.", "Ne aing'oru imorijo enye.", "Ne aing'oru imorijo enye."),
                copy("Forced marriage talk.", "Olng'obani enkaiya enkaina.", "Olng'obani enkaiya enkaina."),
                copy("A girl is hidden before a �ceremony�.", "Enkitok ne olperesh o ncho enkop ol.", "Enkitok ne olperesh o ncho enkop ol.")
            ),
            whatToDo = listOf(
                copy("Name the sign to yourself.", "Aitito enkomom ishara eno.", "Aitito enkomom ishara eno."),
                copy("Tell a safe person.", "Jok mtu sidai.", "Jok oltungani sidai."),
                copy("Make a simple safety plan.", "Tito mpango rahisi wa Enkigwanak.", "Tito enkop ol enkigwanak."),
                copy("Call 1195 or 116 if a child is at risk.", "Piga 1195 au 116 mtoto akiwa olchani.", "Aisho 1195 kata 116 nchorro olchani.")
            ),
            whereHelp = listOf(
                copy("Trusted Contacts in this app.", "Mawasiliano ya Kuaminika katika programu hii.", "Olaiteng'en app eno."),
                copy("GBV helpline 1195.", "esimu ya GBV 1195.", "esimu ya GBV 1195."),
                copy("Childline 116 for children.", "Childline 116 kwa watoto.", "Childline 116 ne nchorro."),
                copy("Emergency Assistance in this app.", "Minto oldeke katika programu hii.", "Minto oldeke app eno.")
            ),
            learnMore = listOf(
                copy("Control can come before hitting.", "Enkiteng ne aji o ncho olmurata.", "Enkiteng ne aji o ncho olmurata."),
                copy("FGM risk can rise around ceremonies.", "Hatari ya FGM inaweza kuongezeka karibu na sherehe.", "Olchani FGM ne aji oleng'ata enkop ol."),
                copy("You are not imagining it.", "Si wewe unayefikiria tu.", "Meji enye eng'eni."),
            ),
            slides = listOf(
                SlideSrc(copy("See it", "Nchoo", "Nchoo"), copy("Fear, control, and secret travel can be warnings.", "Hofu, udhibiti, na safari za siri zinaweza kuwa onyo.", "Olchani, enkiteng, na aitubul eng'eni ne aji onyo.")),
                SlideSrc(copy("Act early", "Aji enkop", "Aji enkop"), copy("Tell someone safe. You do not wait.", "Jok mtu sidai. Husubiri.", "Jok oltungani sidai. Melo eng'eti.")),
                SlideSrc(copy("Help", "Minto", "Minto"), copy("1195 � 116 � 999", "1195 � 116 � 999", "1195 � 116 � 999"))
            ),
            quiz = QuizSrc(
                question = copy("If someone controls your phone, is that a warning?", "Mtu akidhibiti simu yako, hiyo ni onyo?", "Oltungani ne aitum esimu enye, eno naa onyo?"),
                options = listOf(
                    Triple("yes", copy("Yes", "Ndiyo", "Ee"), true),
                    Triple("love", copy("No, it means love", "Hapana, ni ishara ya upendo", "Maya, naa ishara enkop oleng'ata"), false),
                    Triple("later", copy("Only after they hit you", "Tu oleng'ata olmurata", "Tu oleng'ata olmurata"), false)
                ),
                explain = copy("Yes. Control of a phone can be a GBV warning sign.", "Ndiyo. Kudhibiti simu kunaweza kuwa onyo la GBV.", "Ee. Enkiteng esimu ne aji onyo la GBV.")
            )
        ),
        TopicSrc(
            id = "prevention",
            iconResName = "ic_edu_safety",
            illustrationResName = "ic_edu_safety",
            title = copy("Prevention", "Enkigwanak", "Enkigwanak"),
            summary = copy("Small plans can reduce GBV and FGM risk.", "Mipango midogo inaweza kupunguza hatari ya GBV na FGM.", "Enkop ol enkop ne aji kewa olchani GBV na FGM."),
            means = listOf(
                copy("Prevention means acting before harm.", "Enkigwanak ni kuAji enkop kabla ya madhara.", "Enkigwanak naa aji enkop o ncho olchani."),
                copy("A safety plan is people, place, and a code word.", "Mpango wa Enkigwanak ni watu, mahali, na neno la siri.", "Enkop ol enkigwanak naa oltungani, enkop, na olng'obani eng'eni."),
                copy("Protecting girls from FGM is prevention.", "Kulinda wasichana dhidi ya FGM ni Enkigwanak.", "Enkigwanak enkitok ne FGM naa Enkigwanak.")
            ),
            warningSigns = listOf(
                copy("No one to call in a crisis.", "Melo oltungani ne aisho esimu wakati wa oldeke.", "Melo oltungani ne aisho esimu ne oldeke."),
                copy("Shared PINs with someone unsafe.", "PIN unazoshiriki na mtu hatari.", "PIN ne aisho na oltungani olchani."),
                copy("A girl is being prepared for cutting.", "enkitok anaandaliwa kukeketwa.", "Enkitok ne aji enkop ol FGM."),
            ),
            whatToDo = listOf(
                copy("Save 1�5 trusted contacts here.", "Aibuk mawasiliano 1�5 hapa.", "Aibuk olaiteng'en 1–5 ene."),
                copy("Agree a code word for �help me�.", "Nchoo olng'obani eng'eni ne aitum minto.", "Nchoo olng'obani eng'eni ne aitum minto."),
                copy("Know 1195, 116, 999.", "Nchoo 1195, 116, 999.", "Nchoo 1195, 116, 999."),
                copy("Use Hide to look like a calculator.", "Aitum Ficha ionekane kama calculator.", "Aitum Olperesh ionekane kama calculator.")
            ),
            whereHelp = listOf(
                copy("Trusted Contacts in this app.", "Mawasiliano ya Kuaminika katika programu hii.", "Olaiteng'en app eno."),
                copy("Education cards in this section.", "Kadi za elimu katika sehemu hii.", "Kadi eisom eno."),
                copy("County gender desk, Kajiado.", "Gender desk enkop Kajiado.", "Gender desk enkop Kajiado."),
                copy("Teachers, clinics, and trusted elders who protect girls.", "Olmuasani, olchani, na oltungani oleng'ata ne enkigwanak enkitok.", "Olmuasani, olchani, na oltungani oleng'ata ne enkigwanak enkitok.")
            ),
            learnMore = listOf(
                copy("Prevention is a community duty.", "Enkigwanak ni wajibu wa jamii.", "Enkigwanak naa enchoo enkop olchani."),
                copy("You can say no to cutting and forced marriage.", "Unaweza kukataa ukeketaji na ndoa ya kulazimishwa.", "Ne aji ukata FGM kata enkaiya enkaina."),
                copy("Do not risk yourself to hide that you sought help.", "Melo aikuweke olchani ne aolperesh ne aitafuta Minto.", "Melo aikuweke olchani ne aolperesh ne aitafuta minto.")
            ),
            quiz = QuizSrc(
                question = copy("What belongs in a safety plan?", "Nini kiko katika mpango wa Enkigwanak?", "Naa ne enkop ol enkigwanak?"),
                options = listOf(
                    Triple("people", copy("Safe people and a code word", "Watu sidai na neno la siri", "Olaiteng'en sidai na olng'obani eng'eni"), true),
                    Triple("secret", copy("Keep all harm a secret forever", "Ficha madhara milele", "Olperesh olchani milele"), false),
                    Triple("alone", copy("Handle it only by yourself", "Aji eng'eni tu", "Aji eng'eni tu"), false)
                ),
                explain = copy("A plan is people, a safe place, and how to call help.", "Mpango ni watu, mahali sidai, na jinsi ya aisho eesimu ya minto.", "Enkop ol naa olaiteng'en, enkop sidai, na ne aisho esimu minto."),
            )
        ),
        TopicSrc(
            id = "immediate_safety",
            iconResName = "ic_edu_emergency",
            illustrationResName = "ic_edu_emergency",
            title = copy("Immediate safety", "Enkigwanak sidai", "Enkigwanak sidai"),
            summary = copy("If you are in danger now, your life comes first.", "Ukiwa olchani sasa, maisha yako yanatangulia.", "Ne olchani sidai, oltau lino naa oleng'ata."),
            means = listOf(
                copy("Immediate safety means getting out of harm now.", "Enkigwanak sidai ni kutoka olchani sasa.", "Enkigwanak sidai naa aitoka olchani sidai."),
                copy("You do not need to explain first.", "Si lazima ueleze kwanza.", "Melo nchoo aeleze oleng'ata."),
                copy("Leave belongings if you must.", "Acha vitu ikiwa ni lazima.", "Aas eng'eni ne nchoo.")
            ),
            warningSigns = listOf(
                copy("Someone is attacking you.", "Mtu anakushambulia.", "Oltungani ne aikosh olchani enye."),
                copy("A weapon is present.", "Kuna silaha.", "Kejap eng'eni olchani."),
                copy("You cannot leave the house.", "Huwezi kutoka nyumbani.", "Meji aitoka enkaji."),
                copy("A girl will be cut tonight or tomorrow.", "enkitok atakeketwa usiku huu au kesho.", "Enkitok ne aitum FGM eno o ncho.")
            ),
            whatToDo = listOf(
                copy("Move to a place with an exit.", "Nenda mahali penye njia ya kutoka.", "Aing'oru enkop ne aji enjira."),
                copy("Call 999 or 112.", "Piga 999 au 112.", "Aisho 999 kata 112."),
                copy("Open Emergency Assistance in this app.", "Fungua Minto oldeke katika programu hii.", "Aing'oru Minto oldeke app eno."),
                copy("Alert a trusted person if you can.", "Jok mtu wa kuaminika ikiwa unaweza.", "Aitum olaiteng'en ne aji.")
            ),
            whereHelp = listOf(
                copy("Police / emergency 999 or 112.", "Polisi / oldeke 999 au 112.", "Polisi / oldeke 999 kata 112."),
                copy("GBV helpline 1195.", "esimu ya GBV 1195.", "esimu ya GBV 1195."),
                copy("Childline 116.", "Childline 116.", "Childline 116."),
                copy("Emergency Assistance in this app.", "Minto oldeke katika programu hii.", "Minto oldeke app eno.")
            ),
            learnMore = listOf(
                copy("SOS from the calculator alerts your contacts by SMS.", "SOS kutoka calculator huwaarifu mawasiliano kwa SMS.", "SOS enkop calculator ne aitum olaiteng'en ne SMS."),
                copy("Access Code opens the app without sending SMS.", "Msimbo wa Ufikiaji hufungua programu bila SMS.", "Access Code aing'oru app meji SMS."),
                copy("This app is not a live police dispatch.", "Programu hii si dashibodi ya polisi moja kwa moja.", "App eno meji polisi eng'eni.")
            ),
            slides = listOf(
                SlideSrc(copy("Now", "Sasa", "Sidai"), copy("Get safer. Call 999 or 112.", "Kuwa sidai. Piga 999 au 112.", "Ewu sidai. Aisho 999 kata 112.")),
                SlideSrc(copy("People", "Watu", "Oltungani"), copy("Tell a trusted person. Use Emergency Assistance.", "Jok mtu wa kuaminika. Aitum Minto oldeke.", "Jok olaiteng'en oleng'ata. Aitum Minto oldeke.")),
                SlideSrc(copy("Children", "Watoto", "Nchorro"), copy("Childline 116 if a child is in danger.", "Childline 116 mtoto akiwa olchani.", "Childline 116 nchorro olchani."))
            ),
            quiz = QuizSrc(
                question = copy("If you are being attacked, what first?", "Ukishambuliwa, nini kwanza?", "Ne aikosh olchani, naa oleng'ata?"),
                options = listOf(
                    Triple("safe", copy("Get safer and call 999 / 112", "Kuwa sidai na piga 999 / 112", "Ewu sidai na aisho 999 / 112"), true),
                    Triple("explain", copy("Stay and explain everything", "Tej na aeleze eng'eni", "Tej na aeleze eng'eni"), false),
                    Triple("things", copy("Pack all belongings first", "Pakia vitu vyote kwanza", "Aibuk eng'eni oleng'ata"), false)
                ),
                explain = copy("Your life comes first. Call emergency numbers.", "Maisha yako yanatangulia. Piga nambari za oldeke.", "Oltau lino naa oleng'ata. Aisho nambari za oldeke.")
            )
        ),
        TopicSrc(
            id = "reporting_evidence",
            iconResName = "ic_edu_reporting",
            illustrationResName = "ic_edu_reporting",
            title = copy("Reporting and evidence", "Ripoti na ushahidi", "Ripoti na enkop ol"),
            summary = copy("You choose if and when to report. Stay safe first.", "Wewe unachagua kama na lini Ripoti. Enkigwanak kwanza.", "Oltungani enye ne aji ripoti naa oleng'ata. Enkigwanak oleng'ata."),
            means = listOf(
                copy("Reporting means telling police, a desk, or a helpline.", "Ripoti ni jok polisi, dawati, au esimu ya minto.", "Ripoti naa aitum polisi, gender desk, kata esimu minto."),
                copy("Evidence can be notes, messages, or medical papers.", "Ushahidi unaweza kuwa maelezo, ujumbe, au karatasi za hospitali.", "Enkop ol ne aji olng'obani, imorijo, kata olng'obani hospital."),
                copy("You may wait until it is safer.", "Unaweza kusubiri iwe sidai zaidi.", "Ne aji eng'eti oleng'ata sidai.")
            ),
            warningSigns = listOf(
                copy("Someone says you must not tell.", "Mtu anasema usiseme.", "Oltungani ne aji meji aitum."),
                copy("You are asked for a bribe to report.", "Unaulizwa rushwa ili uripoti.", "Ne aji aom enkop olchani ne aripoti."),
                copy("Keeping proof would put you in danger.", "Aibuk enkop ol ne aikuweke olchani.", "Aibuk enkop ol ne aikuweke olchani.")
            ),
            whatToDo = listOf(
                copy("Save a private GBV/FGM note on this phone first.", "Aibuk maelezo ya GBV/FGM kwenye simu hii kwanza.", "Aibuk olng'obani GBV/FGM enkop esimu eno oleng'ata."),
                copy("Do not collect proof if it is unsafe.", "Usikusanye ushahidi ikiwa si sidai.", "Melo aikusi enkop ol ne meji enkigwanak."),
                copy("Ask a hospital for documentation if you go.", "Aom hospital olng'obani ne aing'oru.", "Aom hospital olng'obani ne aing'oru."),
                copy("Report to police or the gender desk when ready.", "Ripoti polisi au dawati la jinsia ukiwa tayari.", "Ripoti polisi kata gender desk ne sidai.")
            ),
            whereHelp = listOf(
                copy("GBV/FGM Reporting in this app.", "Ripoti GBV/FGM katika programu hii.", "Ripoti GBV/FGM app eno."),
                copy("Police 999 / 112.", "Polisi 999 / 112.", "Polisi 999 / 112."),
                copy("County gender desk, Kajiado.", "Gender desk enkop Kajiado.", "Gender desk enkop Kajiado."),
                copy("GBV helpline 1195.", "esimu ya GBV 1195.", "esimu ya GBV 1195.")
            ),
            learnMore = listOf(
                copy("Notes in this app stay on this phone first.", "Maelezo katika programu hii yanakaa kwenye simu hii kwanza.", "Olng'obani app eno nejap esimu eno oleng'ata."),
                copy("Upload is optional, with your consent.", "Kupakia ni hiari, kwa idhini yako.", "Aitum backend naa eng'eni, ne enchoo enye."),
                copy("NGO staff do not see another user�s private text.", "Wafanyakazi wa NGO hawaoni maandishi ya faragha ya mtumiaji mwingine.", "Olaiteng'en NGO meji aonyish olng'obani eng'eni oltungani ang'eni.")
            ),
            quiz = QuizSrc(
                question = copy("Must you report the same day?", "lazima uripoti siku hiyo hiyo?", "Ne aji aripoti eno sidai?"),
                options = listOf(
                    Triple("no", copy("No. You can wait until you are safer.", "Hapana. Unaweza kusubiri uwe sidai.", "Maya. Ne aji eng'eti oleng'ata sidai."), true),
                    Triple("yes", copy("Yes, or it does not count", "Ndiyo, au haihesabiki", "Ee, kata meji ne aji enkop ol."), false),
                    Triple("bribe", copy("Only if you pay a bribe", "Tu ukilipa rushwa", "Tu ne aji lipa enkop olchani"), false)
                ),
                explain = copy("You can report later. Safety comes first. No bribe is required.", "Unaweza Ripoti baadaye. Enkigwanak kwanza. Rushwa si lazima.", "Ne aji aripoti oleng'ata. Enkigwanak oleng'ata. Enkop olchani meji ne aji.")
            )
        ),
        TopicSrc(
            id = "seeking_help",
            iconResName = "ic_edu_relationships",
            illustrationResName = "ic_edu_survivor",
            title = copy("Seeking help", "Aom minto", "Aom minto"),
            summary = copy("Asking for help is strength. You are not alone in Kajiado.", "Kuomba Minto ni nguvu. Huwa peke yako Kajiado.", "Kuomba minto naa enkop oleng'ata. Meji eng'eni Kajiado."),
            means = listOf(
                copy("Help can be a friend, clinic, helpline, or police.", "Minto unaweza kuwa rafiki, kliniki , esimu, au polisi.", "Minto ne aji olaiteng'en, olchani, esimu, kata polisi."),
                copy("You choose who to tell.", "Wewe unachagua wa kuambia.", "Oltungani enye ne aji naa aitum."),
                copy("Professional counselling is available by referral.", "Ushauri nasaha wa kitaalamu unapatikana kwa rufaa.", "Olaiteng'en enkop enye ne aji enkop olaiteng'en.")
            ),
            warningSigns = listOf(
                copy("Shame tells you to stay silent.", "Aibu inakuambia nyamaza.", "Olng'aa ne aji aar."),
                copy("Someone laughs at you for asking help.", "Mtu anakucheka kwa kuomba Minto.", "Oltungani ne aicheka ne aom minto."),
                copy("You do not know a safe number.", "Hujui nambari sidai.", "Meji aijui Namba sidai.")
            ),
            whatToDo = listOf(
                copy("Open Survivor Support in this app.", "Fungua Minto kwa Waathiriwa katika programu hii.", "Aing'oru Minto Olaiteng'en app eno."),
                copy("Call 1195 and ask for counselling.", "Piga 1195 uulize ushauri nasaha.", "Aisho 1195 ne aom olaiteng'en enkop enye."),
                copy("Go to Kajiado Referral Hospital if you need care.", "Nenda Hospitali ya Rufaa ya Kajiado ukihitaji huduma.", "Aing'oru Hospital Kajiado ne nchoo olchani."),
                copy("Save trusted contacts for SOS SMS.", "Aibuk mawasiliano kwa SMS ya SOS.", "Aibuk olaiteng'en ne SMS SOS.")
            ),
            whereHelp = listOf(
                copy("Survivor Support in this app.", "Minto kwa Waathiriwa katika programu hii.", "Minto Olaiteng'en app eno."),
                copy("GBV helpline 1195.", "esimu ya GBV 1195.", "esimu ya GBV 1195."),
                copy("Childline 116.", "Childline 116.", "Childline 116."),
                copy("Hospital and gender desk in Kajiado.", "Hospitali na dawati la jinsia Kajiado.", "Hospital na gender desk Kajiado.")
            ),
            learnMore = listOf(
                copy("This app does not replace a counsellor.", "Programu hii haichukui nafasi ya mshauri.", "App eno meji aitum nafasi olaiteng'en."),
                copy("Confirm phone numbers locally.", "Thibitisha nambari kwa sasa.", "Nchoo Namba sidai enkop Kajiado."),
                copy("Friends should listen and not share your story.", "Marafiki wasikilize, wasishiriki hadithi yako.", "Olaiteng'en ne ting'o na meji aisho olng'obani enye.")
            ),
            quiz = QuizSrc(
                question = copy("Is asking for help weakness?", "kuomba Minto ni udhaifu?", "Kuomba minto naa enkop olchani?"),
                options = listOf(
                    Triple("no", copy("No. It is a brave step.", "Hapana. Ni hatua ya ujasiri.", "Maya. Naa enkop ol oleng'ata."), true),
                    Triple("yes", copy("Yes. Stay silent.", "Ndiyo. Nyamaza.", "Ee. Aar."), false),
                    Triple("family", copy("Only family may know", "Familia tu ndiyo inayostahili kujua", "Olfamilia eng'eni tu ne aji nchoo"), false)
                ),
                explain = copy("Seeking help is strength. You choose who to tell.", "Kuomba Minto ni nguvu. Wewe unachagua wa kuambia.", "Kuomba minto naa enkop oleng'ata. Oltungani enye ne aji naa aitum.")
            )
        ),
        TopicSrc(
            id = "dignity",
            iconResName = "ic_edu_recovery",
            illustrationResName = "ic_edu_recovery",
            title = copy("Dignity and wellbeing", "Heshima na ustawi", "Enkop na enkop oleng'ata"),
            summary = copy("You are not less of a person. Healing can take time.", "Hupunguzwi kama binadamu. Uponyaji unaweza kuchukua muda.", "Meji aipungu enkop oltungani. Enkop olchani ne aji oleng'ata."),
            means = listOf(
                copy("Dignity means you still have worth.", "Heshima naa bado una thamani.", "Enkop naa bado kejap enkop enye."),
                copy("Feelings after harm can be strong.", "Hisia baada ya madhara zinaweza kuwa kali.", "Enkop enye oleng'ata olchani ne aji nene."),
                copy("You deserve safety and respect.", "Kejap sidai Enkigwanak na heshima.", "Kejap sidai enkigwanak na enkop.")
            ),
            warningSigns = listOf(
                copy("You blame yourself for the harm.", "Ne aji olchani enye kwa madhara.", "Ne aji olchani enye ne olchani."),
                copy("You feel you must stay silent forever.", "Ne aingoru lazima unyamaze milele.", "Ne aingoru aar milele."),
                copy("Someone says you brought shame.", "Mtu anasema ulileta aibu.", "Oltungani ne aji aileta olng'aa.")
            ),
            whatToDo = listOf(
                copy("Remember: the harm is not your fault.", "Nchoo: madhara si kosa lako.", "Nchoo: olchani meji olchani enye."),
                copy("Talk to someone you trust.", "Olaitu na mtu unayemwamini.", "Olaitu na olaiteng'en oleng'ata."),
                copy("Rest. Healing is not a race.", "Engeti. Uponyaji si mbio.", "Engeti. Enkop olchani meji eng'eti."),
                copy("Open Survivor Support for more care words.", "Fungua Minto kwa Waathiriwa kwa maneno zaidi ya faraja.", "Aing'oru Minto Olaiteng'en ne olng'obani oleng'ata enkop eng'eni.")
            ),
            whereHelp = listOf(
                copy("Survivor Support in this app.", "Minto kwa Waathiriwa katika programu hii.", "Minto Olaiteng'en app eno."),
                copy("GBV helpline 1195 for counselling.", "esimu ya GBV 1195 kwa ushauri nasaha.", "esimu ya GBV 1195 ne olaiteng'en enkop enye."),
                copy("A trusted friend, teacher, or faith leader who does not blame you.", "Rafiki, mwalimu, au kiongozi wa imani asiyekulaumu.", "Olaiteng'en, olmuasani, kata oltungani enkop ol ne meji aikulaumu."),
                copy("Hospital care if your body needs it.", "Huduma ya hospitali mwili ukihitaji.", "Minto hospital ne nchoo oltau lino.")
            ),
            learnMore = listOf(
                copy("Shame belongs to the person who caused harm.", "Aibu ni ya aliyefanya madhara.", "Olng'aa naa enkop oltungani ne aipang olchani."),
                copy("This app is not therapy.", "Programu hii si tiba.", "App eno meji olaiteng'en olchani."),
                copy("You can belong in your community and still seek safety.", "Unaweza kuwa sehemu ya jamii na bado kutafuta Enkigwanak.", "Ne aji enkop olchani na bado aitafuta enkigwanak.")
            ),
            quiz = QuizSrc(
                question = copy("Does GBV or FGM make you worth less?", "GBV au FGM inakupunguzia thamani?", "GBV kata FGM ne aipungu enkop enye?"),
                options = listOf(
                    Triple("no", copy("No. You still have full worth.", "Hapana. Bado una thamani kamili.", "Maya. Kejap enkop enye oleng'ata."), true),
                    Triple("yes", copy("Yes, you must hide", "Ndiyo, lazima ujifiche", "Ee, ne aji olperesh"), false),
                    Triple("wait", copy("Only after you report", "Tu baada ya Ripoti", "Tu oleng'ata ripoti"), false)
                ),
                explain = copy("You are not less of a person. Harm does not define your worth.", "Hupunguzwi kama binadamu. Madhara hayafafanui thamani yako.", "Meji aipungu enkop oltungani. Olchani meji aifafanui enkop enye.")
            )
        )
    )
}
