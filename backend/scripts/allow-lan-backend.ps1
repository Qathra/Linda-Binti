# Allow phone to reach Linda Binti backend on port 8080 (run once as Administrator).
# Usage:
#   powershell -ExecutionPolicy Bypass -File backend\scripts\allow-lan-backend.ps1

$ruleName = "Linda Binti Backend 8080"
$existing = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "Firewall rule already exists: $ruleName"
} else {
    New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 | Out-Null
    Write-Host "Created firewall rule: $ruleName"
}
Write-Host "Phones on the same Wi-Fi can now reach http://192.168.100.44:8080"
