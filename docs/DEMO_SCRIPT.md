# Linda Binti — demo / viva script (~12–15 min)

Use this for a defense or stakeholder walkthrough. Keep the phone’s **Access PIN** and **SOS code** ready (you chose them at setup). Prefer a **second phone** as a trusted contact so SOS SMS is visible without surprising real emergency contacts.

**Prep (before the room fills)**

1. Backend running: `.\gradlew :backend:run` — health OK on your PC.
2. Debug app installed; `BACKEND_BASE_URL` matches your PC IP.
3. Optional: OpenAI key in `backend/.env` so the assistant can show ONLINE AI.
4. Browser open: `http://127.0.0.1:8080/dashboard/` (do not log in yet).
5. Demo survivor login ready: `survivor@linda.local` / `LindaBinti1!`
6. Admin: `admin@linda.local` (password from `devResetAdmin` or your seed).

---

## 1. Problem & stack (1 min)

> “Linda Binti supports GBV/FGM prevention and response in Kajiado with a **Calculator disguise**, **offline-first** safety tools, and **optional cloud** sync and AI. Stack: Kotlin Compose + Ktor + NGO dashboard — not Flutter.”

Point at README architecture table if needed.

---

## 2. Disguise & unlock (1–2 min)

1. Show home screen: app name is **Calculator**.
2. Open app → do a normal sum → Equals (ordinary calculator).
3. Enter **Access PIN** → Equals → enter **safe home**.
4. Tap **Hide** → back to calculator instantly.

**Say:** PIN never leaves the device; Hide protects against shoulder surfing / recent apps.

---

## 3. SOS path (2 min) — careful

1. From calculator enter **SOS code** → Equals (or use Emergency inside the app).
2. Show discreet notification title **Calculator** / “Working…”.
3. Confirm SMS reached the demo contact (with or without location link).

**Say:** SOS uses **cellular SMS**, not the Wi‑Fi “Online” chip. It works when the backend is down.

---

## 4. Connectivity & offline report (2 min)

1. On Home, show **Online / Offline** chip.
2. Turn airplane mode **on** (or disable Wi‑Fi+data).
3. File a short confidential/anonymous report → saved locally (`pending`).
4. Airplane **off** → reports sync if Settings → report sync is on (default on; user can turn it off).

**Say:** Queue + backoff; sync waits for a valid session; no report body in notifications.

---

## 5. Hybrid AI assistant (2–3 min)

1. Open Assistant; point at privacy disclaimer.
2. Ask a calm FAQ (e.g. FGM law / where to get help) — offline or online answer.
3. If ONLINE AI configured: ask once; note chip stays session-aware (not flapping per message).
4. Optionally type an urgent phrase (“I am in danger right now”) → CRITICAL / emergency guidance with hotlines — **no fake crisis roleplay with the audience**.

**Say:** Offline FAQ always works; cloud LLM is optional; RAG + safety filter strip PIN-like digits; rate limits fall back to rules AI.

---

## 6. Education videos (1–2 min)

1. Education hub → **video library** (DEMO placeholders).
2. Play one stream; mention download for offline.
3. (Optional) On dashboard later: Education CMS edit — proves NGO-managed content.

---

## 7. NGO dashboard (3 min)

1. Log in as NGO admin.
2. Map / cases / AI summary panel.
3. **Education** button → CMS list (DEMO videos).
4. **Audit** button → recent staff actions (login, status, etc.) — no survivor free text.

**Say:** Staff tools are role-gated; audit supports accountability without exposing case narratives in the log UI.

---

## 8. Security highlights (1 min)

- Refresh tokens revoked on logout / password change.
- Auth rate limiting.
- Release builds: HTTPS only.
- Settings wipe clears local sensitive data (PIN-gated).

---

## 9. Close (30 s)

> “Student build: replace DEMO media, harden hosting, and keep claiming only what is configured — offline rules always; online LLM when the backend key and user consent are present.”

Invite questions. If asked for code paths: `SyncCoordinator`, `AssistantRiskDetector` / `AssistantSafetyFilter`, `AuthService` + `RefreshTokenStore`.

---

## Backup if live AI fails

- Show Offline AI / rules answer still responding.
- Show `GET /v1/health` and AI status label in health/capabilities.
- Run unit tests slide: `AuthSecurityHardeningTest`, `AssistantRagSafetyTest`, sync planner tests.
