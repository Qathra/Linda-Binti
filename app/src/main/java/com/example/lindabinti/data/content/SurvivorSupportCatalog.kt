package com.example.lindabinti.data.content



/**

 * One combined offline Survivor Support catalog (emotional support, guidance, counselling).

 * English is the fallback for languages that are not yet translated.

 * This is not counselling, medical care, or a diagnosis.

 */

data class SupportTruth(

    val id: String,

    val title: String,

    val body: String

)



data class SupportMessage(

    val id: String,

    val text: String

)



data class SupportNextStep(

    val id: String,

    val title: String,

    val body: String

)



enum class SupportServiceKind {

    EMERGENCY,

    GBV,

    CHILD_PROTECTION,

    MEDICAL,

    COUNSELLING,

    POLICE,

    FGM

}



enum class SurvivorHelpAction {

    OPEN_EMERGENCY,

    OPEN_CONTACTS,

    OPEN_REPORTING,

    FILTER_GBV,

    FILTER_MEDICAL,

    FILTER_COUNSELLING,

    FILTER_POLICE,

    FILTER_CHILD,

    FILTER_FGM

}



data class SurvivorHelpShortcut(

    val id: String,

    val action: SurvivorHelpAction

)



object SurvivorSupportCatalog {



    val truthIds = listOf(

        "not_to_blame",

        "worth",

        "safety_dignity",

        "seeking_help",

        "talk_trusted",

        "medical_right",

        "protection_right",

        "report_when_ready",

        "recovery_time",

        "professional_help"

    )



    val shortcuts = listOf(

        SurvivorHelpShortcut("emergency", SurvivorHelpAction.OPEN_EMERGENCY),

        SurvivorHelpShortcut("contacts", SurvivorHelpAction.OPEN_CONTACTS),

        SurvivorHelpShortcut("gbv", SurvivorHelpAction.FILTER_GBV),

        SurvivorHelpShortcut("fgm", SurvivorHelpAction.FILTER_FGM),

        SurvivorHelpShortcut("medical", SurvivorHelpAction.FILTER_MEDICAL),

        SurvivorHelpShortcut("counselling", SurvivorHelpAction.FILTER_COUNSELLING),

        SurvivorHelpShortcut("police", SurvivorHelpAction.FILTER_POLICE),

        SurvivorHelpShortcut("child", SurvivorHelpAction.FILTER_CHILD),

        SurvivorHelpShortcut("reporting", SurvivorHelpAction.OPEN_REPORTING)

    )



    fun coreMessage(language: String): String =

        when (SupportedLanguages.catalogCode(language)) {

            SupportedLanguages.SW -> CORE_SW

            SupportedLanguages.MAA -> CORE_MAA

            else -> CORE_EN

        }



    fun truths(language: String): List<SupportTruth> =

        when (SupportedLanguages.catalogCode(language)) {

            SupportedLanguages.SW -> truthsSw

            SupportedLanguages.MAA -> truthsMaa

            else -> truthsEn

        }



    fun messages(language: String): List<SupportMessage> =

        when (SupportedLanguages.catalogCode(language)) {

            SupportedLanguages.SW -> messagesSw

            SupportedLanguages.MAA -> messagesMaa

            else -> messagesEn

        }



    fun nextSteps(language: String): List<SupportNextStep> =

        when (SupportedLanguages.catalogCode(language)) {

            SupportedLanguages.SW -> stepsSw

            SupportedLanguages.MAA -> stepsMaa

            else -> stepsEn

        }



    fun kindForAction(action: SurvivorHelpAction): SupportServiceKind? = when (action) {

        SurvivorHelpAction.FILTER_GBV -> SupportServiceKind.GBV

        SurvivorHelpAction.FILTER_MEDICAL -> SupportServiceKind.MEDICAL

        SurvivorHelpAction.FILTER_COUNSELLING -> SupportServiceKind.COUNSELLING

        SurvivorHelpAction.FILTER_POLICE -> SupportServiceKind.POLICE

        SurvivorHelpAction.FILTER_CHILD -> SupportServiceKind.CHILD_PROTECTION

        SurvivorHelpAction.FILTER_FGM -> SupportServiceKind.FGM

        else -> null

    }



