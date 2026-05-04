param([string]$Tag = "latest")

$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot

Write-Host "Generating Docker images for OneTap..."

# create app-service
Write-Host "> Creating app-service image..."
docker build -f "$PSScriptRoot\app-service.Dockerfile" -t "anxelito/onetap-app-service:$Tag" "$RootDir"

# create utility-service
Write-Host "> Creating utility-service image..."
docker build -f "$PSScriptRoot\utility-service.Dockerfile" -t "anxelito/onetap-utility-service:$Tag" "$RootDir"

Write-Host "Images created successfully"
