# CHAPTER FOUR: REQUIREMENT GATHERING AND ANALYSIS

**Student:** Qathra Hamdi Ahmed · BSIT/2024/52344 · Umma University  
**Project:** Linda Binti: An AI-powered application for the prevention and response to GBV and FGM in Kajiado County  

*Copy this chapter into your Word report. Replace bracketed APA citations with the same sources you use in Chapter Two. Do not invent interview names or questionnaire percentages. If you later collect UAT scores, put those numbers in the testing chapter (system validation), not as fake “already collected” figures here.*

---

## 4.1 Introduction

This chapter presents how requirements for Linda Binti were gathered and analysed before (and during) design and development. The purpose of requirement gathering was to turn the research problem — barriers to discreet GBV and FGM reporting and response in Kajiado County — into a clear list of what the system must do and how it must behave.

The proposal adopted an **Agile** approach: needs were identified from the problem domain, existing digital tools, Kenyan law and statistics, and stakeholder-oriented instruments (questionnaires and interviews). Those needs were then analysed into **functional requirements** (features) and **non-functional requirements** (quality attributes such as privacy, offline use, and discretion). Chapter Five (Design) and Chapter Six (System Development) implement the requirements listed in Section 4.4.

The chapter is organised as follows: Section 4.2 describes data collection methods; Section 4.3 presents analysis of the data and the needs that emerged; Section 4.4 specifies functional and non-functional requirements; Section 4.5 summarises the chapter.

---

## 4.2 Data Collection Methods

Mixed methods were used so that both **user-facing needs** (discretion, SOS, reporting, language) and **organisation-facing needs** (NGO visibility of cases in Kajiado) could be captured. Primary instruments were questionnaires and interviews, as stated in the research proposal. These were supported by **document review** of Kenyan law, demographic surveys, and existing GBV/FGM digital platforms. This combination is suitable for a software project because it links lived barriers (stigma, connectivity, fear of the perpetrator seeing the phone) to technical decisions.

All human data collection for this academic project is anonymised. No survivor is required to give a real name, phone number, or incident narrative for the study. The application itself stores trusted-contact numbers and PINs only on the user’s device.

### 4.2.1 Questionnaires

A structured questionnaire was designed to capture perceptions of GBV/FGM reporting barriers and of the proposed Linda Binti features. The instrument used a five-point Likert scale (1 = strongly disagree, 5 = strongly agree) plus short open questions.

**Target respondents (intended / actual as available):** potential users (women and girls at risk, or community members in Kajiado), and NGO/staff or gender-desk officers who would use a case map.

**Themes covered by the questionnaire:**

1. Whether a **calculator-style disguise** would make a safety app feel safer to open.  
2. Whether a secret **SOS code** to trusted contacts (with location when available) is useful in an emergency.  
3. Whether **confidential, on-phone reporting** of GBV/FGM is understandable.  
4. Whether **AI guidance** on GBV/FGM/Kajiado safety is useful.  
5. Whether **education and support** information is easy to find.  
6. For NGO respondents: whether a **web dashboard / hotspot map** would help them see case volume by area.  
7. Overall whether such a system could improve reporting and response in Kajiado.

The same family of questions is used for **user acceptance testing** after development (see the project UAT form). Requirements-stage answers tell the developer *what to build*; UAT-stage answers tell the examiner *whether the built system is acceptable*. Questionnaire data are analysed descriptively (means and comments), not as clinical research on survivors.

### 4.2.2 Interviews

Semi-structured interviews were selected to allow probing of issues that a tick-box cannot capture — for example, why a woman would not install an app named “GBV”, what happens if there is no internet, and whether SOS should go to police automatically or to people she trusts.

**Interview guide (core questions):**

- What stops people in Kajiado from reporting GBV or FGM?  
- If the phone is checked by a partner or relative, what would still feel safe to open?  
- In an emergency, who should be alerted first, and should the message include location?  
- What should happen when there is no mobile data?  
- What should NGO staff see, and what must they *not* see (PIN, SOS code, private story)?  
- Which languages matter (English, Kiswahili, Maa)?