    private const val CORE_EN =

        "You are not less of a person because of what happened to you."



    private const val CORE_SW =

        "Yaliyokupata hayakupunguzi kama binadamu."



    private const val CORE_MAA =

        "Olchani olaita enye melo aipungu enkop oltungani enye."



    private val truthsEn = listOf(

        SupportTruth(

            "not_to_blame",

            "You are not to blame",

            "Nothing you wore, said, or did gives anyone the right to harm you. The responsibility belongs with the person who caused harm."

        ),

        SupportTruth(

            "worth",

            "Your experience does not define your worth",

            "What happened is part of your story. It is not the whole of who you are. You remain a person of value."

        ),

        SupportTruth(

            "safety_dignity",

            "You deserve safety and dignity",

            "Every person deserves to live without fear, shame, or violence. Your safety and dignity matter."

        ),

        SupportTruth(

            "seeking_help",

            "Seeking help is not weakness",

            "Asking for care is a brave step. You do not have to carry this by yourself."

        ),

        SupportTruth(

            "talk_trusted",

            "It is okay to talk to someone you trust",

            "A trusted relative, friend, teacher, faith leader, or neighbour can listen. You choose who, and you choose when."

        ),

        SupportTruth(

            "medical_right",

            "You have the right to seek medical care",

            "Care after violence or FGM-related harm is your right. You do not need anyone's permission to go to a clinic or hospital."

        ),

        SupportTruth(

            "protection_right",

            "You have the right to seek protection",

            "You can ask police, a gender desk, Childline, or a trusted adult to help keep you safer. You should not have to pay a bribe for this."

        ),

        SupportTruth(

            "report_when_ready",

            "You can report when you are ready and safe",

            "Reporting is your choice. If it is not safe now, wait. A private note in this app can stay on this phone until you are ready."

        ),

        SupportTruth(

            "recovery_time",

            "Recovery can take time",

            "Shock, fear, anger, numbness, or sleeplessness can happen after harm. Healing is not a race, and it does not look the same for everyone."

        ),

        SupportTruth(

            "professional_help",

            "You can seek professional counselling and support",

            "Trained counsellors and GBV workers can walk with you. This app can point you toward them. It cannot take their place."

        )

    )



    private val truthsSw = listOf(

        SupportTruth(

            "not_to_blame",

            "Si kosa lako",

            "Hakuna ulichovaa, kusema, au kufanya kinachompa mtu haki ya kukudhuru. Wajibu ni wa aliyefanya madhara."

        ),

        SupportTruth(

            "worth",

            "Yaliyokupata hayafafanui thamani yako",

            "Kilichotokea ni sehemu ya hadithi yako. Si wewe wote. Wewe bado ni mtu mwenye thamani."

        ),

        SupportTruth(

            "safety_dignity",

            "Unastahili usalama na heshima",

            "Kila mtu anastahili kuishi bila hofu, aibu, au vurugu. Usalama na heshima yako ni muhimu."

        ),

        SupportTruth(

            "seeking_help",

            "Kuomba msaada si udhaifu",

            "Kuomba huduma ni hatua ya ujasiri. Si lazima ubebe mzigo huu peke yako."

        ),

        SupportTruth(

            "talk_trusted",

            "Ni sawa kuzungumza na mtu unayemwamini",

            "Ndugu, rafiki, mwalimu, kiongozi wa imani, au jirani anayeaminika anaweza kusikiliza. Wewe unachagua nani na lini."

        ),

        SupportTruth(

            "medical_right",

            "Una haki ya kutafuta matibabu",

            "Huduma baada ya vurugu au madhara yanayohusiana na FGM ni haki yako. Huhitaji ruhusa ya mtu kwenda kliniki au hospitali."

        ),

        SupportTruth(

            "protection_right",

            "Una haki ya kutafuta ulinzi",

            "Unaweza kuomba polisi, dawati la jinsia, Childline, au mtu mzima wa kuaminika kukusaidia kuwa salama zaidi. Hupaswi kulipa rushwa kwa hili."

        ),

        SupportTruth(

            "report_when_ready",

            "Unaweza kuripoti ukiwa tayari na salama",

            "Kuripoti ni uamuzi wako. Ikiwa si salama sasa, subiri. Maelezo ya faragha katika programu hii yanaweza kukaa kwenye simu hii hadi uwe tayari."

        ),

        SupportTruth(

            "recovery_time",

            "Uponyaji unaweza kuchukua muda",

            "Mshtuko, hofu, hasira, kukosa hisia, au usingizi mgumu vinaweza kutokea baada ya madhara. Uponyaji si mbio, wala hauonekani sawa kwa kila mtu."

        ),

        SupportTruth(

            "professional_help",

            "Unaweza kutafuta ushauri nasaha na msaada wa kitaalamu",

            "Washauri waliofunzwa na wahudumu wa GBV wanaweza kutembea nawe. Programu hii inaweza kukuonyesha njia. Haiwezi kuchukua nafasi yao."

        )

    )



