# Linda Binti — Production go-live

Public API / dashboard: **https://lindabinti.com**  
Release Android app uses that URL automatically (`app/build.gradle.kts` release `BACKEND_BASE_URL`).

## Before you start

1. Own DNS for `lindabinti.com` (or set `linda.production.api.url` in `gradle.properties`).
2. Host account: **Render** (blueprint [render.yaml](../render.yaml)) or any Docker VPS.
3. Secrets ready (never commit):
   - `LINDA_JWT_SECRET` (≥ 32 chars)
   - `LINDA_USSD_GATEWAY_SECRET` (Africa’s Talking callback auth)
   - `LINDA_AI_API_KEY` (optional online AI; offline AI always works)
4. Set `LINDA_DEMO_SEED=0` and `LINDA_ENV=production` (defaults in Docker/Render).
5. Rotate any AI key that was ever pasted into chat or committed by mistake.

## Deploy (Render)

1. Push this repository to GitHub.
2. Render → New → Blueprint → select repo (`render.yaml`).
3. In Environment, set `LINDA_AI_API_KEY` and `LINDA_USSD_GATEWAY_SECRET`.
4. Custom Domains → `lindabinti.com` and `www.lindabinti.com`.
5. Wait for deploy; open `https://lindabinti.com/v1/health`.

## Deploy (Docker VPS)

```bash
docker build -f deploy/Dockerfile -t lindabinti .
docker run -d --name lindabinti -p 8080:8080 -v lindabinti-data:/var/data \
  -e LINDA_PUBLIC_URL=https://lindabinti.com \
  -e LINDA_ENV=production \
  -e LINDA_DEMO_SEED=0 \
  -e LINDA_JWT_SECRET='…' \
  -e LINDA_USSD_GATEWAY_SECRET='…' \
  -e LINDA_AI_API_KEY='…' \
  lindabinti
```

Terminate TLS with Caddy/Nginx on :443 → proxy to :8080.

## Health checks

| URL | Expect |
|-----|--------|
| `https://lindabinti.com/v1/health` | `"status":"ok"`; after USSD secret: `"ussd":"gateway_key_configured"` |
| `https://lindabinti.com/dashboard/` | NGO login page |

Local script (from repo root):

```powershell
.\backend\scripts\go-live-lindabinti.ps1
```

## Africa’s Talking USSD

See [USSD_PATHWAY.md](USSD_PATHWAY.md). Callback:

`https://lindabinti.com/v1/ussd/africas-talking?key=<LINDA_USSD_GATEWAY_SECRET>`

## Android release

```powershell
.\gradlew.bat :app:assembleRelease
```

Install `app\build\outputs\apk\release\app-release.apk`.  
Log in under Settings (cloud USER) for report/SOS NGO sync.  
Keep phone **Location** ON for SOS coordinates.

## Admin

First deploy seeds `admin@lindabinti.com`. Reset password only with controlled SSH/DB access.

## Ethics

Linda Binti **complements** police, health, legal, and counselling services. It does **not** replace them. SOS SMS goes to trusted contacts (+ optional escalation number). USSD queues anonymized flags for NGO staff.
