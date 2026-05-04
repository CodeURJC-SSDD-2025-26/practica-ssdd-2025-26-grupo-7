# ==============================================================
# publish_images.ps1
# Builds and pushes Docker images to DockerHub.
# Requirement: Docker installed and logged in to DockerHub.
#   Run: docker login  (before executing this script)
# ==============================================================

param(
    [string]$Tag = "latest"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " OneTap eLeague - DockerHub Publisher  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ── Step 1: Build images ───────────────────────────────────
Write-Host "Building images first..." -ForegroundColor Yellow
& "$PSScriptRoot\create_images.ps1" -Tag $Tag

# ── Step 2: Push app-service ───────────────────────────────
Write-Host ""
Write-Host "[1/2] Pushing app-service to DockerHub..." -ForegroundColor Yellow
docker push "anxelito/onetap-app-service:$Tag"

if ($LASTEXITCODE -ne 0) {
    Write-Error "ERROR: Failed to push app-service image. Are you logged in to DockerHub?"
    exit 1
}
Write-Host "  app-service pushed successfully." -ForegroundColor Green

# ── Step 3: Push utility-service ───────────────────────────
Write-Host ""
Write-Host "[2/2] Pushing utility-service to DockerHub..." -ForegroundColor Yellow
docker push "anxelito/onetap-utility-service:$Tag"

if ($LASTEXITCODE -ne 0) {
    Write-Error "ERROR: Failed to push utility-service image."
    exit 1
}
Write-Host "  utility-service pushed successfully." -ForegroundColor Green

Write-Host ""
Write-Host "All images published to DockerHub!" -ForegroundColor Green
Write-Host "  https://hub.docker.com/r/anxelito/onetap-app-service"
Write-Host "  https://hub.docker.com/r/anxelito/onetap-utility-service"