    private val messagesEn = listOf(

        SupportMessage("m1", "You still belong. Harm does not take away your place in your family or community."),

        SupportMessage("m2", "Your feelings are allowed. You do not have to be 'strong' every minute."),

        SupportMessage("m3", "One safe conversation can be a beginning. You do not have to tell everything at once."),

        SupportMessage("m4", "Rest, prayer, school, work, or quiet time can all be part of healing — at your pace."),

        SupportMessage("m5", "You are worthy of kindness, including kindness toward yourself.")

    )



    private val messagesSw = listOf(

        SupportMessage("m1", "Bado una nafasi yako. Madhara hayakunyimi mahali pako katika familia au jamii."),

        SupportMessage("m2", "Hisia zako zinaruhusiwa. Si lazima uwe 'shujaa' kila dakika."),

        SupportMessage("m3", "Mazungumzo salama moja yanaweza kuwa mwanzo. Si lazima ueleze yote mara moja."),

        SupportMessage("m4", "Kupumzika, sala, shule, kazi, au wakati wa utulivu vinaweza kuwa sehemu ya uponyaji — kwa kasi yako."),

        SupportMessage("m5", "Unastahili fadhili, pamoja na fadhili kwako mwenyewe.")

    )



    private val stepsEn = listOf(

        SupportNextStep(

            "danger",

            "If you are in danger now",

            "Move toward a safer place if you can. Open Emergency Assistance, call 999 or 112, or alert a trusted person. You do not have to explain everything first."

        ),

        SupportNextStep(

            "breathe",

            "If you are safer but shaken",

            "Sit somewhere you can leave easily. Drink water if you can. You may feel many things at once. That does not mean you are 'broken'."

        ),

        SupportNextStep(

            "tell",

            "If you can tell someone",

            "Choose a person who listens without blaming you. Open Trusted Contacts to save them in this app, or call them when it is safe."

        ),

        SupportNextStep(

            "medical",

            "If you need medical care",

            "Go to a hospital or clinic and ask for gender-based violence or reproductive health support. Bring a trusted person if that feels safer."

        ),

        SupportNextStep(

            "child",

            "If a child is at risk",

            "A child facing violence, FGM, or forced marriage needs adult protection. Call Childline 116, or use Emergency Assistance."

        ),

        SupportNextStep(

            "counsel",

            "If you want to talk with a counsellor",

            "Start with GBV helpline 1195 and ask for psychosocial support. The listings below are starting points, not a booking service."

        ),

        SupportNextStep(

            "report",

            "If you want to write it down or report later",

            "Use GBV/FGM Reporting in this app when you are ready. Notes save on this phone first. You can wait until it is safer to tell anyone else."

        )

    )



