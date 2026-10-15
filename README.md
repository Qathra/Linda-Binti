# Linda Binti

AI-assisted GBV/FGM prevention and response for Kajiado County — hybrid online/offline Android app with a Ktor backend and NGO web dashboard.

**Stack:** Kotlin + Jetpack Compose (Android) · Ktor/Kotlin backend · SQLite · static NGO dashboard  
**Launcher disguise:** the phone app appears as **Calculator**.  
**Current app version:** see `app/build.gradle.kts` (`versionName`).

---

## Proposal alignment

See **[docs/PROPOSAL_ALIGNMENT.md](docs/PROPOSAL_ALIGNMENT.md)** for how this service maps to the BSIT proposal abstract, aim, and objectives.  
UAT form for Objective 4: **[docs/UAT_QUESTIONNAIRE.md](docs/UAT_QUESTIONNAIRE.md)**.  
Production go-live: [docs/PRODUCTION_GO_LIVE.md](docs/PRODUCTION_GO_LIVE.md) · [docs/PRODUCTION_STATUS.md](docs/PRODUCTION_STATUS.md) · [docs/USSD_PATHWAY.md](docs/USSD_PATHWAY.md).  
Add partner videos via **[docs/HOW_TO_ADD_VIDEOS.md](docs/HOW_TO_ADD_VIDEOS.md)**.

---

| Area | Behaviour |
|------|-----------|
| **Calculator unlock** | Real maths; Access PIN opens the safe area; SOS code triggers SMS to trusted contacts |
| **SOS** | Works without internet (cellular SMS + optional location) |
| **Reporting** | Offline-first (Room); syncs when online + consent + cloud session |
| **Education** | Bundled lessons + streamed/downloadable CMS videos from the backend |
| **AI assistant** | Local FAQ always; optional cloud LLM with RAG + safety filters when consented |
| **NGO dashboard** | Map, cases, AI signals, education CMS, audit log |

PIN, SOS codes, and trusted-contact numbers stay on the device. Cloud features never receive them.

---

## Project layout

```
app/                 Android app (Compose)
backend/             Ktor API + dashboard static files
  src/main/resources/dashboard/   NGO web UI
```

---

## Prerequisites

- JDK 17+ (backend toolchain)
- Android Studio / Android SDK (app: compileSdk 36, minSdk 24)
- Optional: OpenAI-compatible API key for live online AI

---

## Backend (local)

```powershell
cd backend
copy .env.example .env
# Edit .env — add LINDA_AI_API_KEY only if you want live online AI

cd ..
.\gradlew :backend:run
```

- Health: `http://127.0.0.1:8080/v1/health`
- NGO dashboard: `http://127.0.0.1:8080/dashboard/`

### Seeded demo accounts (local)

| Role | Email | Password |
|------|-------|----------|
| Survivor (USER) | `survivor@linda.local` | `LindaBinti1!` |
| NGO admin | `admin@linda.local` | See note below |

**Admin password:** if no `LINDA_SEED_ADMIN_PASSWORD` is set, a random placeholder is created. Reset for local dev:

```powershell
.\gradlew :backend:devResetAdmin
```

(Console prints a one-time password; you may be asked to change it on first login.)

For tests, the harness seeds `admin@linda.local` / `ChangeMeAdmin1!`.

### Useful env vars

See `backend/.env.example`. Never commit `backend/.env` or real API keys.

| Variable | Purpose |
|----------|---------|
| `LINDA_AI_ENABLED` / `LINDA_AI_API_KEY` | Online LLM |
| `LINDA_HOST` / `LINDA_PORT` | Bind (default `0.0.0.0:8080`) |
| `LINDA_DEMO_SEED` | Panel map demo cases (`1` force / `0` off) |
| `LINDA_SEED_ADMIN_PASSWORD` | Initial NGO admin password |

---

## Android app

1. Point debug backend URL at your machine in `app/build.gradle.kts`  
   (`debug` → `BACKEND_BASE_URL`).  
   - Physical device on LAN: `http://YOUR_PC_IP:8080/`  
   - Emulator: often `http://10.0.2.2:8080/`
2. Build & install:

```powershell
.\gradlew :app:assembleDebug
.\gradlew :app:installDebug
```

**Release** builds use HTTPS only (`https://lindabinti.com/` by default) and do not allow cleartext LAN.

### First-run on phone

1. Open **Calculator** → complete setup (Access PIN ≠ SOS code, trusted contacts, permissions).
2. Optional: Settings → cloud consent → sign in as `survivor@linda.local`.
3. Demo SOS carefully (real SMS to contacts you added).

---

## Tests

```powershell
.\gradlew :backend:test
.\gradlew :app:testDebugUnitTest
```

Optional live AI check (needs key in env):

```powershell
.\gradlew :backend:test -DLINDA_AI_LIVE_TEST=1
```

---

## Demo / viva

Follow **[docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md)** for a timed walkthrough (disguise → SOS → report → AI → education → NGO dashboard).

---

## Honest AI claims

- **Offline:** rule/FAQ and local risk heuristics — always available.
- **Online:** optional LLM via backend when `LINDA_AI_*` is configured, user consents, and session is valid. Rate limits fall back to rules AI.
- Do not claim “full end-to-end ML” without a configured provider and a live path.

---

## Security notes (student build)

- Refresh tokens are stored server-side and revoked on logout / password change / rotation.
- Auth endpoints are rate-limited.
- Release network config is HTTPS-only.
- NGO **Audit** panel lists staff actions without survivor free-text bodies.

---

## License / academic use

Student project build for Linda Binti (Kajiado GBV/FGM prevention & response). Replace DEMO education videos with NGO-approved media before any field deployment.
