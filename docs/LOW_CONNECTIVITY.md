# Low-connectivity architecture

Linda Binti is **offline-first on Android**. People at risk of GBV/FGM may not own a smartphone, may have limited literacy, or may have no internet. The phone app must still work. A future **USSD** channel is designed for feature phones; it is **not** a live nationwide service in this build.

## What works on the Android app without internet

| Capability | Notes |
|---|---|
| Education | Bundled GBV/FGM cards, quizzes, optional on-device clips |
| Safety guidance | Education + emergency tips on device |
| Survivor emotional support | Bundled messages and next steps (not counselling) |
| Risk awareness questions | Local scoring; stored on device |
| Local report storage | Room first; upload only later with consent |
| Trusted contacts | Room only; never sent to the backend |
| SOS SMS | Cellular SMS to trusted contacts — **not** internet messaging |
| Emergency guidance | Bundled 999 / 112 / 1195 / 116 |
| Support-service information | Static Kajiado/Kenya directory |

SOS SMS is **not** replaced by the backend. It still uses `SmsManager`.

## What each extra system is for

### Android permissions

- **SEND_SMS** — required to send SOS SMS.
- **ACCESS_FINE/COARSE_LOCATION** — optional map link in the SOS text; never blocks send.
- **POST_NOTIFICATIONS** — optional discreet SOS banner.
- **ACCESS_NETWORK_STATE / INTERNET** — optional cloud sync, health check, NGO dashboard. Not used by SOS.

Access PIN and SOS code never leave the device.

### Mobile network (not the same as internet)

- SOS SMS send/receive and delivery reports.
- Voice calls to 999 / 112 / 1195 / 116.

A phone can be “offline” for internet and still send SMS.

### Linda Binti backend (internet)

- Optional report sync (USER JWT + consent).
- AI risk flags and retry.
- Stakeholder dashboard.
- `POST /v1/ussd/inbound` for a **future** gateway.

### USSD provider / telecom integration

**Not configured as a live service.** A licensed operator or aggregator must:

1. Issue a shortcode (not invented in this repo).
2. Sign a commercial agreement.
3. Call `POST /v1/ussd/inbound` with optional `X-Linda-Ussd-Key` when `LINDA_USSD_GATEWAY_SECRET` is set.

There is **no Safaricom integration** unless that gateway is actually connected.

Planned flow:

Feature phone → USSD gateway → Linda Binti backend → anonymous GBV/FGM report or safety request → authorized NGO dashboard.

See `docs/USSD_PATHWAY.md`.
