# Set LINDA_AI_API_KEY in backend/.env and restart the backend.
# Usage:
#   cd "c:\Users\hp\Desktop\LINDA BINTI FOLDER"
#   powershell -ExecutionPolicy Bypass -File backend\scripts\configure-ai-key.ps1

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$envFile = Join-Path $repoRoot "backend\.env"
$example = Join-Path $repoRoot "backend\.env.example"

if (-not (Test-Path $envFile)) {
    if (Test-Path $example) {
        Copy-Item $example $envFile
        Write-Host "Created backend/.env from template."
    } else {
        throw "Missing backend/.env and .env.example"
    }
}

Write-Host ""
Write-Host "Linda Binti - Online AI setup"
Write-Host "Paste your API key (input is hidden). Press Enter when done."
Write-Host ""
$secure = Read-Host "LINDA_AI_API_KEY" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $key = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr).Trim()
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) | Out-Null
}

if ([string]::IsNullOrWhiteSpace($key)) {
    throw "API key cannot be empty."
}

$lines = Get-Content $envFile -Encoding UTF8
$updatedKey = $false
$updatedEnabled = $false
$newLines = foreach ($line in $lines) {
    if ($line -match '^\s*LINDA_AI_API_KEY\s*=') {
        $updatedKey = $true
        "LINDA_AI_API_KEY=$key"
    } elseif ($line -match '^\s*LINDA_AI_ENABLED\s*=') {
        $updatedEnabled = $true
        "LINDA_AI_ENABLED=1"
    } else {
        $line
    }
}
if (-not $updatedKey) {
    $newLines += "LINDA_AI_API_KEY=$key"
}
if (-not $updatedEnabled) {
    $newLines += "LINDA_AI_ENABLED=1"
}
Set-Content -Path $envFile -Value $newLines -Encoding UTF8

Write-Host ""
Write-Host "Saved API key to backend/.env (not printed)."
& (Join-Path $repoRoot "backend\scripts\restart-backend.ps1")
