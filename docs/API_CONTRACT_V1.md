# Linda Binti API Contract v1

Base URL: configured per build (`BuildConfig.BACKEND_BASE_URL`).  
Transport: HTTPS in production. JSON. Bearer JWT after login.

## Auth

### POST `/v1/auth/register`
Body: `{ "email": string, "password": string, "displayName": string }`  
Creates `USER`. Never accepts Access PIN or SOS code.  
Response: `{ "accessToken", "refreshToken", "role", "userId", "expiresAtEpochMs" }`

### POST `/v1/auth/login`
Body: `{ "email", "password" }`  
Response: same as register.

### POST `/v1/auth/logout`
Bearer required. Invalidates refresh token (best-effort).

## Reports (USER only for write/read own)

### POST `/v1/reports`
Headers: `Authorization`, `Idempotency-Key: <clientUuid>`  
Body: `{ "clientUuid", "categoryId", "categoryLabel", "body", "createdAtEpochMs", "includeBody": boolean }`  
If `includeBody` is false, server stores empty body (category + time only).  
Response: `{ "serverReportId", "clientUuid", "status": "stored" }`  
Duplicate `clientUuid` for same user → same `serverReportId` (no double insert).

### GET `/v1/reports/me`
Returns the caller’s reports only. Never returns another user’s reports.

### GET `/v1/reports/flags`
Roles: `NGO_STAFF`, `NGO_ADMIN`  
Returns AI-assisted urgency flags for synced reports: `serverReportId`, `categoryId`, `categoryLabel`, `createdAtEpochMs`, `weekLabel`, `urgency`, `explanation`, `classifierSource`, `disclaimer`.  
**Never** includes report body, user id, email, or phone. Classification is a priority aid, not a diagnosis.

## Analytics (anonymized)

### POST `/v1/analytics/events`
Body: `{ "events": [ { "eventType": "report"|"risk"|"education"|"sos", "categoryOrBand", "weekLabel", "count" } ] }`  
No names, phones, descriptions, or coordinates.  
Server stores for aggregation only.

### GET `/v1/analytics/summary`
Roles: `NGO_STAFF`, `NGO_ADMIN`  
Query: optional `county` (default `kajiado`)  
Response aggregates with **minimum threshold** (default 3): buckets below threshold are omitted or collapsed to `"suppressed"`.

## NGO admin

### GET `/v1/admin/staff` — `NGO_ADMIN`
### POST `/v1/admin/staff` — `NGO_ADMIN`  
Body: `{ "email", "password", "role": "NGO_STAFF"|"NGO_ADMIN" }`
### GET `/v1/admin/audit` — `NGO_ADMIN`

## Health

### GET `/v1/health` → `{ "status": "ok", "version": "1.0" }`

## USSD stub (not a live telecom service)

### POST `/v1/ussd/inbound`
Public adapter for a **future** licensed USSD gateway. Not a live Safaricom (or other) shortcode.  
Header: optional `X-Linda-Ussd-Key` — required only when `LINDA_USSD_GATEWAY_SECRET` is set.  
Body: `{ "sessionId", "msisdn", "text", "serviceCode" }`.  
Response: `{ "message", "endSession", "note", "channelStatus", "reportAccepted", "referenceId" }`.  
Never persists MSISDN. Options 4–6 queue an anonymized case for NGO staff.  
The Android app is not a USSD channel. See `docs/USSD_PATHWAY.md` and `docs/LOW_CONNECTIVITY.md`.
