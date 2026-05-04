# ==============================================================
# publish_docker_compose.ps1
# Publishes the docker-compose.yml as an OCI Artifact to DockerHub.
# Requirements:
#   - Docker CLI installed and logged in to DockerHub
#   - Docker with OCI artifact support (Docker 23+)
# ==============================================================

$ErrorActionPreference = "Stop"

$OCI_IMAGE = "anxelito/onetap-compose:latest"
$COMPOSE_FILE = "$PSScriptRoot\docker-compose.yml"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Publishing docker-compose as OCI Artifact" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $COMPOSE_FILE)) {
    Write-Error "docker-compose.yml not found at: $COMPOSE_FILE"
    exit 1
}

Write-Host "Pushing $COMPOSE_FILE as OCI artifact to $OCI_IMAGE ..." -ForegroundColor Yellow

docker buildx imagetools create `
    --annotation "index,manifest:org.opencontainers.image.title=onetap-compose" `
    --annotation "index,manifest:org.opencontainers.image.description=Docker Compose for OneTap eLeague (Practica 3 SSDD)" `
    -t $OCI_IMAGE `
    | Out-Null

# Use ORAS-compatible approach with docker save workaround
# Push as a config OCI artifact using docker manifest
Write-Host ""
Write-Host "Using 'docker config' OCI artifact approach..." -ForegroundColor Yellow

$tempDir = New-TemporaryFile | ForEach-Object { Remove-Item $_; New-Item -ItemType Directory -Path $_ }

Copy-Item $COMPOSE_FILE "$tempDir\docker-compose.yml"

Push-Location $tempDir
docker build --no-cache -t $OCI_IMAGE . -f - << EOF
FROM scratch
COPY docker-compose.yml /docker-compose.yml
EOF

docker push $OCI_IMAGE
Pop-Location
Remove-Item $tempDir -Recurse -Force

Write-Host ""
Write-Host "docker-compose published as OCI Artifact!" -ForegroundColor Green
Write-Host ""
Write-Host "To pull and run from DockerHub:" -ForegroundColor Cyan
Write-Host "  docker compose -f oci://$OCI_IMAGE up -d"
Write-Host ""
Write-Host "Or using the compose file directly:" -ForegroundColor Cyan
Write-Host "  docker run --rm $OCI_IMAGE cat /docker-compose.yml > docker-compose.yml"
Write-Host "  docker compose up -d"