Interviews were treated as **requirement elicitation**, not as counselling. Respondents may stop at any time. Findings were coded into themes (Section 4.3.1) and then into shall-statements (Section 4.4).

**Document review (supporting method).** In addition to questionnaires and interviews, secondary sources were reviewed so that requirements would not contradict Kenyan law or known Kajiado context: the Prohibition of Female Genital Mutilation Act (2011), the Protection Against Domestic Violence Act (2015), the Sexual Offences Act (2006), KDHS findings on FGM and intimate-partner violence, and Kajiado’s county Anti-FGM policy direction (Kajiado County Government, 2019; Kenya National Bureau of Statistics [KNBS], 2015, 2022). Existing apps such as SafePal and related GBV reporting tools were reviewed to identify gaps (integration of disguise, SOS SMS without internet, offline reports, and an NGO map in one product).

---

## 4.3 Data Analysis

Data were analysed qualitatively (thematic coding of interview notes and open questionnaire comments) and descriptively (Likert items). Secondary statistics were used only to **justify** requirements (for example, high FGM burden in Kajiado), not to claim that Linda Binti has already reduced prevalence.

### 4.3.1 Thematic analysis of challenges (user needs)

The following themes were identified. Each theme is paired with the system response that became a requirement.

**Theme 1: Stigma and discovery risk.**  
Survivors and at-risk users fear that a clearly named GBV app on the home screen will expose them.  
*Implication:* The app must look like an ordinary **Calculator**. A one-tap **Hide** control must return to the calculator. The launcher name must not be “Linda Binti”.

**Theme 2: Emergency help must work without Wi-Fi.**  
Kajiado includes pastoral and low-connectivity settings. A cloud-only “panic button” would fail when data is off.  
*Implication:* SOS must send a **normal GSM SMS** to trusted contacts. Location is included when the phone Location toggle is on and a fix is available. The NGO server is optional for SOS.

**Theme 3: Trusted people first, not automatic police dispatch.**  
Users may not want the first alert to be a public emergency call. They want 1–5 people they chose.  
*Implication:* Trusted contacts (minimum 1, maximum 5). SMS is queued a short interval apart so the radio does not reject a burst. Verified hotlines 999, 112, 1195 and 116 remain in Emergency Assistance. The system **complements** police and hospitals; it does not replace them.

**Theme 4: Confidential reporting on the device.**  
Reports must not disappear if the internet is down, and must not be visible to other phone users as a public feed.  
*Implication:* Reports are stored **locally first** (Room SQLite). Each phone shows only that user’s reports. Optional cloud sync requires consent and a USER login.

**Theme 5: Knowledge on GBV, FGM and Kajiado.**  
Users ask for explanations (what FGM is, law, stigma, where to get help in the county), not only a blank chat box.  
*Implication:* Education modules; Survivor Support; an AI assistant with **on-device knowledge** plus optional online LLM. The assistant must not invent extra phone numbers. Only 999, 112, 1195 and 116 are treated as verified.

**Theme 6: NGO monitoring without reading private SOS secrets.**  
Staff need a map of case volume by Kajiado area. They must not receive Access PIN, SOS code, or trusted-contact lists.  
*Implication:* Ktor backend + SQLite + web dashboard (Leaflet). Role-based login (USER vs NGO). Audit log of staff actions.

**Theme 7: Dual secret codes.**  
One code must open the safe app; a different code must trigger SOS from the calculator so a bystander sees only maths.  
*Implication:* Access PIN ≠ SOS code. Equals key evaluates maths unless the secret matches.

**Theme 8: Language and literacy.**  
English, Kiswahili and Maa matter in Kajiado.  
*Implication:* Interface strings in those languages where provided; AI and education available in the selected language.

### 4.3.2 Analysis of existing digital solutions (gap analysis)

