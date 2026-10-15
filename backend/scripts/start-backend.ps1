# Start Linda Binti backend with optional online AI from backend/.env
# Usage (from repo root OR backend folder):
#   powershell -ExecutionPolicy Bypass -File backend/scripts/start-backend.ps1

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$backendDir = Join-Path $repoRoot "backend"
$envFile = Join-Path $backendDir ".env"
$example = Join-Path $backendDir ".env.example"

Set-Location $backendDir

if (-not (Test-Path $envFile)) {
  Write-Host "No backend/.env found."
  Write-Host "Copy .env.example to .env and set LINDA_AI_API_KEY for online AI."
  Write-Host "Offline rules AI and the web dashboard still work without a key."
  if (Test-Path $example) {
    Copy-Item $example $envFile
    Write-Host "Created backend/.env from .env.example - edit the API key, then re-run."
  }
}

Write-Host "Dashboard (after start): http://127.0.0.1:8080/dashboard/"
Write-Host "Health: http://127.0.0.1:8080/v1/health"
Write-Host "Starting backend..."

& (Join-Path $repoRoot "gradlew.bat") ":backend:run"
