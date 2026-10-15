# LINDA BINTI — go live at https://lindabinti.com
# Run from repo root after you own the domain and have Render/VPS + USSD credentials.

$ErrorActionPreference = "Continue"
$BaseUrl = if ($env:LINDA_PUBLIC_URL) { $env:LINDA_PUBLIC_URL.TrimEnd('/') } else { "https://lindabinti.com" }

Write-Host "=== LINDA BINTI production checklist ===" -ForegroundColor Cyan
Write-Host ""

Write-Host "ENV CHECKLIST (set on host — do not commit)" -ForegroundColor Yellow
$required = @(
    "LINDA_JWT_SECRET",
    "LINDA_USSD_GATEWAY_SECRET",
    "LINDA_AI_API_KEY",
    "LINDA_PUBLIC_URL",
    "LINDA_DEMO_SEED"
)
foreach ($k in $required) {
    $v = [Environment]::GetEnvironmentVariable($k)
    if ([string]::IsNullOrWhiteSpace($v)) {
        Write-Host "   [ ] $k  (missing locally — set on Render/VPS)" -ForegroundColor DarkYellow
    } else {
        Write-Host "   [x] $k  (present in this shell)" -ForegroundColor Green
    }
}
Write-Host "   Production expects LINDA_DEMO_SEED=0 and LINDA_ENV=production" -ForegroundColor Gray
Write-Host ""

Write-Host "1. DOMAIN" -ForegroundColor Yellow
Write-Host "   Register/own lindabinti.com (or override linda.production.api.url for the release APK)."
Write-Host ""

Write-Host "2. DEPLOY BACKEND" -ForegroundColor Yellow
Write-Host "   A) Render.com (recommended)"
Write-Host "      - Push repo to GitHub → New → Blueprint → render.yaml"
Write-Host "      - Dashboard secrets: LINDA_AI_API_KEY, LINDA_USSD_GATEWAY_SECRET"
Write-Host "      - Custom Domains: lindabinti.com + www"
Write-Host ""
Write-Host "   B) VPS + Docker"
Write-Host "      docker build -f deploy/Dockerfile -t lindabinti ."
Write-Host "      docker run -d -p 8080:8080 -v lindabinti-data:/var/data \"
Write-Host "        -e LINDA_PUBLIC_URL=https://lindabinti.com \"
Write-Host "        -e LINDA_ENV=production -e LINDA_DEMO_SEED=0 \"
Write-Host "        -e LINDA_JWT_SECRET=<long-random-secret> \"
Write-Host "        -e LINDA_USSD_GATEWAY_SECRET=<ussd-secret> \"
Write-Host "        -e LINDA_AI_API_KEY=<key> \"
Write-Host "        lindabinti"
Write-Host "      Put Caddy/Nginx in front for HTTPS :443 → :8080."
Write-Host ""

Write-Host "3. DNS" -ForegroundColor Yellow
Write-Host "   lindabinti.com → A/ALIAS to Render (or VPS)"
Write-Host "   www → CNAME to Render hostname"
Write-Host ""

Write-Host "4. AFRICA'S TALKING USSD" -ForegroundColor Yellow
Write-Host "   Callback URL:"
Write-Host "   $BaseUrl/v1/ussd/africas-talking?key=<LINDA_USSD_GATEWAY_SECRET>"
Write-Host "   (Or send header X-Linda-Ussd-Key with the same secret.)"
Write-Host "   JSON alternative: POST $BaseUrl/v1/ussd/inbound"
Write-Host ""

Write-Host "5. VERIFY (live HTTP)" -ForegroundColor Yellow
function Test-LindaUrl([string]$Path, [string]$ExpectSubstring) {
    $url = "$BaseUrl$Path"
    try {
        $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 20
        $ok = $resp.StatusCode -ge 200 -and $resp.StatusCode -lt 400
        $match = [string]::IsNullOrEmpty($ExpectSubstring) -or ($resp.Content -match [regex]::Escape($ExpectSubstring))
        if ($ok -and $match) {
            Write-Host "   OK  $url" -ForegroundColor Green
            return $true
        }
        Write-Host "   FAIL $url (status=$($resp.StatusCode))" -ForegroundColor Red
        return $false
    } catch {
        Write-Host "   FAIL $url — $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}
$null = Test-LindaUrl "/v1/health" "ok"
$null = Test-LindaUrl "/dashboard/" ""
Write-Host "   Manual: confirm health JSON ussd=gateway_key_configured after setting the USSD secret."
Write-Host ""

Write-Host "6. ANDROID RELEASE" -ForegroundColor Yellow
Write-Host "   Release APK uses https://lindabinti.com/ by default."
Write-Host "   Build: .\gradlew.bat :app:assembleRelease"
Write-Host "   APK: app\build\outputs\apk\release\app-release.apk"
Write-Host "   Phone Location toggle must be ON for SOS GPS."
Write-Host ""

Write-Host "7. ADMIN + CONTENT" -ForegroundColor Yellow
Write-Host "   Login: admin@lindabinti.com (seeded on first deploy)."
Write-Host "   Publish education videos via dashboard → Education CMS."
Write-Host "   See docs/PRODUCTION_GO_LIVE.md and docs/HOW_TO_ADD_VIDEOS.md"
Write-Host ""
Write-Host "Done. Full guide: docs/PRODUCTION_GO_LIVE.md" -ForegroundColor Cyan