Review of existing GBV-related digital tools showed useful ideas (anonymous reporting, helpline lists) but typical gaps for Kajiado:

| Gap in many existing tools | Linda Binti requirement |
|---------------------------|-------------------------|
| App icon/name reveals GBV | Calculator disguise + Hide |
| Panic features need internet | GSM SOS SMS |
| No location in the first SMS | GPS when Location is on |
| Reports only in the cloud | Offline-first Room storage |
| No NGO map for Kajiado sub-counties | Dashboard hotspots |
| AI with no local fallback | Hybrid AI (offline pack + optional cloud) |
| English-only | EN / SW / Maa |

This gap analysis directly produced the functional list in Section 4.4.1.

### 4.4.1 Mapping needs to system modules

Table 4.1 summarises how analysed needs map to Linda Binti modules (the specification that Design must follow).

**Table 4.1**  
*Needs–module matrix for Linda Binti*

| Analysed need | Module / component |
|---------------|-------------------|
| Discreet access | Calculator, Access PIN, Hide |
| Immediate alert | SOS code, Trusted Contacts (1–5), SMS + maps link |
| Hotlines | Emergency Assistance (999, 112, 1195, 116) |
| Private case note | Reporting (GBV and FGM categories) |
| Learning | Education (Understanding GBV, Understanding FGM, related topics) |
| Guidance | AI Safety Assistant (offline knowledge + optional online AI) |
| Emotional / service info | Survivor Support |
| Situational awareness | Risk Assessment, Safety Compass |
| NGO view | Web dashboard, Kajiado map, audit |
| Settings / consent | Settings, cloud USER login, cloud-AI consent |

---

## 4.4 System Requirements Specification

Requirements below are written as **shall** statements. They are the contract for Chapter Five (Design) and Chapter Six (Implementation). They match the system that was subsequently built in **Kotlin and Jetpack Compose** (Android) with a **Ktor** API and **SQLite** (Agile change from the proposal’s Flutter/Firebase table, justified by SMS control, calculator disguise, and offline reporting).

### 4.4.1 Functional requirements

**FR-01 Disguise.** The installed application shall appear on the device as **Calculator** and shall perform ordinary arithmetic when the Access PIN or SOS code is not entered.

**FR-02 Access control.** The user shall set an Access PIN during setup. Entering that PIN and Equals shall open the protected Linda Binti home. The PIN shall never be sent to the server.

**FR-03 SOS.** The user shall set an SOS code different from the Access PIN. Entering that code from the calculator (or using Emergency SOS in-app) shall send an SMS emergency alert to all saved trusted contacts.

**FR-04 Trusted contacts.** The system shall allow between **one and five** trusted contacts. Duplicate numbers shall be rejected. Contact numbers shall remain on the device.

**FR-05 SOS message content.** The SMS shall identify an emergency, request assistance, and include time. When location is available it shall include a maps link and coordinates; otherwise it shall indicate that location is pending. The SMS shall not contain the Access PIN, SOS code, or private report text.

**FR-06 Staggered send.** When several contacts exist, messages shall be sent in sequence with a short delay so the mobile radio is less likely to reject a burst.

**FR-07 Emergency assistance.** The app shall display verified national numbers: police **999** and **112**, GBV helpline **1195**, Childline **116**, and shall not present the assistant as a replacement for those services.

**FR-08 Reporting.** The user shall be able to file a confidential GBV or FGM-related report (categories such as physical, sexual, emotional, domestic, harassment, threats, FGM threat/incident/pressure, forced marriage, other GBV/FGM-related). Reports shall save on the phone first.

**FR-09 Optional sync.** Report upload to the NGO backend shall occur only when the user is online, has consented, and has a USER cloud session. Other users’ phones shall not see that report list.

**FR-10 Education.** The education hub shall present topics starting with Understanding GBV and Understanding FGM, plus related safety topics. Content shall stay within GBV/FGM scope.

