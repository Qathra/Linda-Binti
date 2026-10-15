# Linda Binti — Production Backend Phase 1

## Architecture selected: HTTPS REST API (Ktor) + offline-first Android client

**Not Firebase.** Reasons:

1. **Sensitive GBV data** needs explicit server-side authorization, audit logging, and data minimization that are clearer in a first-party API than in Firebase Security Rules alone.
2. **Dual-code secrecy** (Access PIN / SOS) must never leave the device; a custom auth model for cloud accounts stays separate from calculator codes.
3. **NGO roles** (`USER`, `NGO_STAFF`, `NGO_ADMIN`) require least-privilege endpoints and aggregation thresholds that a REST contract documents cleanly.
4. **Offline-first sync** (local → queue → backend) maps naturally to idempotent REST with `clientUuid` deduplication.
5. Prior FYP builds were honest about being local-first; Firebase would imply a different product claim without solving NGO authorization better than a controlled API.

## Trust boundaries

| Data | Device | Backend |
|------|--------|---------|
| Access PIN / SOS code (hashes) | Yes (EncryptedSharedPreferences) | **Never** |
| Trusted contacts | Yes | **Never** (Phase 1) |
| Private report body | Yes | Only if user consents to report sync; readable only by owning `USER` |
| Anonymized aggregates | Optional local prep | NGO roles only; min threshold |
| NGO staff credentials | Session JWT in encrypted prefs | Hashed passwords (BCrypt) |

## Roles

- `USER` — own reports + submit anonymized analytics (if consented)
- `NGO_STAFF` — aggregate dashboard only (no raw report bodies)
- `NGO_ADMIN` — aggregates + list/create staff + read audit log

## Sync states (reports)

`local` → `pending` → `synced` | `failed` (retryable)

## Live status

The backend is **implemented in-repo** but **not live** until you run the server and set `BACKEND_BASE_URL` / TLS. See final report section “What you need to do manually.”
