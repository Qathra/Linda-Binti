# Delimitations (Proposal §1.6.2) — how they are handled in production

These are **boundaries of the study / ethics**, not missing features. Production hosting and USSD do **not** remove them.

## 1. GBV and FGM only (not all crime)

| Delimitation | How Linda Binti respects it |
|--------------|-----------------------------|
| No other crimes as the product focus | Reporting categories are GBV/FGM only; AI knowledge is GBV/FGM/safety; SOS is a personal safety alert to trusted contacts (+ optional escalation), not a county police CAD system |

**Report wording:**  
*“The system is limited to GBV and FGM prevention and response in Kajiado County and does not address other forms of crime.”*

## 2. Complements services — does not replace them

| Delimitation | How it is handled |
|--------------|-------------------|
| Not a substitute for counselling, healthcare, police, or courts | Emergency screen prioritises 999/112/1195/116; AI and education give guidance; About states the system **complements** professional services; USSD menu repeats the same |

**Report wording:**  
*“Linda Binti facilitates discreet reporting, SOS, USSD flags, and AI guidance; it does not replace professional counselling, healthcare, law enforcement, or legal intervention.”*

## 3. Dependency on device, GPS, and connectivity

| Delimitation | How it is handled |
|--------------|-------------------|
| GPS / some cloud features need device capability | SOS SMS works on cellular without internet; map link only when Location permission + phone Location ON + fix; cloud AI/sync need network + login; USSD needs the gateway and shortcode |

**Report wording:**  
*“GPS sharing and cloud features depend on device permissions, GPS availability, and connectivity; core SOS SMS and offline guidance remain available without internet.”*

## 4. Deployment scope

| Delimitation (proposal) | Production interpretation |
|-------------------------|---------------------------|
| Time/resource limit → prototype | The codebase is now a **deployable production service** (HTTPS API, dashboard, release APK, Africa’s Talking USSD). County-wide adoption still requires partner distribution, shortcode activation, and organisational buy-in — it is not an automatic government mandate. |

**Report wording:**  
*“The study delivered a production-ready Android + Ktor service that can be hosted for Kajiado partners; official county-wide adoption remains a partnership and policy step beyond the student deliverable.”*

## What you should NOT claim in the viva

- That AI is a therapist or lawyer  
- That SOS automatically dispatches police (it SMS trusted contacts / optional escalation; users should still dial 999)  
- That Flutter/Firebase were used if you present this Kotlin/Ktor build  
- That a Safaricom shortcode exists before Africa’s Talking assigns one  

## Checklist for your final report Chapter 1 / 5

- [ ] Copy §1.6.2 delimitations (ethics) unchanged or lightly polished  
- [ ] In Implementation, note complement-not-replace, GPS limits, and go-live hosting/USSD  
- [ ] In Limitations, repeat GPS/SMS dependency and partner shortcode/distribution  
- [ ] In Recommendations, suggest county gender desk / Anti-FGM Board / NGO rollout  

Ops: [PRODUCTION_GO_LIVE.md](PRODUCTION_GO_LIVE.md) · [PRODUCTION_STATUS.md](PRODUCTION_STATUS.md)
