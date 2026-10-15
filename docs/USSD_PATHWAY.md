# USSD / feature-phone pathway (production)

Linda Binti’s **Android app is not USSD**. Feature phones use a licensed gateway (Africa’s Talking / MNO shortcode) that calls this backend.

## Production callback (Africa’s Talking)

1. Set host secret `LINDA_USSD_GATEWAY_SECRET` (Render/VPS).
2. In Africa’s Talking → USSD → your channel → **Callback URL**:

```
https://lindabinti.com/v1/ussd/africas-talking?key=<LINDA_USSD_GATEWAY_SECRET>
```

3. Dial the shortcode AT assigns you (not invented in this repo).
4. Confirm `GET https://lindabinti.com/v1/health` shows `"ussd":"gateway_key_configured"`.

### Request (AT → Linda Binti)

`POST /v1/ussd/africas-talking`  
`Content-Type: application/x-www-form-urlencoded`

Typical fields: `sessionId`, `phoneNumber`, `serviceCode`, `text`.

Auth (any one):

- Query `?key=` matching `LINDA_USSD_GATEWAY_SECRET`
- Header `X-Linda-Ussd-Key: <secret>`
- Optional alias env `LINDA_USSD_AT_API_KEY` (used if gateway secret empty)

**Raw MSISDN is never stored.**

### Response (Linda Binti → AT)

Plain text:

- `CON …` — continue session (menu)
- `END …` — close session

## Provider-neutral JSON (tests / other gateways)

`POST /v1/ussd/inbound`  
Header: `X-Linda-Ussd-Key: <secret>` (required when secret is set)

```json
{
  "sessionId": "gateway-session-id",
  "msisdn": "",
  "text": "",
  "serviceCode": ""
}
```

`text` uses path format (`""`, `"1"`, `"1*1*Jane"`).

## Menu (Safaricom-style)

1. Report GBV  
2. Report FGM  
3. Emergency numbers (999 / 112 / 1195 / 116)  
0. Exit  

After 1 or 2:

1. Kajiado Central  
2. Kajiado East  
3. Kajiado North  
4. Kajiado West  
5. Kajiado South  

Then enter your name and send. The NGO spatial map flags that sub-county automatically.

Local demo page: `http://YOUR-PC:8080/ussd/`

Reports appear on the NGO dashboard priority queue and map. **Raw MSISDN is never stored.**

## Stub vs live

| `LINDA_USSD_GATEWAY_SECRET` | Behavior |
|-----------------------------|----------|
| Empty | Dev stub — inbound allowed without key; health `ussd=stub` |
| Set | Live — missing/wrong key → **401**; health `ussd=gateway_key_configured` |

SOS SMS in the Android app remains separate and does **not** require internet. See `docs/LOW_CONNECTIVITY.md` and `docs/PRODUCTION_GO_LIVE.md`.
