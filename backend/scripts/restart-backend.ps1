# Restart Linda Binti backend (reads backend/.env on startup).
# Usage:
#   cd "c:\Users\hp\Desktop\LINDA BINTI FOLDER"
#   powershell -ExecutionPolicy Bypass -File backend\scripts\restart-backend.ps1

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$gradlew = Join-Path $repoRoot "gradlew.bat"

Write-Host "Stopping anything on port 8080..."
$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($conn) {
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

Write-Host "Starting backend..."
Set-Location $repoRoot
Start-Process -FilePath $gradlew -ArgumentList ":backend:run" -WorkingDirectory $repoRoot -WindowStyle Minimized

Write-Host "Waiting for backend (Gradle can take 30-90 seconds on first start)..."
$ok = $false
for ($i = 0; $i -lt 45; $i++) {
    Start-Sleep -Seconds 2
    $seconds = ($i + 1) * 2
    Write-Host ("  ... still starting (" + $seconds + "s)")
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:8080/v1/health" -TimeoutSec 3
        if ($health.status -eq "ok") {
            $ok = $true
            Write-Host ""
            Write-Host "Backend is up."
            Write-Host ("AI status: " + $health.onlineAiStatus)
            Write-Host "Dashboard: http://127.0.0.1:8080/dashboard/"
            Write-Host "Health:    http://127.0.0.1:8080/v1/health"
            break
        }
    } catch {
        # still starting
    }
}

if (-not $ok) {
    Write-Host "Backend is still starting. Open http://127.0.0.1:8080/v1/health in your browser."
}

Write-Host ""
Write-Host "On your phone:"
Write-Host "  1. Settings - Register or Sign in (USER account)"
Write-Host "  2. Turn ON Cloud AI consent"
Write-Host "  3. Open AI Safety Assistant and ask a question"
Write-Host ""
