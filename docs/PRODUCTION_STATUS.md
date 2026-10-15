# Linda Binti — Production status

Student production service for GBV/FGM prevention and response in Kajiado County.

## Live when hosted

| Capability | Status |
|------------|--------|
| HTTPS API + NGO dashboard | Production-ready (`https://lindabinti.com` after DNS + Render/VPS) |
| Android release APK | Points at `https://lindabinti.com/` |
| Calculator dual-code SOS + Access | Live on device (SMS, no internet required) |
| Optional escalation SMS | Settings → NGO/helpline number |
| Offline AI FAQ | Always on device |
| Online AI | When `LINDA_AI_API_KEY` set on server + user consent |
| Report sync + SOS CRITICAL flags | When USER logged in + network |
| USSD (Africa’s Talking) | Live when `LINDA_USSD_GATEWAY_SECRET` set + AT callback configured |
| Education CMS | Dashboard publish; `LINDA_DEMO_SEED=0` skips demo auto-seed |

## Partner-activated (you / NGO / AT)

- Domain DNS → host
- Africa’s Talking shortcode + callback URL
- Real education MP4/YouTube URLs in CMS
- Sideload or Play distribution to partners

## Never claimed

- Official county government CAD / police dispatch system
- Replacement for counselling, clinical care, courts, or police
- Therapist / lawyer AI

See [PRODUCTION_GO_LIVE.md](PRODUCTION_GO_LIVE.md), [USSD_PATHWAY.md](USSD_PATHWAY.md), [DELIMITATIONS.md](DELIMITATIONS.md).
