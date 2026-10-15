# Add videos (production CMS)

YouTube links open outside the app (**Watch**). For **Play** / **Download** inside Linda Binti you need a direct **`.mp4`** (HTTPS) URL.

Production dashboard: **https://lindabinti.com/dashboard/** → **Education**  
(Local: `http://127.0.0.1:8080/dashboard/`)

With `LINDA_DEMO_SEED=0`, the server does **not** auto-seed demo catalog rows — publish your own.

---

## Production (recommended)

1. Upload an NGO-approved `.mp4` to HTTPS storage you control  
   (or put it on the server under `backend/data/education_videos/` and serve via `/media/education/filename.mp4`).
2. Sign in as NGO admin on the dashboard → **Education**.
3. Create item:
   - Type: **video**
   - Category: `what_is_gbv` / `what_is_fgm` / `how_to_get_help` / `safety_planning`
   - URL: full `https://…/file.mp4` (or `https://lindabinti.com/media/education/file.mp4`)
4. Save → Active.
5. On the phone (release build): Education & Safety → Videos → Play / Download.

YouTube / Vimeo: set URL to the watch link; users tap **Watch** (no offline download).

---

## Local PC host (dev only)

```
backend/data/education_videos/fgm-kajiado.mp4
```

```powershell
.\gradlew :backend:run
```

Test: `http://127.0.0.1:8080/media/education/fgm-kajiado.mp4`  
Dashboard URL for LAN phone: `http://YOUR_PC_IP:8080/media/education/fgm-kajiado.mp4`

Phone and PC must share Wi‑Fi.

---

## Rights

Use only media you or your NGO partners have rights to publish. Do not pirate content.

See [PRODUCTION_GO_LIVE.md](PRODUCTION_GO_LIVE.md).
