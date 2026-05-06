param([string]$Tag = "latest")

$ErrorActionPreference = "Stop"

Write-Host "Subiendo imagenes a DockerHub..."

& "$PSScriptRoot\create_images.ps1" -Tag $Tag

Write-Host "> Subiendo app-service..."
docker push "anxelito/onetap-app-service:$Tag"

Write-Host "> Subiendo utility-service..."
docker push "anxelito/onetap-utility-service:$Tag"

Write-Host "Todo subido correctamente"