**FR-11 AI assistant.** The assistant shall answer questions on GBV, FGM, rights, stigma, and Kajiado help using on-device knowledge at all times, and may call a cloud model when the server, network, USER login, and cloud-AI consent are present. It shall not reveal PIN/SOS secrets or invent extra emergency numbers.

**FR-12 NGO dashboard.** Authorised NGO staff shall log in on the web, view a Kajiado case-volume map, cases, education CMS, and an audit trail. Survivor PIN/SOS/contacts shall not appear there.

**FR-13 Registration.** Cloud USER registration/login shall exist for sync and online AI. NGO roles shall be separate from survivor USER accounts.

**FR-14 Languages.** The user shall be able to select English, Kiswahili, or Maa for interface copy where translations exist.

**FR-15 Hide.** From protected screens, Hide shall return immediately to the calculator.

**FR-16 USSD pathway (scope).** The backend shall expose a USSD simulation/API for feature-phone style flags. A live Safaricom shortcode is **out of scope** until a partner assigns one.

### 4.4.2 Non-functional requirements

**NFR-01 Privacy.** Access PIN, SOS code, and trusted contacts shall remain on-device (encrypted preferences / local database). Cloud payloads shall be minimised.

**NFR-02 Offline behaviour.** SOS SMS and local reports shall work without internet. Education text and offline AI FAQ shall work without internet.

**NFR-03 Security.** Passwords on the server shall be hashed. NGO routes shall be role-gated. Debug/demo accounts shall not be treated as a production county system.

**NFR-04 Usability.** Calculator interaction shall stay familiar (C, digits, Equals). Protected home shall use clear modules.

**NFR-05 Performance.** SOS shall attempt to attach location quickly (last-known or a short wait) so the first SMS is not delayed for a long GPS hunt.

**NFR-06 Reliability.** SMS send failure shall not be reported as success. Network error on send shall be treated as failed, not “submitted”.

**NFR-07 Ethics / delimitation.** The product shall address GBV and FGM only, for a Kajiado-oriented prototype, and shall state that it complements counselling, clinical care, police, and courts.

**NFR-08 Scalability (honest).** The NGO database is SQLite suitable for a **pilot**, not thousands of simultaneous dashboard users.

**NFR-09 Compatibility.** Android minSdk 24; SOS depends on `SEND_SMS` and, for coordinates, location permission plus the system Location toggle.

---

## 4.5 Summary

This chapter showed how Linda Binti requirements were gathered (questionnaires, interviews, and supporting document review) and analysed into themes: stigma, offline emergencies, trusted-contact SMS, on-device reporting, GBV/FGM knowledge, and NGO monitoring without secret leakage.

The analysis produced a requirements specification (FR-01 to FR-16 and NFR-01 to NFR-09) that the implemented system follows: a Calculator-disguised Android app, GSM SOS with optional GPS, local-first reports, hybrid AI, education and support, and a Ktor/SQLite NGO dashboard.

Chapter Five uses these requirements to present front-end and back-end design. Chapter Six describes how the system was developed against the same list. Validation with respondents (UAT questionnaire) remains part of testing the developed system and should be reported with real scores when collected.

---

### References (expand in your main reference list — APA)

Kenya National Bureau of Statistics. (2015). *Kenya demographic and health survey 2014*.  
Kenya National Bureau of Statistics. (2023). *Kenya demographic and health survey 2022*.  
Republic of Kenya. (2006). *Sexual Offences Act, No. 3 of 2006*.  
Republic of Kenya. (2010). *The Constitution of Kenya*.  
Republic of Kenya. (2011). *Prohibition of Female Genital Mutilation Act, No. 32 of 2011*.  
Republic of Kenya. (2015). *Protection Against Domestic Violence Act, No. 2 of 2015*.  
Kajiado County Government. (2019). *Kajiado County anti-FGM policy* [as cited from county / partner publications].  

*Add the exact SafePal / Pasha / UNFPA / Equality Now sources you used in Chapter Two.*
