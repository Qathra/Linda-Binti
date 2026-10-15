# Linda Binti — REAL DEVICE TEST CHECKLIST (P6)

**Purpose:** Verify SOS / SMS / location readiness on a **physical Android phone**.  
**Do not mark “real-device testing complete” until each item is run on hardware.**

**Locked product rules (must never change):**
- `1102` + Equals → SOS SMS to trusted contacts, then protected Home.
- Separate Access Code + Equals → protected Home **without** SMS.
- Never claim SMS **delivered** unless the system DELIVERED callback confirmed it.

**Build under test:** versionName **1.8** (debug and/or release APK).  
**Device:** _________________  **Android version:** _______  **Date:** _______  **Tester:** _______

---

## Pre-conditions

- [ ] Fresh install or known-good setup completed (name, Access Code ≠ SOS, ≥1 trusted contact).
- [ ] Default SOS code left as `1102` (or note if changed).
- [ ] Access Code recorded separately (e.g. `1111`) — **not** equal to SOS.
- [ ] Test phone has a working SIM; note dual-SIM if applicable.
- [ ] Tester has a **second phone** to receive SOS SMS.

---

## Checklist

### 1. Normal calculator
- [ ] Launcher shows **Calculator** name/icon.
- [ ] Digits, operators, Equals, Clear behave like a calculator.
- [ ] No Linda Binti branding on the calculator screen.

**Result:** Pass / Fail — notes: _______________

### 2. Access Code
- [ ] Enter Access Code → Equals.
- [ ] Opens protected Home.
- [ ] **No** SOS SMS sent to contacts.
- [ ] Quick Hide returns to calculator.

**Result:** Pass / Fail — notes: _______________

### 3. 1102 SOS
- [ ] From calculator, enter `1102` → Equals.
- [ ] SOS path starts (Home shows SOS banner / “preparing”).
- [ ] Foreground notification may appear briefly as **Calculator / Working…** (discreet).

**Result:** Pass / Fail — notes: _______________

### 4. SMS sent
- [ ] Home banner moves to **queued** then **submitted** (SENT accepted by radio).
- [ ] Status does **not** say “delivered” yet.
- [ ] Recipient phone shows message arrived **or** still pending — note which.

**Result:** Pass / Fail — notes: _______________

### 5. SMS delivered
- [ ] Wait for network DELIVERED callback.
- [ ] Banner shows **delivery confirmed** only after that.
- [ ] If delivery never confirms, banner stays submitted / sent_unconfirmed — **not** falsely delivered.

**Result:** Pass / Fail — notes: _______________

### 6. Location available
- [ ] Grant location permission.
- [ ] Trigger SOS outdoors / with GPS on.
- [ ] SMS body includes maps link + coordinates when a fix is found.
- [ ] Home banner location note: location included.

**Result:** Pass / Fail — notes: _______________

### 7. Location unavailable
- [ ] Deny location **or** enable airplane GPS-off / indoor timeout.
- [ ] Trigger SOS.
- [ ] SMS still attempted; body says **Location unavailable** (no maps link).
- [ ] Banner notes location unavailable — SMS may still proceed.

**Result:** Pass / Fail — notes: _______________

### 8. SMS permission denied
- [ ] Deny SMS (or revoke in Settings).
- [ ] Emergency Assistance preflight shows SMS required.
- [ ] Calculator `1102` still starts service; Home shows **failed** (not fake success).
- [ ] Failed banner offers **Open Settings for permissions**.

**Result:** Pass / Fail — notes: _______________

### 9. Location permission denied
- [ ] Deny location; keep SMS allowed + ≥1 contact.
- [ ] SOS still sends SMS without location.
- [ ] Preflight warns location optional.

**Result:** Pass / Fail — notes: _______________

### 10. No airtime
- [ ] SIM with no credit / SMS blocked by carrier.
- [ ] Expect SENT failure or never-delivered — banner **failed** or stuck non-delivered.
- [ ] App must **not** claim delivered.

**Result:** Pass / Fail — notes: _______________

### 11. Dual SIM
- [ ] On dual-SIM device, note which SIM is **default for SMS**.
- [ ] Trigger SOS; confirm SMS leaves via that default SIM.
- [ ] UI note mentions default SMS SIM.

**Result:** Pass / Fail — SIM used: _______ notes: _______________

### 12. Quick Hide
- [ ] From Home / Emergency / Settings, tap Hide.
- [ ] Returns to calculator disguise immediately.
- [ ] Recent apps / screenshots of protected screens should be blanked where FLAG_SECURE applies.

**Result:** Pass / Fail — notes: _______________

### 13. Wrong Access Code / lockout
- [ ] Enter wrong Access Code → Equals repeatedly.
- [ ] After 5 failures, Access path locks ~60 seconds.
- [ ] SOS code `1102` still works during Access lockout (separate path).

**Result:** Pass / Fail — notes: _______________

### 14. Foreground notification
- [ ] During SOS, discreet **Calculator / Working…** notification appears (if notifications allowed).
- [ ] Notification does not expose “SOS” or Linda Binti in the title.
- [ ] Notification clears when work finishes (or after shortService timeout on Android 14+).

**Result:** Pass / Fail — notes: _______________

---

## Extra readiness checks (recommended)

- [ ] **Zero contacts:** remove all contacts (if allowed) or wipe — SOS records **failed**, not silent success.
- [ ] **Permanent SMS denial:** deny twice / Don’t ask again → Emergency offers **Open app settings** deep-link.
- [ ] **Multipart:** long alert with location still sends; statuses update honestly.
- [ ] **Release APK:** install release build; repeat items 2–5 at minimum.

---

## Sign-off

| Build | Device tested? | All 14 items run? | Tester signature |
|-------|----------------|-------------------|------------------|
| Debug | Yes / No | Yes / No | |
| Release | Yes / No | Yes / No | |

**Real-device testing complete only if:** physical phone used, and checklist signed above.  
Emulator-only runs are **not** sufficient for SMS DELIVERED / dual-SIM / airtime claims.
