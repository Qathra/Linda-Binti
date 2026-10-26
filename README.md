# Linda Binti

**An AI-assisted mobile and web platform for the prevention of, and response to, gender-based violence (GBV) and female genital mutilation (FGM) in Kajiado County, Kenya.**

Bachelor of Science in Information Technology · Umma University  
Student: **Qathra Hamdi Ahmed** · BSIT/2024/52344

[![Version](https://img.shields.io/badge/version-3.0.1-5B2C8A)](https://github.com/Qathra/Linda-Binti/releases/latest)
[![Platform](https://img.shields.io/badge/Android-minSdk%2024-3D1A63)](https://github.com/Qathra/Linda-Binti)
[![Kotlin](https://img.shields.io/badge/Kotlin-7B4AA8)](https://github.com/Qathra/Linda-Binti)
[![License](https://img.shields.io/badge/license-MIT-6B5F78)](LICENSE)

The phone application opens as a **calculator**. A private access code opens Linda Binti. A separate SOS code sends an SMS to trusted contacts, including location when the phone can provide it. That alert does not need the internet.

Linda Binti complements the police, health workers, and counsellors. It does not replace 999, 112, 1195, or 116.

| | |
|---|---|
| **Download** | [Latest APK on GitHub Releases](https://github.com/Qathra/Linda-Binti/releases/latest) |
| **Current version** | 3.0.1 (version code 46) |
| **Repository** | [github.com/Qathra/Linda-Binti](https://github.com/Qathra/Linda-Binti) |

## Contents

- [About the project](#about-the-project)
- [Technology used](#technology-used)
- [Features](#features)
- [Screenshots](#screenshots)
- [Download the APK](#download-the-apk)
- [How to run the project](#how-to-run-the-project)
- [About the developer](#about-the-developer)
- [Project status](#project-status)

---

## About the project

Survivors in Kajiado still face stigma, fear that someone will see the phone, and places where mobile data is unreliable. Linda Binti is a discreet, offline-first response to that problem.

A person can:

- hide the app behind a working calculator
- alert trusted contacts by SMS, with location when it is available
- save a GBV or FGM report on the phone, then sync it only with consent
- read safety education and ask a safety assistant that works without the internet
- give authorised NGO staff a web map of case volume by Kajiado sub-county

The study designed and built this system with an Agile process. The research aim is an AI-powered mobile and web platform for GBV and FGM prevention and response in Kajiado County. The product is a working prototype for partners. It is not a county government dispatch system.

Further mapping of the proposal to the software is in [docs/PROPOSAL_ALIGNMENT.md](docs/PROPOSAL_ALIGNMENT.md). What the system deliberately does not do is in [docs/DELIMITATIONS.md](docs/DELIMITATIONS.md).

---

## Technology used

The proposal considered Flutter and Firebase. The delivered system uses native Android and a first-party API so that SOS messages and offline reports still work when there is no data connection.

| Layer | Technology |
|---|---|
| Android app | Kotlin, Jetpack Compose, Navigation, Room |
| Phone storage | SQLite on the device. Access PIN, SOS code, and trusted contacts stay on the phone |
| Networking | Retrofit, OkHttp |
| Backend API | Kotlin, Ktor |
| Database | SQLite |
| NGO dashboard | HTML, CSS, JavaScript, Leaflet map |
| AI | On-device safety answers at all times. Optional online model through an OpenAI-compatible API, with consent, retrieval checks, and a fallback to the on-device answers |
| Feature phones | USSD gateway adapter (Africa's Talking) for a partner shortcode |
| Minimum phone | Android 7.0 (API 24). Compiled for API 36 |
| Runtime | JDK 17 for the backend |

---

## Features

| Feature | What it does |
|---|---|
| **Calculator disguise** | The launcher looks and behaves like a calculator. Hide returns to it at any time. |
| **Access code and SOS code** | The two codes are different. One opens the safe area. The other sends the alert. |
| **SOS without internet** | SMS to one to five trusted contacts. Location is added when the phone has a fix. |
| **Confidential reporting** | GBV and FGM categories only. The report is saved on the phone first. |
| **Cloud sync** | Optional upload when the person is online, signed in, and has allowed sync. Staff do not receive the PIN, SOS code, or contact list. |
| **Education and safety** | Lessons on the phone, plus videos the NGO dashboard can publish. |
| **AI Safety Assistant** | Answers on GBV, FGM, rights, and safety. Not a diagnosis, a lawyer, or the police. |
| **AI Safety Compass** | A personal safety overview and suggested next steps. |
| **Risk assessment** | A short guided check on the device. |
| **Emergency assistance** | National hotlines 999, 112, 1195, and 116, then SOS to trusted contacts. |
| **Survivor support** | A directory of care and support services. |
| **NGO dashboard** | Sign-in, Kajiado case-volume map, education content, and an audit log. |
| **Languages** | English, Kiswahili, and Maa in the education content. |
| **USSD path** | A feature-phone menu is implemented for a partner shortcode. It is not a live public shortcode until that partner connects it. |

A timed demonstration for a panel is written in [docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md).

---

## Screenshots

The phone opens as a calculator. After the access code, Linda Binti shows the safety tools. The NGO view is a website, not a page inside the phone app.

The figures below follow the shipped screens and the words used in the app. The map counts are a **sample layout**, not Kajiado statistics.

<p align="center">
  <img src="docs/screenshots/calculator.png" width="220" alt="Calculator disguise, the screen other people see" />
  <img src="docs/screenshots/home.png" width="220" alt="Protected home with safety tools" />
  <img src="docs/screenshots/emergency.png" width="220" alt="Emergency assistance and national hotlines" />
</p>

<p align="center">
  <img src="docs/screenshots/reporting.png" width="220" alt="GBV and FGM report saved on the phone first" />
  <img src="docs/screenshots/assistant.png" width="220" alt="Safety assistant that works offline" />
</p>

<p align="center">
  <img src="docs/screenshots/dashboard.png" width="720" alt="NGO dashboard showing a sample Kajiado case-volume map" />
</p>

| Screen | What the panel is seeing |
|---|---|
| Calculator | The disguise. Real arithmetic. The access code and the SOS code are entered here. |
| Home | The nine safety tools, connection status, and Hide. |
| Emergency | Hotlines first, then SOS by SMS. |
| Reporting | Category, optional description, save on this device. |
| Safety Assistant | On-device guidance. Online AI is optional. |
| NGO dashboard | Case volume by sub-county for authorised staff. |

---

## Download the APK

Install the latest build from GitHub Releases. You do not need Android Studio for this step.

**[Download Linda Binti 3.0.1 (APK)](https://github.com/Qathra/Linda-Binti/releases/latest)**

1. Open the latest release and download `Linda-Binti-3.0.1.apk`.
2. On the phone, allow installation from your browser or the Files app if Android asks.
3. Install, then open the app. The icon and the first screen are a **calculator**.
4. Complete safe setup: choose a language and name, set an access code, set a different SOS code, add at least one trusted contact, and allow SMS.
5. To open Linda Binti later, type the access code and press equals. To alert contacts, type the SOS code and press equals. Use **Hide** if someone is looking.

On-device tools work without a server: the calculator, Hide, SOS by SMS, local reports, education stored on the phone, and the offline assistant.

Cloud sign-in, report sync, online AI, and the live NGO map need the backend running. The release build looks for `https://lindabinti.com/`. That domain is not live yet, so those online features wait until the API is hosted. See [docs/PRODUCTION_GO_LIVE.md](docs/PRODUCTION_GO_LIVE.md).

This package is for direct installation at a demonstration. It is not published on Google Play.

---

## How to run the project

Use this section if you are running the code as a developer or demonstrating the full system, including the dashboard.

### What you need

- JDK 17 or newer
- Android Studio, with Android SDK (the app compiles against API 36)
- Windows PowerShell, or any terminal that can run `gradlew`
- Optional: an OpenAI-compatible API key if you want the live online assistant

### 1. Start the backend

```powershell
cd backend
copy .env.example .env
cd ..
.\gradlew :backend:run
```

- Health check: `http://127.0.0.1:8080/v1/health`
- NGO dashboard: `http://127.0.0.1:8080/dashboard/`

Add `LINDA_AI_API_KEY` in `backend/.env` only if you want the live online assistant. Do not commit `.env` or a real key.

Local demo accounts, created on your own machine:

| Role | Email | Password |
|---|---|---|
| Survivor | `survivor@linda.local` | `LindaBinti1!` |
| NGO administrator | `admin@linda.local` | Set with the command below |

If you did not set `LINDA_SEED_ADMIN_PASSWORD`, reset the local administrator and read the one-time password from the console:

```powershell
.\gradlew :backend:devResetAdmin
```

These accounts are for a local demonstration only.

### 2. Run the Android app

In `app/build.gradle.kts`, the **debug** build type sets `BACKEND_BASE_URL`.

- Emulator: `http://10.0.2.2:8080/`
- A phone on the same Wi-Fi: `http://YOUR_COMPUTER_IP:8080/`

```powershell
.\gradlew :app:assembleDebug
.\gradlew :app:installDebug
```

Open the calculator, finish setup, and use SOS only with contacts you added yourself. A real SMS will be sent.

The **release** build does not allow a local `http://` address. It uses HTTPS only.

### 3. Run the tests

```powershell
.\gradlew :backend:test
.\gradlew :app:testDebugUnitTest
```

### Useful settings

| Variable | Purpose |
|---|---|
| `LINDA_AI_ENABLED` / `LINDA_AI_API_KEY` | Online assistant |
| `LINDA_HOST` / `LINDA_PORT` | Where the API listens. Default `0.0.0.0:8080` |
| `LINDA_DEMO_SEED` | `1` adds sample map cases. `0` turns that off |
| `LINDA_SEED_ADMIN_PASSWORD` | First password for the NGO administrator |

The full list is in `backend/.env.example`.

---

## About the developer

**Qathra Hamdi Ahmed**  
Bachelor of Science in Information Technology  
Umma University · registration **BSIT/2024/52344**  
Kajiado, Kenya

Linda Binti is the final-year system for the study *Linda Binti: An AI-powered application for the prevention and response to GBV and FGM in Kajiado County*.

- GitHub: [github.com/Qathra](https://github.com/Qathra)
- LinkedIn: [qathra-hamdi](https://www.linkedin.com/in/qathra-hamdi-204ab6278/)
- Project repository: [Qathra/Linda-Binti](https://github.com/Qathra/Linda-Binti)

---

## Project status

**Version 3.0.1** · version code **46** · status: **working prototype, ready to demonstrate**

| Part | Status |
|---|---|
| Android application | Built. Calculator disguise, SOS, reporting, education, and on-device AI are in the app. |
| Backend API and NGO dashboard | Built. Run locally with the steps above, or host them for a partner. |
| Release APK | Published under [Releases](https://github.com/Qathra/Linda-Binti/releases/latest) for this version. |
| Public website `lindabinti.com` | Not registered yet. Online features in the release APK stay quiet until a host is connected. |
| USSD shortcode | The menu and gateway code are in the project. A live shortcode needs the telecom partner. |
| User acceptance testing | The questionnaire is ready in [docs/UAT_QUESTIONNAIRE.md](docs/UAT_QUESTIONNAIRE.md). Results belong in the research report. |
| Field media | Replace the sample education videos with NGO-approved material before any community deployment. [docs/HOW_TO_ADD_VIDEOS.md](docs/HOW_TO_ADD_VIDEOS.md) |

### What this version claims, and what it does not

- The offline assistant is a curated safety guide on the phone. It is always available.
- The online assistant runs only when the server has a key, the person has consented, and the session is valid. If the limit is reached, the app returns to the on-device answers.
- The dashboard shows case volume and priority flags for authorised staff. It is not a police CAD system.
- The app does not provide therapy, legal advice, or clinical care.

### This repository

The public GitHub page is the record a panel can open beside this README.

| Section | What it shows |
|---|---|
| **Releases** | [Version 3.0.1](https://github.com/Qathra/Linda-Binti/releases/latest), including the installable file `Linda-Binti-3.0.1.apk` |
| **Contributors** | [Qathra Hamdi Ahmed](https://github.com/Qathra) |
| **Languages** | Kotlin for the Android app and the API. HTML, CSS, and JavaScript for the NGO dashboard |
| **License** | [MIT](LICENSE), academic project, 2026 |

Panel walkthrough: [docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md)  
Go-live checklist: [docs/PRODUCTION_STATUS.md](docs/PRODUCTION_STATUS.md)
