$OCI_IMAGE = "anxelito/onetap-compose:latest"
$COMPOSE_FILE = "$PSScriptRoot\docker-compose.yml"

Write-Host "Publicando docker-compose en DockerHub..."

$tempDir = New-TemporaryFile | ForEach-Object { Remove-Item $_; New-Item -ItemType Directory -Path $_ }
Copy-Item $COMPOSE_FILE "$tempDir\docker-compose.yml"

Push-Location $tempDir
Set-Content -Path "Dockerfile" -Value "FROM scratch`nCOPY docker-compose.yml /docker-compose.yml"
docker build -t $OCI_IMAGE .

docker push $OCI_IMAGE
Pop-Location

Remove-Item $tempDir -Recurse -Force
Write-Host "Publicado correctamente en $OCI_IMAGE"
