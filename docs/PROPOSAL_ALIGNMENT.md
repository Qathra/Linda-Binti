# Linda Binti — Proposal Abstract & Objectives vs the App

Student: **Qathra Hamdi Ahmed** · BSIT/2024/52344 · Umma University  
Title: *Linda Binti: An AI-powered application for the prevention and response to GBV and FGM in Kajiado County*

Use this file in the **final report / viva**. Official proposal wording is quoted below, then mapped to what was built.

---

## Overall verdict

**Yes — the app aligns with the Aim, Abstract product claims, and Scope.**  
The four specific objectives are **research steps**: 1–2 live in Chapters 1–2; **3 = this system**; **4 needs your UAT questionnaire results** (template: [UAT_QUESTIONNAIRE.md](UAT_QUESTIONNAIRE.md)).

Do **not** claim Flutter/Firebase, live Safaricom USSD, or that the app replaces 999.

---

## Official Abstract (proposal)

> Gender-Based Violence (GBV) and Female Genital Mutilation (FGM) remain major challenges in Kenya, particularly in Kajiado County, where cultural practices, fear of stigma, and limited access to timely support continue to hinder reporting and intervention. Although several initiatives have been introduced to address these issues, many survivors still face barriers in accessing emergency assistance, reporting incidents confidentially, and obtaining support services. This study proposes the development of Linda Binti, a secure mobile application designed to provide a discreet and accessible platform for reporting GBV and FGM incidents, sending emergency alerts with location information to trusted contacts, and connecting users with relevant support services.
>
> The study aims to design and develop the Linda Binti application and evaluate its potential in improving the reporting and response process for GBV and FGM cases in Kajiado County. The research will adopt an Agile development methodology to facilitate iterative system design and continuous user feedback. Data will be collected through questionnaires and interviews involving potential users and relevant stakeholders to identify system requirements and assess user needs.
>
> The expected outcome is a functional mobile application that enhances personal safety, promotes confidential reporting, and improves access to timely support for individuals at risk of or affected by GBV and FGM. The findings of this study are expected to contribute to technology-based interventions that support the prevention of GBV and FGM while complementing existing efforts by government agencies, healthcare providers, and non-governmental organizations in protecting vulnerable individuals.

### Abstract → app (what examiners care about)

| Abstract promise | App status | Where |
|------------------|------------|--------|
| Discreet / stigma-aware access | **Done** | Calculator disguise + Hide |
| Report GBV/FGM confidentially | **Done** | Reporting (offline-first) + sync |
| Emergency alerts + location to trusted contacts | **Done** | SOS code → SMS + GPS when Location ON |
| Connect to support services | **Done** | Survivor Support + hotlines 999/112/1195/116 |
| Agile + iterative | **Done** | Phased build (Kotlin/Ktor) |
| Questionnaires / interviews | **Your research step** | [UAT_QUESTIONNAIRE.md](UAT_QUESTIONNAIRE.md) |
| Complements gov/NGO/health — does not replace | **Done** | About + Emergency + delimitations |

---

## Official Aim (§1.3.1)

> To develop Linda Binti, an AI-powered mobile and web-based platform for the prevention and response to Gender-Based Violence and Female Genital Mutilation in Kajiado County.

| Aim phrase | App |
|------------|-----|
| AI-powered | Offline FAQ + optional online LLM (hybrid) |
| Mobile | Android app (Calculator → Linda Binti) |
| Web-based | NGO dashboard (map, cases, education CMS) |
| GBV + FGM | Reporting categories + AI knowledge scoped |
| Kajiado | Sub-county areas + county map |

**Viva line:** *The aim is met: a hybrid AI mobile app plus NGO web dashboard for GBV/FGM prevention and response in Kajiado.*

---

## Official Specific Objectives (§1.3.2)

| # | Objective | Alignment | What to show / write |
|---|-----------|-----------|----------------------|
| 1 | Identify challenges affecting GBV/FGM reporting and response in Kajiado | **Research (Ch. 1–2)** | Literature + questionnaires; app **addresses** stigma via disguise/SOS/offline report |
| 2 | Review existing digital solutions | **Research (Ch. 2)** | SafePal / Pasha contrast; Linda Binti integrates SOS + AI + reporting + NGO map |
| 3 | Design and develop the Linda Binti system | **Fully met by this codebase** | Demo: calculator → SOS → report → AI → education → dashboard hotspots |
| 4 | Validate the developed system | **Partial until UAT** | Unit tests + device checklist + **your** questionnaire results |

---

## Official Scope (§1.6.1) — feature checklist

| Scope requirement | Status | Module |
|-------------------|--------|--------|
| User registration | **Done** | Cloud USER / NGO admin login |
| Manage emergency contacts | **Done** | Trusted Contacts (1–5) |
| Discreet SOS with secret code | **Done** | User-chosen SOS code + Equals (must differ from Access PIN) |
| AI chatbot guidance | **Done** | AI Safety Assistant |
| Secure GBV/FGM reporting | **Done** | Reporting + sync |
| GPS to trusted contacts on SOS | **Done** | Location on SMS when available |
| NGO web dashboard | **Done** | Cases, trends, **area hotspots by case count** |
| AI-assisted analysis / high-risk areas | **Done** | Priority flags + Kajiado sub-county volume hotspots |
| Prototype for Kajiado (not full county gov rollout) | **Matches delimitations** | Partner hosting / USSD shortcode still ops |

**Extras beyond proposal (strengthens discretion):** Calculator disguise, Quick Hide, Risk Assessment, Safety Compass, Education hub, USSD API (partner shortcode).

---

## Technology note (must say in viva)

Proposal Table 3.1 listed **Flutter + Firebase + Google Maps + OpenAI**.

**Delivered:** Kotlin + Jetpack Compose · Ktor + SQLite · Leaflet map · optional OpenAI-compatible API · HTML/CSS/JS dashboard.

> Agile iteration chose native Android + first-party API so SOS SMS and offline reporting work without internet, and so the calculator disguise stays credible.

---

## What is still *your* work (not missing APK features)

1. Finish **Objective 1–2** write-up in Chapters 1–2.  
2. Run **Objective 4 UAT** with [UAT_QUESTIONNAIRE.md](UAT_QUESTIONNAIRE.md) and put results in Chapter 4/5.  
3. Optional go-live: register domain, host backend, Africa’s Talking shortcode ([PRODUCTION_GO_LIVE.md](PRODUCTION_GO_LIVE.md)).

---

## Delimitations (still true)

See [DELIMITATIONS.md](DELIMITATIONS.md): GBV/FGM only; complements (does not replace) police/clinical/legal/counselling; GPS/cloud need device capability; prototype / partner deployment — not automatic county-wide government system.

---

## Panel one-liners

- **Does the app match the abstract?** Yes — discreet reporting, SOS with location, support links, NGO-facing monitoring.  
- **Does it match the aim?** Yes — AI-assisted mobile + web platform for GBV/FGM in Kajiado.  
- **Objective 3?** Fulfilled by the working system.  
- **Objective 4?** Tests + demo done; effectiveness claims need your UAT numbers.  
- **Flutter?** No — Kotlin/Ktor for offline SOS (say Agile).  