    private val stepsSw = listOf(

        SupportNextStep(

            "danger",

            "Ukiwa hatarini sasa",

            "Nenda mahali salama zaidi ikiwa unaweza. Fungua Msaada wa Dharura, piga 999 au 112, au mwambie mtu wa kuaminika. Si lazima ueleze yote kwanza."

        ),

        SupportNextStep(

            "breathe",

            "Ukiwa salama zaidi lakini umeshtuka",

            "Kaa mahali unapoweza kutoka kwa urahisi. Kunywa maji ikiwa unaweza. Unaweza kuhisi mambo mengi pamoja. Hiyo haimaanishi umevunjika."

        ),

        SupportNextStep(

            "tell",

            "Ukiweza kumwambia mtu",

            "Chagua mtu anayesikiliza bila kukulaumu. Fungua Mawasiliano ya Kuaminika kuwahifadhi, au wapigie simu ikiwa ni salama."

        ),

        SupportNextStep(

            "medical",

            "Ukihitaji matibabu",

            "Nenda hospitali au kliniki na uulize msaada wa GBV au afya ya uzazi. Nenda na mtu wa kuaminika ikiwa ni salama zaidi."

        ),

        SupportNextStep(

            "child",

            "Mtoto akiwa hatarini",

            "Mtoto anayekabiliwa na vurugu, FGM, au ndoa ya kulazimishwa anahitaji ulinzi wa mtu mzima. Piga Childline 116, au tumia Msaada wa Dharura."

        ),

        SupportNextStep(

            "counsel",

            "Ukipenda kuzungumza na mshauri",

            "Anza na simu ya GBV 1195 na uulize msaada wa kisaikolojia. Orodha hapa chini ni mahali pa kuanzia, si huduma ya kuweka miadi."

        ),

        SupportNextStep(

            "report",

            "Ukipenda kuandika au kuripoti baadaye",

            "Tumia Kuripoti GBV/FGM katika programu hii ukiwa tayari. Maelezo huhifadhiwa kwenye simu hii kwanza. Unaweza kusubiri hadi iwe salama zaidi."

        )

    )



    private val truthsMaa = listOf(

        SupportTruth(

            "not_to_blame",

            "Meji olchani enye",

            "Melo enkop oloita, oloitaa, kata olchani olaita enye aji ne oltungani oloitaa olchani. Enkop olchani naa oltungani olaitaa."

        ),

        SupportTruth(

            "worth",

            "Olchani olaita melo aifafanui enkop enye",

            "Olchani olaita naa enkop olng'obani enye. Meji enye eng'eni. Oltungani enye kejap sidai enkop enye."

        ),

        SupportTruth(

            "safety_dignity",

            "Kejap sidai enkigwanak na enkop enye",

            "Oltungani wote kejap sidai aisho melo olchani, olng'aa, kata olchani. Enkigwanak na enkop enye naa enkop eng'eni."

        ),

        SupportTruth(

            "seeking_help",

            "Aom minto melo enkop olchani",

            "Aom minto naa enkop oleng'ata. Melo nchoo aibebe olchani eno eng'eni."

        ),

        SupportTruth(

            "talk_trusted",

            "Kejap sidai olaitu olaiteng'en",

            "Olaiteng'en, olaiteng'en, olmuasani, olaiteng'en enkop imani, kata oljirani olaiteng'en ne aji olng'obani. Oltungani enye ne aji naa oleng'ata."

        ),

        SupportTruth(

            "medical_right",

            "Kejap sidai enkop aitafuta olchani",

            "Enkop olchani oleng'ata olchani kata olchani le FGM naa ng'otol enye. Melo nchoo enkop oleng'ata oltungani ne aing'oru olchani kata hospital."

        ),

        SupportTruth(

            "protection_right",

            "Kejap sidai enkop aitafuta enkigwanak",

            "Ne aji aom polisi, gender desk, Childline 116, kata oltungani oleng'ata olaiteng'en ne aji aing'oru enkigwanak. Melo nchoo aisho enkop olchani ne enkop eno."

        ),

        SupportTruth(

            "report_when_ready",

            "Ne aji aripoti oleng'ata sidai na enkigwanak",

            "Ripoti naa enkop enye. Ne meji enkigwanak sidai, eng'eti. Enkop olchani eng'eni app eno ne aji enkop esimu eno oleng'ata sidai."

        ),

        SupportTruth(

            "recovery_time",

            "Enkop olchani ne aji oleng'ata",

            "Olchani, olchani, olchani, olchani, kata meji engeti ne aji oleng'ata olchani olaita. Enkop olchani meji eng'eti, na meji sidai oltungani wote eng'eni."

        ),

        SupportTruth(

            "professional_help",

            "Ne aji aitafuta olchani na enkop olaiteng'en",

            "Olaiteng'en olchani na olaiteng'en GBV ne aji oleng'ata enye. App eno ne aji aonyesha enkop. Meji aichukua enkop olaiteng'en oleng'ata."

        )

    )



