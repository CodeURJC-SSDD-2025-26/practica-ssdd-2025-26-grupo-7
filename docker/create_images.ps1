# ==============================================================
# create_images.ps1
# Builds Docker images for app-service and utility-service.
# Only requirement: Docker installed on host (no JDK/Maven needed).
# ==============================================================

param(
    [string]$Tag = "latest"
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  OneTap eLeague - Docker Image Builder" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ── Build app-service ──────────────────────────────────────
Write-Host "[1/2] Building app-service image..." -ForegroundColor Yellow
docker build `
    -f "$PSScriptRoot\app-service.Dockerfile" `
    -t "anxelito/onetap-app-service:$Tag" `
    "$RootDir"

if ($LASTEXITCODE -ne 0) {
    Write-Error "ERROR: Failed to build app-service image."
    exit 1
}
Write-Host "  app-service image built successfully." -ForegroundColor Green

# ── Build utility-service ──────────────────────────────────
Write-Host ""
Write-Host "[2/2] Building utility-service image..." -ForegroundColor Yellow
docker build `
    -f "$PSScriptRoot\utility-service.Dockerfile" `
    -t "anxelito/onetap-utility-service:$Tag" `
    "$RootDir"

if ($LASTEXITCODE -ne 0) {
    Write-Error "ERROR: Failed to build utility-service image."
    exit 1
}
Write-Host "  utility-service image built successfully." -ForegroundColor Green

Write-Host ""
Write-Host "All images built successfully!" -ForegroundColor Green
Write-Host "  - anxelito/onetap-app-service:$Tag"
Write-Host "  - anxelito/onetap-utility-service:$Tag"