    private val messagesMaa = listOf(

        SupportMessage("m1", "Kejap sidai enkop enye oleng'ata. Olchani melo aikunyimi enkop enye enkop olfamilia kata olchani."),

        SupportMessage("m2", "Enkop enye kejap sidai. Melo nchoo aji 'olchani' eng'eni sidai."),

        SupportMessage("m3", "Olng'obani oleng'ata sidai ne aji enkop oleng'ata. Melo nchoo aeleze eng'eni oleng'ata."),

        SupportMessage("m4", "Eng'eti, olchani, enkolong, olchani, kata oleng'ata ne aji enkop olchani — oleng'ata enkop enye."),

        SupportMessage("m5", "Kejap sidai enkop enye, o ncho enkop enye eng'eni.")

    )



    private val stepsMaa = listOf(

        SupportNextStep(

            "danger",

            "Ne olchani sidai",

            "Aitubul enkop sidai ne aji. Aing'oru Minto oldeke, aisho 999 kata 112, kata aitum olaiteng'en. Melo nchoo aeleze eng'eni oleng'ata."

        ),

        SupportNextStep(

            "breathe",

            "Ne enkigwanak sidai kewa olchani",

            "Tej enkop ne aji aitoka sidai. Kunywa enkop ne aji. Ne aji aing'oru eng'eni e neng'eni. Meji olchani aji 'meji sidai'."

        ),

        SupportNextStep(

            "tell",

            "Ne aji aitum olaiteng'en",

            "Ichoo oltungani olaiteng'en melo aikulaumu. Aing'oru Olaiteng'en ne aibuk, kata aisho ne enkigwanak sidai."

        ),

        SupportNextStep(

            "medical",

            "Ne nchoo olchani",

            "Aing'oru hospital kata olchani na aom minto GBV kata enkop olchani. Aing'oru na olaiteng'en ne enkigwanak sidai."

        ),

        SupportNextStep(

            "child",

            "Nchorro olchani",

            "Nchorro olchani, FGM, kata enkaiya enkaina nchoo enkop oleng'ata. Aisho Childline 116, kata aitum Minto oldeke."

        ),

        SupportNextStep(

            "counsel",

            "Ne aji olng'obani na olaiteng'en",

            "Aisho esimu ya GBV 1195 oleng'ata na aom minto olchani. Enkop oleng'ata eno naa enkop oleng'ata, meji enkop olchani."

        ),

        SupportNextStep(

            "report",

            "Ne aji aitito kata aripoti oleng'ata",

            "Aitum Ripoti GBV/FGM app eno ne sidai. Enkop ne aibuk enkop esimu eno oleng'ata. Ne aji eng'eti oleng'ata enkigwanak sidai."

        )

    )

}


